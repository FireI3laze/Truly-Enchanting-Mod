package com.fireblaze.magic_overhaul.runes;

import com.fireblaze.magic_overhaul.runes.RuneDefinition;
import com.fireblaze.magic_overhaul.runes.RuneJsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class RuneLoader {

    public static final Map<String, RuneDefinition> RUNE_DEFINITIONS = new HashMap<>();

    public static void loadRunes(File runesDir, String modid) {
        RUNE_DEFINITIONS.clear();
        if (!runesDir.exists()) runesDir.mkdirs();

        File[] files = runesDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                // JSON lesen
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                // ResourceLocation für die Rune erstellen
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(modid, file.getName().replace(".json",""));

                // RuneDefinition mit dem manuellen Parser erzeugen
                RuneDefinition rune = RuneJsonParser.parse(id, json);

                if (rune != null && rune.id != null) {
                    RUNE_DEFINITIONS.put(rune.id.toString(), rune);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static RuneDefinition getRuneDefinition(String id) {
        return RUNE_DEFINITIONS.get(id);
    }
}
