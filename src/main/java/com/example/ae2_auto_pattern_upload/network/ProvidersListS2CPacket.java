package com.example.ae2_auto_pattern_upload.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * S2C: 返回供应器列表到客户端
 */
public class ProvidersListS2CPacket implements IMessage {
    public static final int MAX_PROVIDER_NAME_BYTES = 262144;
    private static final int MAX_PROVIDER_COUNT = 10000;
    private static final String DEFAULT_PROVIDER_NAME = "Crafting Provider";

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
        PacketBuffer packetBuf = new PacketBuffer(buf);
        int size = packetBuf.readInt();
        if (size < 0 || size > MAX_PROVIDER_COUNT) {
            throw new IllegalArgumentException("Invalid providers list size: " + size);
        }

        ids = new ArrayList<>(size);
        names = new ArrayList<>(size);
        emptySlots = new ArrayList<>(size);
        
        for (int i = 0; i < size; i++) {
            ids.add(packetBuf.readLong());
            int nameBytesLength = packetBuf.readVarInt();
            if (nameBytesLength < 0 || nameBytesLength > MAX_PROVIDER_NAME_BYTES) {
                throw new IllegalArgumentException("Invalid provider name length: " + nameBytesLength);
            }

            byte[] nameBytes = new byte[nameBytesLength];
            packetBuf.readBytes(nameBytes);
            names.add(new String(nameBytes, StandardCharsets.UTF_8));
            emptySlots.add(packetBuf.readInt());
        }
    }
    
    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer packetBuf = new PacketBuffer(buf);
        int size = Math.min(ids.size(), Math.min(names.size(), emptySlots.size()));
        packetBuf.writeInt(size);

        for (int i = 0; i < size; i++) {
            packetBuf.writeLong(ids.get(i));
            byte[] nameBytes = normalizeProviderName(names.get(i)).getBytes(StandardCharsets.UTF_8);
            if (nameBytes.length > MAX_PROVIDER_NAME_BYTES) {
                throw new IllegalArgumentException("Provider name exceeds max bytes: " + nameBytes.length);
            }

            packetBuf.writeVarInt(nameBytes.length);
            packetBuf.writeBytes(nameBytes);
            packetBuf.writeInt(emptySlots.get(i));
        }
    }

    public static String normalizeProviderName(String name) {
        if (name == null || name.isEmpty()) {
            return DEFAULT_PROVIDER_NAME;
        }
        return name;
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
