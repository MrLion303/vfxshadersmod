package com.mrlion303.vfxshadersmod.client.photon;

import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXEffect;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction3;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.Circle;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.Sphere;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleConfig;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleEmitter;
import com.mrlion303.vfxshadersmod.client.effect.GroundAuroraEffect;
import com.mrlion303.vfxshadersmod.client.effect.TerrainScanEffect;
import com.mrlion303.vfxshadersmod.client.effect.TrampolineWaveEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Puente del mod hacia el runtime real de Photon.
 *
 * Los presets se construyen con los emisores y las formas de Photon; no dibujan
 * partículas mediante el renderer VFX propio. Las mallas y shaders especializados
 * del mod pueden seguir complementándolos cuando el efecto lo requiere.
 */
public final class PhotonEffectBridge {
    private PhotonEffectBridge() {
    }

    public enum Preset {
        ORBIT,
        HORIZON,
        RAINBOW,
        FALLING_STARS,
        STARSSPEED,
        GROUND_WAVE,
        TERRAIN_SCAN,
        VORTEX,
        PORTAL,
        BLACK_HOLE
    }

    /**
     * Crea el sistema Photon apropiado para una de las definiciones del mod.
     * Las coordenadas especiales se toman de la misma configuración que usa el VFX.
     */
    /**
     * Efectos cuya representación visual se ejecuta íntegramente con el runtime de Photon.
     * No deben dibujarse además con los renderizadores heredados del mod.
     */
    public static boolean usesPhotonAsPrimaryRenderer(String effectPath) {
        return switch (effectPath) {
            case "demo_orbit", "falling_stars" -> true;
            default -> false;
        };
    }

    public static Handle playForEffect(String effectPath, Level level) {
        Vec3 anchor = anchorFor(effectPath, level);
        return switch (effectPath) {
            case "blackhole" -> Handle.empty();
            case "demo_orbit" -> play(level, anchor, Preset.ORBIT, 0, 1.0F);
            case "music_horizon_bars" -> play(level, anchor, Preset.HORIZON, 0xFF55DFFF, 2.4F);
            case "rainbow_aurora" -> play(level, anchor, Preset.RAINBOW, 0, 2.8F);
            case "rainbow_sky" -> play(level, anchor, Preset.RAINBOW, 0, 4.0F);
            case "blackhole_sky" -> play(level, anchor, Preset.BLACK_HOLE, 0, 3.0F);
            case "cloud_vortex" -> play(level, anchor, Preset.VORTEX, 0, 3.0F);
            case "starsspeed" -> play(level, anchor, Preset.STARSSPEED, 0xFF8ABEFF, 2.0F);
            case "ground_aurora" -> play(level, anchor, Preset.GROUND_WAVE, GroundAuroraEffect.configuredColor(), 2.4F);
            case "falling_stars" -> play(level, anchor, Preset.FALLING_STARS, 0, 1.7F);
            case "scan" -> play(level, anchor, Preset.TERRAIN_SCAN, TerrainScanEffect.configuredColor(), 1.6F);
            case "trampolin" -> play(level, anchor, Preset.GROUND_WAVE, 0xFF58DFFF, (float) (TrampolineWaveEffect.configuredRadius() / 14.0D));
            case "sky_portal" -> play(level, anchor, Preset.PORTAL, 0, 4.0F);
            default -> play(level, anchor, Preset.ORBIT, 0, 1.0F);
        };
    }

    /** Actualiza el ancla de los sistemas ambientales que deben acompañar al jugador. */
    public static void updateForEffect(String effectPath, Handle handle, Level level) {
        if (handle != null && followsPlayer(effectPath)) {
            handle.moveTo(anchorFor(effectPath, level));
        }
    }

    private static boolean followsPlayer(String effectPath) {
        return switch (effectPath) {
            case "demo_orbit", "music_horizon_bars", "rainbow_aurora", "rainbow_sky",
                    "blackhole_sky", "cloud_vortex", "starsspeed", "falling_stars", "sky_portal" -> true;
            default -> false;
        };
    }

