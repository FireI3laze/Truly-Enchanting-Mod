package com.fireblaze.truly_enchanting.client;

import com.fireblaze.truly_enchanting.client.screen.MonolithScreen;
import com.fireblaze.truly_enchanting.network.RunePacketData;
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
