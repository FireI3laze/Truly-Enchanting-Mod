package com.fireblaze.magic_overhaul.util;

import com.fireblaze.magic_overhaul.blockentity.EnchantingTable.ArcaneEnchantingTableBlockEntity;
import com.fireblaze.magic_overhaul.network.Network;
import com.fireblaze.magic_overhaul.network.SyncBindingPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.*;

public class BindingManager {

    // Cache während des laufenden Spiels
    private static final Map<UUID, BoundTable> playerBindings = new HashMap<>();

    // Spieler an einen Tisch binden
    public static boolean bind(Player player, ArcaneEnchantingTableBlockEntity table) {
        UUID id = player.getUUID();
        if (getBoundTable(player) != null) return false;

        // Dimension des Tables verwenden, nicht die des Spielers
        BoundTable bound = new BoundTable(table.getBlockPos(), table.getLevel().dimension());
        playerBindings.put(id, bound);

        if (player instanceof ServerPlayer sp) Network.sendToClient(sp, new SyncBindingPacket(table.getBlockPos(), table.getLevel().dimension()));

        saveBinding(player, bound); // NBT speichern
        return true;
    }

    // Bindung lösen
    public static void unbind(Player player) {
        UUID id = player.getUUID();
        playerBindings.remove(id);

        if (player instanceof ServerPlayer sp) Network.sendToClient(sp, new SyncBindingPacket(null, null));

        player.getPersistentData().remove("boundTablePos");
        player.getPersistentData().remove("boundTableDim");
    }

    // Liefert die aktuell gebundene Table (Cache oder NBT)
    public static BoundTable getBoundTable(Player player) {
        UUID id = player.getUUID();
        if (playerBindings.containsKey(id)) {
            return playerBindings.get(id);
        }

        BoundTable table = loadBinding(player);
        if (table != null) {
            playerBindings.put(id, table);
        }
        return table;
    }

    // Alle Spieler zurückgeben, die an einem Tisch gebunden sind (Positionsvergleich, Dimension wird aus BoundTable genutzt)
    public static List<UUID> getBoundPlayer(BlockPos pos) {
        List<UUID> boundPlayers = new ArrayList<>();
        for (Map.Entry<UUID, BoundTable> entry : playerBindings.entrySet()) {
            BoundTable table = entry.getValue();
            if (table.pos().equals(pos)) {
                boundPlayers.add(entry.getKey());
            }
        }
        return boundPlayers;
    }

    private static void saveBinding(Player player, BoundTable table) {
        player.getPersistentData().putLong("boundTablePos", table.pos().asLong());
        player.getPersistentData().putString("boundTableDim", table.dimension().location().toString());
    }

    private static BoundTable loadBinding(Player player) {
        if (player.getPersistentData().contains("boundTablePos") &&
                player.getPersistentData().contains("boundTableDim")) {

            BlockPos pos = BlockPos.of(player.getPersistentData().getLong("boundTablePos"));
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION,
                    Objects.requireNonNull(ResourceLocation.tryParse(player.getPersistentData().getString("boundTableDim"))));
            return new BoundTable(pos, dimension);
        }
        return null;
    }

    // Alle Spieler entfernen, die an einem Tisch gebunden sind
    public static void removeBindingsForTable(Level level, BlockPos tablePos) {
        Iterator<Map.Entry<UUID, BoundTable>> iterator = playerBindings.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, BoundTable> entry = iterator.next();
            BoundTable bound = entry.getValue();

            if (bound.pos().equals(tablePos)) { // Dimension egal, da im BoundTable gespeichert
                UUID playerId = entry.getKey();

                // Cache entfernen
                iterator.remove();

                // NBT und Nachricht, falls Spieler online
                Player player = level.getPlayerByUUID(playerId);
                if (player != null) {
                    player.getPersistentData().remove("boundTablePos");
                    player.getPersistentData().remove("boundTableDim");

                    if (player instanceof ServerPlayer sp) {
                        Network.sendToClient(sp, new SyncBindingPacket(null, null));
                    }

                    player.displayClientMessage(
                            Component.literal(
                                    "Arcane Enchanting Table at x" + tablePos.getX() +
                                            " y" + tablePos.getY() +
                                            " z" + tablePos.getZ() +
                                            " destroyed. Binding lost"
                            ),
                            true
                    );
                }
            }
        }
    }
}
