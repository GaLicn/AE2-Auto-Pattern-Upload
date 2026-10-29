package com.gali.ae2_auto_pattern_upload.mixin.gt;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.PotionEffect;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import gregtech.common.blocks.ItemMachines;

/**
 * 禁用 ItemMachines 更新时附加的负面效果
 */
@Mixin(value = ItemMachines.class)
public class DisableDebuff_Mixin {

    /**
     * 拦截负面效果附加调用以禁用 debuff
     */
    @Redirect(
        method = "onUpdate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/EntityLivingBase;addPotionEffect(Lnet/minecraft/potion/PotionEffect;)V"))
    private void NHUtilities$disableDebuff(EntityLivingBase instance, PotionEffect potionEffect) {
        // 空方法体，用于吞掉原始负面效果调用
    }
}
