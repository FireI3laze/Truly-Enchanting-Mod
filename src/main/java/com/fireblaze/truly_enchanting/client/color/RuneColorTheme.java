package com.fireblaze.truly_enchanting.client.color;

import java.awt.Color;

public class RuneColorTheme {
    public final int primary;   // z. B. Überschrift
    public final int secondary; // z. B. Balken
    public final int accent;    // z. B. Gradients / Highlights

    public RuneColorTheme(int primary, int secondary, int accent) {
        this.primary = primary;
        this.secondary = secondary;
        this.accent = accent;
    }

    // Hilfsmethode: erzeugt Theme aus einer Basisfarbe
    public static RuneColorTheme fromBaseColor(int baseColor) {
        float[] hsb = Color.RGBtoHSB(
                (baseColor >> 16) & 0xFF,
                (baseColor >> 8) & 0xFF,
                baseColor & 0xFF,
                null
        );

        float hue = hsb[0];
        float sat = hsb[1];
        float bri = hsb[2];

        int primary = baseColor;
        int secondary = Color.HSBtoRGB(hue, Math.max(0, sat - 0.2f), Math.min(1, bri + 0.2f));
        int accent = Color.HSBtoRGB((hue + 0.05f) % 1f, sat, Math.min(1, bri + 0.5f));

        return new RuneColorTheme(primary, secondary, accent);
    }
}
