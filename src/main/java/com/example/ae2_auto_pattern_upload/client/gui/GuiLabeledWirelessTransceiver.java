package com.example.ae2_auto_pattern_upload.client.gui;

import com.example.ae2_auto_pattern_upload.Tags;
import com.example.ae2_auto_pattern_upload.container.ContainerLabeledWirelessTransceiver;
import com.example.ae2_auto_pattern_upload.network.LabelNetworkActionC2SPacket;
import com.example.ae2_auto_pattern_upload.network.LabelNetworkListC2SPacket;
import com.example.ae2_auto_pattern_upload.network.ModNetwork;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiLabeledWirelessTransceiver extends GuiContainer {
    private static final ResourceLocation TEX =
            new ResourceLocation(Tags.MOD_ID, "textures/gui/lable_wireless_transceiver_gui.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;
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

    private final BlockPos pos;
    private GuiTextField searchBox;
    private final List<LabelEntry> entries = new ArrayList<>();
    private final List<LabelEntry> filtered = new ArrayList<>();
    private int scrollOffset;
    private int selectedIndex = -1;
    private String currentLabel = "";
    private String currentOwner = "";
    private int onlineCount;
    private int usedChannels;
    private int maxChannels = 32;

    public GuiLabeledWirelessTransceiver(ContainerLabeledWirelessTransceiver inventorySlotsIn, BlockPos pos) {
        super(inventorySlotsIn);
        this.pos = pos;
        this.xSize = 256;
        this.ySize = 156;
    }

    public boolean isFor(BlockPos pos) {
        return this.pos.equals(pos);
    }

    public void updateData(
            List<String> labels,
            List<Long> channels,
            String currentLabel,
            String currentOwner,
            int onlineCount,
            int usedChannels,
            int maxChannels) {
        String selectedLabel = getSelectedLabel();
        this.entries.clear();
        for (int i = 0; i < labels.size(); i++) {
            long channel = i < channels.size() ? channels.get(i) : 0L;
            this.entries.add(new LabelEntry(labels.get(i), channel));
        }

        this.currentLabel = currentLabel == null ? "" : currentLabel;
        this.currentOwner = currentOwner == null ? "" : currentOwner;
        this.onlineCount = onlineCount;
        this.usedChannels = usedChannels;
        this.maxChannels = maxChannels <= 0 ? 32 : maxChannels;
        applyFilter(selectedLabel.isEmpty() ? this.currentLabel : selectedLabel);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        this.searchBox = new GuiTextField(0, this.fontRenderer, this.guiLeft + 134, this.guiTop + 23, 116, 12);
        this.searchBox.setMaxStringLength(64);
        this.searchBox.setEnableBackgroundDrawing(false);

        int startX = this.guiLeft + 145;
        int startY = this.guiTop + 101;
        int secondColX = startX + 58;
        int secondRowY = startY + 24;
        this.buttonList.add(new TextureButton(1, startX, startY, "gui.ae2_auto_pattern_upload.labeled_wireless.button.new"));
        this.buttonList.add(new TextureButton(2, secondColX, startY, "gui.ae2_auto_pattern_upload.labeled_wireless.button.delete"));
        this.buttonList.add(new TextureButton(3, startX, secondRowY, "gui.ae2_auto_pattern_upload.labeled_wireless.button.set"));
        this.buttonList.add(new TextureButton(4, secondColX, secondRowY, "gui.ae2_auto_pattern_upload.labeled_wireless.button.refresh"));
        requestList();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.searchBox.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.searchBox.drawTextBox();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEX);
        drawModalRectWithCustomSizedTexture(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize, TEX_W, TEX_H);
        drawRect(this.guiLeft + 9, this.guiTop + 27, this.guiLeft + 119, this.guiTop + 141, 0x20FFFFFF);
        drawRect(this.guiLeft + 123, this.guiTop + 21, this.guiLeft + 129, this.guiTop + 142, 0x20000000);
        drawRect(this.guiLeft + 134, this.guiTop + 41, this.guiLeft + 250, this.guiTop + 93, 0x10FFFFFF);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRenderer.drawString(I18n.format("tile.ae2_auto_pattern_upload.labeled_wireless_transceiver.name"), 8, 8, 0x404040);
        this.fontRenderer.drawString(I18n.format("gui.ae2_auto_pattern_upload.labeled_wireless.info"), 134, 8, 0x404040);
        drawLabelList();
        drawInfo();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 1:
                sendSet(this.searchBox.getText());
                break;
            case 2:
                sendDelete();
                break;
            case 3:
                sendSet(getSelectedLabel());
                break;
            case 4:
                sendDisconnect();
                break;
            default:
                break;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.searchBox.mouseClicked(mouseX, mouseY, mouseButton);
        if (isMouseInList(mouseX, mouseY)) {
            int row = (mouseY - (this.guiTop + LIST_Y)) / ROW_H;
            int idx = this.scrollOffset + row;
            if (idx >= 0 && idx < this.filtered.size()) {
                this.selectedIndex = idx;
            }
        } else if (isMouseInScrollbar(mouseX, mouseY)) {
            updateScrollByMouse(mouseY);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.searchBox.textboxKeyTyped(typedChar, keyCode)) {
            applyFilter(getSelectedLabel());
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            if (isMouseInList(mouseX, mouseY) || isMouseInScrollbar(mouseX, mouseY)) {
                int maxOffset = Math.max(0, this.filtered.size() - VISIBLE_ROWS);
                this.scrollOffset = Math.max(0, Math.min(maxOffset, this.scrollOffset - Integer.signum(wheel)));
            }
        }
    }

    private void drawLabelList() {
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int idx = this.scrollOffset + row;
            if (idx >= this.filtered.size()) {
                break;
            }

            int y = LIST_Y + row * ROW_H;
            if (idx == this.selectedIndex) {
                drawRect(LIST_X, y, LIST_X + LIST_W, y + ROW_H, 0x40FFFFFF);
            }

            LabelEntry entry = this.filtered.get(idx);
            String text = this.fontRenderer.trimStringToWidth(entry.label, LIST_W - 4);
            this.fontRenderer.drawString(text, LIST_X + 2, y + 2, 0x404040);
        }

        drawScrollBar();
    }

    private void drawScrollBar() {
        if (this.filtered.size() <= VISIBLE_ROWS) {
            return;
        }

        int maxOffset = this.filtered.size() - VISIBLE_ROWS;
        int knobHeight = Math.max(10, (int) ((double) VISIBLE_ROWS / (double) this.filtered.size() * SCROLL_H));
        int knobY = SCROLL_Y + (int) ((SCROLL_H - knobHeight) * (this.scrollOffset / (double) maxOffset));
        drawRect(SCROLL_X, knobY, SCROLL_X + SCROLL_W, knobY + knobHeight, 0x80FFFFFF);
    }

    private void drawInfo() {
        String labelText = I18n.format("gui.ae2_auto_pattern_upload.labeled_wireless.current_label")
                + ": "
                + (this.currentLabel.isEmpty() ? "-" : this.currentLabel);
        String ownerText = I18n.format("gui.ae2_auto_pattern_upload.labeled_wireless.current_owner")
                + ": "
                + (this.currentOwner.isEmpty() ? "-" : this.currentOwner);
        String onlineText = I18n.format("gui.ae2_auto_pattern_upload.labeled_wireless.online_count")
                + ": "
                + this.onlineCount;
        String channelText = I18n.format("gui.ae2_auto_pattern_upload.labeled_wireless.channels")
                + ": "
                + this.usedChannels
                + "/"
                + this.maxChannels;

        this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(labelText, 114), 134, 43, 0x404040);
        this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(ownerText, 114), 134, 55, 0x404040);
        this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(onlineText, 114), 134, 67, 0x404040);
        this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(channelText, 114), 134, 79, 0x404040);
    }

    private void requestList() {
        ModNetwork.CHANNEL.sendToServer(new LabelNetworkListC2SPacket(this.pos));
    }

    private void sendSet(String label) {
        ModNetwork.CHANNEL.sendToServer(
                new LabelNetworkActionC2SPacket(this.pos, label, LabelNetworkActionC2SPacket.Action.SET));
    }

    private void sendDelete() {
        String label = getSelectedLabel();
        if (label.isEmpty()) {
            label = this.searchBox.getText();
        }
        ModNetwork.CHANNEL.sendToServer(
                new LabelNetworkActionC2SPacket(this.pos, label, LabelNetworkActionC2SPacket.Action.DELETE));
    }

    private void sendDisconnect() {
        ModNetwork.CHANNEL.sendToServer(
                new LabelNetworkActionC2SPacket(this.pos, "", LabelNetworkActionC2SPacket.Action.DISCONNECT));
    }

    private void applyFilter(String preferredSelection) {
        String query = this.searchBox == null ? "" : this.searchBox.getText().trim();
        this.filtered.clear();
        for (LabelEntry entry : this.entries) {
            if (query.isEmpty() || entry.label.contains(query)) {
                this.filtered.add(entry);
            }
        }

        this.scrollOffset = 0;
        this.selectedIndex = -1;
        if (preferredSelection != null && !preferredSelection.isEmpty()) {
            for (int i = 0; i < this.filtered.size(); i++) {
                if (preferredSelection.equals(this.filtered.get(i).label)) {
                    this.selectedIndex = i;
                    ensureSelectionVisible();
                    break;
                }
            }
        }
    }

    private void ensureSelectionVisible() {
        if (this.selectedIndex < 0) {
            return;
        }

        int maxOffset = Math.max(0, this.filtered.size() - VISIBLE_ROWS);
        if (this.selectedIndex < this.scrollOffset) {
            this.scrollOffset = this.selectedIndex;
        } else if (this.selectedIndex >= this.scrollOffset + VISIBLE_ROWS) {
            this.scrollOffset = Math.min(maxOffset, this.selectedIndex - VISIBLE_ROWS + 1);
        }
    }

    private String getSelectedLabel() {
        if (this.selectedIndex >= 0 && this.selectedIndex < this.filtered.size()) {
            return this.filtered.get(this.selectedIndex).label;
        }
        return "";
    }

    private boolean isMouseInList(int mouseX, int mouseY) {
        return mouseX >= this.guiLeft + LIST_X
                && mouseX < this.guiLeft + LIST_X + LIST_W
                && mouseY >= this.guiTop + LIST_Y
                && mouseY < this.guiTop + LIST_Y + LIST_H;
    }

    private boolean isMouseInScrollbar(int mouseX, int mouseY) {
        return mouseX >= this.guiLeft + SCROLL_X
                && mouseX < this.guiLeft + SCROLL_X + SCROLL_W
                && mouseY >= this.guiTop + SCROLL_Y
                && mouseY < this.guiTop + SCROLL_Y + SCROLL_H;
    }

    private void updateScrollByMouse(int mouseY) {
        if (this.filtered.size() <= VISIBLE_ROWS) {
            return;
        }

        int maxOffset = this.filtered.size() - VISIBLE_ROWS;
        int relativeY = mouseY - (this.guiTop + SCROLL_Y);
        int knobHeight = Math.max(10, (int) ((double) VISIBLE_ROWS / (double) this.filtered.size() * SCROLL_H));
        double ratio = (relativeY - knobHeight / 2.0D) / (double) Math.max(1, SCROLL_H - knobHeight);
        ratio = Math.max(0.0D, Math.min(1.0D, ratio));
        this.scrollOffset = (int) Math.round(ratio * maxOffset);
    }

    private static final class LabelEntry {
        private final String label;
        private final long channel;

        private LabelEntry(String label, long channel) {
            this.label = label;
            this.channel = channel;
        }
    }

    private static final class TextureButton extends GuiButton {
        private static final int BTN_U = 2;
        private static final int BTN_V = 159;
        private static final int BTN_W = 28;
        private static final int BTN_H = 16;
        private final String langKey;

        private TextureButton(int buttonId, int x, int y, String langKey) {
            super(buttonId, x, y, BTN_W, BTN_H, "");
            this.langKey = langKey;
        }

        @Override
        public void drawButton(net.minecraft.client.Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!this.visible) {
                return;
            }

            this.hovered = mouseX >= this.x
                    && mouseY >= this.y
                    && mouseX < this.x + this.width
                    && mouseY < this.y + this.height;
            mc.getTextureManager().bindTexture(TEX);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            int u = BTN_U;
            int v = BTN_V;
            if (!this.enabled) {
                v += 36;
            } else if (this.hovered) {
                v += 18;
            }

            drawModalRectWithCustomSizedTexture(this.x, this.y, u, v, this.width, this.height, TEX_W, TEX_H);
            String text = I18n.format(this.langKey);
            String trimmed = mc.fontRenderer.trimStringToWidth(text, this.width - 4);
            int textX = this.x + (this.width - mc.fontRenderer.getStringWidth(trimmed)) / 2;
            int textY = this.y + (this.height - 8) / 2;
            mc.fontRenderer.drawString(trimmed, textX, textY, 0xFFFFFF);
        }
    }
}
