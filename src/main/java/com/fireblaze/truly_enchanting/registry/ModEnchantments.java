package com.fireblaze.truly_enchanting.registry;

import com.fireblaze.truly_enchanting.TrulyEnchanting;
import com.fireblaze.truly_enchanting.enchantments.VeilOfEternityEnchantment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEnchantments {

    // DeferredRegister für Enchantments
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, TrulyEnchanting.MODID);

    // Veil of Eternity registrieren
    public static final RegistryObject<Enchantment> VEIL_OF_ETERNITY =
            ENCHANTMENTS.register("veil_of_eternity", () ->
                    new VeilOfEternityEnchantment(
                            Enchantment.Rarity.VERY_RARE,           // Seltenheit
                            EnchantmentCategory.BREAKABLE,     // Kategorie: Werkzeuge/Waffen
                            EquipmentSlot.MAINHAND,            // Slots: hier Haupt-Hand
                            EquipmentSlot.OFFHAND              // optional weitere Slots
                    )
            );
}
