package com.fireblaze.truly_enchanting.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record BoundTable(BlockPos pos, ResourceKey<Level> dimension) {
}
