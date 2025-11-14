package com.example.modid.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import java.util.*;

/**
 * 供应器选择 GUI
 */
public class GuiProviderSelect extends GuiScreen {
    private final GuiScreen parent;
    private final List<Long> ids;
    private final List<String> names;
    private final List<Integer> emptySlots;
    
    // 分组后的数据
    private final Map<String, ProviderGroup> groups = new LinkedHashMap<>();
    private final List<String> groupNames = new ArrayList<>();
    private final List<Long> groupIds = new ArrayList<>();
    private final List<Integer> groupTotalSlots = new ArrayList<>();
    private final List<Integer> groupCounts = new ArrayList<>();
    
    // 过滤后的数据
    private final List<String> filteredNames = new ArrayList<>();
    private final List<Long> filteredIds = new ArrayList<>();
    private final List<Integer> filteredSlots = new ArrayList<>();
    private final List<Integer> filteredCounts = new ArrayList<>();
    
    private GuiTextField searchBox;
    private String query = "";
    private int page = 0;
    private static final int PAGE_SIZE = 6;
    
    public GuiProviderSelect(List<Long> ids, List<String> names, List<Integer> emptySlots) {
        super();
        this.parent = null;
        this.ids = ids;
        this.names = names;
        this.emptySlots = emptySlots;
        
        buildGroups();
        applyFilter();
    }
    
    private void buildGroups() {
        groups.clear();
        groupNames.clear();
        groupIds.clear();
        groupTotalSlots.clear();
        groupCounts.clear();
        
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            long id = ids.get(i);
            int slots = emptySlots.get(i);
            
            ProviderGroup group = groups.computeIfAbsent(name, k -> new ProviderGroup());
            group.count++;
            group.totalSlots += Math.max(0, slots);
            
            if (slots > group.bestSlots) {
                group.bestSlots = slots;
                group.bestId = id;
            }
        }
        
        for (Map.Entry<String, ProviderGroup> e : groups.entrySet()) {
            groupNames.add(e.getKey());
            groupIds.add(e.getValue().bestId);
            groupTotalSlots.add(e.getValue().totalSlots);
            groupCounts.add(e.getValue().count);
        }
    }
    
    private void applyFilter() {
        filteredNames.clear();
        filteredIds.clear();
        filteredSlots.clear();
        filteredCounts.clear();
        
        String q = query.trim().toLowerCase();
        for (int i = 0; i < groupNames.size(); i++) {
            String name = groupNames.get(i).toLowerCase();
            if (q.isEmpty() || name.contains(q)) {
                filteredNames.add(groupNames.get(i));
                filteredIds.add(groupIds.get(i));
                filteredSlots.add(groupTotalSlots.get(i));
                filteredCounts.add(groupCounts.get(i));
            }
        }
    }
    
    @Override
    public void initGui() {
        this.buttonList.clear();
        
        int centerX = this.width / 2;
        int startY = this.height / 2 - 70;
        
        // 搜索框
        this.searchBox = new GuiTextField(0, this.mc.fontRenderer,
            centerX - 120, startY - 25, 240, 18);
        this.searchBox.setText(query);
        
        // 供应器按钮
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filteredIds.size());
        
        for (int i = start; i < end; i++) {
            String label = filteredNames.get(i) + " (" + filteredSlots.get(i) + ") x" + filteredCounts.get(i);
            GuiButton btn = new GuiButton(i - start + 1,
                centerX - 120, startY + (i - start) * 25, 240, 20, label);
            this.buttonList.add(btn);
        }
        
        // 分页按钮
        int navY = startY + PAGE_SIZE * 25 + 10;
        GuiButton prevBtn = new GuiButton(100, centerX - 60, navY, 20, 20, "<");
        GuiButton nextBtn = new GuiButton(101, centerX + 40, navY, 20, 20, ">");
        prevBtn.enabled = page > 0;
        nextBtn.enabled = (page + 1) * PAGE_SIZE < filteredIds.size();
        this.buttonList.add(prevBtn);
        this.buttonList.add(nextBtn);
        
        // 关闭按钮
        GuiButton closeBtn = new GuiButton(102, centerX - 40, navY, 80, 20, "Close");
        this.buttonList.add(closeBtn);
    }
    
    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id >= 1 && button.id <= PAGE_SIZE) {
            int idx = page * PAGE_SIZE + button.id - 1;
            if (idx < filteredIds.size()) {
                long providerId = filteredIds.get(idx);
                // TODO: 发送上传样板的包
                this.mc.displayGuiScreen(null);
            }
        } else if (button.id == 100) {
            // 上一页
            if (page > 0) {
                page--;
                this.initGui();
            }
        } else if (button.id == 101) {
            // 下一页
            if ((page + 1) * PAGE_SIZE < filteredIds.size()) {
                page++;
                this.initGui();
            }
        } else if (button.id == 102) {
            // 关闭
            this.mc.displayGuiScreen(parent);
        }
    }
    
    @Override
    public void updateScreen() {
        this.searchBox.updateCursorCounter();
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        if (this.searchBox.textboxKeyTyped(typedChar, keyCode)) {
            String newQuery = this.searchBox.getText();
            if (!newQuery.equals(query)) {
                query = newQuery;
                page = 0;
                applyFilter();
                this.initGui();
            }
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        
        // 标题
        String title = "Select Provider";
        this.mc.fontRenderer.drawStringWithShadow(title,
            this.width / 2 - this.mc.fontRenderer.getStringWidth(title) / 2,
            this.height / 2 - 100, 0xFFFFFF);
        
        // 搜索框
        this.searchBox.drawTextBox();
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
    
    private static class ProviderGroup {
        long bestId = Long.MIN_VALUE;
        int bestSlots = Integer.MIN_VALUE;
        int totalSlots = 0;
        int count = 0;
    }
}
