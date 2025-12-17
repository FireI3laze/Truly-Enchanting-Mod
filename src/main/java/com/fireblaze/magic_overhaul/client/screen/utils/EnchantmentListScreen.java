package com.fireblaze.magic_overhaul.client.screen.utils;

import com.fireblaze.magic_overhaul.menu.ArcaneEnchantingTableMenu;
import com.fireblaze.magic_overhaul.network.Network;
import com.fireblaze.magic_overhaul.network.SetEnchantSelectionPacket;
import com.fireblaze.magic_overhaul.util.MagicCostCalculator;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.fireblaze.magic_overhaul.client.screen.utils.BlocklistScreen.getRegistrySortId;

/**
 * EnchantmentListScreen
 *
 * Regeln implementiert nach Vorgaben:
 * - Variante A für Rechtsklick (erste Auswahl = minSelectable, weitere Klicks +1, wrap auf minSelectable)
 * - Scroll über ein ausgewähltes Enchantment ändert dessen Stufe (caps, kein wrap)
 * - Scroll sonst scrollt die Liste
 * - Sichtbarkeitsregeln wie beschrieben
 */
public class EnchantmentListScreen {

    private final ArcaneEnchantingTableMenu menu;
    private final Font font;
    private final ScreenController controller;

    private int scrollOffset = 0;
    private final int rowHeight;
    private final int listWidth;

    public EnchantmentListScreen(ArcaneEnchantingTableMenu menu, Font font, ScreenController controller, int listWidthOverride) {
        this.menu = menu;
        this.font = font;
        this.controller = controller;
        this.rowHeight = controller.getRowHeight();
        this.listWidth = listWidthOverride;
    }

    // Convenience constructor if you used fixed width earlier
    public EnchantmentListScreen(ArcaneEnchantingTableMenu menu, Font font, ScreenController controller) {
        this(menu, font, controller, 130);
    }

    // ---------------------------
    // Rendering
    // ---------------------------
    public void render(GuiGraphics gui, int mouseX, int mouseY) {
        var visibleEntries = computeVisibleEntries();
        visibleEntries.sort(Comparator.comparingInt(BlocklistScreen::getRegistrySortId));

        int listX = controller.getListX();
        int listY = controller.getListY();
        int visibleRows = controller.getVisibleRows();

        int totalRows = visibleEntries.size();
        if (scrollOffset > totalRows - visibleRows) scrollOffset = Math.max(totalRows - visibleRows, 0);

        if (visibleEntries.isEmpty()) {
            int areaWidth = listWidth - 8; // kleiner Padding-Rand
            int areaHeight = visibleRows * rowHeight;

            Component text = Component.literal(
                    """
                            No Enchantments unlocked.

                            Use a wand to bind yourself to the table and link it with monoliths to unlock enchantments."""
            );

            // Automatischer Zeilenumbruch basierend auf Controller-Breite
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(text, areaWidth);

            int totalTextHeight = lines.size() * font.lineHeight;
            int startY = listY + (areaHeight - totalTextHeight) / 2;

            int y = startY;
            for (var line : lines) {
                int lineWidth = font.width(line);
                int x = listX + (listWidth - lineWidth) / 2;

                gui.drawString(
                        font,
                        line,
                        x,
                        y,
                        0xAAAAAA,
                        false
                );
                y += font.lineHeight;
            }
            return;
        }

        for (int visibleIndex = 0; visibleIndex < visibleRows; visibleIndex++) {
            int entryIndex = visibleIndex + scrollOffset;
            if (entryIndex >= totalRows) break;

            var entry = visibleEntries.get(entryIndex);
            Enchantment ench = entry.getKey();
            int maxLevel = entry.getValue();

            Map<Enchantment, Integer> selectedMap = menu.getSelectedEnchantments();
            int selectedLevel = selectedMap.getOrDefault(ench, 0);

            ItemStack targetItem = menu.getItemInSlot0();
            int currentOnItem = (targetItem == null) ? 0 : targetItem.getEnchantmentLevel(ench);

            int y = listY + visibleIndex * rowHeight;

            // Background
            int bgColor = selectedLevel > 0 ? 0xA028143C : 0x44000000;
            gui.fill(listX, y, listX + listWidth, y + rowHeight, bgColor);

            // Hover border
            if (isMouseOver(mouseX, mouseY, listX, y, listWidth, rowHeight)) {
                int borderColor = 0x5FAA0FFF;
                int thickness = 1;
                gui.fill(listX, y, listX + listWidth, y + thickness, borderColor);
                gui.fill(listX, y + rowHeight - thickness, listX + listWidth, y + rowHeight, borderColor);
                gui.fill(listX, y, listX + thickness, y + rowHeight, borderColor);
                gui.fill(listX + listWidth - thickness, y, listX + listWidth, y + rowHeight, borderColor);
            }

            // Name
            String name;
            name = ench.getFullname(selectedMap.getOrDefault(ench, maxLevel)).getString();
            gui.drawString(font, name, listX + 20, y + 2, 0xFFFFFF);

            // Icon
            ItemStack bookStack = new ItemStack(Items.ENCHANTED_BOOK);
            EnchantedBookItem.addEnchantment(bookStack, new EnchantmentInstance(ench, Math.min(currentOnItem + 1, Math.max(1, maxLevel))));
            gui.renderItem(bookStack, listX + 2, y + 2);

            // Slider: only when selected and has multiple levels
            if (selectedLevel > 0 && maxLevel > 1) {
                renderSlider(gui, ench, selectedLevel, maxLevel, currentOnItem, listX + 20, y + 12);
            }

            // Tooltip when hovering on icon area
            if (mouseX >= listX && mouseX < listX + 16 && mouseY >= y && mouseY < y + 16) {
                Component costLine;
                if (selectedLevel == 0) costLine = Component.literal("Cost: 0");
                else {
                    costLine = Component.literal(
                            "Cost: " + MagicCostCalculator.calculateMagicRequirement(ench, selectedLevel)
                    );
                }

                List<Component> tooltip = List.of(
                        Component.literal(name),
                        Component.literal(
                                "Selected: " + Math.min(selectedLevel, maxLevel) + " / Max: " + maxLevel
                        ),
                        costLine
                );
                gui.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            }
        }
    }

