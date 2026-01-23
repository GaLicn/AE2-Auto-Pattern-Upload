package com.gali.ae2_auto_pattern_upload.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.gali.ae2_auto_pattern_upload.container.ContainerLabeledWirelessTransceiver;
import com.gali.ae2_auto_pattern_upload.network.ModNetwork;
import com.gali.ae2_auto_pattern_upload.network.PacketApplyLabel;
import com.gali.ae2_auto_pattern_upload.tile.TileLabeledWirelessTransceiver;

/**
 * 标签无线收发器 GUI
 */
public class GuiLabeledWirelessTransceiver extends GuiContainer {

    private final TileLabeledWirelessTransceiver tile;
    private GuiTextField labelField;
    private GuiButton applyButton;
    private GuiButton clearButton;

    public GuiLabeledWirelessTransceiver(InventoryPlayer playerInv, TileLabeledWirelessTransceiver tile) {
        super(new ContainerLabeledWirelessTransceiver(playerInv, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 100;
    }

    @Override
    public void initGui() {
        super.initGui();
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;

        // 标签输入框
        labelField = new GuiTextField(fontRendererObj, x + 10, y + 25, 100, 16);
        labelField.setMaxStringLength(64);
        labelField.setText(tile.getLabelForDisplay() != null ? tile.getLabelForDisplay() : "");
        labelField.setFocused(true);

        // 应用按钮
        applyButton = new GuiButton(
            0,
            x + 115,
            y + 22,
            50,
            20,
            StatCollector.translateToLocal("gui.ae2_auto_pattern_upload.labeled_wireless.apply"));
        buttonList.add(applyButton);

        // 清除按钮
        clearButton = new GuiButton(
            1,
            x + 115,
            y + 46,
            50,
            20,
            StatCollector.translateToLocal("gui.ae2_auto_pattern_upload.labeled_wireless.clear"));
        buttonList.add(clearButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            // 应用标签
            String label = labelField.getText();
            ModNetwork.channel.sendToServer(new PacketApplyLabel(tile.xCoord, tile.yCoord, tile.zCoord, label, false));
        } else if (button.id == 1) {
            // 清除标签
            labelField.setText("");
            ModNetwork.channel.sendToServer(new PacketApplyLabel(tile.xCoord, tile.yCoord, tile.zCoord, "", true));
        }
    }

    @Override
    protected void keyTyped(char c, int keyCode) {
        if (labelField.isFocused()) {
            if (keyCode == 28) { // Enter键
                actionPerformed(applyButton);
                return;
            }
            labelField.textboxKeyTyped(c, keyCode);
        } else {
            super.keyTyped(c, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button);
        labelField.mouseClicked(x, y, button);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        labelField.updateCursorCounter();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        // 绘制简单背景
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        drawRect(x, y, x + xSize, y + ySize, 0xFFC6C6C6);
        drawRect(x + 2, y + 2, x + xSize - 2, y + ySize - 2, 0xFF8B8B8B);
        drawRect(x + 4, y + 4, x + xSize - 4, y + ySize - 4, 0xFFC6C6C6);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString(
            StatCollector.translateToLocal("gui.ae2_auto_pattern_upload.labeled_wireless.title"),
            8,
            6,
            0x404040);

        // 显示当前状态
        String status;
        if (tile.getFrequency() > 0) {
            status = String.format(
                StatCollector.translateToLocal("gui.ae2_auto_pattern_upload.labeled_wireless.channel"),
                tile.getFrequency());
        } else {
            status = StatCollector.translateToLocal("gui.ae2_auto_pattern_upload.labeled_wireless.not_connected");
        }
        fontRendererObj.drawString(status, 8, 75, 0x404040);

        // 显示放置者
        if (tile.getPlacerName() != null) {
            fontRendererObj.drawString(
                String.format(
                    StatCollector.translateToLocal("gui.ae2_auto_pattern_upload.labeled_wireless.owner"),
                    tile.getPlacerName()),
                8,
                85,
                0x808080);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        labelField.drawTextBox();
    }
}
