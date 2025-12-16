package com.fireblaze.magic_overhaul.network;

import com.fireblaze.magic_overhaul.item.MagicEssenceItem;
import com.fireblaze.magic_overhaul.util.BindingManager;
import com.fireblaze.magic_overhaul.blockentity.EnchantingTable.ArcaneEnchantingTableBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ConsumeMagicEssencePacket {
    private final int amount;

    public ConsumeMagicEssencePacket(int amount) {
        this.amount = amount;
    }

    public static void encode(ConsumeMagicEssencePacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.amount);
    }

    public static ConsumeMagicEssencePacket decode(FriendlyByteBuf buf) {
        return new ConsumeMagicEssencePacket(buf.readInt());
    }

    public static void handle(ConsumeMagicEssencePacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            // Cursor/Carried stack (ItemStack auf der Maus)
            var carried = player.containerMenu.getCarried();
            if (carried.isEmpty()) return;
            if (!(carried.getItem() instanceof MagicEssenceItem)) return;

            // require Charged
            var tag = carried.getTag();
            boolean charged = tag != null && tag.getBoolean(MagicEssenceItem.NBT_CHARGED);
            if (!charged) return;

            // actual amount to consume
            int actual = Math.min(pkt.amount, carried.getCount());
            if (actual <= 0) return;

            // bound table
            var boundTable = BindingManager.getBoundTable(player);
            if (boundTable == null || boundTable.pos() == null || boundTable.dimension() == null) return;
            var level = player.level();
            if (!(level.dimension() == boundTable.dimension())) return;
            var be = level.getBlockEntity(boundTable.pos());
            if (!(be instanceof ArcaneEnchantingTableBlockEntity tableBE)) return;
            if (!(boundTable.pos().equals(tableBE.getBlockPos()))) return;

            // perform conversion: add magic power and consume items
            if (!tableBE.getMagicAccumulator().magicEssenceToMagicPower(actual)) return;

            carried.shrink(actual);
            tableBE.setChanged();
        });
        ctx.setPacketHandled(true);
    }
}
