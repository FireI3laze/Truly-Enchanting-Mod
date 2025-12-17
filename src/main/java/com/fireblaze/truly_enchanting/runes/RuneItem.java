package com.fireblaze.truly_enchanting.runes;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class RuneItem extends Item {

    public RuneItem() {
        super(new Item.Properties().stacksTo(1));
    }
    @Override
    public Component getName(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("rune_id")) {
            ResourceLocation id = ResourceLocation.tryParse(stack.getTag().getString("rune_id"));
            RuneDefinition rune = RuneLoader.getRuneDefinition(id.toString());

            if (rune != null) {
                // Anzeige direkt aus dem displayName
                return Component.literal(rune.displayName)
                        .withStyle(style -> style.withColor(TextColor.fromRgb(rune.baseColor)));
            }
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                Level level,
                                List<Component> tooltip,
                                TooltipFlag flag) {

        if (stack.hasTag() && stack.getTag().contains("rune_id")) {
            ResourceLocation id = ResourceLocation.tryParse(stack.getTag().getString("rune_id"));
            RuneDefinition rune = RuneLoader.getRuneDefinition(id.toString());

            if (rune != null) {
                // Beschreibung
                tooltip.add(
                        Component.literal("Insert into a monolith to unleash magical powers")
                                .withStyle(ChatFormatting.GRAY)
                );
            }
        } else {
            tooltip.add(
                    Component.literal("Rune has no valid NBT")
                            .withStyle(ChatFormatting.GRAY)
            );
        }

        super.appendHoverText(stack, level, tooltip, flag);
    }

}

