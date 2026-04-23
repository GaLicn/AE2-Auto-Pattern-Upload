package com.example.ae2_auto_pattern_upload.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 网络包注册管理器
 */
public class ModNetwork {
    public static final SimpleNetworkWrapper CHANNEL = 
        NetworkRegistry.INSTANCE.newSimpleChannel("apu");
    
    private static int packetId = 0;
    
    public static void registerPackets() {
        // C2S: 请求供应器列表
        CHANNEL.registerMessage(
            RequestProvidersListPacket.Handler.class,
            RequestProvidersListPacket.class,
            packetId++,
            Side.SERVER);
        
        // S2C: 返回供应器列表
        CHANNEL.registerMessage(
            ProvidersListS2CPacket.Handler.class,
            ProvidersListS2CPacket.class,
            packetId++,
            Side.CLIENT);
        
        // C2S: 上传样板到供应器
        CHANNEL.registerMessage(
            UploadPatternPacket.Handler.class,
            UploadPatternPacket.class,
            packetId++,
            Side.SERVER);

        CHANNEL.registerMessage(
            LabelNetworkListC2SPacket.Handler.class,
            LabelNetworkListC2SPacket.class,
            packetId++,
            Side.SERVER);

        CHANNEL.registerMessage(
            LabelNetworkListS2CPacket.Handler.class,
            LabelNetworkListS2CPacket.class,
            packetId++,
            Side.CLIENT);

        CHANNEL.registerMessage(
            LabelNetworkActionC2SPacket.Handler.class,
            LabelNetworkActionC2SPacket.class,
            packetId++,
            Side.SERVER);

        CHANNEL.registerMessage(
            OpenCraftAmountFromBookmarkPacket.Handler.class,
            OpenCraftAmountFromBookmarkPacket.class,
            packetId++,
            Side.SERVER);
    }
}
