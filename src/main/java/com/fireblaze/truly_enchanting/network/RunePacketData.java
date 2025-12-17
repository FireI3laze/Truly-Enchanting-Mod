package com.fireblaze.truly_enchanting.network;

import com.fireblaze.truly_enchanting.runes.RuneDefinition;
import com.fireblaze.truly_enchanting.util.MagicSourceBlockTags;
import com.fireblaze.truly_enchanting.util.MagicSourceBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RunePacketData {
    private final BlockPos pos;
    private final RuneDefinition rune;

    public RunePacketData(BlockPos pos, RuneDefinition rune) {
        this.pos = pos;
        this.rune = rune;
    }

    public BlockPos getPos() { return pos; }
    public RuneDefinition getRune() { return rune; }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(rune.baseColor);

        buf.writeInt(rune.blockMap.size());
        for (var entry : rune.blockMap.entrySet()) {
            buf.writeUtf(BuiltInRegistries.BLOCK.getKey(entry.getKey()).toString());
            entry.getValue().write(buf);
        }

        buf.writeInt(rune.blockTagsMap.size());
        for (var entry : rune.blockTagsMap.entrySet()) {
            buf.writeUtf(entry.getKey().location().toString());
            entry.getValue().write(buf);
        }

        buf.writeInt(rune.enchantments.size());
        for (Enchantment ench : rune.enchantments) {
            buf.writeUtf(Objects.requireNonNull(BuiltInRegistries.ENCHANTMENT.getKey(ench)).toString());
        }
    }

    public static RunePacketData decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();

        int baseColor = buf.readInt();
        RuneDefinition rune = new RuneDefinition(null, baseColor, null, null, null, null, null, null, null);

        List<Enchantment> temp = new ArrayList<>(rune.enchantments);

        int blockCount = buf.readInt();
        for (int i = 0; i < blockCount; i++) {
            String key = buf.readUtf();
            Block block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.tryParse(key)).orElse(null);
            if (block != null) {
                rune.blockMap.put(block, MagicSourceBlocks.read(buf, block));
            }
        }

        int tagCount = buf.readInt();
        for (int i = 0; i < tagCount; i++) {
            String key = buf.readUtf();
            TagKey<Block> tag = TagKey.create(BuiltInRegistries.BLOCK.key(), Objects.requireNonNull(ResourceLocation.tryParse(key)));
            rune.blockTagsMap.put(tag, MagicSourceBlockTags.read(buf, tag));
        }

        int enchCount = buf.readInt();
        for (int i = 0; i < enchCount; i++) {
            String key = buf.readUtf();
            Enchantment ench = BuiltInRegistries.ENCHANTMENT.getOptional(ResourceLocation.tryParse(key)).orElse(null);
            if (ench != null) temp.add(ench);
        }
        rune.enchantments = temp;

        return new RunePacketData(pos, rune);
    }
}

