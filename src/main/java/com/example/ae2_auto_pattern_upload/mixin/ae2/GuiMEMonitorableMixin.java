package com.example.ae2_auto_pattern_upload.mixin.ae2;

import appeng.client.gui.implementations.GuiMEMonitorable;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Mixin到AE2的终端GUI，实现按F键将JEI标签中的道具名称写入搜索框
 */
@Mixin(value = GuiMEMonitorable.class, remap = false)
public abstract class GuiMEMonitorableMixin {

    @Shadow(remap = false)
    protected appeng.client.gui.widgets.MEGuiTextField searchField;

    @Shadow(remap = false)
    protected appeng.client.me.ItemRepo repo;

    @Inject(
        method = "keyTyped",
        at = @At("HEAD"),
        cancellable = false
    )
    private void onKeyTyped(char character, int key, CallbackInfo ci) throws IOException {
        // 检测F键 (KEY_F = 33)
        if (key != Keyboard.KEY_F) {
            return;
        }

        // 检查JEI是否加载
        if (!Loader.isModLoaded("jei")) {
            return;
        }

        // 检查搜索框是否存在
        if (searchField == null) {
            return;
        }

        try {
            // 获取鼠标位置 - 使用JEI的MouseHelper或直接计算
            int mouseX, mouseY;
            try {
                // 尝试使用JEI的MouseHelper
                Class<?> mouseHelperClass = Class.forName("mezz.jei.input.MouseHelper");
                Method getXMethod = mouseHelperClass.getMethod("getX");
                Method getYMethod = mouseHelperClass.getMethod("getY");
                mouseX = (Integer) getXMethod.invoke(null);
                mouseY = (Integer) getYMethod.invoke(null);
            } catch (Exception e) {
                // 如果MouseHelper不可用，使用直接计算
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.currentScreen == null) {
                    return;
                }
                mouseX = Mouse.getEventX() * mc.currentScreen.width / mc.displayWidth;
                mouseY = mc.currentScreen.height - Mouse.getEventY() * mc.currentScreen.height / mc.displayHeight - 1;
            }

            // 通过反射获取JEI的IngredientListOverlay
            Object ingredientListOverlay = getJeiIngredientListOverlay();
            if (ingredientListOverlay == null) {
                return;
            }

            // 获取鼠标下的道具
            Object clickedIngredient = getIngredientUnderMouse(ingredientListOverlay, mouseX, mouseY);
            if (clickedIngredient == null) {
                return;
            }

            // 提取道具值
            Object ingredientValue = getIngredientValue(clickedIngredient);
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

            // 设置搜索框焦点
            searchField.setFocused(true);

        } catch (Exception e) {
            // 静默处理异常，避免影响正常功能
            e.printStackTrace();
        }
    }

    /**
     * 通过反射获取JEI的IngredientListOverlay实例
     */
    private Object getJeiIngredientListOverlay() {
        try {
            // 获取JEI的Internal类
            Class<?> internalClass = Class.forName("mezz.jei.Internal");
            
            // 获取getRuntime方法
            Method getRuntimeMethod = internalClass.getMethod("getRuntime");
            Object runtime = getRuntimeMethod.invoke(null);
            
            if (runtime == null) {
                return null;
            }

            // 从运行时获取IngredientListOverlay
            // JeiRuntime有getIngredientListOverlay方法
            Method getOverlayMethod = runtime.getClass().getMethod("getIngredientListOverlay");
            return getOverlayMethod.invoke(runtime);
            
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取鼠标下的道具
     */
    private Object getIngredientUnderMouse(Object ingredientListOverlay, int mouseX, int mouseY) {
        try {
            Method method = ingredientListOverlay.getClass().getMethod("getIngredientUnderMouse", int.class, int.class);
            return method.invoke(ingredientListOverlay, mouseX, mouseY);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从IClickedIngredient中提取道具值
     */
    private Object getIngredientValue(Object clickedIngredient) {
        try {
            Method getValueMethod = clickedIngredient.getClass().getMethod("getValue");
            return getValueMethod.invoke(clickedIngredient);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取道具的显示名称
     */
    private String getItemDisplayName(Object ingredient) {
        if (ingredient instanceof ItemStack) {
            ItemStack stack = (ItemStack) ingredient;
            if (!stack.isEmpty()) {
                return stack.getDisplayName();
            }
        }
        
        // 如果不是ItemStack，尝试通过JEI的IIngredientHelper获取
        try {
            Class<?> internalClass = Class.forName("mezz.jei.Internal");
            Method getRegistryMethod = internalClass.getMethod("getIngredientRegistry");
            Object registry = getRegistryMethod.invoke(null);
            
            if (registry != null) {
                Method getHelperMethod = registry.getClass().getMethod("getIngredientHelper", Object.class);
                Object helper = getHelperMethod.invoke(registry, ingredient);
                
                if (helper != null) {
                    Method getDisplayNameMethod = helper.getClass().getMethod("getDisplayName", Object.class);
                    return (String) getDisplayNameMethod.invoke(helper, ingredient);
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
        
        return null;
    }
}

