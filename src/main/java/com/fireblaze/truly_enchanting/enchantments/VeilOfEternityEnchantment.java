package com.fireblaze.truly_enchanting.enchantments;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.ItemStack;

public class VeilOfEternityEnchantment extends Enchantment {

    public VeilOfEternityEnchantment(Rarity rarity, EnchantmentCategory category, EquipmentSlot... slots) {
        super(rarity, category, slots);
    }

    @Override
    public boolean isTreasureOnly() {
        return true; // ob die Verzauberung nur als Schatz verfügbar ist
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        // z.B. nur auf Werkzeuge und Waffen
        return super.canApplyAtEnchantingTable(stack);
    }

    @Override
    public int getMaxLevel() {
        return 1; // nur eine Stufe
    }
}
