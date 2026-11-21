package com.vividtech.client.network;

import com.vividtech.client.gui.PatternReplacerScreen;
import com.vividtech.common.network.TagCraftabilityPacket;

import net.minecraft.client.Minecraft;

public final class ClientPacketHandler {

    private ClientPacketHandler() {}

    public static void applyCraftability(TagCraftabilityPacket packet) {
        if (Minecraft.getInstance().screen instanceof PatternReplacerScreen screen) {
            screen.applyCraftability(packet.tagId(), packet.flags());
        }
    }
}