    // ---------------------------
    // Slider rendering helper
    // ---------------------------
    private void renderSlider(GuiGraphics gui, Enchantment ench, int selectedLevel, int maxLevel, int currentOnItem, int x, int y) {
        int sliderWidth = 80;
        // blockedWidth = portion representing levels <= currentOnItem (darker purple)
        int blockedWidth = (int) (sliderWidth * (currentOnItem / (float) maxLevel));
        int filledWidth = (int) (sliderWidth * (selectedLevel / (float) maxLevel));

        // background
        gui.fill(x, y, x + sliderWidth, y + 6, 0xFF555555);
        // blocked (already present on item)
        if (blockedWidth > 0) gui.fill(x, y, x + blockedWidth, y + 6, 0x88462878);
        // selected range (lighter)
        if (filledWidth > blockedWidth) gui.fill(x + blockedWidth, y, x + filledWidth, y + 6, 0x889678C8);
    }

    // ---------------------------
    // Input handling
    // ---------------------------

    /**
     * mouseScrolled: returns true if event handled (either adjusted selected enchant level or scrolled list)
     * Behavior:
     * - If hovered entry is currently SELECTED and supports multiple levels -> change its level (cap at min/max, NO wrap)
     * - Else: if mouse is inside list area and there are more rows than visible -> scroll list
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int listX = controller.getListX();
        int listY = controller.getListY();
        int visibleRows = controller.getVisibleRows();

        var visibleEntries = computeVisibleEntries();
        int totalRows = visibleEntries.size();
        if (totalRows == 0) return false;

        // check if mouse is inside list area
        if (!(mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + visibleRows * rowHeight)) {
            return false;
        }

        int localIndex = (int) ((mouseY - listY) / rowHeight);
        int entryIndex = localIndex + scrollOffset;
        Map<Enchantment, Integer> selectedMap = menu.getSelectedEnchantments();

        // If we're hovering an actual entry
        if (entryIndex >= 0 && entryIndex < totalRows) {
            var entry = visibleEntries.get(entryIndex);
            Enchantment ench = entry.getKey();
            int maxLevel = entry.getValue();
            int currentOnItem = menu.getItemInSlot0().getEnchantmentLevel(ench);
            int selectedLevel = selectedMap.getOrDefault(ench, 0);

            // If this enchantment is selected and has multiple levels -> adjust its level
            if (selectedLevel > 0 && maxLevel > 1) {
                int minSelectable = Math.max(1, currentOnItem + 1);

                // delta > 0 = scroll up = increase
                if (delta > 0) {
                    selectedLevel = Math.min(selectedLevel + 1, maxLevel); // cap at max (no wrap)
                } else {
                    selectedLevel = Math.max(selectedLevel - 1, minSelectable); // cap at minSelectable
                }

                // write back & send packet if changed
                selectedMap.put(ench, selectedLevel);
                Network.sendToServer(new SetEnchantSelectionPacket(ench, selectedLevel));
                return true;
            }
        }

        // Otherwise: scroll the list (if more rows than visible)
        if (totalRows > visibleRows) {
            scrollOffset -= (delta > 0 ? 1 : -1);
            if (scrollOffset < 0) scrollOffset = 0;
            int maxScroll = totalRows - visibleRows;
            if (scrollOffset > maxScroll) scrollOffset = maxScroll;
            return true;
        }

        return false;
    }

    /**
     * mouseClicked: returns true if event handled
     *
     * Left click: toggle selection (select highest available; deselect if already selected)
     * Right click: if not selected -> set to minSelectable; if selected -> increment and wrap to minSelectable when exceeding max
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listX = controller.getListX();
        int listY = controller.getListY();
        int visibleRows = controller.getVisibleRows();

        var visibleEntries = computeVisibleEntries();
        int totalRows = visibleEntries.size();
        if (totalRows == 0) return false;

        if (!(mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + visibleRows * rowHeight)) {
            return false;
        }

        int localIndex = (int) ((mouseY - listY) / rowHeight);
        int entryIndex = localIndex + scrollOffset;

        if (entryIndex < 0 || entryIndex >= totalRows) return false;

        var entry = visibleEntries.get(entryIndex);
        Enchantment ench = entry.getKey();
        int maxLevel = entry.getValue();
        ItemStack targetItem = menu.getItemInSlot0();
        int currentOnItem = targetItem.getEnchantmentLevel(ench);

        // If item not present -> nothing selectable
        if (targetItem.isEmpty()) return false;

        // compute min selectable (strictly greater than currentOnItem)
        int minSelectable = Math.max(1, currentOnItem + 1);

        // ignore if the enchantment cannot be applied or already at max on item
        if (!ench.canEnchant(targetItem) || currentOnItem >= maxLevel) return false;

        Map<Enchantment, Integer> selectedMap = menu.getSelectedEnchantments();
        int currentSelected = selectedMap.getOrDefault(ench, 0);

        if (button == 0) { // left click
            if (currentSelected == 0) {
                int newLevel = maxLevel; // left click selects highest available
                // ensure not <= currentOnItem
                if (newLevel <= currentOnItem) newLevel = minSelectable;
                selectedMap.put(ench, newLevel);
                Network.sendToServer(new SetEnchantSelectionPacket(ench, newLevel));
            } else {
                // deselect
                selectedMap.put(ench, 0);
                Network.sendToServer(new SetEnchantSelectionPacket(ench, 0));
            }
            return true;
        } else if (button == 1) { // right click
            if (currentSelected == 0) {
                // first right-click selects the minimum selectable
                int newLevel = minSelectable;
                selectedMap.put(ench, newLevel);
                Network.sendToServer(new SetEnchantSelectionPacket(ench, newLevel));
            } else {
                // increment and wrap to minSelectable after max
                int newLevel = currentSelected + 1;
                if (newLevel > maxLevel) newLevel = minSelectable; // wrap
                selectedMap.put(ench, newLevel);
                Network.sendToServer(new SetEnchantSelectionPacket(ench, newLevel));
            }
            return true;
        }

        return false;
    }

    // ---------------------------
    // Helpers
    // ---------------------------

    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /**
     * Builds the list of visible entries according to rules:
     * - If no item in table -> show all unlocked (not selectable)
     * - If item present -> show enchantments that:
     *     * canEnchant(target) AND currentOnItem < maxLevel
     *     * If an entry is incompatible with a currently SELECTED enchantment, hide it,
     *       except if the incompatibility is caused by an enchantment that is already present on the item.
     */
    private List<Map.Entry<Enchantment, Integer>> computeVisibleEntries() {
        ItemStack targetItem = menu.getItemInSlot0();
        Map<Enchantment, Integer> unlocked = menu.getUnlockedEnchantments();
        Map<Enchantment, Integer> selectedMap = menu.getSelectedEnchantments();

        // Liste, die wir zurückgeben
        List<Map.Entry<Enchantment, Integer>> result = new java.util.ArrayList<>();

        // 1) ALLE AUSGEWÄHLTEN immer anzeigen – unabhängig von den Filtern
        for (var e : unlocked.entrySet()) {
            Enchantment ench = e.getKey();
            int sel = selectedMap.getOrDefault(ench, 0);
            if (sel > 0) {
                result.add(e); // immer ganz oben, immer sichtbar
            }
        }

        // Wenn kein Item → Rest der Liste anzeigen
        if (targetItem == null || targetItem.isEmpty()) {
            for (var e : unlocked.entrySet()) {
                if (!result.contains(e))
                    result.add(e);
            }
            return result;
        }

        // 2) Normale Einträge (nicht ausgewählte) filtern
        for (var entry : unlocked.entrySet()) {
            if (result.contains(entry)) continue; // ausgewählte haben wir schon

            Enchantment ench = entry.getKey();
            int maxLevel = entry.getValue();
            int currentOnItem = targetItem.getEnchantmentLevel(ench);

            // Ausschlusskriterien nur für NICHT-ausgewählte
            if (!ench.canEnchant(targetItem)) continue;
            if (currentOnItem >= maxLevel) continue;

            // Kompatibilität prüfen
            boolean compatible = true;
            for (var sel : selectedMap.entrySet()) {
                if (sel.getValue() <= 0) continue;
                Enchantment selEnch = sel.getKey();

                if (!ench.isCompatibleWith(selEnch)) {
                    // Der einzige erlaubte Fall: selEnch ist schon auf dem Item
                    if (targetItem.getEnchantmentLevel(selEnch) == 0) {
                        compatible = false;
                        break;
                    }
                }
            }

            if (compatible) {
                result.add(entry);
            }
        }

        return result;
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public void setScrollOffset(int offset) {
        this.scrollOffset = Math.max(0, offset);
    }

    public int calculateSelectedMagicCost() {
        int totalCost = 0;
        Map<Enchantment, Integer> selected = menu.getSelectedEnchantments();
        for (Map.Entry<Enchantment, Integer> entry : selected.entrySet()) {
            totalCost += MagicCostCalculator.calculateMagicRequirement(entry.getKey(), entry.getValue());
        }
        return totalCost;
    }
}
