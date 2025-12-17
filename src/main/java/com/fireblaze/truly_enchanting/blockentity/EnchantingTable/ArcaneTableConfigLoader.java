package com.fireblaze.truly_enchanting.blockentity.EnchantingTable;

import com.fireblaze.truly_enchanting.util.MagicSourceBlocks;
import com.fireblaze.truly_enchanting.util.MagicSourceBlockTags;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.io.InputStream;
import java.io.InputStreamReader;

public class ArcaneTableConfigLoader {

    public static void loadIntoAccumulator(MagicAccumulator acc, String jsonPath) {
        try (InputStream stream = ArcaneTableConfigLoader.class.getResourceAsStream(jsonPath)) {
            if (stream == null) throw new RuntimeException("ArcaneTable config not found: " + jsonPath);

            JsonReader reader = new JsonReader(new InputStreamReader(stream));
            ArcaneTableConfig cfg = new Gson().fromJson(reader, ArcaneTableConfig.class);

            // Blocks hinzufügen
            for (MagicBlockConfig b : cfg.blocks()) {
                String[] parts = b.name().split(":", 2); // "minecraft:stone" → ["minecraft", "stone"]

                Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]));
                acc.addBlock(new MagicSourceBlocks(block, b.magicPower(), b.magicCap()));
            }

            // Tags hinzufügen
            for (MagicTagConfig t : cfg.tags()) {
                String[] parts = t.name().split(":", 2);

                TagKey<Block> tag = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]));
                acc.addTag(new MagicSourceBlockTags(tag, t.magicPower(), t.magicCap()));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Arcane Table config", e);
        }
    }

    public record MagicBlockConfig(String name, int magicPower, int magicCap) {}
    public record MagicTagConfig(String name, int magicPower, int magicCap) {}
    public record ArcaneTableConfig(MagicBlockConfig[] blocks, MagicTagConfig[] tags) {}
}
