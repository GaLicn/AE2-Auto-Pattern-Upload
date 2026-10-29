package com.gali.ae2_auto_pattern_upload.mixin.tconstruct;

import java.lang.ref.WeakReference;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.gali.ae2_auto_pattern_upload.util.CombinedAdjacentInventory;

import tconstruct.tools.inventory.CraftingStationContainer;
import tconstruct.tools.logic.CraftingStationLogic;
import tconstruct.tools.logic.FurnaceLogic;
import tconstruct.tools.logic.PatternChestLogic;
import tconstruct.tools.logic.ToolStationLogic;

@Mixin(value = CraftingStationLogic.class, remap = false)
public abstract class CraftingStationLogicMixin {

    @Shadow
    public ForgeDirection chestDirection;
    @Shadow
    public int chestSize;
    @Shadow
    public WeakReference<IInventory> chest;
    @Shadow
    public WeakReference<IInventory> doubleChest;
    @Shadow
    public WeakReference<IInventory> patternChest;
    @Shadow
    public WeakReference<IInventory> furnace;
    @Shadow
    public boolean tinkerTable;
    @Shadow
    public int invRows;
    @Shadow
    public int invColumns;
    @Shadow
    public int slotCount;

    @Shadow
    private boolean isBlacklisted(Class<? extends TileEntity> clazz) {
        return false;
    }

    /**
     * @author C-H716
     * @reason 让合成站同时挂载六个方向可访问的相邻容器
     */
    @Overwrite
    public Container getGuiContainer(InventoryPlayer inventoryplayer, World world, int x, int y, int z) {
        chest = null;
        chestSize = 0;
        slotCount = 0;
        chestDirection = ForgeDirection.UNKNOWN;
        doubleChest = null;
        patternChest = null;
        furnace = null;
        tinkerTable = false;

        CombinedAdjacentInventory combinedInventory = new CombinedAdjacentInventory();

        for (final ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            final int xPos = x + dir.offsetX, yPos = y + dir.offsetY, zPos = z + dir.offsetZ;
            final TileEntity tile = world.getTileEntity(xPos, yPos, zPos);
            if (!(tile instanceof IInventory inv) || (tile instanceof CraftingStationLogic)
                || isBlacklisted(tile.getClass())) continue;

            if (patternChest == null && tile instanceof PatternChestLogic) {
                patternChest = new WeakReference<>(inv);
                continue;
            } else if (furnace == null && (tile instanceof TileEntityFurnace || tile instanceof FurnaceLogic)) {
                furnace = new WeakReference<>(inv);
                continue;
            } else if (!tinkerTable && tile instanceof ToolStationLogic) {
                tinkerTable = true;
                continue;
            }

            int accessSide = dir.getOpposite()
                .ordinal();
            if (tile instanceof ISidedInventory sidedIvn && sidedIvn.getAccessibleSlotsFromSide(accessSide).length == 0)
                continue;

            if (inv.isUseableByPlayer(inventoryplayer.player)) {
                combinedInventory.addInventory(inv, accessSide);
            }
        }

        if (!combinedInventory.isEmpty()) {
            chest = new WeakReference<>(combinedInventory);
            chestDirection = ForgeDirection.UNKNOWN;
            invColumns = 6;
            chestSize = combinedInventory.getSizeInventory();
            slotCount = chestSize;
            invRows = (int) Math.ceil((double) slotCount / invColumns);
        }

        return new CraftingStationContainer(inventoryplayer, (CraftingStationLogic) (Object) this, x, y, z);
    }
}