    private static Vec3 anchorFor(String effectPath, Level level) {
        Player player = Minecraft.getInstance().player;
        Vec3 base = player == null ? Vec3.ZERO : player.position().add(0.0D, 1.0D, 0.0D);
        return switch (effectPath) {
            case "ground_aurora" -> GroundAuroraEffect.configuredOrigin();
            case "scan" -> TerrainScanEffect.configuredOrigin();
            case "trampolin" -> TrampolineWaveEffect.configuredOrigin();
            case "blackhole_sky" -> base.add(0.0D, 26.0D, -65.0D);
            case "cloud_vortex" -> base.add(0.0D, 35.0D, -72.0D);
            case "sky_portal" -> {
                if (player == null) yield base;
                Vec3 look = player.getLookAngle().normalize();
                yield base.add(look.scale(65.0D)).add(0.0D, 18.0D, 0.0D);
            }
            case "rainbow_sky" -> base.add(0.0D, 12.0D, 0.0D);
            default -> base;
        };
    }

    public static Handle play(Level level, Vec3 position, Preset preset, int color, float scale) {
        if (level == null || !level.isClientSide()) {
            return Handle.empty();
        }

        FX fx = new FX();
        float safeScale = Math.max(0.1F, Math.min(20.0F, scale));
        int warm = color == 0 ? 0xFFFF7A24 : color;
        switch (preset) {
            case ORBIT -> {
                addRing(fx, "Photon Orbit Outer", 1.8F * safeScale, 1.0F, 1.2F, 70, 0.06F, 0.13F, 0xFFB16BFF, 0);
                addRing(fx, "Photon Orbit Inner", 1.1F * safeScale, 1.0F, 1.8F, 48, 0.08F, 0.10F, 0xFF4FE6FF, 1);
                addRing(fx, "Photon Orbit Accent", 0.65F * safeScale, 1.0F, 0.9F, 40, 0.10F, 0.08F, 0xFFFF65B8, 2);
            }
            case HORIZON -> {
                addRing(fx, "Photon Horizon Cyan", 12.0F * safeScale, 0.0F, 3.0F, 100, 0.06F, 0.12F, 0xFF55DFFF);
                addRing(fx, "Photon Horizon Violet", 16.0F * safeScale, 2.0F, 2.0F, 90, 0.05F, 0.16F, 0xFF966CFF);
            }
            case RAINBOW -> {
                int[] colors = {0xFFFF4545, 0xFFFFA43A, 0xFFFFFF4A, 0xFF56FF76, 0xFF48E6FF, 0xFF6575FF, 0xFFE86BFF};
                for (int i = 0; i < colors.length; i++) {
                    addRing(fx, "Photon Rainbow " + i, (8.0F + i * 1.45F) * safeScale, i * 0.9F,
                            1.5F, 72, 0.055F, 0.10F, colors[i]);
                }
            }
            case FALLING_STARS -> {
                if (color == 0) {
                    int[] rainbow = {0xFFFF5555, 0xFFFFA347, 0xFFFFFF55, 0xFF67FF77,
                            0xFF4FEAFF, 0xFF6680FF, 0xFFE874FF};
                    for (int i = 0; i < rainbow.length; i++) {
                        addFallingStarEmitter(fx, safeScale, rainbow[i],
                                12.0F / rainbow.length, i * 0.025F);
                    }
                } else {
                    addFallingStarEmitter(fx, safeScale, color, 12.0F, 0.0F);
                }
            }
            case STARSSPEED -> addStarsspeedEmitter(fx, safeScale, color);
            case GROUND_WAVE -> {
                addRing(fx, "Photon Ground Wave Bright", 2.8F * safeScale, 0.10F, 4.0F, 55, 0.32F, 0.18F, color);
                addRing(fx, "Photon Ground Wave Halo", 4.0F * safeScale, 0.18F, 3.0F, 65, 0.25F, 0.24F, tint(color, 0.55F));
                addRing(fx, "Photon Ground Wave Sparks", 1.8F * safeScale, 0.28F, 2.0F, 42, 0.42F, 0.11F, 0xFFE9FFFF);
            }
            case TERRAIN_SCAN -> {
                addRing(fx, "Photon Scan Outer", 2.5F * safeScale, 0.02F, 7.0F, 36, 0.75F, 0.08F, color);
                addRing(fx, "Photon Scan Core", 1.5F * safeScale, 0.04F, 4.5F, 28, 0.95F, 0.12F, 0xFFFFFFFF);
            }
            case VORTEX -> {
                addRing(fx, "Photon Vortex Outer", 7.0F * safeScale, -1.5F, 4.0F, 82, 0.10F, 0.20F, 0xFF9A1618);
                addRing(fx, "Photon Vortex Mid", 4.6F * safeScale, 0.8F, 4.5F, 90, 0.13F, 0.16F, 0xFFFF4A0A);
                addRing(fx, "Photon Vortex Inner", 2.4F * safeScale, 1.7F, 3.0F, 80, 0.09F, 0.13F, 0xFFFFD49A);
            }
            case PORTAL -> {
                addRing(fx, "Photon Portal Rim", 3.2F * safeScale, 0.0F, 5.0F, 100, 0.07F, 0.13F, 0xFF55DFFF, 1);
                addRing(fx, "Photon Portal Inner", 2.5F * safeScale, 0.0F, 3.5F, 78, 0.11F, 0.09F, 0xFF9EEFFF, 1);
                addRing(fx, "Photon Portal Shards", 4.0F * safeScale, 0.0F, 2.0F, 44, 0.45F, 0.055F, 0xFFFFFFFF, 1);
            }
            case BLACK_HOLE -> {
                addRing(fx, "Photon Accretion Outer", 3.8F * safeScale, 0.0F, 4.0F, 95, 0.08F, 0.16F, 0xFFFFA34A);
                addRing(fx, "Photon Accretion Inner", 2.0F * safeScale, 0.0F, 3.0F, 85, 0.04F, 0.11F, 0xFFFFF1CE);
            }
        }

        PhotonInstance instance = new PhotonInstance(fx, level, position);
        instance.start();
        return new Handle(instance);
    }

