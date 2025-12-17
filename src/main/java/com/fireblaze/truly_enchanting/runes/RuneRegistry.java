package com.fireblaze.truly_enchanting.runes;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RuneRegistry {

    private static final Map<ResourceLocation, RuneDefinition> RUNES = new HashMap<>();

    public static void clear() {
        RUNES.clear();
    }

    public static void register(RuneDefinition rune) {
        RUNES.put(rune.id, rune);
    }

    public static RuneDefinition get(ResourceLocation id) {
        return RUNES.get(id);
    }

    public static Collection<RuneDefinition> getAll() {
        return RUNES.values();
    }
}
