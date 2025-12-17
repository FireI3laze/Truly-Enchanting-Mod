package com.fireblaze.truly_enchanting.network;

import com.fireblaze.truly_enchanting.blockentity.EnchantingTable.ArcaneEnchantingTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SyncLinkedMonolithsPacket {

    private final BlockPos tablePos;
    private final List<BlockPos> monoliths;

    public SyncLinkedMonolithsPacket(BlockPos tablePos, List<BlockPos> monoliths) {
        this.tablePos = tablePos;
        this.monoliths = monoliths;
    }

    public static void encode(SyncLinkedMonolithsPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.tablePos);
        buf.writeInt(msg.monoliths.size());
        for (BlockPos pos : msg.monoliths) {
            buf.writeBlockPos(pos);
        }
    }

    public static SyncLinkedMonolithsPacket decode(FriendlyByteBuf buf) {
        BlockPos tablePos = buf.readBlockPos();
        int size = buf.readInt();
        List<BlockPos> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(buf.readBlockPos());
        }
        return new SyncLinkedMonolithsPacket(tablePos, list);
    }

    public static void handle(SyncLinkedMonolithsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var level = Minecraft.getInstance().level;
            if (level == null) return;

            var be = level.getBlockEntity(msg.tablePos);
            if (be instanceof ArcaneEnchantingTableBlockEntity table) {
                table.getLinkedMonolithManager().setLinkedMonoliths(msg.monoliths);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
