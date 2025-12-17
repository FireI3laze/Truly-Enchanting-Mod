package com.fireblaze.truly_enchanting.client;

import com.fireblaze.truly_enchanting.TrulyEnchanting;
import com.fireblaze.truly_enchanting.runes.RuneLoader;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.util.HashMap;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        // Desync Runes from Server and Sync with client
        RuneLoader.replaceAll(new HashMap<>());
        File runesDir = new File(
                FMLPaths.CONFIGDIR.get().toFile(),
                "truly_enchanting/runes"
        );

        boolean isSinglePlayer = Minecraft.getInstance().isLocalServer();
        if (isSinglePlayer) RuneLoader.reloadRunes(runesDir, TrulyEnchanting.MODID);
    }
}