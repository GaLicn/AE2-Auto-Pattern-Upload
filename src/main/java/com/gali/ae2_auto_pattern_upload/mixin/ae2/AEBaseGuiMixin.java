package com.gali.ae2_auto_pattern_upload.mixin.ae2;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gali.ae2_auto_pattern_upload.crafting.CraftingItemsCache;

import appeng.client.gui.AEBaseGui;

/**
 * 拦截 AE 终端界面的滚轮事件，避免与本模组的快捷操作冲突
 */
@Mixin(AEBaseGui.class)
public abstract class AEBaseGuiMixin extends GuiContainer {

    /**
     * 构造占位父类，满足 Mixin 对 GuiContainer 继承层次的要求
     */
    public AEBaseGuiMixin() {
        super(null);
    }

    /**
     * 在界面初始化结束后重置已完成合成物品的展示计时
     */
    @Inject(method = "initGui", at = @At("TAIL"))
    private void onInitGui(CallbackInfo ci) {
        CraftingItemsCache.onTerminalOpened();
    }

    /**
     * 在滚轮事件开始时判断是否取消 AE 原本的 shift+滚轮处理
     */
    @Inject(method = "mouseWheelEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private void onMouseWheelEvent(int x, int y, int wheel, CallbackInfoReturnable<Boolean> cir) {
        // 只有按住 Shift 时才需要拦截 AE 的滚轮逻辑
        if (!isShiftKeyDown()) {
            return;
        }

        // 玩家背包区域通常位于界面底部，上方区域视为 ME 物品显示区
        int inventoryStartY = this.guiTop + this.ySize - 90;

        if (y < inventoryStartY) {
            // 鼠标位于物品显示区时，阻止 AE 默认处理并标记事件已消费
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
