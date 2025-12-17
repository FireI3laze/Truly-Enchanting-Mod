package com.fireblaze.truly_enchanting.world;

public class RuneSpawner {
    /*
    public static void spawnRune(Level world, BlockPos basePos, String structureName, RandomSource random) {
        if (world.isClientSide()) return;

        // Rune finden
        RuneType matchedRune = Arrays.stream(RuneType.values())
                .filter(r -> Arrays.asList(r.structureIds).contains(structureName))
                .findFirst().orElse(null);

        if (matchedRune == null) return;

        ItemStack runeStack = new ItemStack(ModRunes.RUNES.get(matchedRune).get());

        // 🟨 1) Spezialfall Buried Treasure
        if (structureName.equals("buried_treasure")) {
            boolean success = tryPutRuneInBuriedTreasure(world, basePos, runeStack);

            world.players().forEach(p ->
                    p.sendSystemMessage(Component.literal(
                            success
                                    ? "Rune '" + matchedRune.id + "' in Buried Treasure Kiste gesetzt!"
                                    : "KEINE Kiste für Buried Treasure gefunden!"
                    )));

            return;
        }

        // 🟩 2) Normal: Monolith platzieren
        BlockPos spot = findValidMonolithSpot(world, basePos);

        if (spot == null) {
            world.players().forEach(p -> p.sendSystemMessage(
                    Component.literal("⚠ Keine valide Position für Monolith bei Struktur " + structureName)));
            return;
        }

        placeMonolithWithRune(world, spot, matchedRune);

        world.players().forEach(player ->
                player.sendSystemMessage(Component.literal(
                        "Monolith mit Rune '" + matchedRune.id + "' gespawnt bei "
                                + spot.getX() + ", " + spot.getY() + ", " + spot.getZ()
                )));
    }

    private static boolean tryPutRuneInBuriedTreasure(Level world, BlockPos basePos, ItemStack runeStack) {
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                BlockPos pos = basePos.offset(dx, 0, dz);

                if (world.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
                    chest.setItem(0, runeStack);
                    chest.setChanged();
                    return true;
                }
            }
        }
        return false;
    }

    private static BlockPos findValidMonolithSpot(Level world, BlockPos center) {
        for (int dy = -5; dy <= 5; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {

                    BlockPos pos = center.offset(dx, dy, dz);

                    // Boden muss solid sein
                    if (!world.getBlockState(pos).isSolid()) continue;

                    // Check 3 Blöcke Luft
                    if (!world.getBlockState(pos.above()).isAir()) continue;
                    if (!world.getBlockState(pos.above(2)).isAir()) continue;
                    if (!world.getBlockState(pos.above(3)).isAir()) continue;

                    return pos.above(); // Platz für LOWER half
                }
            }
        }
        return null;
    }

    private static void placeMonolithWithRune(Level world, BlockPos pos, RuneType runeType) {
        BlockPos lower = pos;
        BlockPos upper = pos.above();

        // LOWER half
        world.setBlock(lower, ModBlocks.MONOLITH.get().defaultBlockState(), 3);
        // UPPER half
        world.setBlock(upper, ModBlocks.MONOLITH.get()
                .defaultBlockState()
                .setValue(MonolithBlock.HALF, DoubleBlockHalf.UPPER), 3);

        // Rune in BE speichern
        var be = world.getBlockEntity(lower);
        if (be instanceof MonolithBlockEntity monolith) {
            monolith.insertRune(runeType);
            monolith.setChanged();
        }
    }
    */
}

