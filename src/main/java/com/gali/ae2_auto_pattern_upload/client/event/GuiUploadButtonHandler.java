package com.gali.ae2_auto_pattern_upload.client.event;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import com.gali.ae2_auto_pattern_upload.mixin.GuiContainerAccessor;
import com.gali.ae2_auto_pattern_upload.network.ModNetwork;
import com.gali.ae2_auto_pattern_upload.network.RequestProvidersListPacket;

import appeng.client.gui.implementations.GuiPatternTerm;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class GuiUploadButtonHandler {

    public static final int BUTTON_UPLOAD_ID = 999;
    private GuiButton uploadButton;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new GuiUploadButtonHandler());
    }

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen gui = event.gui;
        if (gui == null) {
            return;
        }

        // 兼容样板终端与增广样板终端，两者布局一致，共享同一按钮位置
        // GuiPatternTermEx继承了GuiPatternTerm，因此无需多余判断
        if (!(gui instanceof GuiPatternTerm)) {
            return;
        }

        GuiContainerAccessor accessor = (GuiContainerAccessor) gui;

        // 获取编码终端ui界面的坐标信息
        int guiLeft = accessor.getGuiLeft();
        int guiTop = accessor.getGuiTop();
        int ySize = accessor.getYSize();

        int encodeButtonX = guiLeft + 147;
        int encodeButtonY = guiTop + ySize - 142;

        int uploadBtnWidth = 12;
        int uploadBtnHeight = 12;
        int uploadBtnX = encodeButtonX - uploadBtnWidth;
        int uploadBtnY = encodeButtonY + 2;
        this.uploadButton = new GuiButton(
            BUTTON_UPLOAD_ID,
            uploadBtnX,
            uploadBtnY,
            uploadBtnWidth,
            uploadBtnHeight,
            "↑");
        event.buttonList.add(this.uploadButton);
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.button == null || uploadButton == null) {
            return;
        }
        if (event.button.id == BUTTON_UPLOAD_ID && event.button == uploadButton) {
            ModNetwork.CHANNEL.sendToServer(new RequestProvidersListPacket());
            event.setCanceled(true);
        }
    }
}
