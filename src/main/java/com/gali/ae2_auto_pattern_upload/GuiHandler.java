package com.gali.ae2_auto_pattern_upload;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gali.ae2_auto_pattern_upload.block.GuiIds;
import com.gali.ae2_auto_pattern_upload.client.gui.GuiLabeledWirelessTransceiver;
import com.gali.ae2_auto_pattern_upload.container.ContainerLabeledWirelessTransceiver;
import com.gali.ae2_auto_pattern_upload.tile.TileLabeledWirelessTransceiver;

import cpw.mods.fml.common.network.IGuiHandler;

/**
 * GUI Handler
 */
public class GuiHandler implements IGuiHandler {

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(x, y, z);

        if (id == GuiIds.LABELED_WIRELESS_TRANSCEIVER && te instanceof TileLabeledWirelessTransceiver) {
            return new ContainerLabeledWirelessTransceiver(player.inventory, (TileLabeledWirelessTransceiver) te);
        }

        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(x, y, z);

        if (id == GuiIds.LABELED_WIRELESS_TRANSCEIVER && te instanceof TileLabeledWirelessTransceiver) {
            return new GuiLabeledWirelessTransceiver(player.inventory, (TileLabeledWirelessTransceiver) te);
        }

        return null;
    }
}
