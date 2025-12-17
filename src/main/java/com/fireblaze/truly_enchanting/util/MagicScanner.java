package com.fireblaze.truly_enchanting.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class MagicScanner {

    public static class ScanResult {
        public final int totalMagic;
        public final Map<Block, Integer> blockPowerMap;
        public final Map<TagKey<Block>, Integer> tagPowerMap;

        public ScanResult(int totalMagic, Map<Block, Integer> blockPowerMap, Map<TagKey<Block>, Integer> tagPowerMap) {
            this.totalMagic = totalMagic;
            this.blockPowerMap = blockPowerMap;
            this.tagPowerMap = tagPowerMap;
        }
    }

    /**
     * Scannt die Umgebung nach Blöcken/Tags aus der Rune-Palette.
     *
     * @param level     Level
     * @param startPos  Startposition (Monolith / Table)
     * @param scanCap   Max. Anzahl gezählter Blöcke
     * @param radiusCap Max. Scanradius
     * @return ScanResult mit totalMagic und Magic pro Block
     */
    public static ScanResult scanMagicBlocks(Level level, BlockPos startPos, Map <Block, MagicSourceBlocks> blockPalette, Map <TagKey<Block>, MagicSourceBlockTags> tagPalette, int scanCap, int radiusCap) {
        if (level == null) return new ScanResult(0, Map.of(), Map.of());

        Map<Block, Integer> blockAccumulatedPower = new HashMap<>();
        Map<TagKey<Block>, Integer> tagAccumulatedPower = new HashMap<>();
        Map<Block, Integer> blockCurrentPower = new HashMap<>();
        Map<TagKey<Block>, Integer> tagCurrentPower = new HashMap<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(startPos);
        visited.add(startPos);

        int totalMagicPower = 0;
        int scannedBlocks = 0;

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (current.distSqr(startPos) > radiusCap * radiusCap) continue;

            BlockState state = level.getBlockState(current);
            Block block = state.getBlock();

            // Prüfe Block-Palette
            MagicSourceBlocks rb = blockPalette.get(block);
            if (rb != null) {
                int accumulated = blockAccumulatedPower.getOrDefault(block, 0);
                int remaining = rb.magicCap - accumulated;

                if (remaining > 0) {
                    int toAdd = Math.min(rb.magicPower, remaining);
                    totalMagicPower += toAdd;
                    blockAccumulatedPower.put(block, accumulated + toAdd);

                    int currentBlockPower = blockCurrentPower.getOrDefault(block, 0);
                    blockCurrentPower.put(block, currentBlockPower + toAdd);

                    scannedBlocks++;
                }
            }

            // Prüfe Tag-Palette
            if (!tagPalette.isEmpty()) {
                for (Map.Entry<TagKey<Block>, MagicSourceBlockTags> entry : tagPalette.entrySet()) {
                    TagKey<Block> tag = entry.getKey();

                    if (state.is(tag)) {
                        MagicSourceBlocks tagRb = entry.getValue().toMagicSourceBlock();
                        int accumulatedTag = tagAccumulatedPower.getOrDefault(tag, 0);
                        int remainingTag = tagRb.magicCap - accumulatedTag;

                        if (remainingTag > 0) {
                            int toAddTag = Math.min(tagRb.magicPower, remainingTag);
                            totalMagicPower += toAddTag;
                            tagAccumulatedPower.put(tag, accumulatedTag + toAddTag);

                            int currentTagPower = tagCurrentPower.getOrDefault(tag, 0);
                            tagCurrentPower.put(tag, currentTagPower + toAddTag);
                        }
                    }
                }
            }

            if (scannedBlocks >= scanCap) break;

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (!visited.contains(neighbor) && level.isLoaded(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return new ScanResult(totalMagicPower, blockCurrentPower, tagCurrentPower);
    }
}
