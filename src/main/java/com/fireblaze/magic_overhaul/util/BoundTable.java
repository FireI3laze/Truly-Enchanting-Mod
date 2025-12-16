package com.fireblaze.magic_overhaul.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record BoundTable(BlockPos pos, ResourceKey<Level> dimension) {
}
