package com.gali.ae2_auto_pattern_upload.client.gui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;

import com.gali.ae2_auto_pattern_upload.network.ModNetwork;
import com.gali.ae2_auto_pattern_upload.network.UploadPatternPacket;
import com.gali.ae2_auto_pattern_upload.util.RecipeNameUtil;

/**
 * 供应器选择界面，移植自 1.12.2 版本，兼容 1.7.10。
 */
public class GuiProviderSelect extends GuiScreen {

    private static final int BUTTON_PREV = 100;
    private static final int BUTTON_NEXT = 101;
    private static final int BUTTON_RELOAD = 102;
    private static final int BUTTON_ADD = 103;
    private static final int BUTTON_DELETE = 104;
    private static final int BUTTON_CLOSE = 105;
    private static final int ENTRY_BUTTON_BASE = 200;
    private static final int PAGE_SIZE = 6;

    private final GuiScreen parent;
    private final List<Long> ids;
    private final List<String> names;
    private final List<Integer> emptySlots;

    private final List<GroupEntry> groups = new ArrayList<>();
    private final List<GroupEntry> filtered = new ArrayList<>();

    private GuiTextField searchBox;
    private GuiTextField mappingField;
    private String query = "";
    private int page = 0;
    private boolean needsRefresh = false;
    private String lastAddedMappingName = null;

    private static class GroupEntry {

        long id;
        String name;
        int totalSlots;
        int count;
        int bestSlots;
    }

    public GuiProviderSelect(List<Long> ids, List<String> names, List<Integer> emptySlots) {
        this(null, ids, names, emptySlots);
    }

    public GuiProviderSelect(GuiScreen parent, List<Long> ids, List<String> names, List<Integer> emptySlots) {
        this.parent = parent;
        this.ids = ids == null ? new ArrayList<Long>() : new ArrayList<Long>(ids);
        this.names = names == null ? new ArrayList<String>() : new ArrayList<String>(names);
        this.emptySlots = emptySlots == null ? new ArrayList<Integer>() : new ArrayList<Integer>(emptySlots);

        String recent = RecipeNameUtil.getLastRecipeName();
        if (recent != null && !recent.isEmpty()) {
            this.query = recent;
            RecipeNameUtil.clearLastRecipeName();
        }

        buildGroups();
        applyFilter();
    }

