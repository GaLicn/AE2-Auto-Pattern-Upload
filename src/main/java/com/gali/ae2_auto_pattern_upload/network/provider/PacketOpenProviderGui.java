package com.gali.ae2_auto_pattern_upload.network.provider;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.event.ClickEvent;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.DimensionalCoord;
import appeng.core.sync.GuiBridge;
import appeng.helpers.IInterfaceHost;
import appeng.parts.misc.PartInterface;
import appeng.tile.misc.TileInterface;
import appeng.util.Platform;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import io.netty.buffer.ByteBuf;
import reobf.proghatches.gt.metatileentity.DualInputHatch;

/**
 * 打开供应器(Interface)界面的数据包
 * 客户端发送供应器位置，服务器端打开对应的GUI
 */
public class PacketOpenProviderGui implements IMessage {

    private int x;
    private int y;
    private int z;
    private int dimension;
    private int sideOrdinal;

    public PacketOpenProviderGui() {}

    public PacketOpenProviderGui(DimensionalCoord coord, ForgeDirection side) {
        this.x = coord.x;
        this.y = coord.y;
        this.z = coord.z;
        this.dimension = coord.getDimension();
        this.sideOrdinal = side.ordinal();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.x = buf.readInt();
        this.y = buf.readInt();
        this.z = buf.readInt();
        this.dimension = buf.readInt();
        this.sideOrdinal = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(dimension);
        buf.writeInt(sideOrdinal);
    }

    public static class Handler implements IMessageHandler<PacketOpenProviderGui, IMessage> {

        @Override
        public IMessage onMessage(PacketOpenProviderGui message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) {
                return null;
            }

            if (player.worldObj.provider.dimensionId != message.dimension) {
                return null;
            }

            World world = player.worldObj;
            if (world == null) {
                return null;
            }

            int x = message.x;
            int y = message.y;
            int z = message.z;
            ForgeDirection side = ForgeDirection.getOrientation(message.sideOrdinal);

            if (!world.blockExists(x, y, z)) {
                return null;
            }

            TileEntity te = world.getTileEntity(x, y, z);
            if (te == null) {
                return null;
            }

            if (te instanceof TileInterface tileInterface) {
                Platform.openGUI(player, tileInterface, side, GuiBridge.GUI_INTERFACE);
                sendTeleportMessage(player, x, y, z);
                return null;
            }

            if (te instanceof IPartHost partHost) {
                for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                    IPart part = partHost.getPart(dir);
                    if (part instanceof PartInterface partInterface) {
                        Platform.openGUI(player, te, dir, GuiBridge.GUI_INTERFACE);
                        sendTeleportMessage(player, x, y, z);
                        return null;
                    }
                }
            }

            if (te instanceof IInterfaceHost interfaceHost) {
                Platform.openGUI(player, te, side, GuiBridge.GUI_INTERFACE);
                sendTeleportMessage(player, x, y, z);
                return null;
            }

            if (te instanceof IGregTechTileEntity gregTechTileEntity) {
                IMetaTileEntity metaTileEntity = gregTechTileEntity.getMetaTileEntity();
                if (metaTileEntity instanceof DualInputHatch dualInputHatch) {
                    dualInputHatch.openGui(player);
                    sendTeleportMessage(player, x, y, z);
                    return null;
                }
            }

            return null;
        }

        private void sendTeleportMessage(EntityPlayerMP player, int x, int y, int z) {
            ChatComponentText message = new ChatComponentText("[" + x + ", " + y + ", " + z + "]");
            String tpCommand = "/tp @p " + x + " " + (y + 1) + " " + z;
            ChatStyle style = new ChatStyle().setColor(EnumChatFormatting.GREEN)
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                .setUnderlined(true);
            message.setChatStyle(style);

            ChatComponentTranslation prefix = new ChatComponentTranslation(
                "ae2_auto_pattern_upload.info.provider_location");
            prefix.appendSibling(message);

            player.addChatMessage(prefix);
        }
    }
}
