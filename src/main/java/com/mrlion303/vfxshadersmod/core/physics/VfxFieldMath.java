package com.mrlion303.vfxshadersmod.core.physics;

import net.minecraft.world.phys.Vec3;

/** Operaciones estables compartidas por campos de fuerza y deformaciones VFX. */
public final class VfxFieldMath {
    private VfxFieldMath() {
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double smoothstep(double edge0, double edge1, double value) {
        if (edge1 <= edge0) {
            return value < edge0 ? 0.0D : 1.0D;
        }
        double t = clamp((value - edge0) / (edge1 - edge0), 0.0D, 1.0D);
        return t * t * (3.0D - 2.0D * t);
    }

    /** Caída radial suave que alcanza cero en el borde del campo. */
    public static double radialFalloff(double distance, double radius) {
        if (radius <= 0.0D || distance >= radius) {
            return 0.0D;
        }
        double normalized = 1.0D - clamp(distance / radius, 0.0D, 1.0D);
        return normalized * normalized;
    }

    public static Vec3 safeDirection(Vec3 vector) {
        double lengthSqr = vector.lengthSqr();
        return lengthSqr < 1.0E-8D ? Vec3.ZERO : vector.scale(1.0D / Math.sqrt(lengthSqr));
    }
}
