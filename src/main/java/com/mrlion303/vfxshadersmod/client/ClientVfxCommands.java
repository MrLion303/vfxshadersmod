package com.mrlion303.vfxshadersmod.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mrlion303.vfxshadersmod.VfxShadersMod;
import com.mrlion303.vfxshadersmod.core.effect.EffectId;
import com.mrlion303.vfxshadersmod.core.effect.EffectRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

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
                                .executes(ClientVfxCommands::status))));
    }

    private static int help(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        source.sendSuccess(() -> Component.literal("VFX Shaders Mod — comandos locales"), false);
        source.sendSuccess(() -> Component.literal("/vfx list — lista los efectos disponibles, sus categorías y estado"), false);
        source.sendSuccess(() -> Component.literal("/vfx enable <efecto> — activa un efecto"), false);
        source.sendSuccess(() -> Component.literal("/vfx disable <efecto> — desactiva un efecto"), false);
        source.sendSuccess(() -> Component.literal("/vfx toggle <efecto> — alterna su estado"), false);
        source.sendSuccess(() -> Component.literal("/vfx status <efecto> — consulta su estado"), false);
        source.sendSuccess(() -> Component.literal("/vfx disable all — desactiva todos los efectos"), false);
        source.sendSuccess(() -> Component.literal("Puedes usar el nombre corto (demo_orbit) o el identificador completo."), false);
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
}