    private static void addRing(FX fx, String name, float radius, float y, float rate,
                                int lifetime, float speed, float size, int color) {
        addRing(fx, name, radius, y, rate, lifetime, speed, size, color, 0);
    }

    /**
     * Crea un anillo con una orientación 3D y movimiento orbital calculado por Photon.
     * plane 0 = XZ, 1 = XY, 2 = YZ.
     */
    private static void addRing(FX fx, String name, float radius, float y, float rate,
                                int lifetime, float speed, float size, int color, int plane) {
        ParticleEmitter emitter = new ParticleEmitter();
        emitter.setName(name);
        ParticleConfig config = emitter.config;
        config.setDuration(120);
        config.setLooping(true);
        config.setStartLifetime(NumberFunction.constant(lifetime));
        config.setStartSpeed(NumberFunction.constant(speed));
        config.setStartSize(new NumberFunction3(size, size, size));
        config.setStartColor(NumberFunction.color(color));
        config.setMaxParticles(1200);
        config.emission.setEmissionRate(NumberFunction.constant(rate));

        Circle circle = new Circle();
        circle.setRadius(1.0F);
        circle.setRadiusThickness(0.88F);
        config.shape.setShape(circle);
        config.shape.setPosition(new NumberFunction3(0.0F, y, 0.0F));
        config.shape.setRotation(new NumberFunction3(
                plane == 1 ? 90.0F : 0.0F, 0.0F, plane == 2 ? 90.0F : 0.0F));
        config.shape.setScale(new NumberFunction3(radius, 1.0F, radius));
        config.velocityOverLifetime.setEnable(true);
        config.velocityOverLifetime.setOrbitalMode(
                com.lowdragmc.photon.client.gameobject.emitter.data.VelocityOverLifetimeSetting.OrbitalMode.AngularVelocity);
        config.velocityOverLifetime.setOrbital(switch (plane) {
            case 1 -> new NumberFunction3(0.0F, 0.0F, 0.14F);
            case 2 -> new NumberFunction3(0.14F, 0.0F, 0.0F);
            default -> new NumberFunction3(0.0F, 0.14F, 0.0F);
        });
        config.renderer.setBloomEffect(true);
        config.renderer.setBloomColor(color);
        config.physics.setEnable(false);
        fx.getMainFX().objects().add(emitter);
    }

