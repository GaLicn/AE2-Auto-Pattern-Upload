package com.example.modid.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 网络包注册管理器
 */
public class ModNetwork {
    public static final SimpleNetworkWrapper CHANNEL = 
        NetworkRegistry.INSTANCE.newSimpleChannel("ae2_auto_pattern_upload");
    
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
    }
}
