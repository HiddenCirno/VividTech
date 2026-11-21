package com.vividtech.common.network;

import com.vividtech.client.network.ClientPacketHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：某个标签内物品是否可以在当前 ME 网络中合成。
 * 数组顺序与 {@code PatternReplacerData.itemsFor(tag)} 完全一致。
 */
public record TagCraftabilityPacket(ResourceLocation tagId, boolean[] flags) {

    public static void encode(TagCraftabilityPacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.tagId);
        buf.writeVarInt(packet.flags.length);
        for (boolean flag : packet.flags) {
            buf.writeBoolean(flag);
        }
    }

    public static TagCraftabilityPacket decode(FriendlyByteBuf buf) {
        ResourceLocation tagId = buf.readResourceLocation();
        int size = buf.readVarInt();
        boolean[] flags = new boolean[size];
        for (int i = 0; i < size; i++) {
            flags[i] = buf.readBoolean();
        }
        return new TagCraftabilityPacket(tagId, flags);
    }

    public static void handle(TagCraftabilityPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.applyCraftability(packet)));
        context.setPacketHandled(true);
    }
}
