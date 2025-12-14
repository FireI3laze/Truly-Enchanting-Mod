package com.fireblaze.magic_overhaul.runes;

import net.minecraft.resources.ResourceLocation;

public class RuneLoot {

    public final ResourceLocation[] chestLootTables;
    public final float chance;

    public RuneLoot(ResourceLocation[] chestLootTables, float chance) {
        this.chestLootTables = chestLootTables;
        this.chance = chance;
    }
}
