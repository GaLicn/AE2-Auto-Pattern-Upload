package com.example.ae2_auto_pattern_upload.network;

import com.example.ae2_auto_pattern_upload.container.ContainerLabeledWirelessTransceiver;
import com.example.ae2_auto_pattern_upload.tile.TileLabeledWirelessTransceiver;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class LabelNetworkListC2SPacket implements IMessage {
    private BlockPos pos;

    public LabelNetworkListC2SPacket() {
    }

    public LabelNetworkListC2SPacket(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = new PacketBuffer(buf).readBlockPos();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        new PacketBuffer(buf).writeBlockPos(this.pos);
    }

    public static class Handler implements IMessageHandler<LabelNetworkListC2SPacket, IMessage> {
        @Override
        public IMessage onMessage(LabelNetworkListC2SPacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null || player.world == null) {
                return null;
            }

            if (!(player.openContainer instanceof ContainerLabeledWirelessTransceiver)) {
                return null;
            }
            ContainerLabeledWirelessTransceiver container =
                    (ContainerLabeledWirelessTransceiver) player.openContainer;
            if (!container.getPos().equals(message.pos)) {
                return null;
            }

            TileEntity tileEntity = player.world.getTileEntity(message.pos);
            if (!(tileEntity instanceof TileLabeledWirelessTransceiver)) {
                return null;
            }

            ModNetwork.CHANNEL.sendTo(
                    LabelNetworkListS2CPacket.fromTile(message.pos, (TileLabeledWirelessTransceiver) tileEntity),
                    player);
            return null;
        }
    }
}
