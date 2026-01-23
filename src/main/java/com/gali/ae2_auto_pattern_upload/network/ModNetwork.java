package com.gali.ae2_auto_pattern_upload.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class ModNetwork {

    public static final String CHANNEL_ID = "ae2apu";
    public static final SimpleNetworkWrapper channel = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_ID);

    private static int discriminator = 0;

    private ModNetwork() {}

    public static void registerPackets() {
        channel.registerMessage(
            RequestProvidersListPacket.Handler.class,
            RequestProvidersListPacket.class,
            discriminator++,
            Side.SERVER);

        channel.registerMessage(
            ProvidersListS2CPacket.Handler.class,
            ProvidersListS2CPacket.class,
            discriminator++,
            Side.CLIENT);

        channel.registerMessage(
            UploadPatternPacket.Handler.class,
            UploadPatternPacket.class,
            discriminator++,
            Side.SERVER);

        // 标签无线收发器网络包
        channel.registerMessage(PacketApplyLabel.Handler.class, PacketApplyLabel.class, discriminator++, Side.SERVER);

        // 标签列表同步
        channel.registerMessage(
            RequestLabelListPacket.Handler.class,
            RequestLabelListPacket.class,
            discriminator++,
            Side.SERVER);
        channel
            .registerMessage(LabelListS2CPacket.Handler.class, LabelListS2CPacket.class, discriminator++, Side.CLIENT);
    }
}
