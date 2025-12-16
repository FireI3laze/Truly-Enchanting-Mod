package com.fireblaze.magic_overhaul.network;

import com.fireblaze.magic_overhaul.util.ClientBindingState;
import com.fireblaze.magic_overhaul.util.BoundTable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.core.Registry;

import java.util.function.Supplier;
import java.util.Objects;

public class SyncBindingPacket {
    private final boolean hasBinding;
    private final BlockPos pos;
    private final ResourceKey<Level> dimension;

    // Konstruktor für Server-seitiges Senden
    public SyncBindingPacket(BlockPos pos, ResourceKey<Level> dimension) {
        this.hasBinding = pos != null && dimension != null;
        this.pos = pos;
        this.dimension = dimension;
    }

    // Decoder (Client empfängt)
    public static SyncBindingPacket decode(FriendlyByteBuf buf) {
        boolean has = buf.readBoolean();
        BlockPos p = null;
        ResourceKey<Level> dim = null;

        if (has) {
            p = buf.readBlockPos();
            String dimString = buf.readUtf();
            ResourceLocation rl = ResourceLocation.tryParse(dimString);
            if (rl != null) {
                dim = ResourceKey.create(Registries.DIMENSION, rl);
            }
        }

        return new SyncBindingPacket(p, dim);
    }

    // Encoder (Server sendet)
    public static void encode(SyncBindingPacket pkt, FriendlyByteBuf buf) {
        buf.writeBoolean(pkt.hasBinding);
        if (pkt.hasBinding && pkt.pos != null && pkt.dimension != null) {
            buf.writeBlockPos(pkt.pos);
            buf.writeUtf(pkt.dimension.location().toString());
        }
    }

    // Handler: läuft auf Network-Thread — schedule auf Client-Thread
    public static void handle(SyncBindingPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            Minecraft.getInstance().execute(() -> {
                if (msg.hasBinding && msg.pos != null && msg.dimension != null) {
                    ClientBindingState.setBoundTable(msg.pos, msg.dimension);
                } else {
                    ClientBindingState.setBoundTable(null, null);
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
