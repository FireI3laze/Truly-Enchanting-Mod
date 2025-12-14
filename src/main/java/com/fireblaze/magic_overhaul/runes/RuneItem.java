package com.fireblaze.magic_overhaul.runes;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class RuneItem extends Item {

    public static final String RUNE_ID_TAG = "RuneId";

    public RuneItem() {
        super(new Item.Properties().stacksTo(1));
    }

    public static void setRune(ItemStack stack, ResourceLocation runeId) {
        stack.getOrCreateTag().putString(RUNE_ID_TAG, runeId.toString());
    }

    public static RuneDefinition getRune(ItemStack stack) {
        if (!stack.hasTag()) return null;
        String id = stack.getTag().getString(RUNE_ID_TAG);
        return RuneRegistry.get(ResourceLocation.tryParse(id));
    }
}

