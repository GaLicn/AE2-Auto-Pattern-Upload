package com.gali.network;

import com.gali.client.gui.ProviderSelectScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
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
    
    public ProvidersListS2CPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        this.ids = new ArrayList<>(size);
        this.names = new ArrayList<>(size);
        this.emptySlots = new ArrayList<>(size);
        
        for (int i = 0; i < size; i++) {
            ids.add(buf.readLong());
            names.add(buf.readUtf());
            emptySlots.add(buf.readInt());
        }
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            buf.writeLong(ids.get(i));
            buf.writeUtf(names.get(i));
            buf.writeInt(emptySlots.get(i));
        }
    }
    
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new ProviderSelectScreen(ids, names, emptySlots));
        });
        ctx.get().setPacketHandled(true);
    }
}
