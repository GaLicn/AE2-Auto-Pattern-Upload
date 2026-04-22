package com.example.ae2_auto_pattern_upload.network;

import com.example.ae2_auto_pattern_upload.ExampleMod;
import com.example.ae2_auto_pattern_upload.tile.TileLabeledWirelessTransceiver;
import com.example.ae2_auto_pattern_upload.wireless.LabeledWirelessTransceiverRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LabelNetworkListS2CPacket implements IMessage {
    private BlockPos pos;
    private List<String> labels = new ArrayList<>();
    private List<Long> channels = new ArrayList<>();
    private String currentLabel = "";
    private String currentOwner = "";
    private int onlineCount;
    private int usedChannels;
    private int maxChannels = 32;

    public LabelNetworkListS2CPacket() {
    }

    public static LabelNetworkListS2CPacket fromTile(BlockPos pos, TileLabeledWirelessTransceiver tile) {
        LabelNetworkListS2CPacket packet = new LabelNetworkListS2CPacket();
        packet.pos = pos;
        List<LabeledWirelessTransceiverRegistry.LabelNetworkSnapshot> snapshots =
                LabeledWirelessTransceiverRegistry.listNetworks(tile.getWorld());
        for (LabeledWirelessTransceiverRegistry.LabelNetworkSnapshot snapshot : snapshots) {
            packet.labels.add(snapshot.getLabel());
            packet.channels.add(snapshot.getChannel());
        }
        packet.currentLabel = tile.getLabel();
        packet.currentOwner = tile.getOwnerName();
        packet.onlineCount = tile.getLinkedCount();
        packet.usedChannels = tile.getUsedChannels();
        packet.maxChannels = tile.getMaxChannels();
        return packet;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        this.pos = packetBuffer.readBlockPos();
        int size = packetBuffer.readInt();
        this.labels = new ArrayList<>(size);
        this.channels = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int length = packetBuffer.readVarInt();
            byte[] bytes = new byte[length];
            packetBuffer.readBytes(bytes);
            this.labels.add(new String(bytes, StandardCharsets.UTF_8));
            this.channels.add(packetBuffer.readLong());
        }
        this.currentLabel = readString(packetBuffer);
        this.currentOwner = readString(packetBuffer);
        this.onlineCount = packetBuffer.readInt();
        this.usedChannels = packetBuffer.readInt();
        this.maxChannels = packetBuffer.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        packetBuffer.writeBlockPos(this.pos);
        packetBuffer.writeInt(this.labels.size());
        for (int i = 0; i < this.labels.size(); i++) {
            writeString(packetBuffer, this.labels.get(i));
            packetBuffer.writeLong(i < this.channels.size() ? this.channels.get(i) : 0L);
        }
        writeString(packetBuffer, this.currentLabel);
        writeString(packetBuffer, this.currentOwner);
        packetBuffer.writeInt(this.onlineCount);
        packetBuffer.writeInt(this.usedChannels);
        packetBuffer.writeInt(this.maxChannels);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public List<String> getLabels() {
        return this.labels;
    }

    public List<Long> getChannels() {
        return this.channels;
    }

    public String getCurrentLabel() {
        return this.currentLabel;
    }

    public String getCurrentOwner() {
        return this.currentOwner;
    }

    public int getOnlineCount() {
        return this.onlineCount;
    }

    public int getUsedChannels() {
        return this.usedChannels;
    }

    public int getMaxChannels() {
        return this.maxChannels;
    }

    public static class Handler implements IMessageHandler<LabelNetworkListS2CPacket, IMessage> {
        @Override
        public IMessage onMessage(LabelNetworkListS2CPacket message, MessageContext ctx) {
            if (ctx == null || ctx.side != Side.CLIENT) {
                return null;
            }
            ExampleMod.PROXY.handleLabelNetworkListS2C(message);
            return null;
        }
    }

    private static void writeString(PacketBuffer packetBuffer, String text) {
        byte[] bytes = (text == null ? "" : text).getBytes(StandardCharsets.UTF_8);
        packetBuffer.writeVarInt(bytes.length);
        packetBuffer.writeBytes(bytes);
    }

    private static String readString(PacketBuffer packetBuffer) {
        int length = packetBuffer.readVarInt();
        byte[] bytes = new byte[length];
        packetBuffer.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
