package com.gali.ae2_auto_pattern_upload.client.event;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;

import com.gali.ae2_auto_pattern_upload.mixin.ae2.accessor.GuiContainerAccessor;
import com.gali.ae2_auto_pattern_upload.network.ModNetwork;
import com.gali.ae2_auto_pattern_upload.network.crafting.PacketExtractIngredients;
import com.gali.ae2_auto_pattern_upload.network.crafting.PacketOpenCraftingAmount;
import com.gali.ae2_auto_pattern_upload.network.inventory.PacketExtractItem;
import com.gali.ae2_auto_pattern_upload.network.inventory.PacketScrollTransfer;

import codechicken.nei.ItemPanels;
import codechicken.nei.bookmark.BookmarkGrid;
import codechicken.nei.bookmark.BookmarkItem;
import codechicken.nei.bookmark.BookmarksGridSlot;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.guihook.IContainerInputHandler;

public class KeyInputHandler implements IContainerInputHandler {

    private static final int KEY_F = Keyboard.KEY_F;
    private static final int MOUSE_LEFT = 0;
    private static final int MOUSE_RIGHT = 1;
    private static final int MOUSE_MIDDLE = 2;

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
        boolean result = setSearchText(searchField, itemName);

        // 对于无线终端，需要手动触发搜索更新
        // 因为无线终端的MEGuiTextField没有重写onTextChange方法
        if (result && isWirelessTerminal(gui)) {
            triggerWirelessTerminalSearch(gui, itemName);
        }

