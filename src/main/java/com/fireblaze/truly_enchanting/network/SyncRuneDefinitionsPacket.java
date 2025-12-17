package com.fireblaze.truly_enchanting.network;

import com.fireblaze.truly_enchanting.runes.*;
import com.fireblaze.truly_enchanting.util.MagicSourceBlockTags;
import com.fireblaze.truly_enchanting.util.MagicSourceBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class SyncRuneDefinitionsPacket {

    private final Map<String, RuneDefinition> runes;

    public SyncRuneDefinitionsPacket(Map<String, RuneDefinition> runes) {
        this.runes = runes;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(runes.size());

        for (Map.Entry<String, RuneDefinition> entry : runes.entrySet()) {
            RuneDefinition rune = entry.getValue();

            // --- Key / Identity ---
            buf.writeResourceLocation(ResourceLocation.parse(entry.getKey()));

            // --- Core ---
            buf.writeInt(rune.baseColor);
            buf.writeUtf(rune.displayName);

            // --- Enchantments ---
            buf.writeInt(rune.enchantments.size());
            for (Enchantment ench : rune.enchantments) {
                buf.writeResourceLocation(
                        BuiltInRegistries.ENCHANTMENT.getKey(ench)
                );
            }

            // --- Blocks ---
            buf.writeInt(rune.blocks.size());
            for (MagicSourceBlocks b : rune.blocks) {
                buf.writeResourceLocation(
                        BuiltInRegistries.BLOCK.getKey(b.block)
                );
                b.write(buf);
            }

            // --- BlockTags ---
            buf.writeInt(rune.blockTags.size());
            for (MagicSourceBlockTags t : rune.blockTags) {
                buf.writeResourceLocation(t.tag.location());
                t.write(buf);
            }

            // --- Structures ---
            buf.writeInt(rune.structures.size());
            for (ResourceLocation rl : rune.structures) {
                buf.writeResourceLocation(rl);
            }

            // --- Loot / Trade ---
            RuneLoot.write(buf, rune.loot);
            RuneTrade.write(buf, rune.trade);
        }
    }

    public static SyncRuneDefinitionsPacket decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        Map<String, RuneDefinition> runes = new HashMap<>();

        for (int i = 0; i < count; i++) {

            // --- Key / Identity ---
            ResourceLocation id = buf.readResourceLocation();

            // --- Core ---
            int baseColor = buf.readInt();
            String displayName = buf.readUtf();

            // --- Enchantments ---
            List<Enchantment> enchantments = new java.util.ArrayList<>();
            int enchCount = buf.readInt();
            for (int e = 0; e < enchCount; e++) {
                BuiltInRegistries.ENCHANTMENT
                        .getOptional(buf.readResourceLocation())
                        .ifPresent(enchantments::add);
            }

            // --- Blocks / Tags ---
            List<MagicSourceBlocks> blocks = MagicSourceBlocks.readList(buf);
            List<MagicSourceBlockTags> tags = MagicSourceBlockTags.readList(buf);

            // --- Structures ---
            int structCount = buf.readInt();
            List<ResourceLocation> structures = new java.util.ArrayList<>();
            for (int s = 0; s < structCount; s++) {
                structures.add(buf.readResourceLocation());
            }

            // --- Loot / Trade ---
            RuneLoot loot = RuneLoot.read(buf);
            RuneTrade trade = RuneTrade.read(buf);

            RuneDefinition rune = new RuneDefinition(
                    id,
                    baseColor,
                    displayName,
                    enchantments,
                    blocks,
                    tags,
                    structures,
                    loot,
                    trade
            );

            runes.put(String.valueOf(id), rune);
        }

        return new SyncRuneDefinitionsPacket(runes);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            RuneLoader.replaceAll(runes);
        });
        ctx.get().setPacketHandled(true);
    }
}
