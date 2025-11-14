package com.example.ae2_auto_pattern_upload.mixin.jei;

import appeng.container.implementations.ContainerPatternEncoder;
import com.example.ae2_auto_pattern_upload.util.RecipeNameUtil;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.gui.recipes.RecipeLayout;
import mezz.jei.transfer.RecipeTransferUtil;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在 HEI/JEI 的通用配方传输出口统一捕获 AE2 样板终端的配方分类，
 * 以便在各种整合包和扩展环境中都能可靠地记录最近一次处理配方的名称。
 */
@Mixin(value = RecipeTransferUtil.class, remap = false)
public abstract class RecipeTransferUtilMixin {

    @Inject(
        method = "transferRecipe(Lnet/minecraft/inventory/Container;Lmezz/jei/gui/recipes/RecipeLayout;Lnet/minecraft/entity/player/EntityPlayer;Z)Z",
        at = @At("HEAD")
    )
    private static void ae2_auto_pattern_upload$captureProcessingCategory(Container container,
                                                                          RecipeLayout recipeLayout,
                                                                          EntityPlayer player,
                                                                          boolean maxTransfer,
                                                                          CallbackInfoReturnable<Boolean> cir) {
        if (!(container instanceof ContainerPatternEncoder)) {
            return;
        }

        String uid = recipeLayout.getRecipeCategory().getUid();
        if (uid == null || VanillaRecipeCategoryUid.CRAFTING.equals(uid)) {
            return;
        }

        System.out.println("[AE2 Auto Pattern Upload][Mixin] " + I18n.format("ae2_auto_pattern_upload.mixin.recipe_category_captured", uid));

        String keyword = RecipeNameUtil.mapCategoryUidToSearchKey(uid);
        if (keyword == null || keyword.isEmpty()) {
            keyword = RecipeNameUtil.deriveSearchKeyFromClassName(recipeLayout.getRecipeCategory());
        }

        if (keyword != null && !keyword.isEmpty()) {
            System.out.println("[AE2 Auto Pattern Upload][Mixin] " + I18n.format("ae2_auto_pattern_upload.mixin.mapped_keyword", keyword));
            RecipeNameUtil.setLastRecipeName(keyword);
        }
    }
}
