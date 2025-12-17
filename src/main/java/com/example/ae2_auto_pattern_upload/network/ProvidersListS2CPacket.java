package com.example.ae2_auto_pattern_upload.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

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
            if (ctx == null || ctx.side != Side.CLIENT) {
                return null;
            }
            // 在客户端线程上执行
            try {
                Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
                final Object mc = mcClass.getMethod("getMinecraft").invoke(null);

                Runnable task = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Class<?> guiClass = Class.forName("com.example.ae2_auto_pattern_upload.client.gui.GuiProviderSelect");
                            Object gui = guiClass
                                .getConstructor(List.class, List.class, List.class)
                                .newInstance(message.ids, message.names, message.emptySlots);
                            mcClass.getMethod("displayGuiScreen", Class.forName("net.minecraft.client.gui.GuiScreen"))
                                .invoke(mc, gui);
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                };

                mcClass.getMethod("addScheduledTask", Runnable.class).invoke(mc, task);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            
            return null;
        }
    }
}
