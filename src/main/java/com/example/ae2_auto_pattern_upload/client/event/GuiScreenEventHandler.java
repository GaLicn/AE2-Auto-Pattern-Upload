package com.example.ae2_auto_pattern_upload.client.event;

import com.example.ae2_auto_pattern_upload.network.ModNetwork;
import com.example.ae2_auto_pattern_upload.network.OpenCraftAmountFromBookmarkPacket;
import com.example.ae2_auto_pattern_upload.network.RequestProvidersListPacket;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

/**
 * 使用事件监听器在编码终端添加上传按钮
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(Side.CLIENT)
public class GuiScreenEventHandler {

    private static final long BOOKMARK_MIDDLE_CLICK_COOLDOWN_MS = 200L;
    private static GuiButton uploadBtn = null;
    private static long lastBookmarkMiddleClickAt = 0L;

    /**
     * 检查给定的GUI类名是否是样板终端
     */
    private static boolean isPatternTerminalGui(String guiClassName) {
        return guiClassName.equals("GuiPatternTerm")                    // 普通样板终端
                || guiClassName.equals("GuiExpandedProcessingPatternTerm")  // 增广样板终端
                || guiClassName.equals("GuiWirelessPatternTerminal")        // 无线样板终端
                || guiClassName.contains("PatternTerminal");                // 兼容其他可能的变体
    }

    @SubscribeEvent
    public static void onGuiOpen(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen gui = event.getGui();
        if (gui == null) {
            uploadBtn = null;
            return;
        }
        // 检查是否是样板终端 GUI（使用类名检查避免编译时依赖）
        String guiClassName = gui.getClass().getSimpleName();
        if (!isPatternTerminalGui(guiClassName)) {
            uploadBtn = null;
            return;
        }

        try {
            //使用GuiContainer中get方法获取gui基础信息
            if (!(gui instanceof GuiContainer)) {
                uploadBtn = null;
                return;
            }
            GuiContainer container = (GuiContainer) gui;
            int guiLeft = container.getGuiLeft();
            int guiTop = container.getGuiTop();
            int ySize = container.getYSize();

            // - 按钮尺寸：12x12（缩放为 0.75 倍）
            int encodeButtonX = guiLeft + 147;
            int encodeButtonY = guiTop + ySize - 142;
            int uploadBtnWidth = 12;
            int uploadBtnHeight = 12;
            int uploadBtnX = encodeButtonX - uploadBtnWidth;  // 紧挨着编码按钮的左边
            int uploadBtnY = encodeButtonY + 2;
            
            // 创建上传按钮
            uploadBtn = new GuiButton(999, uploadBtnX, uploadBtnY, uploadBtnWidth, uploadBtnHeight, "↑");
            
            gui.buttonList.add(uploadBtn);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @SubscribeEvent
    public static void onGuiButtonClick(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        // 检查是否点击了上传按钮（使用按钮ID进行比较，更可靠）
        if (uploadBtn != null && event.getButton().id == 999) {
            // 发送请求供应器列表的包
            ModNetwork.CHANNEL.sendToServer(new RequestProvidersListPacket());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onGuiMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!Mouse.getEventButtonState() || Mouse.getEventButton() != 2) {
            return;
        }

        ItemStack bookmarkStack = getHoveredBookmarkStack();
        if (bookmarkStack.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastBookmarkMiddleClickAt < BOOKMARK_MIDDLE_CLICK_COOLDOWN_MS) {
            return;
        }

        lastBookmarkMiddleClickAt = now;
        ModNetwork.CHANNEL.sendToServer(new OpenCraftAmountFromBookmarkPacket(bookmarkStack));
        event.setCanceled(true);
    }

    private static ItemStack getHoveredBookmarkStack() {
        try {
            Class<?> internalClass = Class.forName("mezz.jei.Internal");
            Object runtime = internalClass.getMethod("getRuntime").invoke(null);
            if (runtime == null) {
                return ItemStack.EMPTY;
            }

            Object bookmarkOverlay = runtime.getClass().getMethod("getBookmarkOverlay").invoke(runtime);
            if (bookmarkOverlay == null) {
                return ItemStack.EMPTY;
            }

            Object ingredient = bookmarkOverlay.getClass().getMethod("getIngredientUnderMouse").invoke(bookmarkOverlay);
            return ingredient instanceof ItemStack ? (ItemStack) ingredient : ItemStack.EMPTY;
        } catch (Throwable ignored) {
            return ItemStack.EMPTY;
        }
    }

}
