package com.gali.ae2_auto_pattern_upload.client.event;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;

import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.guihook.IContainerInputHandler;

public class KeyInputHandler implements IContainerInputHandler {

    private static final int KEY_F = Keyboard.KEY_F;

    public static void register() {
        GuiContainerManager.addInputHandler(new KeyInputHandler());
    }

    @Override
    public boolean keyTyped(GuiContainer gui, char keyChar, int keyCode) {
        if (keyCode != KEY_F) {
            return false;
        }

        if (!isAE2Gui(gui)) {
            return false;
        }

        // 查找搜索框
        Object searchField = findSearchField(gui);
        if (searchField == null) {
            return false;
        }

        // 检查搜索框是否已经获得焦点
        if (isSearchFieldFocused(searchField)) {
            return false;
        }

        // 获取鼠标下的物品
        ItemStack stackUnderMouse = GuiContainerManager.getStackMouseOver(gui);
        if (stackUnderMouse == null) {
            return false;
        }

        // 获取物品名称
        String itemName = stackUnderMouse.getDisplayName();
        if (itemName == null || itemName.isEmpty()) {
            return false;
        }

        // 写入搜索框
        return setSearchText(searchField, itemName);
    }

    @Override
    public void onKeyTyped(GuiContainer gui, char keyChar, int keyID) {}

    @Override
    public boolean lastKeyTyped(GuiContainer gui, char keyChar, int keyCode) {
        return false;
    }

    @Override
    public void onMouseClicked(GuiContainer gui, int mousex, int mousey, int button) {}

    @Override
    public boolean mouseClicked(GuiContainer gui, int mousex, int mousey, int button) {
        return false;
    }

    @Override
    public void onMouseUp(GuiContainer gui, int mousex, int mousey, int button) {}

    @Override
    public void onMouseDragged(GuiContainer gui, int mousex, int mousey, int button, long heldTime) {}

    @Override
    public void onMouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {}

    @Override
    public boolean mouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {
        return false;
    }

    private boolean isAE2Gui(GuiContainer gui) {
        if (gui == null) {
            return false;
        }
        String className = gui.getClass().getName();
        return className.startsWith("appeng.") || className.startsWith("com.glodblock.");
    }

    private Object findSearchField(GuiContainer gui) {
        Class<?> clazz = gui.getClass();

        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    String fieldName = field.getName().toLowerCase();
                    // 查找名称包含 search 且类型包含 TextField 的字段
                    if (fieldName.contains("search") && field.getType().getName().contains("TextField")) {
                        field.setAccessible(true);
                        Object value = field.get(gui);
                        if (value != null) {
                            return value;
                        }
                    }
                } catch (Throwable ignored) {}
            }
            clazz = clazz.getSuperclass();
        }

        return null;
    }

    private boolean isSearchFieldFocused(Object searchField) {
        try {
            if (searchField instanceof GuiTextField) {
                return ((GuiTextField) searchField).isFocused();
            }
            Method isFocused = searchField.getClass().getMethod("isFocused");
            return (Boolean) isFocused.invoke(searchField);
        } catch (Throwable ignored) {}
        return false;
    }

    private boolean setSearchText(Object searchField, String text) {
        try {
            if (searchField instanceof GuiTextField) {
                ((GuiTextField) searchField).setText(text);
                return true;
            }
            Method setText = searchField.getClass().getMethod("setText", String.class);
            setText.invoke(searchField, text);
            return true;
        } catch (Throwable ignored) {}
        return false;
    }
}
