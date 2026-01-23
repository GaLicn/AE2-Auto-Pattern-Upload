package com.gali.ae2_auto_pattern_upload.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;

import com.gali.ae2_auto_pattern_upload.tile.TileLabeledWirelessTransceiver;

/**
 * 标签无线收发器 Container
 */
public class ContainerLabeledWirelessTransceiver extends Container {

    private final TileLabeledWirelessTransceiver tile;

    public ContainerLabeledWirelessTransceiver(InventoryPlayer playerInv, TileLabeledWirelessTransceiver tile) {
        this.tile = tile;
    }

    public TileLabeledWirelessTransceiver getTile() {
        return tile;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tile != null && !tile.isInvalid()
            && player.getDistanceSq(tile.xCoord + 0.5, tile.yCoord + 0.5, tile.zCoord + 0.5) <= 64;
    }
}
