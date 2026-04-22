package com.example.ae2_auto_pattern_upload.gui;

import com.example.ae2_auto_pattern_upload.client.gui.GuiLabeledWirelessTransceiver;
import com.example.ae2_auto_pattern_upload.container.ContainerLabeledWirelessTransceiver;
import com.example.ae2_auto_pattern_upload.tile.TileLabeledWirelessTransceiver;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class ModGuiHandler implements IGuiHandler {
    public static final int GUI_LABELED_WIRELESS_TRANSCEIVER = 1;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id != GUI_LABELED_WIRELESS_TRANSCEIVER) {
            return null;
        }

        BlockPos pos = new BlockPos(x, y, z);
        TileEntity tileEntity = world.getTileEntity(pos);
        if (!(tileEntity instanceof TileLabeledWirelessTransceiver)) {
            return null;
        }

        return new ContainerLabeledWirelessTransceiver(player.inventory, pos);
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id != GUI_LABELED_WIRELESS_TRANSCEIVER) {
            return null;
        }

        BlockPos pos = new BlockPos(x, y, z);
        TileEntity tileEntity = world.getTileEntity(pos);
        if (!(tileEntity instanceof TileLabeledWirelessTransceiver)) {
            return null;
        }

        return new GuiLabeledWirelessTransceiver(new ContainerLabeledWirelessTransceiver(player.inventory, pos), pos);
    }
}
