package com.fireblaze.truly_enchanting.registry;

import com.fireblaze.truly_enchanting.item.MagicEssenceItem;
import com.fireblaze.truly_enchanting.item.WandItem;
import com.fireblaze.truly_enchanting.util.Registration;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final RegistryObject<Item> CELESTIAL_CORE =
            Registration.ITEMS.register("celestial_core",
                    () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MAGIC_ESSENCE =
            Registration.ITEMS.register("magic_essence",
                    () -> new MagicEssenceItem(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> WAND =
            Registration.ITEMS.register("wand",
            () -> new WandItem(new Item.Properties().stacksTo(1)));

    public static void register() {}
}