        return result;
    }

    /**
     * 检查是否为无线终端界面
     */
    private boolean isWirelessTerminal(GuiContainer gui) {
        if (gui == null) {
            return false;
        }
        String className = gui.getClass()
            .getName();
        return className.equals("net.p455w0rd.wirelesscraftingterminal.client.gui.GuiWirelessCraftingTerminal");
    }

    /**
     * 手动触发无线终端的搜索更新
     * 无线终端的搜索框没有重写onTextChange，需要手动调用repo的更新方法
     */
    private void triggerWirelessTerminalSearch(GuiContainer gui, String searchText) {
        try {
            // 获取repo字段
            Field repoField = gui.getClass()
                .getDeclaredField("repo");
            repoField.setAccessible(true);
            Object repo = repoField.get(gui);

            if (repo != null) {
                // 调用repo.setSearchString(text)
                Method setSearchString = repo.getClass()
                    .getMethod("setSearchString", String.class);
                setSearchString.invoke(repo, searchText);

                // 调用repo.updateView()
                Method updateView = repo.getClass()
                    .getMethod("updateView");
                updateView.invoke(repo);
            }
        } catch (Throwable e) {
            // 静默失败
        }
    }

    @Override
    public void onKeyTyped(GuiContainer gui, char keyChar, int keyID) {}

    @Override
    public boolean lastKeyTyped(GuiContainer gui, char keyChar, int keyCode) {
        return false;
    }

    @Override
    public boolean mouseClicked(GuiContainer gui, int mousex, int mousey, int button) {
        // 检查鼠标是否在书签面板上 - 优先处理书签组点击
        if (isMouseOverBookmarkPanel(mousex, mousey)) {
            return handleBookmarkPanelClick(gui, mousex, mousey, button);
        }

        // 检查鼠标是否在NEI面板（物品面板、历史记录面板）上
        if (!isMouseOverNEIPanel(mousex, mousey)) {
            return false;
        }

        // 获取鼠标下的物品
        ItemStack stackUnderMouse = GuiContainerManager.getStackMouseOver(gui);
        if (stackUnderMouse == null) {
            return false;
        }

        // 检查是否为AE界面或非AE界面但需要处理的场景
        if (button == MOUSE_LEFT && isShiftKeyDown()) {
            return handleShiftLeftClick(gui, stackUnderMouse);
        }
        if (button == MOUSE_MIDDLE) {
            return handleMiddleClick(gui, stackUnderMouse);
        }

        return false;
    }

    /**
     * 检查鼠标是否在书签面板上
     */
    private boolean isMouseOverBookmarkPanel(int mousex, int mousey) {
        try {
            return ItemPanels.bookmarkPanel.contains(mousex, mousey);
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * 处理书签面板的点击事件
     */
    private boolean handleBookmarkPanelClick(GuiContainer gui, int mousex, int mousey, int button) {
        // 获取鼠标下的书签槽位
        BookmarksGridSlot slot = ItemPanels.bookmarkPanel.getSlotMouseOver(mousex, mousey);
        if (slot == null) {
            return false;
        }

        // 获取鼠标下的物品
        ItemStack stackUnderMouse = GuiContainerManager.getStackMouseOver(gui);
        if (stackUnderMouse == null) {
            return false;
        }

        // Shift+左键点击书签物品
        if (button == MOUSE_LEFT && isShiftKeyDown()) {
            return handleBookmarkShiftClick(slot, stackUnderMouse);
        }

        return false;
    }

    /**
     * 处理Shift+左键点击书签
     * - 如果点击的是组的主物品（RESULT类型），提取组内的所有材料
     * - 如果点击的是单独书签物品（ITEM类型），提取该物品本身
     */
    private boolean handleBookmarkShiftClick(BookmarksGridSlot slot, ItemStack clickedStack) {
        if (slot == null || clickedStack == null) {
            return false;
        }

        try {
            // 获取点击的物品类型
            BookmarkItem.BookmarkItemType type = slot.getType();
            int groupId = slot.getGroupId();

            // 如果是组的主物品（RESULT类型），提取组内所有材料
            if (type == BookmarkItem.BookmarkItemType.RESULT) {
                // 获取该组的所有材料
                List<ItemStack> ingredients = getBookmarkGroupIngredients(groupId);
                if (!ingredients.isEmpty()) {
                    // 发送数据包到服务器提取材料
                    ModNetwork.INSTANCE.sendToServer(new PacketExtractIngredients(ingredients));
                    return true;
                }
            }

            // 如果是单独书签物品（ITEM类型）或组内没有材料，提取该物品本身
            // 使用原有的提取逻辑
            ModNetwork.INSTANCE.sendToServer(new PacketExtractItem(clickedStack, true));
            return true;

        } catch (Throwable e) {
            // 静默失败
            return false;
        }
    }

    /**
     * 获取书签组内的所有材料（INGREDIENT类型）
     */
    private List<ItemStack> getBookmarkGroupIngredients(int groupId) {
        List<ItemStack> ingredients = new ArrayList<>();

        try {
            BookmarkGrid grid = ItemPanels.bookmarkPanel.getGrid();

            // 遍历所有书签项
            for (int i = 0; i < grid.size(); i++) {
                BookmarkItem item = grid.getBookmarkItem(i);

                // 检查是否属于同一组
                if (item.groupId != groupId) {
                    continue;
                }

                // 只提取材料（INGREDIENT类型）
                if (item.type == BookmarkItem.BookmarkItemType.INGREDIENT) {
                    ItemStack stack = item.getItemStack();
                    if (stack != null && stack.stackSize > 0) {
                        ingredients.add(stack);
                    }
                }
            }

        } catch (Throwable e) {
            // 静默失败
        }

        return ingredients;
    }

    @Override
    public void onMouseClicked(GuiContainer gui, int mousex, int mousey, int button) {}

    @Override
    public void onMouseUp(GuiContainer gui, int mousex, int mousey, int button) {}

    @Override
    public boolean mouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {
        // 检查是否为AE终端界面
        if (!isAE2Gui(gui)) {
            return false;
        }

        // 获取鼠标下的物品
        ItemStack stackUnderMouse = GuiContainerManager.getStackMouseOver(gui);
        if (stackUnderMouse == null) {
            return false;
        }

        // 检查鼠标是否在AE终端的物品显示区域内
        if (!isMouseOverAETerminalItemArea(gui, mousex, mousey)) {
            return false;
        }

        // 处理滚轮事件
        // scrolled > 0 表示向上滚动，存入AE（不需要Shift）
        // scrolled < 0 表示向下滚动，从AE取出（需要按住Shift）
        if (scrolled > 0) {
            // 向上滚动 - 存入AE（不需要按住Shift）
            try {
                ModNetwork.INSTANCE.sendToServer(new PacketScrollTransfer(stackUnderMouse, scrolled, 1));
            } catch (Throwable e) {
                // 静默处理异常
            }
            return true;
        } else if (scrolled < 0 && isShiftKeyDown()) {
            // 向下滚动 - 从AE取出（需要按住Shift）
            try {
                int transferAmount = calculateExtractAmount(stackUnderMouse);
                ModNetwork.INSTANCE.sendToServer(new PacketScrollTransfer(stackUnderMouse, scrolled, transferAmount));
            } catch (Throwable e) {
                // 静默处理异常
            }
            return true;
        }

        // 不处理的情况返回false
        return false;
    }

    /**
     * 计算取出数量（仅在按住Shift时调用）
     * 按住Shift：1个
     * 按住Shift+Ctrl：4个
     * 按住Shift+Alt：1组（最大堆叠数）
     */
    private int calculateExtractAmount(ItemStack stack) {
        boolean ctrlDown = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        boolean altDown = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);

        if (altDown) {
            // Shift+Alt：取出一组
            return stack.getMaxStackSize();
        } else if (ctrlDown) {
            // Shift+Ctrl：取出4个
            return Math.min(4, stack.getMaxStackSize());
        } else {
            // 只按Shift：取出1个
            return 1;
        }
    }

    @Override
    public void onMouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {}

    @Override
    public void onMouseDragged(GuiContainer gui, int mousex, int mousey, int button, long heldTime) {}

    /**
     * 检查鼠标是否在NEI面板（物品面板、历史记录面板）上
     */
    private boolean isMouseOverNEIPanel(int mousex, int mousey) {
        try {
            // 检查物品面板（右侧物品列表）
            if (ItemPanels.itemPanel.contains(mousex, mousey)) {
                return true;
            }

            // 检查历史记录面板
            if (ItemPanels.itemPanel.historyPanel.contains(mousex, mousey)) {
                return true;
            }

            return false;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * 处理 Shift + 左键点击
     * 1. 若目标物品有库存，执行拉取
     * 2. 若无库存但该物品可合成，自动打开下单界面
     * 3. 若玩家背包空间不足，执行拉取失败操作并静默失败
     */
    private boolean handleShiftLeftClick(GuiContainer gui, ItemStack stack) {
        if (stack == null) {
            return false;
        }

        try {
            // 发送数据包到服务器处理拉取/下单逻辑
            ModNetwork.INSTANCE.sendToServer(new PacketExtractItem(stack, true));
            return true;
        } catch (Throwable e) {
            // 静默失败
            return false;
        }
    }

    /**
     * 处理鼠标中键点击
     * 1. 检测该物品是否可合成
     * 2. 若可合成，自动打开下单界面
     */
    private boolean handleMiddleClick(GuiContainer gui, ItemStack stack) {
        if (stack == null) {
            return false;
        }

        try {
            // 发送数据包到服务器请求打开合成下单界面
            ModNetwork.INSTANCE.sendToServer(new PacketOpenCraftingAmount(stack));
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private boolean isAE2Gui(GuiContainer gui) {
        if (gui == null) {
            return false;
        }
        String className = gui.getClass()
            .getName();
        return className.startsWith("appeng.") || className.startsWith("com.glodblock.")
            || className.equals("net.p455w0rd.wirelesscraftingterminal.client.gui.GuiWirelessCraftingTerminal");
    }

    /**
     * 检查鼠标是否在AE终端的物品显示区域内
     * AE终端的物品通常显示在GUI的上半部分
     */
    private boolean isMouseOverAETerminalItemArea(GuiContainer gui, int mousex, int mousey) {
        try {
            String className = gui.getClass()
                .getName();

            // 使用Mixin Accessor获取GUI边界
            GuiContainerAccessor accessor = (GuiContainerAccessor) gui;
            int guiLeft = accessor.getGuiLeft();
            int guiTop = accessor.getGuiTop();
            int xSize = accessor.getXSize();
            int ySize = accessor.getYSize();

            // 物品区域通常在搜索框下方，玩家背包上方
            // 大致区域：guiLeft + 7 到 guiLeft + xSize - 7
            // y坐标：guiTop + 17 到 guiTop + ySize - 90 (排除搜索框和玩家背包区域)
            int itemAreaTop = guiTop + 17;
            int itemAreaBottom = guiTop + ySize - 90;
            int itemAreaLeft = guiLeft + 7;
            int itemAreaRight = guiLeft + xSize - 7;

            return mousex >= itemAreaLeft && mousex <= itemAreaRight
                && mousey >= itemAreaTop
                && mousey <= itemAreaBottom;

        } catch (Throwable e) {
            // 如果反射失败，默认允许滚轮操作
            return true;
        }
    }

    private boolean isShiftKeyDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
    }

    private Object findSearchField(GuiContainer gui) {
        Class<?> clazz = gui.getClass();

        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    String fieldName = field.getName()
                        .toLowerCase();
                    // 查找名称包含 search 且类型包含 TextField 的字段
                    if (fieldName.contains("search") && field.getType()
                        .getName()
                        .contains("TextField")) {
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
            Method isFocused = searchField.getClass()
                .getMethod("isFocused");
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
            Method setText = searchField.getClass()
                .getMethod("setText", String.class);
            setText.invoke(searchField, text);
            return true;
        } catch (Throwable ignored) {}
        return false;
    }
}
