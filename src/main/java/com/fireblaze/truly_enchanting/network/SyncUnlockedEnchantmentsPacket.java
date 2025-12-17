package com.fireblaze.truly_enchanting.network;

import com.fireblaze.truly_enchanting.menu.ArcaneEnchantingTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class SyncUnlockedEnchantmentsPacket {

    private final Map<Enchantment, Integer> data;

    public SyncUnlockedEnchantmentsPacket(Map<Enchantment, Integer> data) {
        this.data = data;
    }

    public static void encode(SyncUnlockedEnchantmentsPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.data.size());
        for (var entry : msg.data.entrySet()) {
            buf.writeResourceLocation(Objects.requireNonNull(ForgeRegistries.ENCHANTMENTS.getKey(entry.getKey())));
            buf.writeInt(entry.getValue());
        }
    }

    public static SyncUnlockedEnchantmentsPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<Enchantment, Integer> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(buf.readResourceLocation());
            int level = buf.readInt();
            if (ench != null) map.put(ench, level);
        }
        return new SyncUnlockedEnchantmentsPacket(map);
    }

    public static void handle(SyncUnlockedEnchantmentsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.containerMenu instanceof ArcaneEnchantingTableMenu menu) {
                menu.setUnlockedEnchantments(msg.getData());
            }
        });
        ctx.get().setPacketHandled(true);
    }


    public Map<Enchantment, Integer> getData() {
        return data;
    }
}
