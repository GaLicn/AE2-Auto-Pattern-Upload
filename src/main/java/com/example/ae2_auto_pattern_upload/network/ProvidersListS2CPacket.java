package com.example.ae2_auto_pattern_upload.network;

import com.example.ae2_auto_pattern_upload.ExampleMod;
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

    public List<Long> getIds() {
        return ids;
    }

    public List<String> getNames() {
        return names;
    }

    public List<Integer> getEmptySlots() {
        return emptySlots;
    }
    
    public static class Handler implements IMessageHandler<ProvidersListS2CPacket, IMessage> {
        @Override
        public IMessage onMessage(ProvidersListS2CPacket message, MessageContext ctx) {
            if (ctx == null || ctx.side != Side.CLIENT) {
                return null;
            }
            ExampleMod.PROXY.handleProvidersListS2C(message);
            return null;
        }
    }
}
