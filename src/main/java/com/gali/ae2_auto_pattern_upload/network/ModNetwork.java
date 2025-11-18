package com.gali.ae2_auto_pattern_upload.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class ModNetwork {

    public static final String CHANNEL_ID = "ae2_auto_pattern_upload";
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_ID);

    private static int discriminator = 0;

    private ModNetwork() {}

    public static void registerPackets() {
        CHANNEL.registerMessage(
            RequestProvidersListPacket.Handler.class,
            RequestProvidersListPacket.class,
            discriminator++,
            Side.SERVER);

        CHANNEL.registerMessage(
            ProvidersListS2CPacket.Handler.class,
            ProvidersListS2CPacket.class,
            discriminator++,
            Side.CLIENT);

        CHANNEL.registerMessage(
            UploadPatternPacket.Handler.class,
            UploadPatternPacket.class,
            discriminator++,
            Side.SERVER);
    }
}
