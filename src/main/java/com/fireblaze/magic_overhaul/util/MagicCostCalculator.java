package com.fireblaze.magic_overhaul.util;

import com.fireblaze.magic_overhaul.runes.RuneDefinition;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.HashMap;
import java.util.Map;

public class MagicCostCalculator {

    private static final int BASE_COST = 20;            // fester Grundwert
    private static final float LEVEL_FACTOR = 10.0f;    // Kosten pro Level
    private static final float RARITY_FACTOR = 10.0f;   // multipliziert rarity-Wert
    private static final float TREASURE_FACTOR = 4f; // multiplier für Schatzverzauberungen
    private static final float BALANCING = 6f;      // globaler Feintuning-Slider

    /**
     * Wandelt Vanilla-Rarity in einen numerischen Wert 1–4
     */
    private static float rarityToNumber(Enchantment.Rarity r) {
        return switch (r) {
            case COMMON -> 0.5f;
            case UNCOMMON -> 1;
            case RARE -> 2;
            case VERY_RARE -> 4;
        };
    }

    /**
     * Berechnet die magischen Kosten einer Verzauberung.
     *
     * Formel:
     *   BASE_COST
     * + (rarity * RARITY_FACTOR)
     * + (level * LEVEL_FACTOR)
     * -> bei Treasure Enchantments wird zusätzlich multipliziert
     */
    public static int calculateMagicRequirement(Enchantment enchant, int level) {

        float rarityValue = rarityToNumber(enchant.getRarity());

        float cost = BASE_COST
                + rarityValue * RARITY_FACTOR
                + level * LEVEL_FACTOR;

        if (enchant.isTreasureOnly()) {
            cost *= TREASURE_FACTOR;
        }

        cost *= BALANCING;

        return Math.max(1, Math.round(cost)); // niemals 0
    }

    public static int getUnlockedLevel(Enchantment enchant, float magicPower) {
        for (int level = enchant.getMaxLevel(); level >= 1; level--) {
            if (magicPower >= calculateMagicRequirement(enchant, level)) {
                return level;
            }
        }
        return 0; // noch kein Level freigeschaltet
    }
    public static Map<Enchantment, Integer> getUnlockedLevels(RuneDefinition rune, float magicPower) {
        Map<Enchantment, Integer> map = new HashMap<>();
        for (Enchantment ench : rune.enchantments) {
            int lvl = getUnlockedLevel(ench, magicPower);
            if(lvl > 0) {
                map.put(ench, lvl);
            }
        }
        return map;
    }

    public static int getMaxMagicRequirement(RuneDefinition rune) {
        int maxRequirement = 0;
        for (Enchantment ench : rune.enchantments) {
            for (int level = 1; level <= ench.getMaxLevel(); level++) {
                int cost = calculateMagicRequirement(ench, level);
                if (cost > maxRequirement) {
                    maxRequirement = cost;
                }
            }
        }
        return maxRequirement;
    }
}
