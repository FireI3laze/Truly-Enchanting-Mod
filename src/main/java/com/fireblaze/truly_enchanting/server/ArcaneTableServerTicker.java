package com.fireblaze.truly_enchanting.server;

import net.minecraft.server.MinecraftServer;
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
