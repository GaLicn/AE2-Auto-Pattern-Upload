package com.gali.ae2_auto_pattern_upload.mixin.gt;

import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import gregtech.common.tools.ToolVajra;

/**
 * 在 Vajra 挖掘后自动收集周围掉落物
 */
@Mixin(value = ToolVajra.class)
public class ToolVajraMixin {

    /**
     * 在方块采集完成后尝试回收周围掉落物
     */
    @Inject(
        method = "onItemUse",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;harvestBlock(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;IIII)V",
            shift = At.Shift.AFTER),
        cancellable = false)
    private void ae2_auto_pattern_upload$collectDropsAfterHarvest(ItemStack stack, EntityPlayer player, World world,
        int x, int y, int z, int side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
        if (world.isRemote) return;

        // 收集方块周围的掉落物
        AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(x - 1.5, y - 1.5, z - 1.5, x + 1.5, y + 1.5, z + 1.5);
        List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, aabb);

        for (EntityItem item : items) {
            if (item.isDead) continue;

            ItemStack drop = item.getEntityItem();
            if (!player.inventory.addItemStackToInventory(drop)) {
                // 背包满了，让物品继续存在
                continue;
            }

            // 成功放入背包，移除掉落物
            item.setDead();
        }

        // 通知客户端更新背包显示
        player.inventoryContainer.detectAndSendChanges();
    }
}
