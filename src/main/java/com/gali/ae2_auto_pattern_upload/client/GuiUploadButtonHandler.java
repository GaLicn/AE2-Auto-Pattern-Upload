package com.gali.ae2_auto_pattern_upload.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import appeng.client.gui.implementations.GuiPatternTerm;
import appeng.client.gui.implementations.GuiPatternTermEx;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class GuiUploadButtonHandler {

    public static final int BUTTON_UPLOAD_ID = 999;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new GuiUploadButtonHandler());
    }

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen gui = event.gui;
        if (gui == null) {
            return;
        }

        // 兼容样板终端与扩展样板终端，两者布局一致，共享同一按钮位置
        if (!(gui instanceof GuiPatternTerm) && !(gui instanceof GuiPatternTermEx)) {
            return;
        }

        if (!(gui instanceof GuiContainer)) {
            return;
        }

        GuiContainer container = (GuiContainer) gui;
        int guiLeft;
        int guiTop;
        int ySize;
        try {
            java.lang.reflect.Field fLeft = GuiContainer.class.getDeclaredField("guiLeft");
            java.lang.reflect.Field fTop = GuiContainer.class.getDeclaredField("guiTop");
            java.lang.reflect.Field fYSize = GuiContainer.class.getDeclaredField("ySize");
            fLeft.setAccessible(true);
            fTop.setAccessible(true);
            fYSize.setAccessible(true);
            guiLeft = fLeft.getInt(container);
            guiTop = fTop.getInt(container);
            ySize = fYSize.getInt(container);
        } catch (Exception e) {
            return;
        }

        int encodeButtonX = guiLeft + 147;
        int encodeButtonY = guiTop + ySize - 142;

        int uploadBtnWidth = 12;
        int uploadBtnHeight = 12;
        int uploadBtnX = encodeButtonX - uploadBtnWidth;
        int uploadBtnY = encodeButtonY + 2;

        GuiButton uploadButton = new GuiButton(BUTTON_UPLOAD_ID, uploadBtnX, uploadBtnY, uploadBtnWidth, uploadBtnHeight, "↑");
        event.buttonList.add(uploadButton);
    }
}
