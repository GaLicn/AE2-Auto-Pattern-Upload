package com.gali.ae2_auto_pattern_upload.mixin.ae2;

import net.minecraft.client.gui.GuiButton;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gali.ae2_auto_pattern_upload.client.handler.AutoUploadHandler;

import appeng.api.config.ActionItems;
import appeng.client.gui.implementations.GuiPatternTerm;
import appeng.client.gui.widgets.GuiImgButton;

/**
 * 在基础样板终端编码后触发自动上传逻辑
 */
@Mixin(GuiPatternTerm.class)
public class GuiPatternTermEncodeMixin {

    /**
     * 监听编码按钮点击并在编码完成后尝试自动上传样板
     */
    @Inject(method = "actionPerformed", at = @At("RETURN"))
    private void onEncodeButtonClicked(GuiButton btn, CallbackInfo ci) {
        // 仅在编码按钮触发后继续执行自动上传
        if (btn instanceof GuiImgButton imgBtn) {
            if (imgBtn.getCurrentValue() == ActionItems.ENCODE) {
                AutoUploadHandler.onEncodeButtonClicked();
            }
        }
    }
}