    /** Emisor nativo de Photon para estrellas descendentes con estelas persistentes. */
    private static void addFallingStarEmitter(FX fx, float scale, int color, float rate, float layerOffset) {
        ParticleEmitter emitter = new ParticleEmitter();
        emitter.setName("Photon Falling Stars and Trails");
        ParticleConfig config = emitter.config;
        config.setDuration(120);
        config.setLooping(true);
        config.setStartLifetime(NumberFunction.constant(32));
        config.setStartSpeed(NumberFunction.constant(0.12F));
        config.setStartSize(new NumberFunction3(0.075F, 0.075F, 0.075F));
        config.setStartColor(NumberFunction.color(color));
        config.setMaxParticles(360);
        config.emission.setEmissionRate(NumberFunction.constant(rate));

        Circle circle = new Circle();
        circle.setRadius(1.0F);
        circle.setRadiusThickness(1.0F);
        config.shape.setShape(circle);
        config.shape.setPosition(new NumberFunction3(layerOffset, 20.0F + layerOffset, -layerOffset));
        config.shape.setScale(new NumberFunction3(22.0F * scale, 1.0F, 22.0F * scale));

        // La velocidad vertical y la estela se simulan dentro del sistema de partículas de Photon.
        config.velocityOverLifetime.setEnable(true);
        config.velocityOverLifetime.setLinear(new NumberFunction3(0.0F, -10.0F, 0.0F));

        config.trails.setEnable(true);
        config.trails.setRatio(0.94F);
        config.trails.setInheritParticleColor(true);
        config.trails.setSizeAffectsWidth(false);
        config.trails.config.setTime(14);
        config.trails.config.setMinVertexDistance(0.03F);
        config.trails.config.setWidthOverTrail(NumberFunction.constant(0.12F));
        config.trails.config.setColorOverTrail(NumberFunction.color(color));

        config.renderer.setBloomEffect(true);
        config.renderer.setBloomColor(color);
        // Photon detecta la colisión con el terreno y retira la estrella al tocarlo.
        // Su estela se desvanece después, sin una comprobación de suelo del renderer propio.
        config.physics.setEnable(true);
        config.physics.setHasCollision(true);
        config.physics.setRemovedWhenCollided(true);
        fx.getMainFX().objects().add(emitter);
    }

