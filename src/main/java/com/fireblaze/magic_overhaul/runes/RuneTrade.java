package com.fireblaze.magic_overhaul.runes;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.npc.VillagerProfession;

public class RuneTrade {
    public final VillagerProfession profession;
    public final int professionLevel;
    public final int price;

    public RuneTrade(VillagerProfession profession, int professionLevel, int price) {
        this.profession = profession;
        this.professionLevel = professionLevel;
        this.price = price;
    }

    /* ===================== NETWORK ===================== */

    public static void write(FriendlyByteBuf buf, RuneTrade trade) {
        if (trade == null) {
            buf.writeBoolean(false);
            return;
        }

        buf.writeBoolean(true);
        buf.writeResourceLocation(
                BuiltInRegistries.VILLAGER_PROFESSION.getKey(trade.profession)
        );
        buf.writeInt(trade.professionLevel);
        buf.writeInt(trade.price);
    }

    public static RuneTrade read(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }

        VillagerProfession profession =
                BuiltInRegistries.VILLAGER_PROFESSION.getOptional(
                        buf.readResourceLocation()
                ).orElse(null);

        int level = buf.readInt();
        int price = buf.readInt();

        return new RuneTrade(profession, level, price);
    }
}
