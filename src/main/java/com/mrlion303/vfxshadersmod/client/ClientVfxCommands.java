package com.mrlion303.vfxshadersmod.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mrlion303.vfxshadersmod.VfxShadersMod;
import com.mrlion303.vfxshadersmod.client.effect.BlackHoleEffect;
import com.mrlion303.vfxshadersmod.client.effect.BlackHolePlacement;
import com.mrlion303.vfxshadersmod.core.effect.EffectId;
import com.mrlion303.vfxshadersmod.core.effect.EffectRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.Optional;

public final class ClientVfxCommands {
    private ClientVfxCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("vfx")
                .executes(ClientVfxCommands::help)
                .then(Commands.literal("help").executes(ClientVfxCommands::help))
                .then(Commands.literal("list").executes(ClientVfxCommands::list))
                .then(Commands.literal("enable")
                        .then(Commands.literal("blackhole")
                                .executes(ClientVfxCommands::blackHoleUsage)
                                .then(Commands.argument("diameter", FloatArgumentType.floatArg(
                                                BlackHoleEffect.MIN_DIAMETER, BlackHoleEffect.MAX_DIAMETER))
                                        .then(Commands.argument("position", Vec3Argument.vec3())
                                                .executes(ClientVfxCommands::placeBlackHole))))
                        .then(Commands.argument("effect", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        EffectRegistry.ids().stream().map(id -> id.location().getPath()).toList(), builder))
                                .executes(context -> change(context, Action.ENABLE))))
                .then(Commands.literal("disable")
                        .then(Commands.literal("all").executes(ClientVfxCommands::disableAll))
                        .then(Commands.literal("blackhole").executes(ClientVfxCommands::clearBlackHoles))
                        .then(Commands.argument("effect", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        EffectRegistry.ids().stream().map(id -> id.location().getPath()).toList(), builder))
                                .executes(context -> change(context, Action.DISABLE))))
                .then(Commands.literal("toggle")
                        .then(Commands.argument("effect", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        EffectRegistry.ids().stream().map(id -> id.location().getPath()).toList(), builder))
                                .executes(context -> change(context, Action.TOGGLE))))
                .then(Commands.literal("status")
                        .then(Commands.argument("effect", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        EffectRegistry.ids().stream().map(id -> id.location().getPath()).toList(), builder))
                                .executes(ClientVfxCommands::status))));
    }

    private static int help(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        source.sendSuccess(() -> Component.literal("VFX Shaders Mod — comandos"), false);
        source.sendSuccess(() -> Component.literal("/vfx list — lista los efectos, categorías y estados"), false);
        source.sendSuccess(() -> Component.literal("/vfx enable <efecto> — activa un efecto"), false);
        source.sendSuccess(() -> Component.literal("/vfx enable blackhole <tamaño> <x> <y> <z> — coloca un agujero negro en coordenadas exactas"), false);
        source.sendSuccess(() -> Component.literal("/vfx disable <efecto> — desactiva un efecto"), false);
        source.sendSuccess(() -> Component.literal("/vfx disable blackhole — elimina todos los agujeros negros colocados"), false);
        source.sendSuccess(() -> Component.literal("/vfx enable blackhole_sky — activa el agujero celeste gigante a 50 grados sobre el horizonte"), false);
        source.sendSuccess(() -> Component.literal("/vfx enable rainbow_sky — cambia todo el cielo y las nubes por colores uniformes del arcoíris"), false);
        source.sendSuccess(() -> Component.literal("Los bloques de comandos también pueden ejecutar /vfx; las órdenes se replican a los clientes de la dimensión."), false);
        source.sendSuccess(() -> Component.literal("/vfx toggle <efecto> — alterna su estado"), false);
        source.sendSuccess(() -> Component.literal("/vfx status <efecto> — consulta el estado"), false);
        source.sendSuccess(() -> Component.literal("/vfx disable all — limpia todos los VFX locales"), false);
        source.sendSuccess(() -> Component.literal("El tamaño es el diámetro en bloques (1 a 100); el valor normal es 10."), false);
        source.sendSuccess(() -> Component.literal("Las coordenadas aceptan valores absolutos o relativos con ~."), false);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        var definitions = EffectRegistry.definitions();
        if (definitions.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No hay efectos VFX registrados."), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Efectos VFX disponibles (" + definitions.size() + "):"), false);
        for (EffectRegistry.Definition definition : definitions) {
            boolean active = ClientVfxManager.isActive(definition.id());
            String state = active ? "ACTIVO" : "INACTIVO";
            String description = definition.description().isBlank() ? "" : " — " + definition.description();
            String line = "[" + state + "] [" + definition.category().displayName() + "] "
                    + definition.displayName() + " (" + definition.id() + ")" + description;
            source.sendSuccess(() -> Component.literal(line), false);
        }
        source.sendSuccess(() -> Component.literal("Usa /vfx enable <efecto> o /vfx disable <efecto>."), false);
        return definitions.size();
    }

    private static int blackHoleUsage(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal(
                "Uso: /vfx enable blackhole <tamaño> <x> <y> <z>. El tamaño es el diámetro en bloques."));
        return 0;
    }

    private static int placeBlackHole(CommandContext<CommandSourceStack> context) {
        float diameter = FloatArgumentType.getFloat(context, "diameter");
        Vec3 position = Vec3Argument.getVec3(context, "position");
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
            context.getSource().sendFailure(Component.literal("Entra en un mundo antes de colocar un agujero negro."));
            return 0;
        }

        var instance = BlackHolePlacement.placeAt(diameter, position);
        if (instance == null) {
            context.getSource().sendFailure(Component.literal("No se pudo colocar el agujero negro."));
            return 0;
        }

        String message = String.format(Locale.ROOT,
                "Agujero negro colocado: diámetro %.1f bloques en (%.1f, %.1f, %.1f).",
                diameter, position.x, position.y, position.z);
        context.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int clearBlackHoles(CommandContext<CommandSourceStack> context) {
        int count = ClientWorldVfxInstances.countByEffect(
                BlackHoleEffect.ID, Minecraft.getInstance().level);
        ClientVfxManager.disable(BlackHoleEffect.ID);
        String message = count == 0
                ? "No había agujeros negros colocados."
                : "Se eliminaron " + count + " agujeros negros colocados.";
        context.getSource().sendSuccess(() -> Component.literal(message), false);
        return count;
    }

    private static int change(CommandContext<CommandSourceStack> context, Action action) {
        var source = context.getSource();
        String name = StringArgumentType.getString(context, "effect");
        Optional<EffectId> parsed = parseId(name);
        if (parsed.isEmpty()) {
            source.sendFailure(Component.literal("Identificador inválido: " + name));
            return 0;
        }

        EffectId id = parsed.get();
        if (!EffectRegistry.ids().contains(id)) {
            source.sendFailure(Component.literal("No existe el efecto VFX " + id + ". Consulta /vfx list."));
            return 0;
        }
        if (id.equals(BlackHoleEffect.ID) && action != Action.DISABLE) {
            source.sendFailure(Component.literal(
                    "Para colocar un agujero negro usa /vfx enable blackhole <tamaño> <x> <y> <z>."
            ));
            return 0;
        }

        ClientVfxManager.Result result = switch (action) {
            case ENABLE -> ClientVfxManager.enable(id);
            case DISABLE -> ClientVfxManager.disable(id);
            case TOGGLE -> ClientVfxManager.toggle(id);
        };

        switch (result) {
            case SUCCESS -> {
                boolean active = ClientVfxManager.isActive(id);
                String state = active ? "activado" : "desactivado";
                String label = EffectRegistry.definition(id).map(EffectRegistry.Definition::displayName).orElse(id.toString());
                source.sendSuccess(() -> Component.literal(label + " (" + id + ") " + state + "."), false);
                return 1;
            }
            case ALREADY_ACTIVE -> {
                source.sendSuccess(() -> Component.literal(id + " ya está activo."), false);
                return 0;
            }
            case NOT_ACTIVE -> {
                source.sendSuccess(() -> Component.literal(id + " ya está desactivado."), false);
                return 0;
            }
            case NO_WORLD -> {
                source.sendFailure(Component.literal("Entra en un mundo antes de activar un VFX."));
                return 0;
            }
            case UNKNOWN_EFFECT -> {
                source.sendFailure(Component.literal("No se pudo crear " + id + ". Revisa el registro y el log."));
                return 0;
            }
        }
        return 0;
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        String name = StringArgumentType.getString(context, "effect");
        Optional<EffectId> parsed = parseId(name);
        if (parsed.isEmpty() || !EffectRegistry.ids().contains(parsed.get())) {
            source.sendFailure(Component.literal("No se encontró ese efecto. Consulta /vfx list."));
            return 0;
        }

        EffectId id = parsed.get();
        boolean active = ClientVfxManager.isActive(id);
        String label = EffectRegistry.definition(id).map(EffectRegistry.Definition::displayName).orElse(id.toString());
        source.sendSuccess(() -> Component.literal(label + " (" + id + "): " + (active ? "ACTIVO" : "INACTIVO")), false);
        return 1;
    }

    private static int disableAll(CommandContext<CommandSourceStack> context) {
        int count = ClientVfxManager.disableAll();
        context.getSource().sendSuccess(() -> Component.literal("Se limpiaron " + count + " efectos/instancias VFX."), false);
        return count;
    }

    private static Optional<EffectId> parseId(String input) {
        String fullId = input.indexOf(':') >= 0 ? input : VfxShadersMod.MOD_ID + ":" + input;
        ResourceLocation location = ResourceLocation.tryParse(fullId);
        return location == null ? Optional.empty() : Optional.of(new EffectId(location));
    }

    private enum Action {
        ENABLE,
        DISABLE,
        TOGGLE
    }
}
