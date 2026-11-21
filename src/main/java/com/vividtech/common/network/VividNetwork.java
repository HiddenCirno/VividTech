package com.vividtech.common.network;

import com.vividtech.VividTech;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class VividNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(VividTech.id("main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private VividNetwork() {}

    public static void init() {
        int id = 0;
        CHANNEL.messageBuilder(TagCraftabilityPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TagCraftabilityPacket::encode)
                .decoder(TagCraftabilityPacket::decode)
                .consumerMainThread(TagCraftabilityPacket::handle)
                .add();
    }
}
