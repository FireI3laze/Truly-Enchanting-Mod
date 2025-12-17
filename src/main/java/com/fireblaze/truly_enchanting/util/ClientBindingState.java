package com.fireblaze.truly_enchanting.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Einfacher clientseitiger Speicher für das aktuell gebundene Table des lokalen Spielers.
 * Wird vom SyncBindingPacket aktualisiert.
 */
public class ClientBindingState {
    // null = nicht gebunden
    private static volatile BoundTable boundTable = null;

    public static @Nullable BoundTable getBoundTable() {
        return boundTable;
    }

    public static void setBoundTable(@Nullable BlockPos pos, @Nullable ResourceKey<Level> dimension) {
        if (pos == null || dimension == null) {
            boundTable = null;
        } else {
            boundTable = new BoundTable(pos, dimension);
        }
    }
}
