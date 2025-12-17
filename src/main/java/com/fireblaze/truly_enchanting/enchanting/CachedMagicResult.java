package com.fireblaze.truly_enchanting.enchanting;

import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Map;

public class CachedMagicResult {

    public final long timestamp;
    public final float magicPower;
    public final Map<Enchantment, Integer> unlockedLevels;

    public CachedMagicResult(long timestamp, float magicPower, Map<Enchantment, Integer> unlockedLevels) {
        this.timestamp = timestamp;
        this.magicPower = magicPower;
        this.unlockedLevels = unlockedLevels;
    }
}
