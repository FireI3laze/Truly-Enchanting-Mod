package com.fireblaze.magic_overhaul.runes;

import com.fireblaze.magic_overhaul.util.MagicSourceBlockTags;
import com.fireblaze.magic_overhaul.util.MagicSourceBlocks;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.Registries;

import java.util.ArrayList;
import java.util.List;

public class RuneJsonParser {

    /* -------------------------
       ENTRY
       ------------------------- */

    public static RuneDefinition parse(ResourceLocation id, JsonObject json) {

        int color = Integer.parseInt(
                json.get("color").getAsString().substring(1), 16
        );

        List<Enchantment> enchantments = parseEnchantments(json);
        List<MagicSourceBlocks> blocks = parseBlocks(json);
        List<MagicSourceBlockTags> blockTags = parseBlockTags(json);
        List<ResourceLocation> structures = parseStructures(json);

        RuneLoot loot = parseLoot(json);
        RuneTrade trade = parseTrade(json);
        return new RuneDefinition(
                id,
                color,
                enchantments,
                blocks,
                blockTags,
                structures,
                loot,
                trade
        );
    }

    /* -------------------------
       ENCHANTMENTS
       ------------------------- */

    private static List<Enchantment> parseEnchantments(JsonObject json) {
        List<Enchantment> result = new ArrayList<>();

        if (!json.has("enchantments")) return result;

        JsonArray arr = json.getAsJsonArray("enchantments");
        for (JsonElement el : arr) {
            ResourceLocation id = ResourceLocation.tryParse(el.getAsString());
            Enchantment ench = BuiltInRegistries.ENCHANTMENT.get(id);
            if (ench != null) {
                result.add(ench);
            }
        }
        return result;
    }

    /* -------------------------
       BLOCK PALETTE
       ------------------------- */

    private static List<MagicSourceBlocks> parseBlocks(JsonObject json) {
        List<MagicSourceBlocks> result = new ArrayList<>();

        if (!json.has("blocks")) return result;

        JsonArray arr = json.getAsJsonArray("blocks");
        for (JsonElement el : arr) {
            JsonObject o = el.getAsJsonObject();

            ResourceLocation blockId = ResourceLocation.tryParse(o.get("block").getAsString());
            Block block = BuiltInRegistries.BLOCK.get(blockId);

            int power = o.get("min").getAsInt();
            int cap = o.get("max").getAsInt();

            result.add(new MagicSourceBlocks(block, power, cap));
        }
        return result;
    }

    /* -------------------------
       TAG PALETTE
       ------------------------- */

    private static List<MagicSourceBlockTags> parseBlockTags(JsonObject json) {
        List<MagicSourceBlockTags> result = new ArrayList<>();

        if (!json.has("block_tags")) return result;

        JsonArray arr = json.getAsJsonArray("block_tags");
        for (JsonElement el : arr) {
            JsonObject o = el.getAsJsonObject();

            ResourceLocation tagId = ResourceLocation.tryParse(o.get("tag").getAsString());
            TagKey<Block> tag = TagKey.create(Registries.BLOCK, tagId);

            int power = o.get("min").getAsInt();
            int cap = o.get("max").getAsInt();

            result.add(new MagicSourceBlockTags(tag, power, cap));
        }
        return result;
    }

    /* -------------------------
       STRUCTURES
       ------------------------- */

    private static List<ResourceLocation> parseStructures(JsonObject json) {
        List<ResourceLocation> result = new ArrayList<>();

        if (!json.has("structures")) return result;

        JsonArray arr = json.getAsJsonArray("structures");
        for (JsonElement el : arr) {
            result.add(ResourceLocation.tryParse(el.getAsString()));
        }
        return result;
    }

    /* -------------------------
       LOOT
       ------------------------- */

    private static RuneLoot parseLoot(JsonObject json) {

        if (!json.has("loot")) {
            return new RuneLoot(new ResourceLocation[0], 0f);
        }

        JsonObject o = json.getAsJsonObject("loot");

        float chance = o.get("chance").getAsFloat();

        JsonArray tablesArr = o.getAsJsonArray("tables");
        ResourceLocation[] tables = new ResourceLocation[tablesArr.size()];

        for (int i = 0; i < tablesArr.size(); i++) {
            tables[i] = ResourceLocation.tryParse(tablesArr.get(i).getAsString());
        }

        return new RuneLoot(tables, chance);
    }

    /* -------------------------
       TRADE
       ------------------------- */

    private static RuneTrade parseTrade(JsonObject json) {

        if (!json.has("trade")) {
            return new RuneTrade(null, 0, 0);
        }

        JsonObject o = json.getAsJsonObject("trade");

        VillagerProfession profession = null;
        if (!o.get("profession").isJsonNull()) {
            profession = BuiltInRegistries.VILLAGER_PROFESSION.get(
                    ResourceLocation.tryParse(o.get("profession").getAsString())
            );
        }

        int level = o.get("level").getAsInt();
        int max_uses = o.get("max_uses").getAsInt();

        return new RuneTrade(profession, level, max_uses);
    }
}
