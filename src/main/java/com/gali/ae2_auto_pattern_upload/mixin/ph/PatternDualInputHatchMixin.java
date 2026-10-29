package com.gali.ae2_auto_pattern_upload.mixin.ph;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;

import reobf.proghatches.gt.metatileentity.BufferedDualInputHatch;
import reobf.proghatches.gt.metatileentity.PatternDualInputHatch;

/**
 * 强制双输入仓使用 normalopt 配置
 */
@Mixin(value = PatternDualInputHatch.class, remap = false)
public abstract class PatternDualInputHatchMixin {

    @Shadow
    boolean normalopt;

    /**
     * 在新建双输入仓时默认启用 normalopt
     */
    @Inject(method = "<init>(ILjava/lang/String;Ljava/lang/String;IZIZI[Ljava/lang/String;)V", at = @At("TAIL"))
    private void ae2AutoPatternUpload$enableNormalOptForFreshPlacement(int id, String name, String nameRegional,
        int tier, boolean mMultiFluid, int bufferNum, boolean sf, int page, String[] optional, CallbackInfo ci) {
        this.normalopt = true;
    }

    /**
     * 在初始化扩展配置前强制启用 normalopt
     */
    @Inject(method = "initExConfig", at = @At("HEAD"))
    private void ae2AutoPatternUpload$forceNormalOptBeforeConfig(
        CallbackInfoReturnable<BufferedDualInputHatch.ExConfig> cir) {
        if (((Object) this) instanceof PatternDualInputHatch) {
            this.normalopt = true;
        }
    }

    /**
     * 在创建样板窗口前强制启用 normalopt
     */
    @Inject(method = "createPatternWindow2", at = @At("HEAD"))
    private void ae2AutoPatternUpload$forceNormalOptBeforeWindow(PanelSyncManager syncManager,
        CallbackInfoReturnable<ModularPanel> cir) {
        if (((Object) this) instanceof PatternDualInputHatch) {
            this.normalopt = true;
        }
    }
}
