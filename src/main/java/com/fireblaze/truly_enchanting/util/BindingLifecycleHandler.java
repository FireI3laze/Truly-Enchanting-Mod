package com.fireblaze.truly_enchanting.util;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BindingLifecycleHandler {

    @SubscribeEvent
    public static void onServerStarting(net.minecraftforge.event.server.ServerStartingEvent event) {
        BindingManager.clearAll();
    }

    @SubscribeEvent
    public static void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        BindingManager.clearAll();
    }
}
