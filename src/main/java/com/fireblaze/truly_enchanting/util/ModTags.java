package com.fireblaze.truly_enchanting.util;

import com.fireblaze.truly_enchanting.TrulyEnchanting;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModTags {

    public static class Items {
        public static final TagKey<Item> RUNES =
                TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TrulyEnchanting.MODID, "runes"));
    }
}