    /** Campo de velocidad estelar creado por Photon; las partículas salen del volumen esférico y dejan estelas. */
    private static void addStarsspeedEmitter(FX fx, float scale, int color) {
        ParticleEmitter emitter = new ParticleEmitter();
        emitter.setName("Photon Starsspeed Warp Field");
        ParticleConfig config = emitter.config;
        config.setDuration(120);
        config.setLooping(true);
        config.setStartLifetime(NumberFunction.constant(84));
        config.setStartSpeed(NumberFunction.constant(4.0F));
        config.setStartSize(new NumberFunction3(0.045F, 0.045F, 0.045F));
        config.setStartColor(NumberFunction.color(color));
        config.setMaxParticles(1000);
        config.emission.setEmissionRate(NumberFunction.constant(9.0F));

        Sphere sphere = new Sphere();
        sphere.setRadius(1.0F);
        sphere.setRadiusThickness(0.96F);
        config.shape.setShape(sphere);
        config.shape.setPosition(new NumberFunction3(0.0F, 10.0F, 0.0F));
        config.shape.setScale(new NumberFunction3(58.0F * scale, 42.0F * scale, 58.0F * scale));

        config.trails.setEnable(true);
        config.trails.setRatio(0.98F);
        config.trails.setInheritParticleColor(true);
        config.trails.setSizeAffectsWidth(false);
        config.trails.config.setTime(12);
        config.trails.config.setMinVertexDistance(0.025F);
        config.trails.config.setWidthOverTrail(NumberFunction.constant(0.075F));
        config.trails.config.setColorOverTrail(NumberFunction.color(color));

        config.renderer.setBloomEffect(true);
        config.renderer.setBloomColor(color);
        config.physics.setEnable(false);
        fx.getMainFX().objects().add(emitter);
    }

    private static void addSphere(FX fx, String name, float radius, float y, float rate,
                                  int lifetime, float speed, float size, int color) {
        ParticleEmitter emitter = new ParticleEmitter();
        emitter.setName(name);
        ParticleConfig config = emitter.config;
        config.setDuration(120);
        config.setLooping(true);
        config.setStartLifetime(NumberFunction.constant(lifetime));
        config.setStartSpeed(NumberFunction.constant(speed));
        config.setStartSize(new NumberFunction3(size, size, size));
        config.setStartColor(NumberFunction.color(color));
        config.setMaxParticles(1600);
        config.emission.setEmissionRate(NumberFunction.constant(rate));

        Sphere sphere = new Sphere();
        sphere.setRadius(1.0F);
        sphere.setRadiusThickness(1.0F);
        config.shape.setShape(sphere);
        config.shape.setPosition(new NumberFunction3(0.0F, y, 0.0F));
        config.shape.setScale(new NumberFunction3(radius, radius, radius));
        config.renderer.setBloomEffect(true);
        config.renderer.setBloomColor(color);
        config.physics.setEnable(false);
        fx.getMainFX().objects().add(emitter);
    }

    private static int tint(int color, float factor) {
        int red = Math.min(255, Math.round(((color >> 16) & 255) * factor));
        int green = Math.min(255, Math.round(((color >> 8) & 255) * factor));
        int blue = Math.min(255, Math.round((color & 255) * factor));
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    public static final class Handle implements AutoCloseable {
        private final PhotonInstance instance;

        private Handle(PhotonInstance instance) {
            this.instance = instance;
        }

        private Handle() {
            this.instance = null;
        }

        private static Handle empty() {
            return new Handle();
        }

        public void moveTo(Vec3 position) {
            if (instance != null) {
                instance.moveTo(position);
            }
        }

        public boolean isRunning() {
            return instance != null && instance.isRunning();
        }

        @Override
        public void close() {
            if (instance != null) {
                instance.stopNow();
            }
        }
    }

    private static final class PhotonInstance extends FXEffect {
        private Vec3 position;

        private PhotonInstance(FX fx, Level level, Vec3 position) {
            super(fx, level);
            this.position = position;
        }

        @Override
        public void start() {
            runtime = fx.createRuntime();
            runtime.getRoot().updatePos(toVector(position));
            runtime.getRoot().updateRotation(new Quaternionf());
            runtime.getRoot().updateScale(new Vector3f(1.0F, 1.0F, 1.0F));
            runtime.emmit(this);
        }

        private void moveTo(Vec3 position) {
            this.position = position;
            if (runtime != null) {
                runtime.getRoot().updatePos(toVector(position));
            }
        }

        private void stopNow() {
            if (runtime != null) {
                runtime.destroy(true);
                runtime = null;
            }
        }

        private boolean isRunning() {
            return runtime != null;
        }

        private static Vector3f toVector(Vec3 position) {
            return new Vector3f((float) position.x, (float) position.y, (float) position.z);
        }
    }
}
