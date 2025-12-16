package com.fireblaze.magic_overhaul.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public class MagicSourceBlocks {
    public final Block block;
    public int magicPower; // Magiekraft pro Block
    public int magicCap;   // Maximalwert erreichbarer Magiekraft über diesen Block

    public MagicSourceBlocks(Block block, int magicPower, int magicCap) {
        this.block = block;
        this.magicPower = magicPower;
        this.magicCap = magicCap;
    }

    // --- Serialisierung ---
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(magicPower);
        buf.writeInt(magicCap);
    }

    public static MagicSourceBlocks read(FriendlyByteBuf buf, Block block) {
        int power = buf.readInt();
        int cap = buf.readInt();
        return new MagicSourceBlocks(block, power, cap);
    }

    public static void writeList(FriendlyByteBuf buf, List<MagicSourceBlocks> list) {
        buf.writeInt(list.size());

        for (MagicSourceBlocks entry : list) {
            net.minecraft.resources.ResourceLocation key = BuiltInRegistries.BLOCK.getKey(entry.block);
            buf.writeResourceLocation(key);
            entry.write(buf);
        }
    }

    public static List<MagicSourceBlocks> readList(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<MagicSourceBlocks> list = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            ResourceLocation key = buf.readResourceLocation();
            Block block = BuiltInRegistries.BLOCK.getOptional(key).orElse(null);

            if (block != null) {
                list.add(MagicSourceBlocks.read(buf, block));
            } else {
                // Payload trotzdem konsumieren, um Buffer nicht zu verschieben
                buf.readInt();
                buf.readInt();
            }
        }

        return list;
    }
}
