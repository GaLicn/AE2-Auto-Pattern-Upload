package com.gali.client.gui;

import com.gali.network.ModNetwork;
import com.gali.network.UploadPatternPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 供应器选择界面
 */
public class ProviderSelectScreen extends Screen {
    private static final int BUTTON_PREV = 100;
    private static final int BUTTON_NEXT = 101;
    private static final int BUTTON_CLOSE = 102;
    private static final int ENTRY_BUTTON_BASE = 200;
    private static final int PAGE_SIZE = 6;

    private final List<Long> ids;
    private final List<String> names;
    private final List<Integer> emptySlots;
    
    private final List<GroupEntry> groups = new ArrayList<>();
    private final List<GroupEntry> filtered = new ArrayList<>();
    
    private EditBox searchBox;
    private String query = "";
    private int page = 0;

    private static class GroupEntry {
        long id;
        String name;
        int totalSlots;
        int count;
        int bestSlots;
    }

    public ProviderSelectScreen(List<Long> ids, List<String> names, List<Integer> emptySlots) {
        super(Component.literal("选择样板供应器"));
        this.ids = ids;
        this.names = names;
        this.emptySlots = emptySlots;
        
        buildGroups();
        applyFilter();
    }

    private void buildGroups() {
        Map<String, GroupEntry> map = new LinkedHashMap<>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            long id = ids.get(i);
            int slots = emptySlots.get(i);

            GroupEntry entry = map.computeIfAbsent(name, k -> {
                GroupEntry ge = new GroupEntry();
                ge.name = name;
                return ge;
            });
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
        String q = query == null ? "" : query.trim().toLowerCase();
        for (GroupEntry entry : groups) {
            if (q.isEmpty() || entry.name.toLowerCase().contains(q)) {
                filtered.add(entry);
            }
        }
        if (!q.isEmpty() && filtered.isEmpty()) {
            filtered.addAll(groups);
        }
    }

    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int startY = this.height / 2 - 70;

        // 搜索框
        this.searchBox = new EditBox(this.font, centerX - 120, startY - 25, 240, 18, Component.literal("搜索"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setValue(query);
        this.addRenderableWidget(this.searchBox);

        // 供应器按钮
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filtered.size());
        for (int i = start; i < end; i++) {
            int localIndex = i - start;
            GroupEntry entry = filtered.get(i);
            String label = entry.name + " (" + entry.totalSlots + ") x" + entry.count;
            
            this.addRenderableWidget(Button.builder(
                Component.literal(label),
                btn -> onProviderSelected(entry.id)
            ).bounds(centerX - 120, startY + localIndex * 25, 240, 20).build());
        }

        // 翻页按钮
        int navY = startY + PAGE_SIZE * 25 + 10;
        this.addRenderableWidget(Button.builder(
            Component.literal("<"),
            btn -> changePage(-1)
        ).bounds(centerX - 60, navY, 20, 20).build());
        
        this.addRenderableWidget(Button.builder(
            Component.literal(">"),
            btn -> changePage(1)
        ).bounds(centerX + 40, navY, 20, 20).build());

        // 关闭按钮
        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.cancel"),
            btn -> this.onClose()
        ).bounds(centerX + 70, navY, 60, 20).build());
    }

    private void onProviderSelected(long providerId) {
        ModNetwork.CHANNEL.sendToServer(new UploadPatternPacket(providerId));
        this.onClose();
    }

    private void changePage(int delta) {
        int newPage = page + delta;
        if (newPage < 0 || newPage * PAGE_SIZE >= filtered.size()) {
            return;
        }
        page = newPage;
        this.rebuildWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        if (searchBox != null) {
            String newQuery = searchBox.getValue();
            if (!newQuery.equals(query)) {
                query = newQuery;
                page = 0;
                applyFilter();
                this.rebuildWidgets();
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        
        // 绘制标题
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 100, 0xFFFFFF);
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
