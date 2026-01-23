package com.gali.ae2_auto_pattern_upload.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gali.ae2_auto_pattern_upload.tile.TileLabeledWirelessTransceiver;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 应用/清除标签的网络包
 */
public class PacketApplyLabel implements IMessage {

    private int x, y, z;
    private String label;
    private boolean clear;

    public PacketApplyLabel() {}

    public PacketApplyLabel(int x, int y, int z, String label, boolean clear) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.label = label;
        this.clear = clear;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        label = ByteBufUtils.readUTF8String(buf);
        clear = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        ByteBufUtils.writeUTF8String(buf, label != null ? label : "");
        buf.writeBoolean(clear);
    }

    public static class Handler implements IMessageHandler<PacketApplyLabel, IMessage> {

        @Override
        public IMessage onMessage(PacketApplyLabel msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) {
                return null;
            }

            World world = player.worldObj;
            if (world == null || !world.blockExists(msg.x, msg.y, msg.z)) {
                return null;
            }

            double dx = msg.x + 0.5D;
            double dy = msg.y + 0.5D;
            double dz = msg.z + 0.5D;
            if (player.getDistanceSq(dx, dy, dz) > 64.0D) {
                return null;
            }

            TileEntity te = world.getTileEntity(msg.x, msg.y, msg.z);
            if (!(te instanceof TileLabeledWirelessTransceiver)) {
                return null;
            }

            TileLabeledWirelessTransceiver tile = (TileLabeledWirelessTransceiver) te;
            if (tile.getPlacerId() != null && !tile.getPlacerId()
                .equals(player.getUniqueID())) {
                return null;
            }

            if (msg.clear) {
                tile.clearLabel();
            } else {
                tile.applyLabel(msg.label);
            }
            return null;
        }
    }
}
