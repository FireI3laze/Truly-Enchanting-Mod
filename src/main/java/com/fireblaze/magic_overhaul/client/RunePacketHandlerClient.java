package com.fireblaze.magic_overhaul.client;

import com.fireblaze.magic_overhaul.client.screen.MonolithScreen;
import com.fireblaze.magic_overhaul.network.RunePacketData;
import net.minecraft.client.Minecraft;

public class RunePacketHandlerClient {
    public static void handleRunePacket(RunePacketData data) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen instanceof MonolithScreen screen) {
                screen.updateRune(data.getRune());
            }
        });
    }
}
