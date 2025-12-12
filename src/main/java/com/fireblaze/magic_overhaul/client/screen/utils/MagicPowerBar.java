package com.fireblaze.magic_overhaul.client.screen.utils;

import com.fireblaze.magic_overhaul.client.ClientConfig;
import com.fireblaze.magic_overhaul.util.PlayerSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class MagicPowerBar {

    private final ScreenController controller;
    private final Font font;

    // --- Daten ---
    private float accumulatedMagicPower;
    private float magicPowerCapPerPlayerSoft;
    private int magicPowerCapPerPlayerHard;
    private int currentMagicPowerIncreaseRate;
    // Farben-Liste als Feld
    private final List<Integer> particleColors = List.of(
            0xAAFFFFFF, // Weiß
            0xAAFFFFFF, // Weiß
            0xAA8B3BDC, // Hell-Lila
            0xAAFFD700  // Optional: Gold oder andere Effekte
    );

    // --- Layout ---
    private final int width;        // feste Breite der Bar
    private final int height = 20;  // Höhe der Bar
    private final int margin = 4;
    private boolean motion = true;
    private boolean sparkle = true;
    private float plannedConsumption = 0f;
    private final ClientConfig cfg = ClientConfig.get();



    public MagicPowerBar(ScreenController controller, Font font, int width,
                         float magicPowerCapPerPlayerSoft,
                         int magicPowerCapPerPlayerHard) {
        this.controller = controller;
        this.font = font;
        this.width = width;
        this.magicPowerCapPerPlayerSoft = magicPowerCapPerPlayerSoft;
        this.magicPowerCapPerPlayerHard = magicPowerCapPerPlayerHard;
    }

    // --- Setter ---
    public void setAccumulatedMagicPower(float accumulatedMagicPower) {
        this.accumulatedMagicPower = accumulatedMagicPower;
    }

    public void setMagicPowerCapPerPlayerSoft(float magicPowerCapPerPlayerSoft) {
        this.magicPowerCapPerPlayerSoft = magicPowerCapPerPlayerSoft;
    }

    public void setMagicPowerCapPerPlayerHard(int magicPowerCapPerPlayerHard) {
        this.magicPowerCapPerPlayerHard = magicPowerCapPerPlayerHard;
    }

    public void setCurrentMagicPowerIncreaseRate(int rate) {
        this.currentMagicPowerIncreaseRate = rate;
    }
    public void setMotion(boolean motion) {
        this.motion = motion;
    }

    public void setSparkle(boolean sparkle) {
        this.sparkle = sparkle;
    }
    public void setPlannedConsumption(float amount) {
        this.plannedConsumption = amount;
    }

    // --- Rendering ---
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {

        // Position der Überschrift
        int x = controller.getListX();
        int y = controller.getListY();

// Basis-Text
        String prefix = "Magical Power " + (int) accumulatedMagicPower;

// Zeichne nur den Basis-Text, wenn keine geplante Reduktion existiert
        graphics.drawString(font, prefix, x, y, 0xFFFFFF, false);

// Falls geplant, zeichne die Zahl in Klammern
        if (plannedConsumption != 0) {
            float remaining = accumulatedMagicPower - plannedConsumption;
            String number = "(" + (int) remaining + ")";

            // Farbe: rot, wenn negativ
            int color = remaining < 0 ? 0xCCAA0000 : 0xFFFFFF;

            // Berechne Breite des Basis-Texts für die Positionierung
            int prefixWidth = font.width(prefix) + 2; // +2 für kleinen Abstand

            graphics.drawString(font, number, x + prefixWidth, y, color, false);
        }


        int barY = y + font.lineHeight + margin;

        // Hintergrund: Hard Cap (grau)
        graphics.fill(x, barY, x + width, barY + height, 0x44666666);

        // --- Berechnungen ---
        float fillRatio = Math.min(accumulatedMagicPower / magicPowerCapPerPlayerHard, 1f);
        int fillWidth = (int) (width * fillRatio);

        int softWidth = (int) ((magicPowerCapPerPlayerSoft / magicPowerCapPerPlayerHard) * width);

        // Neue: geplanter Verbrauch
        float plannedRatio = Math.min(plannedConsumption / magicPowerCapPerPlayerHard, 1f);
        int plannedWidth = (int)(width * plannedRatio);

        // --- Zeichne geplante Reduktion als halbtransparent roten Bereich ---
        if (plannedWidth > 0) {
            graphics.fill(controller.getListX() + fillWidth - plannedWidth,
                    controller.getListY() + font.lineHeight + margin,
                    controller.getListX() + fillWidth,
                    controller.getListY() + font.lineHeight + margin + height,
                    0x44AA0000); // halbtransparent rot
        }


        // --- 1. Lila Füllung bis Soft Cap mit Animation ---
        int purpleWidth = Math.min(fillWidth, softWidth);
        double horizontalSpeed = 0.5;
        int timeOffset = (int) ((System.currentTimeMillis() / 100) % 1000000);
        int lineSpacing = 4;

        for (int i = 0; i < purpleWidth; i += 1) {
            for (int yMagic = 0; yMagic < height; yMagic += lineSpacing) {
                // 180°-Drehung
                int px = x + purpleWidth - i - 1; // invertierte X-Position
                int py;
                if (motion)
                    py = barY + height - (yMagic + (int)(i * horizontalSpeed) + timeOffset) % height - 2;
                else
                    py = barY + height - (yMagic + (int)(i * horizontalSpeed)) % height - 2;

                int color2 = 0xFF6A0DAD; // Basis-Lila
                if ((i + yMagic + timeOffset) % 16 < 8) {
                    color2 = 0xFF8B3BDC; // hellere Linie für Fluss-Effekt
                }

                graphics.fill(px, py, px + 1, py + 2, color2); // kleine vertikale Balken
            }
        }

        // --- Zeichne geplante Reduktion als halbtransparent roten Bereich ---
        if (plannedWidth > 0) {
            graphics.fill(controller.getListX() + fillWidth - plannedWidth,
                    controller.getListY() + font.lineHeight + margin,
                    controller.getListX() + fillWidth,
                    controller.getListY() + font.lineHeight + margin + height,
                    0x11AA0000); // halbtransparent rot
        }

        if (sparkle) {
            float fillRatioFull = Math.min(accumulatedMagicPower / magicPowerCapPerPlayerHard, 1f);

            // --- Neue Partikel zufällig erzeugen ---
            if (purpleWidth > 0 && particles.size() < (int) 30 * fillRatioFull) { // Maximal 30 Partikel gleichzeitig
                int px = x + random.nextInt(purpleWidth);  // nur erzeugen, wenn purpleWidth > 0
                int py = barY + random.nextInt(height);
                int life = 20 + random.nextInt(20); // 20-40 Frames

                int color2 = particleColors.get(random.nextInt(particleColors.size()));
                particles.add(new MagicParticle(px, py, life, color2));
            }

            // --- Partikel rendern ---
            Iterator<MagicParticle> it = particles.iterator();
            while (it.hasNext()) {
                MagicParticle p = it.next();

                // Alpha je nach Lebensdauer (fading)
                int alpha = (int) (255 * (p.life / 40.0f));
                int color2 = (p.color & 0x00FFFFFF) | (alpha << 24);

                graphics.fill(p.x, p.y, p.x + 1, p.y + 1, color2);

                p.life--;
                if (p.life <= 0) it.remove();
            }
        }

        // --- 2. Schwarze Streifen IMMER sichtbar: soft → hard ---
        int stripeStart = softWidth;
        int stripeEnd = width;
        int thickness = 2;      // Dicke der Linie
        int spacing = 6;        // Abstand zwischen den Linien

        int markerX = x + softWidth;

        graphics.fill(
                markerX,              // Start X
                barY,                 // Start Y
                markerX + 1,          // 1 Pixel breit
                barY + height,        // voller Bar-Höhenbereich
                0x55000000            // Weiß
        );

        // vertical
        for (int offsetY = 0; offsetY < height; offsetY += spacing) {
            for (int dx = 0; dx <= stripeEnd - stripeStart; dx++) {
                int px = x + stripeStart + dx;
                int py = barY + offsetY + dx; // diagonale Verschiebung
                if (py >= barY && py < barY + height) {
                    graphics.fill(px, py, px + thickness, py + 1, 0x55000000);
                }
            }
        }

        // horizontal
        for (int i = stripeStart + spacing; i < stripeEnd; i += spacing) {

            for (int dy = 0; dy < height; dy++) {
                int px = x + i + dy;  // diagonale Verschiebung
                int py = barY + dy;

                if (px >= x + stripeStart && px <= x + stripeEnd) {
                    graphics.fill(px, py, px + thickness, py + 1, 0x55000000);
                }
            }
        }


        // --- 3. Rate Info ---
        String rateText = "Rate: " + currentMagicPowerIncreaseRate + " / hour";
        //graphics.drawString(font, rateText, x, barY + height + margin, 0xFFFFFF, false); //todo remove?

        // --- 4. Tooltip abhängig vom Maus-Bereich ---
        if (mouseX >= x && mouseX <= x + width &&
                mouseY >= barY && mouseY <= barY + height) {

            List<Component> tooltip = new ArrayList<>();

            int relativeX = mouseX - x;

            if (relativeX <= softWidth) {
                // Accumulation Bereich, zwei Zeilen
                tooltip.add(Component.literal(
                        String.format("%.1f / %.1f Magical Power Accumulated",
                                accumulatedMagicPower,
                                magicPowerCapPerPlayerSoft)
                ));
                tooltip.add(Component.literal(
                        String.format("%.0f Magical Power per hour",
                                (float) currentMagicPowerIncreaseRate)
                ));
            } else {
                // Hard Cap Bereich
                tooltip.add(Component.literal(
                        (int) magicPowerCapPerPlayerSoft + " / " + magicPowerCapPerPlayerHard + " Magic Power Cap Unlocked"
                ));
            }
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    private final List<MagicParticle> particles = new ArrayList<>();
    private final Random random = new Random();

    private static class MagicParticle {
        int x, y;
        int life;
        int color;

        MagicParticle(int x, int y, int life, int color) {
            this.x = x;
            this.y = y;
            this.life = life;
            this.color = color;
        }
    }
    public boolean handleMouseClick(double mouseX, double mouseY, int button) {
        int x = controller.getListX();
        int y = controller.getListY();
        int barY = y + font.lineHeight + margin;

        if (mouseX >= x && mouseX <= x + width &&
                mouseY >= barY && mouseY <= barY + height) {

            if (button == 0) { // Linksklick
                motion = !motion;

            } else if (button == 1) { // Rechtsklick
                sparkle = !sparkle;
            }

            assert Minecraft.getInstance().player != null;
            cfg.magicBarMotion = motion;
            ClientConfig.save();
            cfg.magicBarSparkle = sparkle;
            ClientConfig.save();
            return true; // Klick auf die Bar wurde verarbeitet
        }

        return false; // Klick außerhalb der Bar
    }
}
