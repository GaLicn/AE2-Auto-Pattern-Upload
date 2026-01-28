package com.gali.network;

import com.gali.client.gui.ProviderSelectScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * S2C: 返回供应器列表到客户端
 */
public class ProvidersListS2CPacket {
    private final List<Long> ids;
    private final List<String> names;
    private final List<Integer> emptySlots;
    
    public ProvidersListS2CPacket(List<Long> ids, List<String> names, List<Integer> emptySlots) {
        this.ids = ids;
        this.names = names;
        this.emptySlots = emptySlots;
    }
    
    public static ProvidersListS2CPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Long> ids = new ArrayList<>(size);
        List<String> names = new ArrayList<>(size);
        List<Integer> emptySlots = new ArrayList<>(size);
        
        for (int i = 0; i < size; i++) {
            ids.add(buf.readLong());
            names.add(buf.readUtf());
            emptySlots.add(buf.readVarInt());
        }
        return new ProvidersListS2CPacket(ids, names, emptySlots);
    }
    
    public static void encode(ProvidersListS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.ids.size());
        for (int i = 0; i < msg.ids.size(); i++) {
            buf.writeLong(msg.ids.get(i));
            buf.writeUtf(msg.names.get(i));
            buf.writeVarInt(msg.emptySlots.get(i));
        }
    }
    
    public static void handle(ProvidersListS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(msg));
        ctx.get().setPacketHandled(true);
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(ProvidersListS2CPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.setScreen(new ProviderSelectScreen(msg.ids, msg.names, msg.emptySlots));
    }
}
