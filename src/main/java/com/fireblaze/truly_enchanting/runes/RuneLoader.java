package com.fireblaze.truly_enchanting.runes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class RuneLoader {

    public static final Map<String, RuneDefinition> RUNE_DEFINITIONS = new HashMap<>();

    public static void reloadRunes(File runesDir, String modid) {
        loadRunes(runesDir, modid);

        //SyncRuneDefinitionsPacket packet = todo
        //        new SyncRuneDefinitionsPacket(RuneLoader.getRuneDefinitions());

        //boolean isSinglePlayer = Minecraft.getInstance().isLocalServer();
        //if (!isSinglePlayer) Network.CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }

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

    public static void ensureDefaultRunes(File runesDir, String modid) {

        // 🔑 WICHTIG: Nur beim allerersten Start
        if (runesDir.exists()) {
            return;
        }

        runesDir.mkdirs();

        try {
            String[] defaults = {
                    "ancient_city.json",
                    "buried_treasure.json",
                    "desert_pyramid.json",
                    "end_city.json",
                    "igloo.json",
                    "jungle_pyramid.json",
                    "mineshaft.json",
                    "outpost.json",
                    "ruined_portal.json",
                    "shipwreck.json",
                    "spawner_room.json",
                    "stronghold.json",
                    "underwater_ruin.json",
                    "village.json"
            };

            for (String fileName : defaults) {
                String path = "data/" + modid + "/runes/" + fileName;

                try (InputStream in = RuneLoader.class
                        .getClassLoader()
                        .getResourceAsStream(path)) {

                    if (in == null) {
                        System.err.println("Missing default rune: " + path);
                        continue;
                    }

                    File outFile = new File(runesDir, fileName);
                    Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static RuneDefinition getRuneDefinition(String id) {
        return RUNE_DEFINITIONS.get(id);
    }
    public static Map<String, RuneDefinition> getRuneDefinitions() {
        return RUNE_DEFINITIONS;
    }

    public static void replaceAll(Map<String, RuneDefinition> runes) {
        RUNE_DEFINITIONS.clear();
        RUNE_DEFINITIONS.putAll(runes);
    }
}
