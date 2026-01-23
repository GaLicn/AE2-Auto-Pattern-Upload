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
    private String currentLabel;
    private long currentChannel;

    public LabelListS2CPacket() {}

    public LabelListS2CPacket(int x, int y, int z, String[] labels, long[] channels, String currentLabel,
        long currentChannel) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.labels = labels;
        this.channels = channels;
        this.currentLabel = currentLabel;
        this.currentChannel = currentChannel;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();

        int count = buf.readInt();
        labels = new String[count];
        channels = new long[count];
        for (int i = 0; i < count; i++) {
            labels[i] = ByteBufUtils.readUTF8String(buf);
            channels[i] = buf.readLong();
        }

        currentLabel = ByteBufUtils.readUTF8String(buf);
        currentChannel = buf.readLong();
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
        }

        ByteBufUtils.writeUTF8String(buf, currentLabel != null ? currentLabel : "");
        buf.writeLong(currentChannel);
    }

    public static class Handler implements IMessageHandler<LabelListS2CPacket, IMessage> {

        @Override
        public IMessage onMessage(LabelListS2CPacket message, MessageContext ctx) {
            Minecraft mc = Minecraft.getMinecraft();
            GuiScreen screen = mc.currentScreen;
            if (screen instanceof GuiLabeledWirelessTransceiver) {
                GuiLabeledWirelessTransceiver gui = (GuiLabeledWirelessTransceiver) screen;
                if (gui.isFor(message.x, message.y, message.z)) {
                    gui.updateLabelList(message.labels, message.channels, message.currentLabel, message.currentChannel);
                }
            }
            return null;
        }
    }
}
