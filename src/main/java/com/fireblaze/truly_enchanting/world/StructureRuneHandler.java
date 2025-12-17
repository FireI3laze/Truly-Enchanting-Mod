package com.fireblaze.truly_enchanting.world;

import com.fireblaze.truly_enchanting.TrulyEnchanting;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TrulyEnchanting.MODID)
public class StructureRuneHandler {

    /*
    // Key-Typ für verarbeitete Strukturen
    public record StructureKey(ResourceLocation id, int minX, int minZ) {}

    // Queue für Strukturen, die noch gespawnt werden müssen
    private static final Queue<RuneSpawnTask> pendingSpawns = new ConcurrentLinkedQueue<>();

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide()) return;

        if (!(level instanceof net.minecraft.server.level.ServerLevel world)) return;

        LevelChunk chunk = (LevelChunk) event.getChunk();
        RuneWorldData data = RuneWorldData.get(world);

        chunk.getAllStarts().forEach((structure, start) -> {
            if (start == null || start.getPieces().isEmpty()) return;

            ResourceLocation id = world.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure);
            if (id == null) return;

            var box = start.getBoundingBox();
            StructureKey key = new StructureKey(id, box.minX(), box.minZ());
            if (data.contains(key)) return;

            BlockPos basePos = start.getBoundingBox().getCenter();
            data.add(key);

            // In Queue packen statt sofort spawnen
            pendingSpawns.add(new RuneSpawnTask(world, basePos, id.getPath()));
        });
    }

    @SubscribeEvent
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;

        RuneSpawnTask task;
        while ((task = pendingSpawns.poll()) != null) {
            task.spawn();
        }
    }

    private record RuneSpawnTask(Level world, BlockPos pos, String structureName) {
        void spawn() {
            RuneSpawner.spawnRune(world, pos, structureName, world.getRandom());
        }
    }

    // ------------------ Persistente World-Daten ------------------
    public static class RuneWorldData extends SavedData {

        private static final String DATA_NAME = MagicOverhaul.MODID + "_rune_data";
        private final Set<String> keys = new HashSet<>();

        public static RuneWorldData get(ServerLevel world) {
            return world.getDataStorage().computeIfAbsent(
                    RuneWorldData::new, // aus NBT erzeugen
                    RuneWorldData::new, // Default falls noch nicht vorhanden
                    DATA_NAME
            );
        }

        public RuneWorldData() {}

        public RuneWorldData(CompoundTag nbt) {
            var list = nbt.getList("keys", 8); // 8 = String
            for (int i = 0; i < list.size(); i++) {
                keys.add(list.getString(i));
            }
        }

        @Override
        public CompoundTag save(CompoundTag nbt) {
            var list = new net.minecraft.nbt.ListTag();
            keys.forEach(k -> list.add(net.minecraft.nbt.StringTag.valueOf(k)));
            nbt.put("keys", list);
            return nbt;
        }

        public boolean contains(StructureKey key) {
            return keys.contains(keyToString(key));
        }

        public void add(StructureKey key) {
            keys.add(keyToString(key));
            setDirty();
        }

        private String keyToString(StructureKey key) {
            return key.id() + "@" + key.minX() + "," + key.minZ();
        }
    }
    */
}
