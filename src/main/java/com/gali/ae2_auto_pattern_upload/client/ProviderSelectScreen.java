package com.gali.ae2_auto_pattern_upload.client;

import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.StyleManager;
import appeng.client.gui.widgets.AE2Button;
import appeng.client.gui.widgets.AETextField;
import com.gali.ae2_auto_pattern_upload.network.PatternUploadUtil;
import com.gali.ae2_auto_pattern_upload.network.UploadEncodedPatternC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 使用 AE2 风格组件选择目标样板供应器。 */
public class ProviderSelectScreen extends Screen {
    private static final int MIN_PAGE_SIZE = 2;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ENTRY_GAP = 5;
    private static final int ENTRY_WIDTH = 240;
    private static final int AE_TEXT_FIELD_WIDTH = 128;
    private static final int AE_SEARCH_FIELD_HEIGHT = 20;
    private final Screen parent;
    private final List<Long> ids;
    private final List<Component> names;
    private final List<Integer> emptySlots;
    private final List<Group> groups = new ArrayList<>();
    private final List<Group> filteredGroups = new ArrayList<>();
    private final ScreenStyle aeStyle;
    private AETextField searchBox;
    private String query;
    private int page;
    private int pageSize = 6;
    private boolean refreshPending;

    public ProviderSelectScreen(Screen parent, List<Long> ids, List<Component> names, List<Integer> emptySlots) {
        super(Component.translatable("ae2_auto_pattern_upload.screen.choose_provider.title"));
        this.parent = parent;
        this.ids = ids;
        this.names = names;
        this.emptySlots = emptySlots;
        this.aeStyle = StyleManager.loadStyleDoc("/screens/common/common.json");
        this.query = PatternUploadUtil.consumeLastProviderSearchKey();
        if (this.query == null) {
            this.query = "";
        }
        buildGroups();
        applyFilter();
    }

    private void buildGroups() {
        Map<String, Group> grouped = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(ids.size(), Math.min(names.size(), emptySlots.size())); i++) {
            String name = names.get(i).getString();
            Group group = grouped.get(name);
            if (group == null) {
                group = new Group(name, ids.get(i));
                grouped.put(name, group);
            }
            group.totalSlots += Math.max(0, emptySlots.get(i));
            group.count++;
            if (emptySlots.get(i) > group.representativeSlots) {
                group.representativeId = ids.get(i);
                group.representativeSlots = emptySlots.get(i);
            }
        }
        groups.addAll(grouped.values());
    }

    private void applyFilter() {
        filteredGroups.clear();
        String normalized = query.toLowerCase(Locale.ROOT).trim();
        for (Group group : groups) {
            if (normalized.isEmpty() || group.name.toLowerCase(Locale.ROOT).contains(normalized)) {
                filteredGroups.add(group);
            }
        }
        page = Math.min(page, Math.max(0, (filteredGroups.size() - 1) / Math.max(1, pageSize)));
    }

    private void choose(Group group) {
        PacketDistributor.sendToServer(new UploadEncodedPatternC2SPacket(group.representativeId));
        onClose();
    }

    @Override
    protected void init() {
        clearWidgets();
        int centerX = width / 2;
        int entryUnitHeight = BUTTON_HEIGHT + ENTRY_GAP;
        int availableHeight = height - 150;
        pageSize = Math.max(MIN_PAGE_SIZE, availableHeight / entryUnitHeight);

        int start = page * pageSize;
        int visibleEntries = Math.max(0, Math.min(pageSize, filteredGroups.size() - start));
        int listHeight = visibleEntries == 0 ? BUTTON_HEIGHT : visibleEntries * entryUnitHeight;
        int contentHeight = AE_SEARCH_FIELD_HEIGHT + 5 + listHeight + 10 + BUTTON_HEIGHT + 8 + BUTTON_HEIGHT;
        int searchTop = Math.max(10, (height - contentHeight) / 2);
        int listTop = searchTop + AE_SEARCH_FIELD_HEIGHT + 5;

        // 关闭原版 EditBox 的填充，仅保留 AE2 文本框纹理。
        searchBox = new AETextField(aeStyle, font, centerX - AE_TEXT_FIELD_WIDTH / 2, searchTop,
                AE_TEXT_FIELD_WIDTH, AE_SEARCH_FIELD_HEIGHT);
        searchBox.setBordered(false);
        searchBox.setMaxLength(256);
        searchBox.setPlaceholder(Component.translatable("ae2_auto_pattern_upload.screen.search"));
        searchBox.setValue(query);
        searchBox.setResponder(value -> {
            if (value.equals(query)) {
                return;
            }
            query = value;
            page = 0;
            applyFilter();
            refreshPending = true;
        });
        addRenderableWidget(searchBox);

        int end = Math.min(filteredGroups.size(), start + visibleEntries);
        for (int i = start; i < end; i++) {
            Group group = filteredGroups.get(i);
            Button button = new AE2Button(centerX - ENTRY_WIDTH / 2,
                    listTop + (i - start) * entryUnitHeight, ENTRY_WIDTH, BUTTON_HEIGHT,
                    Component.literal(group.label()), ignored -> choose(group));
            addRenderableWidget(button);
        }

        int navigationY = listTop + listHeight + 10;
        Button previous = new AE2Button(centerX - 60, navigationY, 20, BUTTON_HEIGHT,
                Component.literal("<"), ignored -> changePage(-1));
        Button next = new AE2Button(centerX + 40, navigationY, 20, BUTTON_HEIGHT,
                Component.literal(">"), ignored -> changePage(1));
        previous.active = page > 0;
        next.active = (page + 1) * pageSize < filteredGroups.size();
        addRenderableWidget(previous);
        addRenderableWidget(next);

        Button close = new AE2Button(centerX - 60, navigationY + 28, 120, BUTTON_HEIGHT,
                Component.translatable("gui.cancel"), ignored -> onClose());
        close.setTooltip(Tooltip.create(Component.translatable("ae2_auto_pattern_upload.screen.cancel.tooltip")));
        addRenderableWidget(close);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (filteredGroups.isEmpty()) {
            int centerX = width / 2;
            graphics.drawCenteredString(font,
                    Component.translatable("ae2_auto_pattern_upload.screen.no_providers"),
                    centerX, height / 2, 0xFFFFFFFF);
        }
    }

    private void changePage(int delta) {
        int newPage = page + delta;
        if (newPage < 0 || newPage * pageSize >= filteredGroups.size()) {
            return;
        }
        page = newPage;
        refreshPending = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (refreshPending) {
            refreshPending = false;
            init();
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class Group {
        private final String name;
        private long representativeId;
        private int representativeSlots;
        private int totalSlots;
        private int count;

        private Group(String name, long representativeId) {
            this.name = name;
            this.representativeId = representativeId;
        }

        private String label() {
            return name + "  (" + totalSlots + ") x" + count;
        }
    }
}
