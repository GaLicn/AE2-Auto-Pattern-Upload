package com.gali.ae2_auto_pattern_upload.mixin.ae2;

import java.util.LinkedList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.block.AEBaseTileBlock;
import appeng.tile.AEBaseTile;
import appeng.util.Platform;

/**
 * Mixin修改AE2的AEBaseTileBlock类
 * 当使用扳手shift右键破坏AE2方块时，掉落物优先进入玩家背包
 */
@Mixin(value = AEBaseTileBlock.class, remap = false)
public class MixinAEBaseTileBlock {

    // 使用ThreadLocal存储当前处理的玩家
    private static final ThreadLocal<EntityPlayer> currentPlayer = new ThreadLocal<>();

    /**
     * 在onBlockActivated方法中，当检测到是扳手shift右键时，保存玩家引用
     * 在removedByPlayer调用之前注入，以便设置disableDrops
     */
    @Inject(
        method = "onBlockActivated",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;removedByPlayer(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;IIIZ)Z",
            ordinal = 0),
        cancellable = false)
    private void onBeforeRemoveBlock(World w, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
        if (player != null && player.isSneaking()) {
            currentPlayer.set(player);
            // 禁用breakBlock中的掉落物生成，因为我们会在onBlockActivated中处理
            AEBaseTile tile = ((AEBaseTileBlock) (Object) this).getTileEntity(w, x, y, z);
            if (tile != null) {
                tile.disableDrops();
            }
        }
    }

    /**
     * Redirect spawnDrops调用，让掉落物优先进入玩家背包
     */
    @Redirect(
        method = "onBlockActivated",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/util/Platform;spawnDrops(Lnet/minecraft/world/World;IIILjava/util/List;)V",
            ordinal = 0))
    private void redirectSpawnDrops(World world, int x, int y, int z, List<ItemStack> drops) {
        EntityPlayer player = currentPlayer.get();

        if (player == null || !Platform.isServer()) {
            Platform.spawnDrops(world, x, y, z, drops);
            return;
        }

        // 清除ThreadLocal
        currentPlayer.remove();

        // 创建一个新的列表存储无法放入背包的物品
        List<ItemStack> remainingDrops = new LinkedList<>();

        for (ItemStack drop : drops) {
            if (drop == null || drop.stackSize <= 0) {
                continue;
            }

            // 尝试将物品放入玩家背包
            ItemStack remaining = addItemToPlayerInventory(player, drop);

            // 如果有剩余物品（背包满了），则加入剩余列表
            if (remaining != null && remaining.stackSize > 0) {
                remainingDrops.add(remaining);
            }
        }

        // 将剩余物品生成到世界
        if (!remainingDrops.isEmpty()) {
            Platform.spawnDrops(world, x, y, z, remainingDrops);
        }
    }

    /**
     * 尝试将物品添加到玩家背包
     * 
     * @param player    玩家
     * @param itemStack 要添加的物品
     * @return 如果有剩余物品无法放入，返回剩余物品；否则返回null
     */
    private static ItemStack addItemToPlayerInventory(EntityPlayer player, ItemStack itemStack) {
        if (player == null || itemStack == null || itemStack.stackSize <= 0) {
            return itemStack;
        }

        InventoryPlayer inventory = player.inventory;
        ItemStack remaining = itemStack.copy();

        // 首先尝试合并到已有堆叠
        for (int i = 0; i < inventory.mainInventory.length && remaining.stackSize > 0; i++) {
            ItemStack slotStack = inventory.mainInventory[i];

            if (slotStack != null && slotStack.isItemEqual(remaining)
                && ItemStack.areItemStackTagsEqual(slotStack, remaining)) {

                int space = slotStack.getMaxStackSize() - slotStack.stackSize;
                int toAdd = Math.min(space, remaining.stackSize);

                if (toAdd > 0) {
                    slotStack.stackSize += toAdd;
                    remaining.stackSize -= toAdd;
                }
            }
        }

        // 然后尝试放入空槽位
        for (int i = 0; i < inventory.mainInventory.length && remaining.stackSize > 0; i++) {
            if (inventory.mainInventory[i] == null) {
                inventory.mainInventory[i] = remaining.copy();
                remaining.stackSize = 0;
                break;
            }
        }

        // 更新客户端
        inventory.markDirty();

        // 强制同步背包到客户端
        if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            ((net.minecraft.entity.player.EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
        }

        return remaining.stackSize > 0 ? remaining : null;
    }
}
