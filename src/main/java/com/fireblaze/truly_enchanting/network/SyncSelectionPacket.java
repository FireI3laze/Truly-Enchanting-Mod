package com.fireblaze.truly_enchanting.network;

import com.fireblaze.truly_enchanting.client.screen.ArcaneEnchantingTableScreen;
import com.fireblaze.truly_enchanting.menu.ArcaneEnchantingTableMenu;
import com.fireblaze.truly_enchanting.blockentity.EnchantingTable.ArcaneEnchantingTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.HashMap;
import java.util.function.Supplier;

public class SyncSelectionPacket {

    private final Map<Enchantment, Integer> selected;

    public SyncSelectionPacket(Map<Enchantment, Integer> selected) {
        this.selected = selected;
    }

    // --- Encoder ---
    public static void encode(SyncSelectionPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.selected.size());
        for (var entry : msg.selected.entrySet()) {
            buf.writeResourceLocation(BuiltInRegistries.ENCHANTMENT.getKey(entry.getKey()));
            buf.writeInt(entry.getValue());
        }
    }

    // --- Decoder ---
    public static SyncSelectionPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<Enchantment, Integer> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            Enchantment ench = BuiltInRegistries.ENCHANTMENT.get(buf.readResourceLocation());
            int level = buf.readInt();
            if (ench != null) map.put(ench, level);
        }
        return new SyncSelectionPacket(map);
    }

    // --- Handler ---
    public static void handle(SyncSelectionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }
            if (mc.player.containerMenu instanceof ArcaneEnchantingTableMenu menu) {
                menu.setSelectedEnchantments(msg.getSelected());
            }
            if (Minecraft.getInstance().screen instanceof ArcaneEnchantingTableScreen screen) {
                screen.onSelectionUpdated(msg.getSelected());
            }
        });
        ctx.get().setPacketHandled(true);
    }


    public Map<Enchantment, Integer> getSelected() {
        return selected;
    }

    // Hilfsfunktion, um von BE alle Spieler mit offenem Menu zu synchronisieren
    public static void sendToClients(ArcaneEnchantingTableBlockEntity be) {
        Map<Enchantment, Integer> selected = be.getSelected();

        be.getLevel().players().forEach(player -> {
            if (player.containerMenu instanceof ArcaneEnchantingTableMenu menu &&
                    menu.getBlockEntity() == be) {
                // ✔ Richtiger Packet-Typ!
                Network.sendToClient((ServerPlayer) player, new SyncSelectionPacket(selected));
            }
        });
    }
}
