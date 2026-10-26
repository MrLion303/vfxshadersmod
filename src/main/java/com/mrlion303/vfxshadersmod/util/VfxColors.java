package com.mrlion303.vfxshadersmod.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Colores sencillos admitidos por los comandos VFX. */
public final class VfxColors {
    private static final Map<String, Integer> COLORS = new LinkedHashMap<>();

    static {
        add(0x3478FF, "blue", "azul");
        add(0x58DFFF, "lightblue", "celeste", "light_blue");
        add(0x00FFFF, "cyan", "cian");
        add(0xFF3D48, "red", "rojo");
        add(0xFF6A32, "orange", "naranja");
        add(0xFFE84A, "yellow", "amarillo");
        add(0x38E879, "green", "verde");
        add(0xA85CFF, "purple", "morado", "violeta");
        add(0xFF72C6, "pink", "rosa");
        add(0xFFFFFF, "white", "blanco");
    }

    private VfxColors() {
    }

    private static void add(int rgb, String... names) {
        for (String name : names) {
            COLORS.put(name, rgb);
        }
    }

    /** Devuelve el RGB o null si el nombre no es válido. También admite #RRGGBB. */
    public static Integer parse(String input) {
        if (input == null) {
            return null;
        }
        String name = input.trim().toLowerCase(Locale.ROOT);
        if (name.startsWith("#")) {
            name = name.substring(1);
        }
        if (name.matches("[0-9a-f]{6}")) {
            try {
                return Integer.parseInt(name, 16);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return COLORS.get(name);
    }

    public static int parseOrDefault(String input, int fallback) {
        Integer result = parse(input);
        return result == null ? fallback : result;
    }

    public static List<String> names() {
        return List.copyOf(COLORS.keySet());
    }

    public static String describeOptions() {
        return "Colores: azul, celeste, cian, rojo, naranja, amarillo, verde, morado, rosa y blanco; también acepta #RRGGBB.";
    }
}
