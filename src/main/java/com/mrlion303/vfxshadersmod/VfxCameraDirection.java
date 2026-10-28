package com.mrlion303.vfxshadersmod;

import java.util.Locale;
import java.util.Optional;

/** Convierte direcciones en español a la rotación de cámara de Minecraft. */
public final class VfxCameraDirection {
    private VfxCameraDirection() {}
    public record Angles(float yaw, float pitch) {}

    public static Optional<Angles> parse(String input) {
        if (input == null || input.isBlank()) return Optional.empty();
        String normalized = input.toLowerCase(Locale.ROOT)
                .replace("á", "a").replace("é", "e").replace("í", "i")
                .replace("ó", "o").replace("ú", "u");
        String[] words = normalized.split("[,\\s]+");
        double x = 0.0D, z = 0.0D;
        boolean up = false, down = false;
        for (String word : words) {
            switch (word) {
                case "", "mirar", "hacia", "y" -> { }
                case "arriba", "up" -> up = true;
                case "abajo", "down" -> down = true;
                case "norte", "north" -> z -= 1.0D;
                case "sur", "south" -> z += 1.0D;
                case "este", "east" -> x += 1.0D;
                case "oeste", "west" -> x -= 1.0D;
                case "noreste", "northeast" -> { x += 1.0D; z -= 1.0D; }
                case "noroeste", "northwest" -> { x -= 1.0D; z -= 1.0D; }
                case "sureste", "southeast" -> { x += 1.0D; z += 1.0D; }
                case "suroeste", "southwest" -> { x -= 1.0D; z += 1.0D; }
                default -> { return Optional.empty(); }
            }
        }
        if (up && down) return Optional.empty();
        x = Math.signum(x);
        z = Math.signum(z);
        boolean horizontal = x != 0.0D || z != 0.0D;
        if (!horizontal && !up && !down) return Optional.empty();
        float yaw = horizontal ? (float) Math.toDegrees(Math.atan2(-x, z)) : 0.0F;
        float pitch = up ? (horizontal ? -35.0F : -90.0F)
                : down ? (horizontal ? 35.0F : 90.0F) : 0.0F;
        return Optional.of(new Angles(yaw, pitch));
    }
}
