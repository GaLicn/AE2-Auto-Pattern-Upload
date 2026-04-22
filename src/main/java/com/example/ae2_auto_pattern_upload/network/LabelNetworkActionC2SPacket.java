package com.example.ae2_auto_pattern_upload.network;

import com.example.ae2_auto_pattern_upload.container.ContainerLabeledWirelessTransceiver;
import com.example.ae2_auto_pattern_upload.tile.TileLabeledWirelessTransceiver;
import com.example.ae2_auto_pattern_upload.wireless.LabeledWirelessTransceiverRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.nio.charset.StandardCharsets;

public class LabelNetworkActionC2SPacket implements IMessage {
    public enum Action {
        SET,
        DELETE,
        DISCONNECT
    }

    private BlockPos pos;
    private String label;
    private Action action;

    public LabelNetworkActionC2SPacket() {
    }

    public LabelNetworkActionC2SPacket(BlockPos pos, String label, Action action) {
        this.pos = pos;
        this.label = label == null ? "" : label;
        this.action = action;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        this.pos = packetBuffer.readBlockPos();
        int length = packetBuffer.readVarInt();
        byte[] bytes = new byte[length];
        packetBuffer.readBytes(bytes);
        this.label = new String(bytes, StandardCharsets.UTF_8);
        this.action = Action.values()[packetBuffer.readInt()];
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        packetBuffer.writeBlockPos(this.pos);
        byte[] bytes = (this.label == null ? "" : this.label).getBytes(StandardCharsets.UTF_8);
        packetBuffer.writeVarInt(bytes.length);
        packetBuffer.writeBytes(bytes);
        packetBuffer.writeInt(this.action.ordinal());
    }

    public static class Handler implements IMessageHandler<LabelNetworkActionC2SPacket, IMessage> {
        @Override
        public IMessage onMessage(LabelNetworkActionC2SPacket message, MessageContext ctx) {
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

            TileLabeledWirelessTransceiver tile = (TileLabeledWirelessTransceiver) tileEntity;
            switch (message.action) {
                case SET:
                    String normalized = LabeledWirelessTransceiverRegistry.normalizeLabel(message.label);
                    if (!normalized.isEmpty()) {
                        tile.setLabel(normalized);
                    }
                    break;
                case DELETE:
                    LabeledWirelessTransceiverRegistry.deleteNetwork(player.world, message.label);
                    break;
                case DISCONNECT:
                    tile.clearLabel();
                    break;
                default:
                    break;
            }

            ModNetwork.CHANNEL.sendTo(LabelNetworkListS2CPacket.fromTile(message.pos, tile), player);
            return null;
        }
    }
}
