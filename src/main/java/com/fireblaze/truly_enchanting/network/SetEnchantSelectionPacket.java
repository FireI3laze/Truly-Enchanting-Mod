package com.fireblaze.truly_enchanting.network;

import com.fireblaze.truly_enchanting.blockentity.EnchantingTable.ArcaneEnchantingTableBlockEntity;
import com.fireblaze.truly_enchanting.menu.ArcaneEnchantingTableMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

public class SetEnchantSelectionPacket {

    private final Enchantment enchantment;
    private final int level;
    private final boolean clearAll;

    public SetEnchantSelectionPacket(Enchantment ench, int level) {
        this.enchantment = ench;
        this.level = level;
        this.clearAll = false;
    }

    // Clear-All Packet
    public SetEnchantSelectionPacket(boolean clearAll) {
        this.enchantment = null;
        this.level = 0;
        this.clearAll = clearAll;
    }

    public static void encode(SetEnchantSelectionPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.clearAll);
        if (!msg.clearAll) {
            buf.writeResourceLocation(Objects.requireNonNull(BuiltInRegistries.ENCHANTMENT.getKey(msg.enchantment)));
            buf.writeInt(msg.level);
        }
    }

    // decode
    public static SetEnchantSelectionPacket decode(FriendlyByteBuf buf) {
        boolean clearAll = buf.readBoolean();
        if (clearAll) {
            return new SetEnchantSelectionPacket(true);
        } else {
            Enchantment ench = BuiltInRegistries.ENCHANTMENT.get(buf.readResourceLocation());
            int level = buf.readInt();
            return new SetEnchantSelectionPacket(ench, level);
        }
    }

    public static void handle(SetEnchantSelectionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (player.containerMenu instanceof ArcaneEnchantingTableMenu menu) {
                ArcaneEnchantingTableBlockEntity be = menu.getBlockEntity();
                // Server updatet BE
                be.setEnchantmentLevel(msg.getEnchantment(), msg.getLevel());
            }
        });
        ctx.get().setPacketHandled(true);
    }


    public boolean isClearAll() {
        return clearAll;
    }

    public Enchantment getEnchantment() {
        return enchantment;
    }

    public int getLevel() {
        return level;
    }
}
