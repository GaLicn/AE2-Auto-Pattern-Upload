package com.example.ae2_auto_pattern_upload.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C: 返回供应器列表到客户端
 */
public class ProvidersListS2CPacket implements IMessage {
    private List<Long> ids;
    private List<String> names;
    private List<Integer> emptySlots;
    
    public ProvidersListS2CPacket() {
        this.ids = new ArrayList<>();
        this.names = new ArrayList<>();
        this.emptySlots = new ArrayList<>();
    }
    
    public ProvidersListS2CPacket(List<Long> ids, List<String> names, List<Integer> emptySlots) {
        this.ids = ids;
        this.names = names;
        this.emptySlots = emptySlots;
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        ids = new ArrayList<>(size);
        names = new ArrayList<>(size);
        emptySlots = new ArrayList<>(size);
        
        for (int i = 0; i < size; i++) {
            ids.add(buf.readLong());
            int nameLen = buf.readInt();
            byte[] nameBytes = new byte[nameLen];
            buf.readBytes(nameBytes);
            names.add(new String(nameBytes, java.nio.charset.StandardCharsets.UTF_8));
            emptySlots.add(buf.readInt());
        }
    }
    
    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            buf.writeLong(ids.get(i));
            byte[] nameBytes = names.get(i).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            buf.writeInt(nameBytes.length);
            buf.writeBytes(nameBytes);
            buf.writeInt(emptySlots.get(i));
        }
    }
    
    public static class Handler implements IMessageHandler<ProvidersListS2CPacket, IMessage> {
        @Override
        public IMessage onMessage(ProvidersListS2CPacket message, MessageContext ctx) {
            // 在客户端线程上执行
            Minecraft.getMinecraft().addScheduledTask(() -> {
                // 打开供应器选择界面
                Minecraft.getMinecraft().displayGuiScreen(
                    new com.example.ae2_auto_pattern_upload.client.gui.GuiProviderSelect(
                        message.ids, message.names, message.emptySlots));
            });
            
            return null;
        }
    }
}
