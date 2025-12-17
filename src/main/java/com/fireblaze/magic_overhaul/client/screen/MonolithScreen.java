package com.fireblaze.magic_overhaul.client.screen;

import com.fireblaze.magic_overhaul.MagicOverhaul;
import com.fireblaze.magic_overhaul.blockentity.EnchantingTable.MagicAccumulator;
import com.fireblaze.magic_overhaul.blockentity.MonolithBlockEntity;
import com.fireblaze.magic_overhaul.client.color.RuneColorTheme;
import com.fireblaze.magic_overhaul.client.screen.utils.BlocklistScreen;
import com.fireblaze.magic_overhaul.client.screen.utils.ScreenController;
import com.fireblaze.magic_overhaul.client.screen.utils.ScreenSide;
import com.fireblaze.magic_overhaul.menu.MonolithMenu;
import com.fireblaze.magic_overhaul.runes.RuneDefinition;
import com.fireblaze.magic_overhaul.util.MagicSourceBlocks;
import com.fireblaze.magic_overhaul.util.MagicSourceBlockTags;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class MonolithScreen extends AbstractContainerScreen<MonolithMenu> {

    public MonolithScreen(MonolithMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    private BlocklistScreen blocklistScreen;
    private final int blockListWidth = 140;

    @Override
    protected void init() {
        super.init();

        MagicAccumulator acc = menu.monolith.getMagicAccumulator();
        Map<Block, MagicSourceBlocks> blockPalette = menu.currentRune.blockMap;
        Map<TagKey<Block>, MagicSourceBlockTags> tagPalette = menu.currentRune.blockTagsMap;

        // Neuen Controller anlegen
        ScreenController controller = new ScreenController(
                leftPos,
                topPos,
                imageWidth,
                imageHeight,
                rowHeight,
                blockListWidth,
                ScreenSide.LEFT
        );

        blocklistScreen = new BlocklistScreen(
                controller.getListX(),
                controller.getListY(),
                controller.getListWidth(),
                imageHeight,
                rowHeight,
                font,
                acc,
                blockPalette,
                tagPalette
        );
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private int scrollOffset = 0;
    private final int rowHeight = 18;
    private int blockScrollOffset = 0;

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        MonolithBlockEntity monolith = menu.monolith;
        if (monolith == null) return;
        MagicAccumulator magicAccumulator = menu.monolith.getMagicAccumulator();

        int magicPower = magicAccumulator.getCurrentMagicPowerIncreaseRate();

        RuneDefinition rune = menu.currentRune;
        if (rune == null || rune.enchantments.isEmpty()) return;

        int startY = 84;
        int barMaxWidth = 60;
        int barHeight = 6;
        blocklistScreen.render(graphics, mouseX, mouseY);

        for (Enchantment ench : rune.enchantments) {
            int enchX = x + 100;
            int enchY = y + startY - 3;

            // Hintergrund
            graphics.fill(enchX, enchY, enchX + barMaxWidth, enchY + barHeight, 0xFF555555);

            // Maximaler MagicCost
            int maxCost = com.fireblaze.magic_overhaul.util.MagicCostCalculator.calculateMagicRequirement(ench, ench.getMaxLevel());

            // Voller Gradient über die gesamte Bar
            RuneColorTheme theme = rune.colorTheme;
            int startColor = theme.secondary;
            int endColor = theme.accent;

            for (int i = 0; i < barMaxWidth; i++) {
                float t = (float) i / (barMaxWidth - 1);
                int color = interpolateColor(startColor, endColor, t);
                graphics.fill(enchX + i, enchY, enchX + i + 1, enchY + barHeight, color);
            }

            // Overlay für ungenutzte Magie (grau)
            int filled = (int) (Math.min((float) magicPower / maxCost, 1.0f) * barMaxWidth);
            if (filled < barMaxWidth) {
                graphics.fill(enchX + filled, enchY, enchX + barMaxWidth, enchY + barHeight, 0xFF555555);
            }

            int currentMaxLevel = 0;
            boolean tooltipShown = false;

            // Level-Trennlinien
            for (int lvl = 1; lvl <= ench.getMaxLevel(); lvl++) {
                int required = com.fireblaze.magic_overhaul.util.MagicCostCalculator.calculateMagicRequirement(ench, lvl);
                float ratio = Math.min((float) required / maxCost, 1.0f);
                int posX = enchX + (int) (ratio * barMaxWidth);
                int color = required <= magicPower ? 0xFF000001 : 0xFF000000;
                graphics.fill(posX, enchY, posX + 1, enchY + barHeight, color);

                // Hover-Check für Level-Trennlinie
                if (mouseX >= posX && mouseX <= posX + 1 && mouseY >= enchY && mouseY <= enchY + barHeight) {
                    MutableComponent name = ench.getFullname(lvl).copy(); // Kopie erzeugen
                    name.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF)));
                    graphics.renderComponentTooltip(
                            font,
                            List.of(
                                    name,
                                    Component.literal((int) Math.min(magicPower, required) + " / " + required)
                            ),
                            mouseX,
                            mouseY
                    );
                    tooltipShown = true; // Tooltip wurde angezeigt
                    break; // keine weiteren Level-Trennlinien prüfen
                }

                if (magicPower >= required) {
                    currentMaxLevel = lvl;
                }
            }

            // Hover-Check für die Bar selbst (nur wenn noch kein Tooltip angezeigt wurde)
            if (!tooltipShown && mouseX >= enchX && mouseX <= enchX + barMaxWidth && mouseY >= enchY && mouseY <= enchY + barHeight) {
                if (currentMaxLevel == 0) {
                    MutableComponent name = ench.getFullname(currentMaxLevel).copy();
                    name.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF)));
                    graphics.renderComponentTooltip(
                            font,
                            List.of(
                                    Component.translatable(ench.getDescriptionId()),
                                    Component.literal((int) magicPower + " / " + maxCost)
                            ),
                            mouseX,
                            mouseY
                    );
                } else {
                    MutableComponent name = ench.getFullname(currentMaxLevel).copy();
                    name.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF)));
                    graphics.renderComponentTooltip(
                            font,
                            List.of(
                                    name,
                                    Component.literal(magicPower + " / " + maxCost)
                            ),
                            mouseX,
                            mouseY
                    );
                }
            }

            startY += 15;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (blocklistScreen.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MagicOverhaul.MODID, "textures/gui/monolith_gui.png");


    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, "Monolith", 4, 6, 0x404040, false);

        RuneDefinition rune = menu.currentRune;
        if (rune != null && !rune.enchantments.isEmpty()) {
            int y = 70; // Start Y Position

            // Überschrift mit Gradient
            drawGradientString(graphics, "Rune Enchantments", 4, 56, rune.colorTheme.secondary, rune.colorTheme.accent, 0);

            y += 10;
            for (Enchantment ench : rune.enchantments) {
                drawGradientString(graphics, Component.translatable(ench.getDescriptionId()).getString(), 8, y,
                        rune.colorTheme.secondary, rune.colorTheme.accent, 15);
                y += 15;
            }
        }
    }

    private void drawGradientString(GuiGraphics graphics, String text, int x, int y, int startColor, int endColor, int maxLength) {
        // Optionales Abschneiden
        if (maxLength > 0 && text.length() > maxLength) {
            int cutLength = Math.max(0, maxLength - 3); // Platz für "..."
            text = text.substring(0, cutLength) + "...";
        }

        int length = text.length();
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            float t = 1.0f - ((float) i / Math.max(length - 1, 1)); // Gradient umgedreht
            int color = interpolateColor(startColor, endColor, t);
            graphics.drawString(font, String.valueOf(c), x, y, color, false);
            x += font.width(String.valueOf(c)); // Cursor nach rechts verschieben
        }
    }


    private static int interpolateColor(int startColor, int endColor, float t) {
        int a1 = (startColor >> 24) & 0xFF;
        int r1 = (startColor >> 16) & 0xFF;
        int g1 = (startColor >> 8) & 0xFF;
        int b1 = startColor & 0xFF;

        int a2 = (endColor >> 24) & 0xFF;
        int r2 = (endColor >> 16) & 0xFF;
        int g2 = (endColor >> 8) & 0xFF;
        int b2 = endColor & 0xFF;

        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public void updateRune(RuneDefinition rune) {
        if (rune == null) return;

        MagicAccumulator acc = menu.monolith.getMagicAccumulator();
        Map<Block, MagicSourceBlocks> blockPalette = rune.blockMap;
        Map<TagKey<Block>, MagicSourceBlockTags> tagPalette = rune.blockTagsMap;

        blocklistScreen = new BlocklistScreen(
                blocklistScreen.left,
                blocklistScreen.top,
                blocklistScreen.blockListWidth,
                blocklistScreen.imageHeight,
                rowHeight,
                font,
                acc,
                blockPalette,
                tagPalette
        );
    }
}
