package com.fireblaze.truly_enchanting.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class PlayerSettings {

    private static final String TAG_MOD_SETTINGS = "magic_overhaul_settings";

    /**
     * Speichert einen Boolean im Persisted NBT des Spielers.
     */
    public static void saveBoolean(Player player, String key, boolean value) {
        // Persisted NBT holen (wird von Minecraft dauerhaft gespeichert)
        CompoundTag persisted = player.getPersistentData();
        CompoundTag tag = persisted.contains(Player.PERSISTED_NBT_TAG)
                ? persisted.getCompound(Player.PERSISTED_NBT_TAG)
                : new CompoundTag();

        CompoundTag modTag = tag.contains(TAG_MOD_SETTINGS)
                ? tag.getCompound(TAG_MOD_SETTINGS)
                : new CompoundTag();

        modTag.putBoolean(key, value);
        tag.put(TAG_MOD_SETTINGS, modTag);
        persisted.put(Player.PERSISTED_NBT_TAG, tag);
    }

    /**
     * Lädt einen Boolean aus dem Persisted NBT des Spielers.
     * Gibt defaultValue zurück, falls die Einstellung noch nicht existiert.
     */
    public static boolean loadBoolean(Player player, String key, boolean defaultValue) {
        CompoundTag persisted = player.getPersistentData();

        if (!persisted.contains(Player.PERSISTED_NBT_TAG)) {
            return defaultValue;
        }

        CompoundTag tag = persisted.getCompound(Player.PERSISTED_NBT_TAG);

        if (!tag.contains(TAG_MOD_SETTINGS)) {
            return defaultValue;
        }

        CompoundTag modTag = tag.getCompound(TAG_MOD_SETTINGS);

        boolean value = modTag.getBoolean(key);
        return value;
    }
}
