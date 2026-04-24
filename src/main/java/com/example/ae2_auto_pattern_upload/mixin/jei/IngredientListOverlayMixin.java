package com.example.ae2_auto_pattern_upload.mixin.jei;

import com.example.ae2_auto_pattern_upload.client.jei.HeiIngredientClickHandler;
import mezz.jei.gui.overlay.IngredientListOverlay;
import mezz.jei.input.IClickedIngredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让右侧 HEI 搜索结果列表复用与左侧书签区相同的自定义点击行为。
 */
@Mixin(value = IngredientListOverlay.class, remap = false)
public abstract class IngredientListOverlayMixin {

    @Shadow
    public abstract IClickedIngredient<?> getIngredientUnderMouse(int mouseX, int mouseY);

    @Inject(method = "handleMouseClicked", at = @At("HEAD"), cancellable = true)
    private void ae2_auto_pattern_upload$handleListActions(int mouseX,
                                                           int mouseY,
                                                           int mouseButton,
                                                           CallbackInfoReturnable<Boolean> cir) {
        IClickedIngredient<?> clicked = this.getIngredientUnderMouse(mouseX, mouseY);
        if (HeiIngredientClickHandler.handle(clicked, mouseButton)) {
            cir.setReturnValue(true);
        }
    }
}
