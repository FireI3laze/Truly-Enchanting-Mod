package com.fireblaze.truly_enchanting.runes;

import com.fireblaze.truly_enchanting.client.color.RuneColorTheme;
import com.fireblaze.truly_enchanting.util.MagicSourceBlockTags;
import com.fireblaze.truly_enchanting.util.MagicSourceBlocks;
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
    public final String displayName;
    public final RuneColorTheme colorTheme;

    public List<Enchantment> enchantments;
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
            String displayName,
            List<Enchantment> enchantments,
            List<MagicSourceBlocks> blocks,
            List<MagicSourceBlockTags> blockTags,
            List<ResourceLocation> structures,
            RuneLoot loot,
            RuneTrade trade
    ) {
        this.id = id;
        this.baseColor = baseColor;
        this.displayName = displayName;
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

    public boolean addBlock(Block block, int min, int max) {
        if (block == null) return false;

        if (blockMap.containsKey(block)) {
            return false;
        }

        MagicSourceBlocks entry = new MagicSourceBlocks(block, min, max);
        blocks.add(entry);
        blockMap.put(block, entry);
        return true;
    }

    public boolean removeBlock(Block block) {
        MagicSourceBlocks entry = blockMap.remove(block);
        if (entry == null) {
            return false;
        }

        blocks.remove(entry);
        return true;
    }

    public boolean addBlockTag(TagKey<Block> tag, int min, int max) {
        if (tag == null) return false;

        // Schon vorhanden?
        if (blockTagsMap.containsKey(tag)) {
            return false;
        }

        MagicSourceBlockTags entry = new MagicSourceBlockTags(tag, min, max);
        blockTags.add(entry);
        blockTagsMap.put(tag, entry);

        return true;
    }

    public boolean removeBlockTag(TagKey<Block> tag) {
        if (tag == null) return false;

        MagicSourceBlockTags entry = blockTagsMap.remove(tag);
        if (entry == null) {
            return false;
        }

        blockTags.remove(entry);
        return true;
    }

    public boolean editBlock(Block block, int newMin, int newMax) {
        if (block == null) return false;

        MagicSourceBlocks entry = blockMap.get(block);
        if (entry == null) return false;

        // Werte updaten
        entry.magicPower = newMin;
        entry.magicCap = newMax;

        // Die List bleibt konsistent, da entry ein Referenzobjekt ist
        return true;
    }

    public boolean editBlockTag(TagKey<Block> tag, int newMin, int newMax) {
        if (tag == null) return false;

        MagicSourceBlockTags entry = blockTagsMap.get(tag);
        if (entry == null) return false;

        // Werte updaten
        entry.magicPower = newMin;
        entry.magicCap = newMax;

        // List bleibt konsistent
        return true;
    }

    public boolean addEnchantment(Enchantment enchantment) {
        if (enchantment == null || enchantments.contains(enchantment)) return false;

        enchantments.add(enchantment);
        return true;
    }

    public boolean removeEnchantment(Enchantment enchantment) {
        if (enchantment == null) return false;

        return enchantments.remove(enchantment);
    }

}
