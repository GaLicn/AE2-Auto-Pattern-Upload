package com.gali.ae2_auto_pattern_upload.mixin.ae2;

import java.util.ArrayList;
import java.util.Iterator;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gali.ae2_auto_pattern_upload.crafting.CraftingItemsCache;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.client.gui.widgets.IScrollSource;
import appeng.client.me.ItemRepo;

/**
 * 为 AE 物品仓库列表增加正在合成物品的置顶展示区域
 */
@Mixin(value = ItemRepo.class, remap = false)
public abstract class ItemRepoMixin {

    @Unique
    private static final int ae2_auto_pattern_upload$PINNED_ROW_SIZE = 9;

    @Unique
    private static final int ae2_auto_pattern_upload$MAX_PINNED_ROW_SIZE = 18;

    @Final
    @Shadow
    private ArrayList<IAEStack<?>> view;

    @Final
    @Shadow
    private IScrollSource src;

    @Shadow
    private int rowSize;

    @Unique
    private final ArrayList<IAEStack<?>> ae2_auto_pattern_upload$pinnedRow = new ArrayList<IAEStack<?>>();

    /**
     * 在视图更新后提取正在合成的物品并放入置顶区域
     */
    @Inject(method = "updateView", at = @At("RETURN"))
    private void ae2_auto_pattern_upload$updatePinnedRow(CallbackInfo ci) {
        ae2_auto_pattern_upload$pinnedRow.clear();

        if (this.view == null || this.view.isEmpty()) {
            return;
        }

        // 先保留 AE2 原本的过滤和排序结果，再把正在合成的物品移入独立置顶区
        Iterator<IAEStack<?>> iterator = this.view.iterator();
        while (iterator.hasNext()
            && ae2_auto_pattern_upload$pinnedRow.size() < ae2_auto_pattern_upload$MAX_PINNED_ROW_SIZE) {
            IAEStack<?> stack = iterator.next();
            if (stack instanceof IAEItemStack && CraftingItemsCache.isCrafting((IAEItemStack) stack)) {
                ae2_auto_pattern_upload$pinnedRow.add(stack);
                iterator.remove();
            }
        }
    }

    /**
     * 优先从置顶区域返回索引对应的引用堆栈
     */
    @Inject(method = "getReferenceStack", at = @At("HEAD"), cancellable = true)
    private void ae2_auto_pattern_upload$getPinnedReferenceStack(int index, CallbackInfoReturnable<IAEStack<?>> cir) {
        ae2_auto_pattern_upload$refreshExpiredPinnedRow();
        if (!ae2_auto_pattern_upload$pinnedRow.isEmpty()) {
            cir.setReturnValue(ae2_auto_pattern_upload$getStack(index));
        }
    }

    /**
     * 优先从置顶区域返回索引对应的物品引用
     */
    @Inject(method = "getReferenceItem", at = @At("HEAD"), cancellable = true)
    private void ae2_auto_pattern_upload$getPinnedReferenceItem(int index, CallbackInfoReturnable<IAEItemStack> cir) {
        ae2_auto_pattern_upload$refreshExpiredPinnedRow();
        if (ae2_auto_pattern_upload$pinnedRow.isEmpty()) {
            return;
        }

        IAEStack<?> stack = ae2_auto_pattern_upload$getStack(index);
        cir.setReturnValue(stack instanceof IAEItemStack ? (IAEItemStack) stack : null);
    }

    /**
     * 优先从置顶区域返回索引对应的原版物品堆
     */
    @Inject(method = "getItem", at = @At("HEAD"), cancellable = true)
    private void ae2_auto_pattern_upload$getPinnedItem(int index, CallbackInfoReturnable<ItemStack> cir) {
        ae2_auto_pattern_upload$refreshExpiredPinnedRow();
        if (ae2_auto_pattern_upload$pinnedRow.isEmpty()) {
            return;
        }

        IAEStack<?> stack = ae2_auto_pattern_upload$getStack(index);
        cir.setReturnValue(stack instanceof IAEItemStack ? ((IAEItemStack) stack).getItemStack() : null);
    }

    /**
     * 根据当前索引从置顶区域或原始视图中取出对应条目
     */
    @Unique
    private IAEStack<?> ae2_auto_pattern_upload$getStack(int index) {
        int reservedSize = ae2_auto_pattern_upload$getReservedSize();
        if (index < reservedSize) {
            if (index < ae2_auto_pattern_upload$pinnedRow.size()) {
                return ae2_auto_pattern_upload$pinnedRow.get(index);
            }
            return null;
        }

        int viewIndex = index - reservedSize + this.src.getCurrentScroll() * this.rowSize;
        if (viewIndex >= 0 && viewIndex < this.view.size()) {
            return this.view.get(viewIndex);
        }
        return null;
    }

    /**
     * 计算置顶区域当前应占用的保留槽位数量
     */
    @Unique
    private int ae2_auto_pattern_upload$getReservedSize() {
        if (ae2_auto_pattern_upload$pinnedRow.isEmpty()) {
            return 0;
        }

        // 初始只保留一行，第一行满后扩展到两行
        return ae2_auto_pattern_upload$pinnedRow.size() > ae2_auto_pattern_upload$PINNED_ROW_SIZE
            ? ae2_auto_pattern_upload$MAX_PINNED_ROW_SIZE
            : ae2_auto_pattern_upload$PINNED_ROW_SIZE;
    }

    /**
     * 检查置顶物品是否已结束合成，必要时刷新视图
     */
    @Unique
    private void ae2_auto_pattern_upload$refreshExpiredPinnedRow() {
        if (ae2_auto_pattern_upload$pinnedRow.isEmpty()) {
            return;
        }

        for (IAEStack<?> stack : ae2_auto_pattern_upload$pinnedRow) {
            if (stack instanceof IAEItemStack && !CraftingItemsCache.isCrafting((IAEItemStack) stack)) {
                ((ItemRepo) (Object) this).updateView();
                return;
            }
        }
    }

    /**
     * 在存在置顶区域时扩展仓库列表的总条目数
     */
    @Inject(method = "size", at = @At("HEAD"), cancellable = true)
    private void ae2_auto_pattern_upload$getPinnedSize(CallbackInfoReturnable<Integer> cir) {
        ae2_auto_pattern_upload$refreshExpiredPinnedRow();
        int reservedSize = ae2_auto_pattern_upload$getReservedSize();
        if (reservedSize > 0) {
            cir.setReturnValue(this.view.size() + reservedSize);
        }
    }
}
