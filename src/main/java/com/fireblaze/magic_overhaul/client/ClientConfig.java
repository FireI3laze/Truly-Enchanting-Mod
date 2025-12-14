package com.fireblaze.magic_overhaul.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Einfaches clientseitiges JSON-Config-Backend.
 * Speichert genau die Werte, die du angefragt hast.
 */
public class ClientConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ClientConfig INSTANCE;

    // >>> Deine gespeicherten Einstellungen
    public boolean magicBarMotion = false;
    public boolean magicBarSparkle = true;

    public boolean leftSideVisible = true;
    public boolean rightSideVisible = true;

    // welche Controller (ids) sind aktuell als sichtbar auf den Seiten ausgewählt
    // Beispiel: leftVisibleController = "blocklist"
    public String leftVisibleController = "blocklist";
    public String rightVisibleController = "enchantments";

    // mapping, welches Controller (by id) auf welcher Seite liegt -> "LEFT"/"RIGHT"/"TOP"/"BOTTOM"
    // keys: "blocklist","enchantments","magicbar"
    public Map<String, String> controllerSideMapping = new HashMap<>();

    // Datei-Pfad
    private static final Path CONFIG_PATH = Path.of("config", "truly_enchanting", "arcane_enchanting_table_preferences.json");

    public static ClientConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static void load() {
        File file = CONFIG_PATH.toFile();

        if (!file.exists()) {
            INSTANCE = new ClientConfig();
            // default mapping
            INSTANCE.controllerSideMapping.put("blocklist", "LEFT");
            INSTANCE.controllerSideMapping.put("enchantments", "LEFT");
            INSTANCE.controllerSideMapping.put("magicbar", "TOP");
            save(); // erzeugt die Datei
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            INSTANCE = GSON.fromJson(reader, ClientConfig.class);
            // ensure defaults for missing keys
            if (INSTANCE.controllerSideMapping == null) INSTANCE.controllerSideMapping = new HashMap<>();
            if (!INSTANCE.controllerSideMapping.containsKey("blocklist"))
                INSTANCE.controllerSideMapping.put("blocklist", "LEFT");
            if (!INSTANCE.controllerSideMapping.containsKey("enchantments"))
                INSTANCE.controllerSideMapping.put("enchantments", "LEFT");
            if (!INSTANCE.controllerSideMapping.containsKey("magicbar"))
                INSTANCE.controllerSideMapping.put("magicbar", "TOP");
        } catch (Exception e) {
            e.printStackTrace();
            INSTANCE = new ClientConfig();
        }
    }

    public static void save() {
        try {
            File file = CONFIG_PATH.toFile();
            file.getParentFile().mkdirs();

            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(get(), writer);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // convenience setters used by the screen
    public void setControllerSide(String controllerId, String side) {
        controllerSideMapping.put(controllerId, side);
    }

    public String getControllerSide(String controllerId) {
        return controllerSideMapping.getOrDefault(controllerId, "LEFT");
    }
}
