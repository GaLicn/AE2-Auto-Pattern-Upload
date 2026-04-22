package com.example.ae2_auto_pattern_upload.mixin.ae2;

import appeng.client.gui.implementations.GuiMEMonitorable;
import com.example.ae2_auto_pattern_upload.ExampleMod;
import mezz.jei.Internal;
import mezz.jei.api.IBookmarkOverlay;
import mezz.jei.api.IIngredientListOverlay;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRegistry;
import mezz.jei.api.IJeiRuntime;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

/**
 * Mixin到AE2的终端GUI，实现按F键将悬停道具名称写入搜索框。
 * 优先读取JEI/HEI物品列表与书签，取不到时再回退到当前GUI悬停槽位中的物品。
 */
@Mixin(value = GuiMEMonitorable.class)
public abstract class GuiMEMonitorableMixin {

    static {
        ExampleMod.LOGGER.info("GuiMEMonitorableMixin 类已加载");
    }

    @Shadow(remap = false)
    protected appeng.client.gui.widgets.MEGuiTextField searchField;

    @Shadow(remap = false)
    protected appeng.client.me.ItemRepo repo;

    @Inject(
        method = "keyTyped",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    private void onKeyTyped(char character, int key, CallbackInfo ci) throws IOException {
        // 检测F键 (KEY_F = 33)
        if (key != Keyboard.KEY_F) {
            return;
        }

        // 检查搜索框是否存在
        if (searchField == null) {
            return;
        }

        // 先检查JEI/HEI覆盖层，再回退到当前GUI悬停槽位
        Object ingredientValue = getHoveredIngredient();
        if (ingredientValue == null) {
            return;
        }

        // 获取道具显示名称
        String itemName = getItemDisplayName(ingredientValue);
        if (itemName == null || itemName.isEmpty()) {
            return;
        }

        // 去除颜色代码（§字符）
        itemName = itemName.replaceAll("§[0-9a-fk-or]", "");

        // 设置搜索框文本
        searchField.setText(itemName);
        searchField.setCursorPositionEnd();

        // 更新搜索
        if (repo != null) {
            repo.setSearchString(itemName);
        }

        // 阻止原始方法继续执行，避免额外输入 "f"
        ci.cancel();
    }

    private Object getHoveredIngredient() {
        IJeiRuntime runtime = Internal.getRuntime();
        if (runtime != null) {
            Object ingredient = getIngredientUnderMouse(runtime);
            if (ingredient != null) {
                return ingredient;
            }
        }

        Slot hoveredSlot = ((GuiContainer) (Object) this).getSlotUnderMouse();
        if (hoveredSlot != null && hoveredSlot.getHasStack()) {
            ItemStack stack = hoveredSlot.getStack();
            if (!stack.isEmpty()) {
                return stack;
            }
        }

        return null;
    }

    private Object getIngredientUnderMouse(IJeiRuntime runtime) {
        IIngredientListOverlay ingredientListOverlay = runtime.getIngredientListOverlay();
        if (ingredientListOverlay != null) {
            Object ingredient = ingredientListOverlay.getIngredientUnderMouse();
            if (ingredient != null) {
                return ingredient;
            }
        }

        IBookmarkOverlay bookmarkOverlay = runtime.getBookmarkOverlay();
        if (bookmarkOverlay != null) {
            return bookmarkOverlay.getIngredientUnderMouse();
        }

        return null;
    }

    /**
     * 获取道具的显示名称
     */
    private String getItemDisplayName(Object ingredient) {
        // 如果是ItemStack，直接获取显示名称
        if (ingredient instanceof ItemStack) {
            ItemStack stack = (ItemStack) ingredient;
            if (!stack.isEmpty()) {
                return stack.getDisplayName();
            }
        }
        
        // 对于其他类型的道具，通过JEI的IIngredientHelper获取
        IIngredientRegistry ingredientRegistry = Internal.getIngredientRegistry();
        if (ingredientRegistry != null) {
            IIngredientHelper<Object> helper = ingredientRegistry.getIngredientHelper(ingredient);
            if (helper != null) {
                return helper.getDisplayName(ingredient);
            }
        }
        
        return null;
    }
}

