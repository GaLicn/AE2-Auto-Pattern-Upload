package com.gali.ae2_auto_pattern_upload.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import appeng.client.gui.ScreenColor;
import appeng.client.gui.widgets.ITooltip;
import appeng.client.texture.ExtraBlockTextures;

public class UploadPatternButton extends GuiButton implements ITooltip {

    private static final int SIZE = 12;
    private static final int ENCODE_ICON_INDEX = 8;

    public UploadPatternButton(int id, int x, int y) {
        super(id, x, y, SIZE, SIZE, "");
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) {
            return;
        }

        this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;

        if (this.enabled) {
            ScreenColor.setGuiColor();
        } else {
            ScreenColor.setDimmedGuiColor();
        }

        mc.renderEngine.bindTexture(ExtraBlockTextures.GuiTexture("guis/states.png"));
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glPushMatrix();
        GL11.glTranslatef(this.xPosition, this.yPosition, 0.0F);
        GL11.glScalef(0.75F, 0.75F, 0.75F);
        this.drawTexturedModalRect(0, 0, 256 - 16, 256 - 16, 16, 16);

        if (!isShiftKeyDown()) {
            GL11.glTranslatef(16.0F, 16.0F, 0.0F);
            GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
        }
        this.drawTexturedModalRect(0, 0, ENCODE_ICON_INDEX * 16, 0, 16, 16);
        GL11.glPopMatrix();
        ScreenColor.resetGuiColor();

        this.mouseDragged(mc, mouseX, mouseY);
    }

    @Override
    public String getMessage() {
        String title = StatCollector.translateToLocal("ae2_auto_pattern_upload.tooltip.upload_pattern");
        String descKey = isShiftKeyDown() ? "ae2_auto_pattern_upload.tooltip.recall_last_pattern"
            : "ae2_auto_pattern_upload.tooltip.upload_pattern_desc";
        String desc = StatCollector.translateToLocal(descKey);
        return title + "\n" + desc;
    }

    @Override
    public int xPos() {
        return this.xPosition;
    }

    @Override
    public int yPos() {
        return this.yPosition;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }

    private boolean isShiftKeyDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
    }
}
