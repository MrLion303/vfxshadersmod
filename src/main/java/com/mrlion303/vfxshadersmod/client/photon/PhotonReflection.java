package com.mrlion303.vfxshadersmod.client.photon;

import com.mojang.blaze3d.shaders.BlendMode;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.renderer.ShaderInstance;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Acceso puntual a campos internos usados por el renderer de Photon.
 * Evita instalar mixins sobre clases vanilla.
 */
public final class PhotonReflection {
    private static final Field LAST_APPLIED_BLEND = findField(BlendMode.class, "lastApplied");
    private static final Field SHADER_BLEND = findField(ShaderInstance.class, "blend");
    private static final Field SHADER_SAMPLERS = findField(ShaderInstance.class, "samplerNames");
    private static final Field SHADER_UNIFORMS = findField(ShaderInstance.class, "uniformMap");

    private PhotonReflection() {
    }

    private static Field findField(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static Object get(Field field, Object owner) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("No se pudo leer un campo interno del renderer.", exception);
        }
    }

    private static void set(Field field, Object owner, Object value) {
        try {
            field.set(owner, value);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("No se pudo restaurar el estado del renderer.", exception);
        }
    }

    public static BlendMode getLastAppliedBlendMode() {
        return (BlendMode) get(LAST_APPLIED_BLEND, null);
    }

    public static void setLastAppliedBlendMode(BlendMode blendMode) {
        set(LAST_APPLIED_BLEND, null, blendMode);
    }

    public static BlendMode getBlend(ShaderInstance shader) {
        return (BlendMode) get(SHADER_BLEND, shader);
    }

    @SuppressWarnings("unchecked")
    public static List<String> getSamplerNames(ShaderInstance shader) {
        return (List<String>) get(SHADER_SAMPLERS, shader);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Uniform> getUniformMap(ShaderInstance shader) {
        return (Map<String, Uniform>) get(SHADER_UNIFORMS, shader);
    }
}