    private void buildGroups() {
        Map<String, GroupEntry> map = new LinkedHashMap<String, GroupEntry>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            long id = ids.get(i);
            int slots = emptySlots.get(i);

            GroupEntry entry = map.get(name);
            if (entry == null) {
                entry = new GroupEntry();
                entry.name = name;
                map.put(name, entry);
            }
            entry.count++;
            entry.totalSlots += Math.max(0, slots);
            if (slots > entry.bestSlots || entry.id == 0L) {
                entry.bestSlots = Math.max(0, slots);
                entry.id = id;
            }
        }
        groups.clear();
        groups.addAll(map.values());
    }

    private void applyFilter() {
        filtered.clear();
        String q = query == null ? ""
            : query.trim()
                .toLowerCase();
        for (GroupEntry entry : groups) {
            if (q.isEmpty() || entry.name.toLowerCase()
                .contains(q)) {
                filtered.add(entry);
            }
        }
        if (!q.isEmpty() && filtered.isEmpty()) {
            filtered.addAll(groups);
        }
    }

    @Override
    public void initGui() {
        this.buttonList.clear();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 70;

        if (this.searchBox == null) {
            this.searchBox = new GuiTextField(this.fontRendererObj, centerX - 120, startY - 25, 240, 18);
            this.searchBox.setMaxStringLength(64);
        } else {
            this.searchBox.xPosition = centerX - 120;
            this.searchBox.yPosition = startY - 25;
        }
        this.searchBox.setText(query);

        int navY = startY + PAGE_SIZE * 25 + 10;
        if (this.mappingField == null) {
            this.mappingField = new GuiTextField(this.fontRendererObj, centerX - 240, navY + 30, 180, 18);
            this.mappingField.setMaxStringLength(64);
        } else {
            this.mappingField.xPosition = centerX - 240;
            this.mappingField.yPosition = navY + 30;
        }

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filtered.size());
        for (int i = start; i < end; i++) {
            int localIndex = i - start;
            GroupEntry entry = filtered.get(i);
            String label = buildLabel(entry);
            GuiButton button = new GuiButton(
                ENTRY_BUTTON_BASE + localIndex,
                centerX - 120,
                startY + localIndex * 25,
                240,
                20,
                label);
            this.buttonList.add(button);
        }
        GuiButton prevBtn = new GuiButton(BUTTON_PREV, centerX - 60, navY, 20, 20, "<");
        GuiButton nextBtn = new GuiButton(BUTTON_NEXT, centerX + 40, navY, 20, 20, ">");
        prevBtn.enabled = page > 0;
        nextBtn.enabled = (page + 1) * PAGE_SIZE < filtered.size();
        this.buttonList.add(prevBtn);
        this.buttonList.add(nextBtn);

        GuiButton addBtn = new GuiButton(
            BUTTON_ADD,
            centerX - 50,
            navY + 30,
            50,
            20,
            translate("gui.ae2_auto_pattern_upload.add"));
        GuiButton reloadBtn = new GuiButton(
            BUTTON_RELOAD,
            centerX + 10,
            navY + 30,
            60,
            20,
            translate("gui.ae2_auto_pattern_upload.reload"));
        GuiButton delBtn = new GuiButton(
            BUTTON_DELETE,
            centerX + 80,
            navY + 30,
            50,
            20,
            translate("gui.ae2_auto_pattern_upload.delete"));
        GuiButton closeBtn = new GuiButton(BUTTON_CLOSE, centerX + 140, navY + 30, 60, 20, translate("gui.cancel"));

        this.buttonList.add(addBtn);
        this.buttonList.add(reloadBtn);
        this.buttonList.add(delBtn);
        this.buttonList.add(closeBtn);
    }

    private String buildLabel(GroupEntry entry) {
        return entry.name + " (" + entry.totalSlots + ") x" + entry.count;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == null || !button.enabled) {
            return;
        }
        int start = page * PAGE_SIZE;
        if (button.id >= ENTRY_BUTTON_BASE && button.id < ENTRY_BUTTON_BASE + PAGE_SIZE) {
            int idx = start + (button.id - ENTRY_BUTTON_BASE);
            if (idx >= 0 && idx < filtered.size()) {
                long providerId = filtered.get(idx).id;
                handleSelect(providerId);
                this.mc.displayGuiScreen(null);
            }
            return;
        }

        switch (button.id) {
            case BUTTON_PREV:
                changePage(-1);
                break;
            case BUTTON_NEXT:
                changePage(1);
                break;
            case BUTTON_RELOAD:
                reloadMappings();
                break;
            case BUTTON_ADD:
                addMappingFromUI();
                break;
            case BUTTON_DELETE:
                deleteMappingFromUI();
                break;
            case BUTTON_CLOSE:
                this.mc.displayGuiScreen(parent);
                break;
            default:
                break;
        }
    }

    protected void handleSelect(long providerId) {
        ModNetwork.CHANNEL.sendToServer(new UploadPatternPacket(providerId));
        if (this.parent != null) {
            this.mc.displayGuiScreen(parent);
        } else {
            this.mc.displayGuiScreen(null);
        }
    }

    private void changePage(int delta) {
        int newPage = page + delta;
        if (newPage < 0) {
            return;
        }
        if (newPage * PAGE_SIZE >= filtered.size()) {
            return;
        }
        page = newPage;
        needsRefresh = true;
    }

    private void reloadMappings() {
        RecipeNameUtil.reloadMappings();
        if (lastAddedMappingName != null && !lastAddedMappingName.isEmpty()) {
            query = lastAddedMappingName;
            page = 0;
        }
        applyFilter();
        needsRefresh = true;
        sendClientMessage(translate("ae2_auto_pattern_upload.info.mappings_reloaded"));
    }

    private void addMappingFromUI() {
        String key = query == null ? "" : query.trim();
        String value = mappingField == null ? ""
            : mappingField.getText()
                .trim();
        if (key.isEmpty()) {
            sendClientMessage(translate("ae2_auto_pattern_upload.info.enter_keyword"));
            return;
        }
        if (value.isEmpty()) {
            sendClientMessage(translate("ae2_auto_pattern_upload.info.enter_mapping_name"));
            return;
        }
        if (RecipeNameUtil.addOrUpdateMapping(key, value)) {
            sendClientMessage(String.format(translate("ae2_auto_pattern_upload.info.mapping_added"), key, value));
            lastAddedMappingName = value;
            RecipeNameUtil.reloadMappings();
            applyFilter();
            needsRefresh = true;
        } else {
            sendClientMessage(translate("ae2_auto_pattern_upload.info.mapping_add_failed"));
        }
    }

    private void deleteMappingFromUI() {
        String value = mappingField == null ? ""
            : mappingField.getText()
                .trim();
        if (value.isEmpty()) {
            sendClientMessage(translate("ae2_auto_pattern_upload.info.enter_mapping_delete"));
            return;
        }
        int removed = RecipeNameUtil.removeMappingsByCnValue(value);
        if (removed > 0) {
            sendClientMessage(String.format(translate("ae2_auto_pattern_upload.info.mapping_deleted"), removed));
            RecipeNameUtil.reloadMappings();
            applyFilter();
            needsRefresh = true;
        } else {
            sendClientMessage(translate("ae2_auto_pattern_upload.info.mapping_not_found"));
        }
    }

    private void sendClientMessage(String msg) {
        if (this.mc != null && this.mc.thePlayer != null && msg != null && !msg.isEmpty()) {
            this.mc.thePlayer.addChatMessage(new ChatComponentText(msg));
        }
    }

    @Override
    public void updateScreen() {
        if (searchBox != null) {
            searchBox.updateCursorCounter();
        }
        if (mappingField != null) {
            mappingField.updateCursorCounter();
        }
        if (needsRefresh) {
            needsRefresh = false;
            initGui();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        try {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        } catch (Exception ignored) {}
        if (searchBox != null) {
            searchBox.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (mappingField != null) {
            mappingField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (mouseButton == 1 && searchBox != null) {
            if (isPointInRegion(
                searchBox.xPosition,
                searchBox.yPosition,
                searchBox.width,
                searchBox.height,
                mouseX,
                mouseY)) {
                if (!searchBox.getText()
                    .isEmpty()) {
                    searchBox.setText("");
                    query = "";
                    page = 0;
                    applyFilter();
                    needsRefresh = true;
                }
                return;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        boolean handled = false;
        if (searchBox != null && searchBox.textboxKeyTyped(typedChar, keyCode)) {
            String newQuery = searchBox.getText();
            if (!Objects.equals(newQuery, query)) {
                query = newQuery;
                page = 0;
                applyFilter();
                needsRefresh = true;
            }
            handled = true;
        }
        if (mappingField != null && mappingField.textboxKeyTyped(typedChar, keyCode)) {
            handled = true;
        }
        if (!handled) {
            super.keyTyped(typedChar, keyCode);
        }
    }

    private boolean isPointInRegion(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        String title = translate("ae2_auto_pattern_upload.select_provider");
        this.fontRendererObj.drawStringWithShadow(
            title,
            this.width / 2 - this.fontRendererObj.getStringWidth(title) / 2,
            this.height / 2 - 130,
            0xFFFFFF);

        if (searchBox != null) {
            searchBox.drawTextBox();
        }
        if (mappingField != null) {
            mappingField.drawTextBox();
        }

        String mappingLabel = translate("gui.ae2_auto_pattern_upload.mapping_label");
        this.fontRendererObj.drawString(
            mappingLabel,
            this.mappingField.xPosition - this.fontRendererObj.getStringWidth(mappingLabel) - 4,
            this.mappingField.yPosition + 2,
            0xFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    protected String translate(String key) {
        return StatCollector.translateToLocal(key);
    }
}
