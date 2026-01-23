package com.gali.mixin.jei;

import appeng.integration.modules.jei.transfer.EncodePatternTransferHandler;
import appeng.integration.modules.jeirei.EncodingHelper;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.gali.util.RecipeTypeNameConfig;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 捕获通过 JEI 点击填充到样板编码终端的处理配方，并记录其工艺名称
 */
@Mixin(value = EncodePatternTransferHandler.class, remap = false)
public abstract class EncodePatternTransferHandlerMixin {

    @Inject(method = "transferRecipe", at = @At("HEAD"), require = 0)
    private void captureProcessingName(PatternEncodingTermMenu menu,
                                      Object recipeBase,
                                      IRecipeSlotsView slotsView,
                                      Player player,
                                      boolean maxTransfer,
                                      boolean doTransfer,
                                      CallbackInfoReturnable<IRecipeTransferError> cir) {
        if (!doTransfer) return;
        
        String name = null;
        if (recipeBase instanceof Recipe<?> recipe) {
            // 仅记录处理配方（非合成配方）
            if (EncodingHelper.isSupportedCraftingRecipe(recipe)) return;
            name = RecipeTypeNameConfig.mapRecipeTypeToSearchKey(recipe);
        } else {
            // 非原版 Recipe<?> 的 JEI 条目，尝试从类名推导关键词
            name = RecipeTypeNameConfig.deriveSearchKeyFromUnknownRecipe(recipeBase);
        }
        
        if (name != null && !name.isBlank()) {
            RecipeTypeNameConfig.setLastProcessingName(name);
        }
    }
}
