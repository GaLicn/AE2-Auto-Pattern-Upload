package com.gali.ae2_auto_pattern_upload.mixin.nei;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gali.ae2_auto_pattern_upload.util.RecipeNameUtil;

import codechicken.nei.recipe.DefaultOverlayHandler;
import codechicken.nei.recipe.IRecipeHandler;

@Mixin(value = DefaultOverlayHandler.class, remap = false)
public abstract class MixinDefaultOverlayHandler {

    @Inject(
        method = "transferRecipe(Lnet/minecraft/client/gui/inventory/GuiContainer;Lcodechicken/nei/recipe/IRecipeHandler;II)I",
        at = @At("HEAD"))
    private void ae2AutoPatternUpload$captureRecipe(GuiContainer gui, IRecipeHandler handler, int recipeIndex,
        int multiplier, CallbackInfoReturnable<Integer> cir) {
        if (handler != null) {
            RecipeNameUtil.captureFromRecipeHandler(handler);
        }
    }
}
