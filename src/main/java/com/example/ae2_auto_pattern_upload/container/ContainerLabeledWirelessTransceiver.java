package com.example.ae2_auto_pattern_upload.container;

import com.example.ae2_auto_pattern_upload.tile.TileLabeledWirelessTransceiver;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public class ContainerLabeledWirelessTransceiver extends Container {
    private final BlockPos pos;

    public ContainerLabeledWirelessTransceiver(InventoryPlayer inventoryPlayer, BlockPos pos) {
        this.pos = pos;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        if (playerIn == null || playerIn.world == null) {
            return false;
        }

        TileEntity tileEntity = playerIn.world.getTileEntity(this.pos);
        if (!(tileEntity instanceof TileLabeledWirelessTransceiver)) {
            return false;
        }

        return playerIn.getDistanceSq(
                        this.pos.getX() + 0.5D,
                        this.pos.getY() + 0.5D,
                        this.pos.getZ() + 0.5D)
                <= 64.0D;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        return ItemStack.EMPTY;
    }
}
