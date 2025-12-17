package com.fireblaze.truly_enchanting.registry;

import com.fireblaze.truly_enchanting.util.Registration;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockItems {
    public static final RegistryObject<BlockItem> MONOLITH =
            Registration.ITEMS.register("monolith",
                    () -> new BlockItem(ModBlocks.MONOLITH.get(), new Item.Properties()));

    public static final RegistryObject<Item> ARCANE_ENCHANTING_TABLE_ITEM =
            Registration.ITEMS.register("arcane_enchanting_table",
                    () -> new BlockItem(ModBlocks.ARCANE_ENCHANTING_TABLE.get(), new Item.Properties()));
    public static void register() {}
}
