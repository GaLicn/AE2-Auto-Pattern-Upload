package com.gali.network;

import com.gali.ae2_auto_pattern_upload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ae2_auto_pattern_upload.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        // C2S: 请求供应器列表
        CHANNEL.messageBuilder(RequestProvidersListPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestProvidersListPacket::encode)
                .decoder(RequestProvidersListPacket::decode)
                .consumerMainThread(RequestProvidersListPacket::handle)
                .add();

        // S2C: 返回供应器列表
        CHANNEL.messageBuilder(ProvidersListS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ProvidersListS2CPacket::encode)
                .decoder(ProvidersListS2CPacket::decode)
                .consumerMainThread(ProvidersListS2CPacket::handle)
                .add();

        // C2S: 上传样板到供应器
        CHANNEL.messageBuilder(UploadPatternPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UploadPatternPacket::encode)
                .decoder(UploadPatternPacket::decode)
                .consumerMainThread(UploadPatternPacket::handle)
                .add();
    }
}
