package com.gali.ae2_auto_pattern_upload.mixin.jei;

import appeng.menu.me.items.PatternEncodingTermMenu;
import com.gali.ae2_auto_pattern_upload.compat.JeiRuntimeCompat;
import com.gali.ae2_auto_pattern_upload.network.PatternUploadUtil;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 捕获 JEI 填充样板时的配方对象，并记录其 JEI 分类标题。 */
@Mixin(targets = "tamaized.ae2jeiintegration.integration.modules.jei.transfer.EncodePatternTransferHandler",
        remap = false)
@Pseudo
public abstract class AE2JeiEncodePatternTransferHandlerMixin {
    @Inject(method = "transferRecipe", at = @At("HEAD"), require = 0, remap = false)
    private void ae2apu$captureRecipeCategory(PatternEncodingTermMenu menu, Object recipe,
                                               IRecipeSlotsView slotsView, Player player,
                                               boolean maxTransfer, boolean doTransfer,
                                               CallbackInfoReturnable<IRecipeTransferError> callback) {
        if (doTransfer) {
            String title = JeiRuntimeCompat.getRecipeCategoryTitle(recipe);
            if (title != null && !title.isBlank()) {
                PatternUploadUtil.setLastProviderSearchKey(title);
            }
        }
    }
}
