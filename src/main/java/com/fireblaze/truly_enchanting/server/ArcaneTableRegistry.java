package com.fireblaze.truly_enchanting.server;

import com.fireblaze.truly_enchanting.blockentity.EnchantingTable.ArcaneEnchantingTableBlockEntity;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ArcaneTableRegistry {

    private static final Set<WeakReference<ArcaneEnchantingTableBlockEntity>> TABLES =
            ConcurrentHashMap.newKeySet();

    public static void register(ArcaneEnchantingTableBlockEntity table) {
        TABLES.add(new WeakReference<>(table));
    }

    public static void unregister(ArcaneEnchantingTableBlockEntity table) {
        TABLES.removeIf(ref -> ref.get() == null || ref.get() == table);
    }

    public static void tickAll() {
        Iterator<WeakReference<ArcaneEnchantingTableBlockEntity>> it = TABLES.iterator();

        while (it.hasNext()) {
            ArcaneEnchantingTableBlockEntity table = it.next().get();
            if (table == null || table.isRemoved() || table.getLevel() == null) {
                it.remove();
                continue;
            }

            table.serverTickIndependent();
        }
    }
}
