package com.gali.ae2_auto_pattern_upload.mixin.nei;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gali.ae2_auto_pattern_upload.util.RecipeNameUtil;

import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.RecipeHandlerRef;

@Mixin(value = RecipeHandlerRef.class, remap = false)
public abstract class MixinRecipeHandlerRef {

    @Shadow(remap = false)
    public IRecipeHandler handler;

    @Inject(
        method = "fillCraftingGrid(Lnet/minecraft/client/gui/inventory/GuiContainer;I)V",
        at = @At("HEAD"))
    private void ae2AutoPatternUpload$captureFromFill(GuiContainer gui, int multiplier, CallbackInfo ci) {
        ae2AutoPatternUpload$captureRecipeName();
    }

    @Inject(
        method = "craft(Lnet/minecraft/client/gui/inventory/GuiContainer;I)Z",
        at = @At("HEAD"))
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
