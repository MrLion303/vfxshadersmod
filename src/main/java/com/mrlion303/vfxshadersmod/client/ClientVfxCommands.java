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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

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
                        .then(Commands.argument("effect", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        EffectRegistry.ids().stream().map(id -> id.location().getPath()).toList(), builder))
                                .executes(context -> change(context, Action.ENABLE))))
                .then(Commands.literal("disable")
                        .then(Commands.literal("all").executes(ClientVfxCommands::disableAll))
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
                                .executes(ClientVfxCommands::status)))
                .then(Commands.literal("blackhole")
                        .then(Commands.literal("spawn")
                                .executes(context -> spawnBlackHole(context, BlackHoleEffect.DEFAULT_DIAMETER))
                                .then(Commands.argument("diameter", FloatArgumentType.floatArg(1.0F, 100.0F))
                                        .executes(context -> spawnBlackHole(
                                                context, FloatArgumentType.getFloat(context, "diameter")))))
                        .then(Commands.literal("clear").executes(ClientVfxCommands::clearBlackHoles))));
    }

    private static int help(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        source.sendSuccess(() -> Component.literal("VFX Shaders Mod — comandos locales"), false);
        source.sendSuccess(() -> Component.literal("/vfx list — lista los efectos, categorías y estados"), false);
        source.sendSuccess(() -> Component.literal("/vfx enable <efecto> — activa un efecto"), false);
        source.sendSuccess(() -> Component.literal("/vfx disable <efecto> — desactiva un efecto"), false);
        source.sendSuccess(() -> Component.literal("/vfx toggle <efecto> — alterna su estado"), false);
        source.sendSuccess(() -> Component.literal("/vfx status <efecto> — consulta su estado"), false);
        source.sendSuccess(() -> Component.literal("/vfx blackhole spawn [diámetro] — coloca un agujero negro (10 bloques por defecto)"), false);
        source.sendSuccess(() -> Component.literal("/vfx blackhole clear — elimina los agujeros negros colocados"), false);
        source.sendSuccess(() -> Component.literal("/vfx disable all — desactiva todos los VFX"), false);
        source.sendSuccess(() -> Component.literal("El diámetro del agujero negro se ajusta entre 1 y 100 bloques."), false);
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
        source.sendSuccess(() -> Component.literal("Usa /vfx enable <efecto>, /vfx disable <efecto> o /vfx toggle <efecto>."), false);
        return definitions.size();
    }

    private static int spawnBlackHole(CommandContext<CommandSourceStack> context, float diameter) {
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
            context.getSource().sendFailure(Component.literal("Entra en un mundo antes de colocar un agujero negro."));
            return 0;
        }

        var instance = BlackHolePlacement.spawnAtAim(diameter);
        if (instance == null) {
            context.getSource().sendFailure(Component.literal("No se pudo colocar el agujero negro."));
            return 0;
        }

        Vec3Holder position = new Vec3Holder(
                instance.position().x, instance.position().y, instance.position().z);
        String message = String.format(Locale.ROOT,
                "Agujero negro colocado: diámetro %.1f bloques en (%.1f, %.1f, %.1f).",
                diameter, position.x(), position.y(), position.z());
        context.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int clearBlackHoles(CommandContext<CommandSourceStack> context) {
        int count = ClientWorldVfxInstances.countByEffect(
                BlackHoleEffect.ID, Minecraft.getInstance().level);
        ClientVfxManager.disable(BlackHoleEffect.ID);
        context.getSource().sendSuccess(
                () -> Component.literal("Se eliminaron " + count + " agujeros negros colocados."), false);
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
        context.getSource().sendSuccess(() -> Component.literal("Se desactivaron " + count + " efectos VFX."), false);
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

    private record Vec3Holder(double x, double y, double z) {
    }
}
