package com.fireblaze.magic_overhaul.network;

import com.fireblaze.magic_overhaul.MagicOverhaul;
import com.fireblaze.magic_overhaul.blockentity.EnchantingTable.ArcaneEnchantingTableBlockEntity;
import com.fireblaze.magic_overhaul.client.RunePacketHandlerClient;
import com.fireblaze.magic_overhaul.util.BindingManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class Network {
    private static final String PROTOCOL = "1.0";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(MagicOverhaul.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals
    );

    public static void register() {
        int id = 0;

        CHANNEL.registerMessage(id++,
                SetEnchantSelectionPacket.class,
                SetEnchantSelectionPacket::encode,
                SetEnchantSelectionPacket::decode,
                SetEnchantSelectionPacket::handle
        );

        CHANNEL.registerMessage(id++,
                SyncUnlockedEnchantmentsPacket.class,
                SyncUnlockedEnchantmentsPacket::encode,
                SyncUnlockedEnchantmentsPacket::decode,
                SyncUnlockedEnchantmentsPacket::handle
        );

        CHANNEL.registerMessage(id++,
                SyncSelectionPacket.class,
                SyncSelectionPacket::encode,
                SyncSelectionPacket::decode,
                SyncSelectionPacket::handle
        );

        CHANNEL.registerMessage(id++,
                SetWandModePacket.class,
                SetWandModePacket::encode,
                SetWandModePacket::decode,
                SetWandModePacket::handle
        );

        CHANNEL.registerMessage(id++,
                SyncLinkedMonolithsPacket.class,
                SyncLinkedMonolithsPacket::encode,
                SyncLinkedMonolithsPacket::decode,
                SyncLinkedMonolithsPacket::handle
        );

        CHANNEL.registerMessage(id++,
                SyncMagicAccumulatorPacket.class,
                SyncMagicAccumulatorPacket::encode,
                SyncMagicAccumulatorPacket::decode,
                SyncMagicAccumulatorPacket::handle
        );

        CHANNEL.registerMessage(id++,
                ConsumeMagicEssencePacket.class,
                ConsumeMagicEssencePacket::encode,
                ConsumeMagicEssencePacket::decode,
                ConsumeMagicEssencePacket::handle
        );

        CHANNEL.registerMessage(id++,
                SyncBindingPacket.class,
                SyncBindingPacket::encode,
                SyncBindingPacket::decode,
                SyncBindingPacket::handle
        );

        CHANNEL.registerMessage(id++,
                RunePacketData.class,
                RunePacketData::encode,
                RunePacketData::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                            RunePacketHandlerClient.handleRunePacket(packet);
                        }
                    });
                    ctx.get().setPacketHandled(true);
                }
        );

        CHANNEL.messageBuilder(
                        SyncRuneDefinitionsPacket.class,
                        id++,
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(SyncRuneDefinitionsPacket::encode)
                .decoder(SyncRuneDefinitionsPacket::decode)
                .consumerMainThread(SyncRuneDefinitionsPacket::handle)
                .add();
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }

    public static void sendToClient(ServerPlayer player, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void syncLinkedMonoliths(ArcaneEnchantingTableBlockEntity be) {
        var list = be.getLinkedMonolithManager().getLinkedMonoliths();
        var packet = new SyncLinkedMonolithsPacket(be.getBlockPos(), list);

        be.getLevel().players().forEach(player -> {
            if (BindingManager.getBoundTable(player) == null) return;
            if (BindingManager.getBoundTable(player).pos().equals(be.getBlockPos())) {
                sendToClient((ServerPlayer) player, packet);
            }
        });
    }

    public static void sendWandModeToServer(int modeId) {
        sendToServer(new SetWandModePacket(modeId));
    }
}
