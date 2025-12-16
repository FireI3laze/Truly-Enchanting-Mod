package com.fireblaze.magic_overhaul.runes;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class RuneLoot {

    public final ResourceLocation[] chestLootTables;
    public final float chance;

    public RuneLoot(ResourceLocation[] chestLootTables, float chance) {
        this.chestLootTables = chestLootTables;
        this.chance = chance;
    }

    /* ===================== NETWORK ===================== */

    public static void write(FriendlyByteBuf buf, RuneLoot loot) {
        if (loot == null) {
            buf.writeBoolean(false);
            return;
        }

        buf.writeBoolean(true);
        buf.writeFloat(loot.chance);

        buf.writeInt(loot.chestLootTables.length);
        for (ResourceLocation rl : loot.chestLootTables) {
            buf.writeResourceLocation(rl);
        }
    }

    public static RuneLoot read(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }

        float chance = buf.readFloat();

        int count = buf.readInt();
        ResourceLocation[] tables = new ResourceLocation[count];
        for (int i = 0; i < count; i++) {
            tables[i] = buf.readResourceLocation();
        }

        return new RuneLoot(tables, chance);
    }
}
