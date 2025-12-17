package com.fireblaze.magic_overhaul.server;

import com.fireblaze.magic_overhaul.blockentity.EnchantingTable.ArcaneEnchantingTableBlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ArcaneTableServerTicker {

    private static int tickCounter = 0;
    private static final int INTERVAL = 1; // 1 = jedes Tick, 20 = 1x pro Sekunde

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = event.getServer();
        if (server == null) return;

        tickCounter++;
        if (tickCounter % INTERVAL != 0) return;

        ArcaneTableRegistry.tickAll();
    }
}
