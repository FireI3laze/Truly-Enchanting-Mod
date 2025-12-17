package com.fireblaze.truly_enchanting.network;

import com.fireblaze.truly_enchanting.item.WandItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetWandModePacket(int modeId) {

    public static void encode(SetWandModePacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.modeId);
    }

    public static SetWandModePacket decode(FriendlyByteBuf buf) {
        return new SetWandModePacket(buf.readInt());
    }

    public static void handle(SetWandModePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                WandItem stackItem = (WandItem) player.getMainHandItem().getItem();
                WandItem.setMode(player.getMainHandItem(), WandItem.WandMode.fromId(pkt.modeId));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
