package com.kcbranding.keycloak.config;

/** Small colour helpers for deriving shades of hex colours. */
final class ColorUtil {

    private ColorUtil() {
    }

    /**
     * Darkens a hex colour by the given fraction (0..1). Non-hex colours (rgb(), hsl(), names) are
     * returned unchanged because deriving them is not worth the complexity.
     */
    static String darken(String color, double amount) {
        int[] rgb = parseHex(color);
        if (rgb == null) {
            return color;
        }
        StringBuilder out = new StringBuilder("#");
        for (int channel : rgb) {
            int value = (int) Math.round(channel * (1 - amount));
            out.append(String.format("%02x", Math.max(0, Math.min(255, value))));
        }
        return out.toString();
    }

    private static int[] parseHex(String color) {
        if (color == null || !color.startsWith("#")) {
            return null;
        }
        String hex = color.substring(1);
        if (hex.length() == 3 || hex.length() == 4) {
            hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
        } else if (hex.length() == 8) {
            hex = hex.substring(0, 6);
        }
        if (hex.length() != 6) {
            return null;
        }
        try {
            int value = Integer.parseInt(hex, 16);
            return new int[] {(value >> 16) & 0xff, (value >> 8) & 0xff, value & 0xff};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
