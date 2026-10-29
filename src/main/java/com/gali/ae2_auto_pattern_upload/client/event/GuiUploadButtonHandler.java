package com.gali.ae2_auto_pattern_upload.client.event;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;

import com.gali.ae2_auto_pattern_upload.client.gui.UploadPatternButton;
import com.gali.ae2_auto_pattern_upload.mixin.ae2.accessor.GuiContainerAccessor;
import com.gali.ae2_auto_pattern_upload.network.ModNetwork;
import com.gali.ae2_auto_pattern_upload.network.provider.RequestProvidersListPacket;
import com.gali.ae2_auto_pattern_upload.network.upload.RecallLastUploadedPatternPacket;

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
        if (!(gui instanceof GuiPatternTerm container)) {
            return;
        }

        GuiContainerAccessor accessor = (GuiContainerAccessor) container;
        int guiLeft = accessor.getGuiLeft();
        int guiTop = accessor.getGuiTop();
        int ySize = accessor.getYSize();
        int encodeButtonX = guiLeft + 147;
        int encodeButtonY = guiTop + ySize - 142;

        this.uploadButton = new UploadPatternButton(BUTTON_UPLOAD_ID, encodeButtonX - 13, encodeButtonY + 2);
        event.buttonList.add(this.uploadButton);
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.button == null || uploadButton == null) {
            return;
        }
        if (event.button.id == BUTTON_UPLOAD_ID && event.button == uploadButton) {
            if (isShiftKeyDown()) {
                ModNetwork.CHANNEL.sendToServer(new RecallLastUploadedPatternPacket());
            } else {
                ModNetwork.CHANNEL.sendToServer(new RequestProvidersListPacket());
            }
            event.setCanceled(true);
        }
    }

    private boolean isShiftKeyDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
    }
}
