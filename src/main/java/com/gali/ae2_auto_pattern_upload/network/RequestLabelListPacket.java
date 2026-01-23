package com.gali.ae2_auto_pattern_upload.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gali.ae2_auto_pattern_upload.tile.TileLabeledWirelessTransceiver;
import com.gali.ae2_auto_pattern_upload.wireless.LabelNetworkRegistry;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 客户端请求标签列表（C2S）
 */
public class RequestLabelListPacket implements IMessage {

    private int x, y, z;

    public RequestLabelListPacket() {}

    public RequestLabelListPacket(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
    }

    public static class Handler implements IMessageHandler<RequestLabelListPacket, IMessage> {

        @Override
        public IMessage onMessage(RequestLabelListPacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            World world = player.worldObj;

            TileEntity te = world.getTileEntity(message.x, message.y, message.z);
            if (!(te instanceof TileLabeledWirelessTransceiver)) {
                return null;
            }

            TileLabeledWirelessTransceiver tile = (TileLabeledWirelessTransceiver) te;
            LabelNetworkRegistry registry = LabelNetworkRegistry.get(world);
            if (registry == null) {
                return new LabelListS2CPacket(
                    message.x,
                    message.y,
                    message.z,
                    new String[0],
                    new long[0],
                    new int[0],
                    "",
                    0,
                    0,
                    0);
            }

            // 获取标签列表（不需要所有者过滤，显示所有标签）
            var list = registry.listNetworks(world, null);
            String[] labels = new String[list.size()];
            long[] channels = new long[list.size()];
            int[] onlineCounts = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                labels[i] = list.get(i).label;
                channels[i] = list.get(i).channel;
                onlineCounts[i] = list.get(i).onlineCount;
            }

            String currentLabel = tile.getLabelForDisplay();
            long currentChannel = tile.getFrequency();
            
            // 获取当前标签的在线数
            int currentOnlineCount = 0;
            if (currentLabel != null && !currentLabel.isEmpty()) {
                LabelNetworkRegistry.LabelNetwork network = registry.getNetwork(world, currentLabel, null);
                if (network != null) {
                    currentOnlineCount = network.endpointCount();
                }
            }

            // 在服务器端获取当前节点使用的频道数
            int currentUsedChannels = 0;
            try {
                appeng.api.networking.IGridNode node = tile.getGridNode();
                if (node != null && node.isActive()) {
                    for (appeng.api.networking.IGridConnection gc : node.getConnections()) {
                        currentUsedChannels = Math.max(gc.getUsedChannels(), currentUsedChannels);
                    }
                }
            } catch (Exception e) {
                currentUsedChannels = 0;
            }

            return new LabelListS2CPacket(
                message.x,
                message.y,
                message.z,
                labels,
                channels,
                onlineCounts,
                currentLabel,
                currentChannel,
                currentOnlineCount,
                currentUsedChannels);
        }
    }
}
