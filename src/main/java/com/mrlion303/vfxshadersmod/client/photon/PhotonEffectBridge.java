package com.mrlion303.vfxshadersmod.client.photon;

import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXEffect;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction3;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.Curve;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.ECBCurves;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.Box;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.Circle;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.Sphere;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleConfig;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleEmitter;
import com.mrlion303.vfxshadersmod.client.effect.BlackHoleSkyEffect;
import com.mrlion303.vfxshadersmod.client.effect.CloudVortexEffect;
import com.mrlion303.vfxshadersmod.client.effect.SkyPortalEffect;
import com.mrlion303.vfxshadersmod.client.effect.GroundAuroraEffect;
import com.mrlion303.vfxshadersmod.client.effect.TerrainScanEffect;
import com.mrlion303.vfxshadersmod.client.effect.TrampolineWaveEffect;
import com.mrlion303.vfxshadersmod.client.render.BlackHoleVfxRenderer;
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
        RAINBOW_AURORA,
        FALLING_STARS,
        STARSSPEED,
        GROUND_WAVE,
        TRAMPOLINE,
        TERRAIN_SCAN,
        VORTEX,
        PORTAL,
        BLACK_HOLE,
        BLACK_HOLE_SKY
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
            case "demo_orbit", "falling_stars", "rainbow_aurora_photon" -> true;
            default -> false;
        };
    }

    public static Handle playForEffect(String effectPath, Level level) {
        Vec3 anchor = anchorFor(effectPath, level);
        return switch (effectPath) {
            case "blackhole" -> Handle.empty();
            case "demo_orbit" -> play(level, anchor, Preset.ORBIT, 0, 1.0F);
            case "music_horizon_bars" -> Handle.empty();
            case "rainbow_aurora" -> Handle.empty();
            case "rainbow_aurora_photon" -> play(level, anchor, Preset.RAINBOW_AURORA, 0, 1.0F);
            // El tinte integral del cielo requiere el shader propio; Photon no simula una cúpula de color.
            case "rainbow_sky" -> Handle.empty();
            // El shader compartido ya dibuja el anillo; evita añadir otro con partículas.
            case "blackhole_sky" -> Handle.empty();
            // El vórtice procedural es el renderizador principal y no necesita capas duplicadas.
            case "cloud_vortex" -> Handle.empty();
            case "starsspeed" -> Handle.empty();
            case "ground_aurora" -> play(level, anchor, Preset.GROUND_WAVE, GroundAuroraEffect.configuredColor(), 2.4F);
            case "falling_stars" -> play(level, anchor, Preset.FALLING_STARS, 0, 1.7F);
            case "scan" -> play(level, anchor, Preset.TERRAIN_SCAN, TerrainScanEffect.configuredColor(), 1.6F);
            case "trampolin" -> play(level, anchor, Preset.TRAMPOLINE, 0xFF58DFFF, (float) (TrampolineWaveEffect.configuredRadius() / 14.0D));
            // El portal propio ya contiene el panorama y las grietas geométricas.
            case "sky_portal" -> Handle.empty();
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
            case "demo_orbit", "rainbow_aurora_photon",
                    "blackhole_sky", "cloud_vortex", "falling_stars", "sky_portal" -> true;
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
            case "blackhole_sky" -> base.add(BlackHoleSkyEffect.centerOffset());
            case "cloud_vortex" -> base.add(CloudVortexEffect.centerOffset());
            case "sky_portal" -> base.add(SkyPortalEffect.centerOffset());
            default -> base;
        };
    }

    public static Handle play(Level level, Vec3 position, Preset preset, int color, float scale) {
        if (level == null || !level.isClientSide()) {
            return Handle.empty();
        }

        FX fx = new FX();
        float safeScale = Math.max(0.1F, Math.min(20.0F, scale));
        switch (preset) {
            case ORBIT -> {
                addRing(fx, "Photon Orbit Outer", 1.8F * safeScale, 1.0F, 1.2F, 70, 0.06F, 0.13F, 0xFFB16BFF, 0);
                addRing(fx, "Photon Orbit Inner", 1.1F * safeScale, 1.0F, 1.8F, 48, 0.08F, 0.10F, 0xFF4FE6FF, 1);
                addRing(fx, "Photon Orbit Accent", 0.65F * safeScale, 1.0F, 0.9F, 40, 0.10F, 0.08F, 0xFFFF65B8, 2);
            }
            case HORIZON -> addHorizonBars(fx);
            case RAINBOW_AURORA -> {
                int[] colors = {0xFFFF4545, 0xFFFFA43A, 0xFFFFFF4A, 0xFF56FF76, 0xFF48E6FF, 0xFF6575FF, 0xFFE86BFF};
                for (int i = 0; i < colors.length; i++) {
                    addAuroraTrailRing(fx, i, colors[i]);
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
                float radius = 20.0F * safeScale;
                addExpandingRing(fx, "Photon Ground Wave Bright", 0.2F, radius, 120, 8, 34.0F, 0.055F, color, 0, true);
                addExpandingRing(fx, "Photon Ground Wave Halo", 0.2F, radius, 120, 8, 24.0F, 0.085F, tint(color, 0.55F), 38, true);
                addExpandingRing(fx, "Photon Ground Wave Sparks", 0.2F, radius, 120, 8, 22.0F, 0.035F, 0xFFE9FFFF, 76, true);
            }
            case TRAMPOLINE -> {
                float radius = 14.0F * safeScale;
                int duration = Math.max(2, Math.min(450,
                        (int) Math.ceil(radius / TrampolineWaveEffect.configuredSpeed())));
                addExpandingRing(fx, "Photon Trampoline Wave Core", 0.2F, radius, duration, 6, 44.0F, 0.055F, color, 0, false);
                addExpandingRing(fx, "Photon Trampoline Wave Halo", 0.2F, radius, duration, 6, 28.0F, 0.095F, tint(color, 0.55F), 2, false);
                addExpandingRing(fx, "Photon Trampoline Wave Sparks", 0.2F, radius, duration, 6, 18.0F, 0.035F, 0xFFE9FFFF, 4, false);
            }
            case TERRAIN_SCAN -> {
                float radius = 62.5F * safeScale;
                addExpandingRing(fx, "Photon Terrain Scan Outer", 0.2F, radius, 70, 6, 56.0F, 0.045F, color, 0, false);
                addExpandingRing(fx, "Photon Terrain Scan Glow", 0.2F, radius, 70, 6, 38.0F, 0.075F, tint(color, 0.65F), 1, false);
                addExpandingRing(fx, "Photon Terrain Scan Core", 0.2F, radius, 70, 6, 24.0F, 0.035F, 0xFFFFFFFF, 2, false);
            }
            case VORTEX -> {
                addVortexLayer(fx, "Photon Vortex Outer Cloud", 88.0F, 25.0F, -4.0F,
                        4.0F, 0.14F, 0xFF651016, 0.55F, 42, true);
                addVortexLayer(fx, "Photon Vortex Mid Cloud", 68.0F, 22.0F, 1.5F,
                        4.0F, 0.17F, 0xFFB12413, 0.82F, 36, true);
                addVortexLayer(fx, "Photon Vortex Inner Plasma", 46.0F, 15.0F, 4.0F,
                        3.0F, 0.20F, 0xFFFF5414, 1.05F, 28, true);
                addVortexLayer(fx, "Photon Vortex Hot Core", 18.0F, 8.0F, 0.0F,
                        3.0F, 0.28F, 0xFFFFD49A, 1.20F, 14, false);
            }
            case PORTAL -> {
                // El frente crece durante la misma ventana de apertura del portal.
                float radius = 6.2F * safeScale;
                addExpandingRing(fx, "Photon Portal Outer Energy Ring", 0.2F, radius, 72, 6, 48.0F, 0.055F, 0xFF55DFFF, 0, false);
                addExpandingRing(fx, "Photon Portal Middle Energy Ring", 0.2F, radius * 0.80F, 72, 6, 36.0F, 0.075F, 0xFF9EEFFF, 2, false);
                addExpandingRing(fx, "Photon Portal Inner Energy Ring", 0.2F, radius * 0.62F, 72, 6, 28.0F, 0.040F, 0xFFFFFFFF, 4, false);
                addRing(fx, "Photon Portal Orbiting Shards", 5.5F * safeScale, 0.0F, 3.0F, 54, 0.24F, 0.045F, 0xFFFFFFFF, 0);
            }
            case BLACK_HOLE -> {
                // El disco para agujeros colocados permanece casi horizontal sobre el horizonte.
                addRing(fx, "Photon Accretion Outer", 2.95F * safeScale, 0.0F, 4.0F, 95, 0.08F, 0.16F, 0xFFFFA34A, 0);
                addRing(fx, "Photon Accretion Inner", 2.0F * safeScale, 0.0F, 3.0F, 85, 0.04F, 0.11F, 0xFFFFF1CE, 0);
            }
            case BLACK_HOLE_SKY -> {
                // En el cielo, el disco se orienta hacia el observador y coincide con la escala del shader.
                addRing(fx, "Photon Sky Accretion Outer", 4.05F * safeScale, 0.0F, 6.0F, 95, 0.08F, 0.16F, 0xFFFFA34A, 0);
                addRing(fx, "Photon Sky Accretion Mid", 3.70F * safeScale, 0.0F, 4.5F, 88, 0.06F, 0.13F, 0xFFFFB95F, 0);
                addRing(fx, "Photon Sky Accretion Inner", 2.60F * safeScale, 0.0F, 3.0F, 80, 0.04F, 0.11F, 0xFFFFF1CE, 0);
            }
        }

        PhotonInstance instance = new PhotonInstance(fx, level, position, orientationFor(preset));
        instance.start();
        return new Handle(instance);
    }

    /**
     * Los emisores se crean en el plano XZ (normal Y); rota cada efecto compuesto
     * para que el disco Photon coincida con el plano de su geometría principal.
     */
    private static Quaternionf orientationFor(Preset preset) {
        Vec3 normal = switch (preset) {
            case VORTEX -> CloudVortexEffect.centerOffset().normalize();
            case PORTAL -> SkyPortalEffect.portalNormal();
            case BLACK_HOLE -> BlackHoleVfxRenderer.diskNormal();
            case BLACK_HOLE_SKY -> BlackHoleSkyEffect.diskNormal();
            default -> new Vec3(0.0D, 1.0D, 0.0D);
        };
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f target = new Vector3f((float) normal.x, (float) normal.y, (float) normal.z).normalize();
        return new Quaternionf().rotationTo(up, target);
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
        config.trails.setEnable(true);
        config.trails.setRatio(0.58F);
        config.trails.setLifetime(NumberFunction.constant(0.34F));
        config.trails.setInheritParticleColor(true);
        config.trails.setSizeAffectsWidth(false);
        config.trails.config.setTime(16);
        config.trails.config.setMinVertexDistance(0.055F);
        config.trails.config.setWidthOverTrail(NumberFunction.constant(Math.max(0.025F, size * 0.55F)));
        config.trails.config.setColorOverTrail(NumberFunction.color(color));
        config.physics.setEnable(false);
        fx.getMainFX().objects().add(emitter);
    }

    /**
     * Capa de un vórtice volumétrico basada en partículas esféricas de Photon,
     * con ruido, rotación orbital y estelas que se curvan alrededor del núcleo.
     */
    private static void addVortexLayer(
            FX fx, String name, float radius, float height, float y,
            float rate, float size, int color, float orbitalSpeed, int trailTime, boolean withTrails
    ) {
        ParticleEmitter emitter = new ParticleEmitter();
        emitter.setName(name);
        emitter.setDelay((int) (radius / 10.0F) % 7);
        ParticleConfig config = emitter.config;
        config.setDuration(120);
        config.setLooping(true);
        config.setStartLifetime(NumberFunction.constant(76));
        config.setStartSpeed(NumberFunction.constant(0.0F));
        config.setStartSize(new NumberFunction3(size, size, size));
        config.setStartColor(NumberFunction.color(color));
        config.setMaxParticles(900);
        config.emission.setEmissionRate(NumberFunction.constant(rate));

        Sphere sphere = new Sphere();
        sphere.setRadius(1.0F);
        sphere.setRadiusThickness(0.34F);
        config.shape.setShape(sphere);
        config.shape.setPosition(new NumberFunction3(0.0F, y, 0.0F));
        config.shape.setScale(new NumberFunction3(radius, height, radius));

        config.velocityOverLifetime.setEnable(true);
        config.velocityOverLifetime.setOrbitalMode(
                com.lowdragmc.photon.client.gameobject.emitter.data.VelocityOverLifetimeSetting.OrbitalMode.AngularVelocity);
        config.velocityOverLifetime.setOrbital(new NumberFunction3(0.0F, orbitalSpeed, 0.0F));

        config.noise.setEnable(true);
        config.noise.setFrequency(0.22F);
        config.noise.setPosition(new NumberFunction3(0.22F, 0.16F, 0.22F));
        config.noise.setRotation(NumberFunction.constant(4.0F));
        config.noise.setSize(NumberFunction.constant(0.06F));

        if (withTrails) {
            config.trails.setEnable(true);
            config.trails.setRatio(0.82F);
            config.trails.setLifetime(NumberFunction.constant(0.48F));
            config.trails.setInheritParticleColor(true);
            config.trails.setSizeAffectsWidth(false);
            config.trails.config.setTime(trailTime);
            config.trails.config.setMinVertexDistance(0.07F);
            config.trails.config.setWidthOverTrail(NumberFunction.constant(Math.max(0.07F, size * 0.9F)));
            config.trails.config.setColorOverTrail(NumberFunction.color(color));
        }

        config.renderer.setBloomEffect(true);
        config.renderer.setBloomColor(color);
        config.physics.setEnable(false);
        fx.getMainFX().objects().add(emitter);
    }

    /**
     * Una banda de la aurora es un emisor circular nativo de Photon con partículas
     * orbitando y estelas largas. Las bandas se apilan en altura y usan una paleta espectral.
     */
    private static void addAuroraTrailRing(FX fx, int layer, int color) {
        ParticleEmitter emitter = new ParticleEmitter();
        emitter.setName("Photon Aurora Trail Band " + (layer + 1));
        emitter.setDelay(layer * 4);
        ParticleConfig config = emitter.config;
        config.setDuration(100);
        config.setLooping(true);
        config.setStartLifetime(NumberFunction.constant(70));
        config.setStartSpeed(NumberFunction.constant(0.0F));
        config.setStartSize(new NumberFunction3(0.055F, 0.055F, 0.055F));
        config.setStartColor(NumberFunction.color(color));
        config.setMaxParticles(180);
        config.emission.setEmissionRate(NumberFunction.constant(2.0F));

        Circle circle = new Circle();
        circle.setRadius(1.0F);
        circle.setRadiusThickness(0.025F);
        config.shape.setShape(circle);
        config.shape.setPosition(new NumberFunction3(0.0F, 9.0F + layer * 3.2F, 0.0F));
        float radius = 74.0F + layer * 1.65F;
        config.shape.setScale(new NumberFunction3(radius, 1.0F, radius));

        config.velocityOverLifetime.setEnable(true);
        config.velocityOverLifetime.setOrbitalMode(
                com.lowdragmc.photon.client.gameobject.emitter.data.VelocityOverLifetimeSetting.OrbitalMode.AngularVelocity);
        config.velocityOverLifetime.setOrbital(
                new NumberFunction3(0.0F, 0.72F + layer * 0.035F, 0.0F));

        config.trails.setEnable(true);
        config.trails.setRatio(0.95F);
        config.trails.setLifetime(NumberFunction.constant(0.42F));
        config.trails.setInheritParticleColor(true);
        config.trails.setSizeAffectsWidth(false);
        config.trails.config.setTime(44);
        config.trails.config.setMinVertexDistance(0.04F);
        config.trails.config.setWidthOverTrail(NumberFunction.constant(0.075F));
        config.trails.config.setColorOverTrail(NumberFunction.color(color));

        config.renderer.setBloomEffect(true);
        config.renderer.setBloomColor(color);
        config.physics.setEnable(false);
        fx.getMainFX().objects().add(emitter);
    }

    /**
     * Ecualizador circular compuesto por emisores Box de Photon. Cada columna tiene
     * un color fijo por ángulo y una curva de altura con fase desfasada respecto a las demás.
     */
    private static void addHorizonBars(FX fx) {
        final int barCount = 96;
        final int duration = 100;
        final float radius = 42.0F;
        final int[] palette = {
                0xFFFF5555, 0xFFFF973D, 0xFFFFFF55, 0xFF65FF72,
                0xFF4FEAFF, 0xFF6680FF, 0xFFE874FF
        };

        for (int i = 0; i < barCount; i++) {
            double angle = Math.PI * 2.0D * i / barCount;
            float x = (float) (Math.cos(angle) * radius);
            float z = (float) (Math.sin(angle) * radius);
            int color = palette[i * palette.length / barCount];

            ParticleEmitter emitter = new ParticleEmitter();
            emitter.setName("Photon Horizon Bar " + (i + 1));
            emitter.setDelay(i * duration / barCount);
            ParticleConfig config = emitter.config;
            config.setDuration(duration);
            config.setLooping(true);
            config.setStartLifetime(NumberFunction.constant(5));
            config.setStartSpeed(NumberFunction.constant(0.0F));
            config.setStartSize(new NumberFunction3(0.22F, 0.22F, 0.22F));
            config.setStartColor(NumberFunction.color(color));
            config.setMaxParticles(24);
            config.emission.setEmissionRate(NumberFunction.constant(3.0F));

            Box box = new Box();
            box.setEmitFrom(Box.Type.Volume);
            config.shape.setShape(box);

            Curve heightCurve = new Curve(0.0F, duration, 0.75F, 7.25F,
                    2.5F, "duration", "bar height");
            heightCurve.setCurves(horizonPulseCurve());
            Curve centerCurve = new Curve(0.0F, duration, -0.625F, 2.625F,
                    0.625F, "duration", "bar center");
            centerCurve.setCurves(horizonPulseCurve());

            config.shape.setPosition(new NumberFunction3(
                    NumberFunction.constant(x), centerCurve, NumberFunction.constant(z)));
            config.shape.setScale(new NumberFunction3(
                    NumberFunction.constant(1.05F), heightCurve, NumberFunction.constant(0.68F)));
            config.renderer.setBloomEffect(true);
            config.renderer.setBloomColor(color);
            config.physics.setEnable(false);
            fx.getMainFX().objects().add(emitter);
        }
    }

    private static ECBCurves horizonPulseCurve() {
        return new ECBCurves(
                0.0F, 0.18F, 0.25F, 0.98F, 0.06F, 0.18F, 0.18F, 0.98F,
                0.25F, 0.98F, 0.50F, 0.34F, 0.32F, 0.98F, 0.43F, 0.34F,
                0.50F, 0.34F, 0.75F, 0.88F, 0.57F, 0.34F, 0.68F, 0.88F,
                0.75F, 0.88F, 1.0F, 0.18F, 0.82F, 0.88F, 0.94F, 0.18F
        );
    }

    /**
     * Anillo de partículas impulsado por la curva temporal de escala de Photon.
     * El radio crece en la propia ShapeSetting: no se anima una malla dibujada por el mod.
     */
    private static void addExpandingRing(
            FX fx, String name, float startRadius, float endRadius,
            int duration, int particleLifetime, float rate, float size, int color,
            int emitterDelay, boolean looping
    ) {
        ParticleEmitter emitter = new ParticleEmitter();
        emitter.setName(name);
        emitter.setDelay(emitterDelay);
        ParticleConfig config = emitter.config;
        config.setDuration(duration);
        config.setLooping(looping);
        config.setStartLifetime(NumberFunction.constant(particleLifetime));
        config.setStartSpeed(NumberFunction.constant(0.0F));
        config.setStartSize(new NumberFunction3(size, size, size));
        config.setStartColor(NumberFunction.color(color));
        config.setMaxParticles(1800);
        config.emission.setEmissionRate(NumberFunction.constant(rate));

        Circle circle = new Circle();
        circle.setRadius(1.0F);
        // Photon interpreta thickness como el grosor relativo de la banda exterior.
        // Un valor pequeño deja un anillo fino y evita rellenar el disco interior.
        circle.setRadiusThickness(0.035F);
        config.shape.setShape(circle);
        config.shape.setPosition(new NumberFunction3(0.0F, 0.14F, 0.0F));

        Curve radiusCurve = new Curve(0.0F, duration, startRadius, endRadius,
                startRadius, "duration", "radius");
        // ECBCurves recibe primero los extremos y después los dos puntos de control.
        radiusCurve.setCurves(new ECBCurves(0.0F, 0.0F, 1.0F, 1.0F,
                0.1F, 0.1F, 0.9F, 0.9F));
        config.shape.setScale(new NumberFunction3(radiusCurve,
                NumberFunction.constant(1.0F), radiusCurve));

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
        private final Quaternionf rotation;

        private PhotonInstance(FX fx, Level level, Vec3 position, Quaternionf rotation) {
            super(fx, level);
            this.position = position;
            this.rotation = new Quaternionf(rotation);
        }

        @Override
        public void start() {
            runtime = fx.createRuntime();
            runtime.getRoot().updatePos(toVector(position));
            runtime.getRoot().updateRotation(new Quaternionf(rotation));
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
