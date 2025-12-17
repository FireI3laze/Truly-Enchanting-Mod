package com.fireblaze.truly_enchanting.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public class MagicSourceBlockTags {
    public final TagKey<Block> tag;
    public int magicPower;
    public int magicCap;

    public MagicSourceBlockTags(TagKey<Block> tag, int magicPower, int magicCap) {
        this.tag = tag;
        this.magicPower = magicPower;
        this.magicCap = magicCap;
    }

    /**
     * Wandelt den Tag in ein RuneBlock-Objekt um, damit es im Scan
     * wie ein normaler Block behandelt werden kann.
     * Der Block selbst ist hier null, weil es für Tags keinen spezifischen Block gibt.
     */
    public MagicSourceBlocks toMagicSourceBlock() {
        return new MagicSourceBlocks(null, this.magicPower, this.magicCap);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(magicPower);
        buf.writeInt(magicCap);
    }

    public static MagicSourceBlockTags read(FriendlyByteBuf buf, TagKey<Block> tag) {
        int power = buf.readInt();
        int cap = buf.readInt();
        return new MagicSourceBlockTags(tag, power, cap);
    }

    public static void writeList(FriendlyByteBuf buf, List<MagicSourceBlockTags> list) {
        buf.writeInt(list.size());

        for (MagicSourceBlockTags entry : list) {
            buf.writeResourceLocation(entry.tag.location());
            entry.write(buf);
        }
    }

    public static List<MagicSourceBlockTags> readList(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<MagicSourceBlockTags> list = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            ResourceLocation rl = buf.readResourceLocation();
            TagKey<Block> tag = TagKey.create(BuiltInRegistries.BLOCK.key(), rl);
            list.add(MagicSourceBlockTags.read(buf, tag));
        }

        return list;
    }
}
