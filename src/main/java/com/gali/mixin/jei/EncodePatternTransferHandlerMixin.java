package com.gali.mixin.jei;

import appeng.integration.modules.jei.transfer.EncodePatternTransferHandler;
import appeng.integration.modules.jeirei.EncodingHelper;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.gali.compat.JeiRuntimeCompat;
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
        
        Recipe<?> recipe = recipeBase instanceof Recipe<?> value ? value : null;
        if (recipe != null && EncodingHelper.isSupportedCraftingRecipe(recipe)) {
            RecipeTypeNameConfig.presetCraftingProviderSearchKey();
            return;
        }

        // 优先读取 JEI 实际分类标题，再使用配方类型或类名作为兼容回退。
        String name = JeiRuntimeCompat.getRecipeCategoryTitle(recipeBase);
        if (name == null || name.isBlank()) {
            name = recipe != null ? RecipeTypeNameConfig.mapRecipeTypeToSearchKey(recipe)
                    : RecipeTypeNameConfig.deriveSearchKeyFromUnknownRecipe(recipeBase);
        }
        
        if (name != null && !name.isBlank()) {
            RecipeTypeNameConfig.setLastProcessingName(name);
        }
    }
}
