package com.gali.ae2_auto_pattern_upload.mixin.nei;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gali.ae2_auto_pattern_upload.util.RecipeNameUtil;

import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.RecipeHandlerRef;

/**
 * 在 NEI 配方填充与合成前记录当前配方名称
 */
@Mixin(value = RecipeHandlerRef.class, remap = false)
public abstract class MixinRecipeHandlerRef {

    @Final
    @Shadow(remap = false)
    public IRecipeHandler handler;

    /**
     * 在填充配方前缓存当前配方名称
     */
    @Inject(method = "fillCraftingGrid(Lnet/minecraft/client/gui/inventory/GuiContainer;I)V", at = @At("HEAD"))
    private void ae2AutoPatternUpload$captureFromFill(GuiContainer gui, int multiplier, CallbackInfo ci) {
        // 从当前配方处理器提取配方名称
        ae2AutoPatternUpload$captureRecipeName();
    }

    /**
     * 在直接合成前缓存当前配方名称
     */
    @Inject(method = "craft(Lnet/minecraft/client/gui/inventory/GuiContainer;I)Z", at = @At("HEAD"))
    private void ae2AutoPatternUpload$captureFromCraft(GuiContainer gui, int multiplier,
        CallbackInfoReturnable<Boolean> cir) {
        ae2AutoPatternUpload$captureRecipeName();
    }

    private void ae2AutoPatternUpload$captureRecipeName() {
        if (this.handler != null) {
            RecipeNameUtil.captureFromRecipeHandler(this.handler);
        }
    }
}
