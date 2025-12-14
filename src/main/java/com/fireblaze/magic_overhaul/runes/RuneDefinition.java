package com.fireblaze.magic_overhaul.runes;

import com.fireblaze.magic_overhaul.client.color.RuneColorTheme;
import com.fireblaze.magic_overhaul.util.MagicSourceBlockTags;
import com.fireblaze.magic_overhaul.util.MagicSourceBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RuneDefinition {

    public final ResourceLocation id;
    public final int baseColor;
    public final RuneColorTheme colorTheme;

    public final List<Enchantment> enchantments;
    public final List<MagicSourceBlocks> blocks;
    public final List<MagicSourceBlockTags> blockTags;
    public final List<ResourceLocation> structures;

    public final Map<Block, MagicSourceBlocks> blockMap;
    public final Map<TagKey<Block>, MagicSourceBlockTags> blockTagsMap;
    public final RuneLoot loot;
    public final RuneTrade trade;

    public RuneDefinition(
            ResourceLocation id,
            int baseColor,
            List<Enchantment> enchantments,
            List<MagicSourceBlocks> blocks,
            List<MagicSourceBlockTags> blockTags,
            List<ResourceLocation> structures,
            RuneLoot loot,
            RuneTrade trade
    ) {
        this.id = id;
        this.baseColor = baseColor;
        this.colorTheme = RuneColorTheme.fromBaseColor(baseColor);

        // Collections nie null
        this.enchantments = enchantments != null ? enchantments : Collections.emptyList();
        this.blocks = blocks != null ? blocks : Collections.emptyList();
        this.blockTags = blockTags != null ? blockTags : Collections.emptyList();
        this.structures = structures != null ? structures : Collections.emptyList();

        this.loot = loot;
        this.trade = trade;

        this.blockMap = new HashMap<>();
        for (MagicSourceBlocks b : this.blocks) {
            this.blockMap.put(b.block, b);
        }

        this.blockTagsMap = new HashMap<>();
        for (MagicSourceBlockTags b : this.blockTags) {
            this.blockTagsMap.put(b.tag, b);
        }
    }
}
