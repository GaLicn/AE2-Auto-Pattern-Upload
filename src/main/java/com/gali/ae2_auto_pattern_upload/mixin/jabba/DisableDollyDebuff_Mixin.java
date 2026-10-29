package com.gali.ae2_auto_pattern_upload.mixin.jabba;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcp.mobius.betterbarrels.common.items.dolly.ItemBarrelMover;

/**
 * 禁用手推车搬运桶时的负面效果
 */
@Mixin(value = ItemBarrelMover.class)
public class DisableDollyDebuff_Mixin {

    /**
     * 在携带桶容器时取消更新逻辑以禁用 debuff
     */
    @Inject(
        method = "onUpdate(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;IZ)V",
        at = @At("HEAD"),
        cancellable = true)
    private void NHUtilities$disableDebuff(ItemStack stack, World world, Entity entity, int par4, boolean par5,
        CallbackInfo ci) {
        // 如果世界不是客户端且物品带有容器 NBT 标签并且实体是玩家，则取消整个 onUpdate 方法
        // 这样可以完全禁用 debuff 效果
        if (!world.isRemote && stack.hasTagCompound()
            && stack.getTagCompound()
                .hasKey("Container")
            && entity instanceof EntityPlayer) {
            ci.cancel();
        }
    }
}
