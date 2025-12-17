package com.fireblaze.truly_enchanting.util;

import net.minecraft.server.level.ServerPlayer;

public class XPUtil {

    public static int xpAtLevel(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        } else if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            return (int) (4.5 * level * level - 162.5 * level + 2220);
        }
    }

    public static int xpBarCap(int level) {
        if (level <= 15) return 2 * level + 7;
        if (level <= 30) return 5 * level - 38;
        return 9 * level - 158;
    }

    public static int getTotalExperience(ServerPlayer player) {
        int level = player.experienceLevel;
        float progress = player.experienceProgress;
        return xpAtLevel(level) + (int) (progress * xpBarCap(level));
    }

    public static void setPlayerTotalExperience(ServerPlayer player, int totalXp) {
        if (totalXp < 0) totalXp = 0;

        int level = 0;
        while (xpAtLevel(level + 1) <= totalXp) {
            level++;
        }

        int xpIntoLevel = totalXp - xpAtLevel(level);
        int cap = xpBarCap(level);

        float progress = cap == 0 ? 0f : (float) xpIntoLevel / cap;

        player.experienceLevel = level;
        player.experienceProgress = progress;
        player.totalExperience = totalXp;
    }
}
