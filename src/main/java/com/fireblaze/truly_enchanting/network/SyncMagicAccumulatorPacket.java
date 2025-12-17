package com.fireblaze.truly_enchanting.network;

import com.fireblaze.truly_enchanting.blockentity.EnchantingTable.ArcaneEnchantingTableBlockEntity;
import com.fireblaze.truly_enchanting.blockentity.EnchantingTable.MagicAccumulator;
import com.fireblaze.truly_enchanting.blockentity.MonolithBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncMagicAccumulatorPacket {

    private final BlockPos sourcePos;
    private final float currentMagicPower;
    private final int rate;
    private final float maxMagicPower;
    private final Map<Block, Integer> blockCurrentPowerMap;
    private final Map<TagKey<Block>, Integer> blockTagCurrentPowerMap;


    public SyncMagicAccumulatorPacket(BlockPos pos, MagicAccumulator accumulator) {
        this.sourcePos = pos;
        this.currentMagicPower = accumulator.getCurrentMagicPower();
        this.rate = accumulator.getCurrentMagicPowerIncreaseRate();
        this.maxMagicPower = accumulator.getMagicPowerCapPerPlayerSoft();
        this.blockCurrentPowerMap = new HashMap<>(accumulator.getBlockCurrentPowerMap());
        this.blockTagCurrentPowerMap = new HashMap<>(accumulator.getBlockTagCurrentPowerMap());
    }


    public SyncMagicAccumulatorPacket(BlockPos pos, float currentMagicPower, int currentMagicPowerIncreaseRate, float maxMagicPower,
                                      Map<Block, Integer> map1, Map<TagKey<Block>, Integer> map2) {
        this.sourcePos = pos;
        this.currentMagicPower = currentMagicPower;
        this.rate = currentMagicPowerIncreaseRate;
        this.maxMagicPower = maxMagicPower;
        this.blockCurrentPowerMap = map1;
        this.blockTagCurrentPowerMap = map2;
    }

    public static void encode(SyncMagicAccumulatorPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.sourcePos);
        buf.writeFloat(pkt.currentMagicPower);
        buf.writeInt(pkt.rate);
        buf.writeFloat(pkt.maxMagicPower);

        buf.writeInt(pkt.blockCurrentPowerMap.size());
        for (Map.Entry<Block, Integer> entry : pkt.blockCurrentPowerMap.entrySet()) {
            buf.writeResourceLocation(BuiltInRegistries.BLOCK.getKey(entry.getKey()));
            buf.writeInt(entry.getValue());
        }

        buf.writeInt(pkt.blockTagCurrentPowerMap.size());
        for (Map.Entry<TagKey<Block>, Integer> entry : pkt.blockTagCurrentPowerMap.entrySet()) {
            buf.writeResourceLocation(entry.getKey().location());
            buf.writeInt(entry.getValue());
        }
    }

    public static SyncMagicAccumulatorPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        float current = buf.readFloat();
        int rate = buf.readInt();
        float max = buf.readFloat();

        int blockSize = buf.readInt();
        Map<Block, Integer> blockMap = new HashMap<>();
        for (int i = 0; i < blockSize; i++) {
            Block block = BuiltInRegistries.BLOCK.get(buf.readResourceLocation());
            int value = buf.readInt();
            blockMap.put(block, value);
        }

        int tagSize = buf.readInt();
        Map<TagKey<Block>, Integer> tagMap = new HashMap<>();
        for (int i = 0; i < tagSize; i++) {
            ResourceLocation rl = buf.readResourceLocation();
            TagKey<Block> tagKey = TagKey.create(BuiltInRegistries.BLOCK.key(), rl);
            int value = buf.readInt();
            tagMap.put(tagKey, value);
        }

        return new SyncMagicAccumulatorPacket(pos, current, rate, max, blockMap, tagMap);
    }

    public static void handle(SyncMagicAccumulatorPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {

            // WICHTIG: Client-Welt holen
            var mc = Minecraft.getInstance();
            var level = mc.level;
            if (level == null) return; // sollte nie passieren, aber sicher ist sicher

            var be = level.getBlockEntity(pkt.sourcePos);
            if (be instanceof ArcaneEnchantingTableBlockEntity table) {

                var acc = table.getMagicAccumulator();
                acc.setArcaneEnchantingTableBE(table);
                acc.setAccumulatedMagicPower(pkt.currentMagicPower);
                acc.setCurrentMagicPowerIncreaseRate(pkt.rate);
                acc.setMagicPowerCapPerPlayerSoft(pkt.maxMagicPower);

                acc.clearCurrentPowerForBlock();
                pkt.blockCurrentPowerMap.forEach(acc::addToBlockCurrentPowerMap);

                acc.clearCurrentPowerForTag();
                pkt.blockTagCurrentPowerMap.forEach(acc::addToTagCurrentPowerMap);

                acc.clearMonolithsCurrentPower();
            }

            if (be instanceof MonolithBlockEntity monolith) {

                var acc = monolith.getMagicAccumulator();
                acc.setCurrentMagicPowerIncreaseRate(pkt.rate);
                acc.setMagicPowerCapPerPlayerSoft(pkt.maxMagicPower);

                acc.clearCurrentPowerForBlock();
                pkt.blockCurrentPowerMap.forEach(acc::addToBlockCurrentPowerMap);

                acc.clearCurrentPowerForTag();
                pkt.blockTagCurrentPowerMap.forEach(acc::addToTagCurrentPowerMap);
            }
        });

        ctx.get().setPacketHandled(true);
    }

    public static void sendToClient(ServerPlayer player, BlockPos pos, MagicAccumulator accumulator) {
        Network.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncMagicAccumulatorPacket(pos, accumulator)
        );
    }
}
