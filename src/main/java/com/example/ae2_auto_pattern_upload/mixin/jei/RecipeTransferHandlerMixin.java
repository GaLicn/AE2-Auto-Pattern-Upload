package com.example.ae2_auto_pattern_upload.mixin.jei;

import appeng.container.implementations.ContainerPatternEncoder;
import com.example.ae2_auto_pattern_upload.util.RecipeNameUtil;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 拦截 JEI 向 AE2 样板终端传输配方时的流程，记录最近一次处理类配方的类别名称，
 * 供供应器选择界面自动填充搜索关键字使用。
 */
@Mixin(targets = "appeng.integration.modules.jei.RecipeTransferHandler", remap = false)
public abstract class RecipeTransferHandlerMixin {

    @Inject(method = "transferRecipe", at = @At("HEAD"))
    private void captureProcessingCategory(Container container,
                                           IRecipeLayout recipeLayout,
                                           EntityPlayer player,
                                           boolean maxTransfer,
                                           boolean doTransfer,
                                           CallbackInfoReturnable<IRecipeTransferError> cir) {
        if (!doTransfer || !(container instanceof ContainerPatternEncoder)) {
            return;
        }

        String uid = recipeLayout.getRecipeCategory().getUid();
        if (uid == null || VanillaRecipeCategoryUid.CRAFTING.equals(uid)) {
            return;
        }

        String keyword = RecipeNameUtil.mapCategoryUidToSearchKey(uid);
        if (keyword == null || keyword.isEmpty()) {
            keyword = RecipeNameUtil.deriveSearchKeyFromClassName(recipeLayout.getRecipeCategory());
        }

        if (keyword != null && !keyword.isEmpty()) {
            RecipeNameUtil.setLastRecipeName(keyword);
        }
    }
}
