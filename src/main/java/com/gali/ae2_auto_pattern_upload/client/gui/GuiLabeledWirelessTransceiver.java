package com.gali.ae2_auto_pattern_upload.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.gali.ae2_auto_pattern_upload.MyMod;
import com.gali.ae2_auto_pattern_upload.container.ContainerLabeledWirelessTransceiver;
import com.gali.ae2_auto_pattern_upload.network.ModNetwork;
import com.gali.ae2_auto_pattern_upload.network.PacketApplyLabel;
import com.gali.ae2_auto_pattern_upload.network.RequestLabelListPacket;
import com.gali.ae2_auto_pattern_upload.tile.TileLabeledWirelessTransceiver;

/**
 * 标签无线收发器 GUI - 完全按照原项目布局
 */
public class GuiLabeledWirelessTransceiver extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        MyMod.MODID,
        "textures/gui/labeled_wireless_transceiver.png");

    // 布局常量 - 参考原项目
    private static final int LIST_X = 9;
    private static final int LIST_Y = 27;
    private static final int LIST_W = 110;
    private static final int LIST_H = 114;
    private static final int ROW_H = 11;
    private static final int VISIBLE_ROWS = LIST_H / ROW_H;

    private static final int SCROLL_X = 123;
    private static final int SCROLL_Y = 21;
    private static final int SCROLL_W = 6;
    private static final int SCROLL_H = 121;

    private static final int INFO_X = 134;
    private static final int INFO_Y = 41;
    private static final int INFO_W = 116;

    private static final int SEARCH_X = 134;
    private static final int SEARCH_Y = 23;
    private static final int SEARCH_W = 116;
    private static final int SEARCH_H = 9;

    private final TileLabeledWirelessTransceiver tile;
    private final int tileX, tileY, tileZ;
    private GuiTextField searchBox;
    private GuiButton newBtn;
    private GuiButton deleteBtn;
    private GuiButton setBtn;
    private GuiButton disconnectBtn;

    private final List<LabelEntry> allLabels = new ArrayList<>();
    private final List<LabelEntry> filteredLabels = new ArrayList<>();
    private int scrollOffset = 0;
    private int selectedIndex = -1;
    private String selectedLabel = "";
    private int currentOnlineCount = 0; // 当前标签的在线数
    private int currentUsedChannels = 0; // 当前节点使用的频道数（从服务器接收）

    public GuiLabeledWirelessTransceiver(InventoryPlayer playerInv, TileLabeledWirelessTransceiver tile) {
        super(new ContainerLabeledWirelessTransceiver(playerInv, tile));
        this.tile = tile;
        this.tileX = tile.xCoord;
        this.tileY = tile.yCoord;
        this.tileZ = tile.zCoord;
        this.xSize = 256;
        this.ySize = 156;
    }

    public boolean isFor(int x, int y, int z) {
        return tileX == x && tileY == y && tileZ == z;
    }

    @Override
    public void initGui() {
        super.initGui();
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;

        // 搜索框
        searchBox = new GuiTextField(fontRendererObj, x + SEARCH_X, y + SEARCH_Y, SEARCH_W, SEARCH_H);
        searchBox.setMaxStringLength(64);
        searchBox.setVisible(true);
        searchBox.setFocused(false);

        // 按钮布局 - 参考原项目
        int btnStartX = x + 145;
        int btnStartY = y + 101;
        int btnW = 28;
        int btnH = 16;
        int hGap = 30;
        int vGap = 8;

        // 第一行按钮
        newBtn = new GuiButton(
            0,
            btnStartX,
            btnStartY,
            btnW,
            btnH,
            StatCollector.translateToLocal("gui.ae2_auto_pattern_upload.labeled_wireless.button.new"));
        buttonList.add(newBtn);

        deleteBtn = new GuiButton(
            1,
            btnStartX + btnW + hGap,
            btnStartY,
            btnW,
            btnH,
            StatCollector.translateToLocal("gui.ae2_auto_pattern_upload.labeled_wireless.button.delete"));
        buttonList.add(deleteBtn);

        // 第二行按钮
        setBtn = new GuiButton(
            2,
            btnStartX,
            btnStartY + btnH + vGap,
            btnW,
            btnH,
            StatCollector.translateToLocal("gui.ae2_auto_pattern_upload.labeled_wireless.button.set"));
        buttonList.add(setBtn);

        disconnectBtn = new GuiButton(
            3,
            btnStartX + btnW + hGap,
            btnStartY + btnH + vGap,
            btnW,
            btnH,
            StatCollector.translateToLocal("gui.ae2_auto_pattern_upload.labeled_wireless.button.refresh"));
        buttonList.add(disconnectBtn);

        // 初始化标签列表
        requestLabelList();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            // 新建 - 使用搜索框的文本
            String label = searchBox.getText();
            if (label != null && !label.trim()
                .isEmpty()) {
                sendApplyLabel(label.trim());
            }
        } else if (button.id == 1) {
            // 删除 - 删除选中的标签（清空所有使用该标签的收发器）
            if (selectedLabel != null && !selectedLabel.isEmpty()) {
                // 注意：删除标签会影响所有使用该标签的收发器
                sendClearLabel();
            }
        } else if (button.id == 2) {
            // 设定 - 应用选中的标签到当前收发器
            if (selectedLabel != null && !selectedLabel.isEmpty()) {
                sendApplyLabel(selectedLabel);
            }
        } else if (button.id == 3) {
            // 断开 - 清除当前收发器的标签
            sendClearLabel();
        }
    }

    private void sendApplyLabel(String label) {
        ModNetwork.channel.sendToServer(new PacketApplyLabel(tileX, tileY, tileZ, label, false));
        searchBox.setText("");
        // 延迟刷新列表
        new Thread(() -> {
            try {
                Thread.sleep(100);
                requestLabelList();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendClearLabel() {
        ModNetwork.channel.sendToServer(new PacketApplyLabel(tileX, tileY, tileZ, "", true));
        searchBox.setText("");
        selectedLabel = "";
        selectedIndex = -1;
        // 延迟刷新列表
        new Thread(() -> {
            try {
                Thread.sleep(100);
                requestLabelList();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void requestLabelList() {
        ModNetwork.channel.sendToServer(new RequestLabelListPacket(tileX, tileY, tileZ));
    }

    /**
     * 更新标签列表（从服务器接收）
     */
    public void updateLabelList(String[] labels, long[] channels, int[] onlineCounts, String currentLabel,
        long currentChannel, int currentOnlineCount, int currentUsedChannels) {
        String prevSelected = selectedLabel;
        allLabels.clear();
        for (int i = 0; i < labels.length; i++) {
            allLabels.add(new LabelEntry(labels[i], channels[i], onlineCounts[i]));
        }

        this.currentOnlineCount = currentOnlineCount;
        this.currentUsedChannels = currentUsedChannels;

        // 恢复选中状态
        if (prevSelected != null && !prevSelected.isEmpty()) {
            selectedLabel = prevSelected;
        }

        applyFilter();
    }

    @Override
    protected void keyTyped(char c, int keyCode) {
        if (searchBox.isFocused()) {
            searchBox.textboxKeyTyped(c, keyCode);
            applyFilter();
        } else {
            super.keyTyped(c, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        searchBox.mouseClicked(mouseX, mouseY, button);

        // 检查是否点击了列表
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        int relX = mouseX - (x + LIST_X);
        int relY = mouseY - (y + LIST_Y);

        if (relX >= 0 && relX < LIST_W && relY >= 0 && relY < LIST_H) {
            int row = relY / ROW_H;
            int idx = scrollOffset + row;
            if (idx >= 0 && idx < filteredLabels.size()) {
                selectedIndex = idx;
                selectedLabel = filteredLabels.get(idx).label;
            }
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        searchBox.updateCursorCounter();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager()
            .bindTexture(TEXTURE);
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        this.drawTexturedModalRect(x, y, 0, 0, xSize, ySize);

        // 绘制列表背景
        drawRect(x + LIST_X, y + LIST_Y, x + LIST_X + LIST_W, y + LIST_Y + LIST_H, 0x20FFFFFF);

        // 绘制滚动条背景
        drawRect(x + SCROLL_X, y + SCROLL_Y, x + SCROLL_X + SCROLL_W, y + SCROLL_Y + SCROLL_H, 0x20000000);

        // 绘制信息区背景
        drawRect(x + INFO_X, y + INFO_Y, x + INFO_X + INFO_W, y + INFO_Y + 51, 0x10FFFFFF);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 标题
        String title = StatCollector.translateToLocal("gui.ae2_auto_pattern_upload.labeled_wireless.title");
        fontRendererObj.drawString(title, 8, 8, 0x404040);

        // 右侧信息区标题
        String infoTitle = StatCollector.translateToLocal("gui.ae2_auto_pattern_upload.labeled_wireless.info");
        fontRendererObj.drawString(infoTitle, INFO_X, 8, 0x404040);

        // 绘制标签列表
        renderLabelList();

        // 绘制滚动条
        renderScrollBar();

        // 绘制当前收发器信息
        renderTransceiverInfo();
    }

    private void renderLabelList() {
        int baseX = LIST_X;
        int baseY = LIST_Y;

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int idx = scrollOffset + row;
            if (idx >= filteredLabels.size()) break;

            int y = baseY + row * ROW_H;

            // 绘制选中高亮
            if (idx == selectedIndex) {
                drawRect(baseX, y, baseX + LIST_W, y + ROW_H, 0x40FFFFFF);
            }

            // 绘制标签文本
            LabelEntry entry = filteredLabels.get(idx);
            String displayText = fontRendererObj.trimStringToWidth(entry.label, LIST_W - 4);
            int ty = y + (ROW_H - fontRendererObj.FONT_HEIGHT) / 2;
            fontRendererObj.drawString(displayText, baseX + 2, ty, 0x404040);
        }
    }

    private void renderScrollBar() {
        int total = filteredLabels.size();
        if (total <= VISIBLE_ROWS) {
            // 静态滚动条
            drawRect(SCROLL_X, SCROLL_Y, SCROLL_X + SCROLL_W, SCROLL_Y + SCROLL_H, 0x20000000);
            return;
        }

        int maxOffset = total - VISIBLE_ROWS;
        int trackX1 = SCROLL_X;
        int trackY1 = SCROLL_Y;
        int trackY2 = trackY1 + SCROLL_H;

        // 绘制轨道
        drawRect(trackX1, trackY1, trackX1 + SCROLL_W, trackY2, 0x20000000);

        // 绘制滑块
        int knobH = Math.max(10, (int) ((double) VISIBLE_ROWS / total * SCROLL_H));
        int knobY = trackY1 + (int) ((SCROLL_H - knobH) * (scrollOffset / (double) maxOffset));
        drawRect(trackX1, knobY, trackX1 + SCROLL_W, knobY + knobH, 0x80FFFFFF);
    }

    private void renderTransceiverInfo() {
        int x = INFO_X;
        int y = INFO_Y;
        int lineHeight = 12;

        // 当前标签
        String currentLabel = tile.getLabelForDisplay();
        String labelLine = StatCollector.translateToLocal("gui.ae2_auto_pattern_upload.labeled_wireless.current_label")
            + ": "
            + (currentLabel != null && !currentLabel.isEmpty() ? currentLabel : "-");
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(labelLine, INFO_W - 4), x + 2, y, 0x404040);

        // 在线数（该标签下的收发器总数）
        String onlineLine = StatCollector.translateToLocal("gui.ae2_auto_pattern_upload.labeled_wireless.online_count")
            + ": "
            + currentOnlineCount;
        fontRendererObj.drawString(onlineLine, x + 2, y + lineHeight, 0x404040);

        // 频道：显示 AE 网络使用的频道数（从服务器接收）
        String channelLine = StatCollector.translateToLocal("gui.ae2_auto_pattern_upload.labeled_wireless.channels")
            + ": "
            + currentUsedChannels;
        fontRendererObj.drawString(channelLine, x + 2, y + lineHeight * 2, 0x404040);

        // 映射频率：显示收发器管理的内部频率
        long freq = tile.getFrequency();
        String freqLine = StatCollector.translateToLocal("gui.ae2_auto_pattern_upload.labeled_wireless.frequency")
            + ": "
            + (freq > 0 ? String.valueOf(freq) : "-");
        fontRendererObj
            .drawString(fontRendererObj.trimStringToWidth(freqLine, INFO_W - 4), x + 2, y + lineHeight * 3, 0x404040);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        searchBox.drawTextBox();
    }

    private void applyFilter() {
        String query = searchBox.getText();
        filteredLabels.clear();

        if (query == null || query.trim()
            .isEmpty()) {
            filteredLabels.addAll(allLabels);
        } else {
            String lowerQuery = query.toLowerCase();
            for (LabelEntry entry : allLabels) {
                if (entry.label.toLowerCase()
                    .contains(lowerQuery)) {
                    filteredLabels.add(entry);
                }
            }
        }

        scrollOffset = 0;
        // 保持选中状态
        selectedIndex = -1;
        if (selectedLabel != null && !selectedLabel.isEmpty()) {
            for (int i = 0; i < filteredLabels.size(); i++) {
                if (filteredLabels.get(i).label.equals(selectedLabel)) {
                    selectedIndex = i;
                    break;
                }
            }
        }
    }

    /** 标签条目 */
    private static class LabelEntry {

        final String label;
        final long channel;
        final int onlineCount; // 在线数

        LabelEntry(String label, long channel, int onlineCount) {
            this.label = label;
            this.channel = channel;
            this.onlineCount = onlineCount;
        }
    }
}
