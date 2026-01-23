package com.gali.ae2_auto_pattern_upload.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import com.gali.ae2_auto_pattern_upload.client.gui.GuiLabeledWirelessTransceiver;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 服务端发送标签列表给客户端（S2C）
 */
public class LabelListS2CPacket implements IMessage {

    private int x, y, z;
    private String[] labels;
    private long[] channels;
    private int[] onlineCounts; // 每个标签的在线数
    private String currentLabel;
    private long currentChannel;
    private int currentOnlineCount; // 当前标签的在线数

    public LabelListS2CPacket() {}

    public LabelListS2CPacket(int x, int y, int z, String[] labels, long[] channels, int[] onlineCounts,
        String currentLabel, long currentChannel, int currentOnlineCount) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.labels = labels;
        this.channels = channels;
        this.onlineCounts = onlineCounts;
        this.currentLabel = currentLabel;
        this.currentChannel = currentChannel;
        this.currentOnlineCount = currentOnlineCount;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();

        int count = buf.readInt();
        labels = new String[count];
        channels = new long[count];
        onlineCounts = new int[count];
        for (int i = 0; i < count; i++) {
            labels[i] = ByteBufUtils.readUTF8String(buf);
            channels[i] = buf.readLong();
            onlineCounts[i] = buf.readInt();
        }

        currentLabel = ByteBufUtils.readUTF8String(buf);
        currentChannel = buf.readLong();
        currentOnlineCount = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);

        buf.writeInt(labels.length);
        for (int i = 0; i < labels.length; i++) {
            ByteBufUtils.writeUTF8String(buf, labels[i]);
            buf.writeLong(channels[i]);
            buf.writeInt(onlineCounts[i]);
        }

        ByteBufUtils.writeUTF8String(buf, currentLabel != null ? currentLabel : "");
        buf.writeLong(currentChannel);
        buf.writeInt(currentOnlineCount);
    }

    public static class Handler implements IMessageHandler<LabelListS2CPacket, IMessage> {

        @Override
        public IMessage onMessage(LabelListS2CPacket message, MessageContext ctx) {
            Minecraft mc = Minecraft.getMinecraft();
            GuiScreen screen = mc.currentScreen;
            if (screen instanceof GuiLabeledWirelessTransceiver) {
                GuiLabeledWirelessTransceiver gui = (GuiLabeledWirelessTransceiver) screen;
                if (gui.isFor(message.x, message.y, message.z)) {
                    gui.updateLabelList(
                        message.labels,
                        message.channels,
                        message.onlineCounts,
                        message.currentLabel,
                        message.currentChannel,
                        message.currentOnlineCount);
                }
            }
            return null;
        }
    }
}
