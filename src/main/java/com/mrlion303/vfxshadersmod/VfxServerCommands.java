package com.mrlion303.vfxshadersmod;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mrlion303.vfxshadersmod.network.VfxCommandPacket;
import com.mrlion303.vfxshadersmod.network.VfxNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Variante de servidor de /vfx. Permite usar activaciones desde command blocks
 * y comunica las órdenes a todos los clientes de la dimensión actual.
 */
@Mod.EventBusSubscriber(modid = VfxShadersMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VfxServerCommands {
    private static final Set<String> EFFECTS = Set.of(
            "demo_orbit",
            "music_horizon_bars",
            "rainbow_aurora",
            "rainbow_sky",
            "blackhole",
            "blackhole_sky",
            "cloud_vortex",
            "starsspeed",
            "ground_aurora",
            "falling_stars",
            "sky_portal"
    );
    private static final Map<ResourceKey<Level>, Set<String>> ACTIVE_BY_DIMENSION = new HashMap<>();

    private VfxServerCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("vfx")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("help").executes(VfxServerCommands::help))
                .then(Commands.literal("list").executes(VfxServerCommands::list))
                .then(Commands.literal("enable")
                        .then(Commands.literal("blackhole")
                                .executes(VfxServerCommands::blackHoleUsage)
                                .then(Commands.argument("diameter", FloatArgumentType.floatArg(1.0F, 100.0F))
                                        .then(Commands.argument("position", Vec3Argument.vec3())
                                                .executes(VfxServerCommands::placeBlackHole))))
                        .then(Commands.literal("starsspeed")
                                .executes(context -> enableConfigured(context, "starsspeed", "blue", Vec3.ZERO))
                                .then(Commands.argument("color", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                com.mrlion303.vfxshadersmod.util.VfxColors.names(), builder))
                                        .executes(context -> enableConfigured(context, "starsspeed",
                                                StringArgumentType.getString(context, "color"), Vec3.ZERO))))
                        .then(Commands.literal("ground_aurora")
                                .then(Commands.argument("position", Vec3Argument.vec3())
                                        .then(Commands.argument("color", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        com.mrlion303.vfxshadersmod.util.VfxColors.names(), builder))
                                                .executes(context -> enableConfigured(context, "ground_aurora",
                                                        StringArgumentType.getString(context, "color"),
                                                        Vec3Argument.getVec3(context, "position"))))))
                        .then(Commands.argument("effect", StringArgumentType.word())
                                .suggests((context, builder) ->
                                        SharedSuggestionProvider.suggest(EFFECTS.stream()
                                                .filter(id -> !id.equals("blackhole"))
                                                .toList(), builder))
                                .executes(VfxServerCommands::enable)))
                .then(Commands.literal("disable")
                        .then(Commands.literal("all").executes(VfxServerCommands::disableAll))
                        .then(Commands.literal("blackhole").executes(VfxServerCommands::clearBlackHoles))
                        .then(Commands.argument("effect", StringArgumentType.word())
                                .suggests((context, builder) ->
                                        SharedSuggestionProvider.suggest(EFFECTS, builder))
                                .executes(VfxServerCommands::disable)))
                .then(Commands.literal("toggle")
                        .then(Commands.argument("effect", StringArgumentType.word())
                                .suggests((context, builder) ->
                                        SharedSuggestionProvider.suggest(EFFECTS, builder))
                                .executes(VfxServerCommands::toggle)))
                .then(Commands.literal("status")
                        .then(Commands.argument("effect", StringArgumentType.word())
                                .suggests((context, builder) ->
                                        SharedSuggestionProvider.suggest(EFFECTS, builder))
                                .executes(VfxServerCommands::status))));
    }

    private static int help(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("VFX Shaders Mod — comandos de servidor/command block"), false);
        context.getSource().sendSuccess(() -> Component.literal("/vfx enable <efecto> — activa el efecto en los clientes de esta dimensión"), false);
        context.getSource().sendSuccess(() -> Component.literal("/vfx enable blackhole <tamaño> <x> <y> <z> — coloca un agujero negro"), false);
        context.getSource().sendSuccess(() -> Component.literal("/vfx enable blackhole_sky — activa el agujero celeste fijo a 50 grados sobre el horizonte"), false);
        context.getSource().sendSuccess(() -> Component.literal("/vfx disable <efecto> — desactiva el efecto"), false);
        context.getSource().sendSuccess(() -> Component.literal("/vfx disable blackhole — elimina los agujeros negros colocados"), false);
        context.getSource().sendSuccess(() -> Component.literal("/vfx disable all — limpia todos los VFX"), false);
        context.getSource().sendSuccess(() -> Component.literal("/vfx enable starsspeed <color> — cielo teñido con estrellas rápidas"), false);
        context.getSource().sendSuccess(() -> Component.literal("/vfx enable ground_aurora <x> <y> <z> <color> — ondas sobre el terreno"), false);
        context.getSource().sendSuccess(() -> Component.literal("/vfxonce scan <x> <y> <z> <color> — escaneo de un solo uso"), false);
        context.getSource().sendSuccess(() -> Component.literal("/vfxonce trampolin <x> <y> <z> [potencia] [radio] [velocidad] — onda circular que deforma visualmente el suelo y lanza a los jugadores"), false);
        context.getSource().sendSuccess(() -> Component.literal("/vfxmove moonjump enable|disable <jugadores>"), false);
        context.getSource().sendSuccess(() -> Component.literal("/vfxmove waterwalk enable|disable <jugadores>"), false);
        context.getSource().sendSuccess(() -> Component.literal("/vfxmove empujon <jugadores> — impulsa a los jugadores seleccionados"), false);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        Set<String> active = ACTIVE_BY_DIMENSION.getOrDefault(context.getSource().getLevel().dimension(), Set.of());
        context.getSource().sendSuccess(() -> Component.literal("Efectos disponibles para command blocks:"), false);
        for (String effect : EFFECTS) {
            String state = active.contains(effect) ? "ACTIVO (última orden del servidor)" : "INACTIVO (última orden del servidor)";
            context.getSource().sendSuccess(() -> Component.literal("[" + state + "] " + effect), false);
        }
        return EFFECTS.size();
    }

    private static int blackHoleUsage(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal(
                "Uso: /vfx enable blackhole <tamaño> <x> <y> <z>. El tamaño es el diámetro en bloques."));
        return 0;
    }

    private static int enable(CommandContext<CommandSourceStack> context) {
        String effect = StringArgumentType.getString(context, "effect");
        if (!isSupported(effect) || effect.equals("blackhole")) {
            context.getSource().sendFailure(Component.literal("Efecto inválido o requiere coordenadas: " + effect));
            return 0;
        }
        send(context.getSource().getLevel(), VfxCommandPacket.ENABLE, effect, 0.0F, Vec3.ZERO);
        active(context.getSource().getLevel()).add(effect);
        context.getSource().sendSuccess(() -> Component.literal("Se activó " + effect + " en los clientes de esta dimensión."), true);
        return 1;
    }

    private static int enableConfigured(
            CommandContext<CommandSourceStack> context, String effect, String colorName, Vec3 position
    ) {
        Integer rgb = com.mrlion303.vfxshadersmod.util.VfxColors.parse(colorName);
        if (rgb == null) {
            context.getSource().sendFailure(Component.literal(
                    "Color inválido. " + com.mrlion303.vfxshadersmod.util.VfxColors.describeOptions()));
            return 0;
        }
        if (!isSupported(effect)) {
            context.getSource().sendFailure(Component.literal("Efecto no soportado: " + effect));
            return 0;
        }
        ServerLevel level = context.getSource().getLevel();
        send(level, VfxCommandPacket.ENABLE_CONFIGURED, effect, 0.0F, position, colorName);
        active(level).add(effect);
        context.getSource().sendSuccess(() -> Component.literal(
                "Se activó " + effect + " con color " + colorName + " en los clientes de esta dimensión."), true);
        return 1;
    }

    private static int placeBlackHole(CommandContext<CommandSourceStack> context) {
        float diameter = FloatArgumentType.getFloat(context, "diameter");
        Vec3 position = Vec3Argument.getVec3(context, "position");
        ServerLevel level = context.getSource().getLevel();
        VfxForceFieldSystem.addBlackHole(level, position, diameter);
        send(level, VfxCommandPacket.PLACE_BLACKHOLE, "blackhole", diameter, position);
        active(level).add("blackhole");
        context.getSource().sendSuccess(() -> Component.literal(String.format(
                java.util.Locale.ROOT,
                "Agujero negro enviado a los clientes: diámetro %.1f en (%.1f, %.1f, %.1f).",
                diameter, position.x, position.y, position.z)), true);
        return 1;
    }

    private static int disable(CommandContext<CommandSourceStack> context) {
        String effect = StringArgumentType.getString(context, "effect");
        if (!isSupported(effect)) {
            context.getSource().sendFailure(Component.literal("No existe el efecto " + effect + ". Usa /vfx list."));
            return 0;
        }
        if (effect.equals("blackhole")) {
            return clearBlackHoles(context);
        }
        send(context.getSource().getLevel(), VfxCommandPacket.DISABLE, effect, 0.0F, Vec3.ZERO);
        active(context.getSource().getLevel()).remove(effect);
        context.getSource().sendSuccess(() -> Component.literal("Se desactivó " + effect + " en los clientes de esta dimensión."), true);
        return 1;
    }

    private static int clearBlackHoles(CommandContext<CommandSourceStack> context) {
        VfxForceFieldSystem.clearBlackHoles(context.getSource().getLevel());
        send(context.getSource().getLevel(), VfxCommandPacket.CLEAR_BLACKHOLES, "blackhole", 0.0F, Vec3.ZERO);
        active(context.getSource().getLevel()).remove("blackhole");
        context.getSource().sendSuccess(() -> Component.literal("Se eliminaron los agujeros negros de los clientes de esta dimensión."), true);
        return 1;
    }

    private static int disableAll(CommandContext<CommandSourceStack> context) {
        VfxForceFieldSystem.clearBlackHoles(context.getSource().getLevel());
        send(context.getSource().getLevel(), VfxCommandPacket.DISABLE_ALL, "", 0.0F, Vec3.ZERO);
        active(context.getSource().getLevel()).clear();
        context.getSource().sendSuccess(() -> Component.literal("Se limpiaron todos los VFX de los clientes de esta dimensión."), true);
        return 1;
    }

    private static int toggle(CommandContext<CommandSourceStack> context) {
        String effect = StringArgumentType.getString(context, "effect");
        if (!isSupported(effect) || effect.equals("blackhole")) {
            context.getSource().sendFailure(Component.literal(
                    "No se puede alternar ese efecto. Para blackhole usa /vfx enable blackhole <tamaño> <x> <y> <z>."));
            return 0;
        }

        Set<String> active = active(context.getSource().getLevel());
        boolean enable = !active.contains(effect);
        send(context.getSource().getLevel(), enable ? VfxCommandPacket.ENABLE : VfxCommandPacket.DISABLE,
                effect, 0.0F, Vec3.ZERO);
        if (enable) {
            active.add(effect);
        } else {
            active.remove(effect);
        }
        context.getSource().sendSuccess(
                () -> Component.literal(effect + (enable ? " activado." : " desactivado.")), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        String effect = StringArgumentType.getString(context, "effect");
        if (!isSupported(effect)) {
            context.getSource().sendFailure(Component.literal("No existe el efecto " + effect + ". Usa /vfx list."));
            return 0;
        }
        boolean isActive = active(context.getSource().getLevel()).contains(effect);
        context.getSource().sendSuccess(
                () -> Component.literal(effect + ": " + (isActive ? "ACTIVO" : "INACTIVO")
                        + " según la última orden ejecutada en el servidor."), false);
        return 1;
    }

    private static Set<String> active(ServerLevel level) {
        return ACTIVE_BY_DIMENSION.computeIfAbsent(level.dimension(), ignored -> new HashSet<>());
    }

    private static boolean isSupported(String effect) {
        return EFFECTS.contains(effect);
    }

    private static void send(ServerLevel level, String operation, String effect, float diameter, Vec3 position) {
        send(level, operation, effect, diameter, position, "");
    }

    private static void send(ServerLevel level, String operation, String effect, float diameter, Vec3 position, String option) {
        VfxNetwork.sendToDimension(level, new VfxCommandPacket(
                operation, effect, level.dimension().location(), diameter, position, option));
    }
}
