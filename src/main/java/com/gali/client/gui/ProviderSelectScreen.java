package com.gali.client.gui;

import com.gali.network.ModNetwork;
import com.gali.network.UploadPatternPacket;
import com.gali.util.RecipeTypeNameConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 供应器选择界面，完全对齐ExtendedAE-Plus的布局
 */
public class ProviderSelectScreen extends Screen {
    private static final int PAGE_SIZE = 6;

    private final Screen parent;
    private final List<Long> ids;
    private final List<String> names;
    private final List<Integer> emptySlots;
    
    // 分组后的数据
    private final List<Long> gIds = new ArrayList<>();
    private final List<String> gNames = new ArrayList<>();
    private final List<Integer> gTotalSlots = new ArrayList<>();
    private final List<Integer> gCount = new ArrayList<>();
    
    // 过滤后的数据
    private final List<Long> fIds = new ArrayList<>();
    private final List<String> fNames = new ArrayList<>();
    private final List<Integer> fTotalSlots = new ArrayList<>();
    private final List<Integer> fCount = new ArrayList<>();
    
    private EditBox searchBox;
    private EditBox cnInput;
    private String query = "";
    private Button prevButton;
    private Button nextButton;
    private int page = 0;
    
    private final List<Button> entryButtons = new ArrayList<>();
    private final int[] buttonIndexMap = new int[PAGE_SIZE];
    
    private static final Map<String, String> componentCache = new HashMap<>();
    private String lastLanguage = "";

    public ProviderSelectScreen(List<Long> ids, List<String> names, List<Integer> emptySlots) {
        super(Component.literal("选择样板供应器"));
        this.parent = null;
        this.ids = ids;
        this.names = names;
        this.emptySlots = emptySlots;
        
        // 从JEI获取最近的配方类型名称
        try {
            String recent = RecipeTypeNameConfig.lastProcessingName;
            if (recent != null && !recent.isBlank()) {
                this.query = recent;
                RecipeTypeNameConfig.lastProcessingName = null;
            }
        } catch (Throwable ignored) {}
        
        buildGroups();
        applyFilter();
    }

    private String deserializeComponentName(String name) {
        return componentCache.computeIfAbsent(name, k -> {
            try {
                if (name.startsWith("{") || name.startsWith("\"")) {
                    Component component = Component.Serializer.fromJson(name);
                    if (component != null) {
                        return component.getString();
                    }
                }
            } catch (Exception ignored) {}
            return name;
        });
    }

    private void buildGroups() {
        Map<String, Group> map = new LinkedHashMap<>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            long id = ids.get(i);
            int slots = emptySlots.get(i);

            String groupKey = deserializeComponentName(name);
            map.compute(groupKey, (k, g) -> {
                if (g == null) {
                    return new Group(id, slots);
                }
                g.merge(id, slots);
                return g;
            });
        }
        
        for (Map.Entry<String, Group> e : map.entrySet()) {
            String name = e.getKey();
            Group g = e.getValue();
            gNames.add(name);
            gIds.add(g.bestId);
            gTotalSlots.add(g.totalSlots);
            gCount.add(g.count);
        }
    }

    private void applyFilter() {
        fIds.clear();
        fNames.clear();
        fTotalSlots.clear();
        fCount.clear();
        
        String q = query == null ? "" : query.trim();
        String qLower = q.toLowerCase(Locale.ROOT);

        for (int i = 0; i < gIds.size(); i++) {
            String name = gNames.get(i);
            if (q.isEmpty() || name.toLowerCase(Locale.ROOT).contains(qLower)) {
                fIds.add(gIds.get(i));
                fNames.add(name);
                fTotalSlots.add(gTotalSlots.get(i));
                fCount.add(gCount.get(i));
            }
        }
        
        if (!q.isEmpty() && fIds.isEmpty()) {
            for (int i = 0; i < gIds.size(); i++) {
                fIds.add(gIds.get(i));
                fNames.add(gNames.get(i));
                fTotalSlots.add(gTotalSlots.get(i));
                fCount.add(gCount.get(i));
            }
        }

        // 自然排序
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < fNames.size(); i++) indices.add(i);
        indices.sort((i1, i2) -> compareNatural(fNames.get(i1), fNames.get(i2)));

        List<Long> sortedIds = new ArrayList<>();
        List<String> sortedNames = new ArrayList<>();
        List<Integer> sortedSlots = new ArrayList<>();
        List<Integer> sortedCount = new ArrayList<>();

        for (int idx : indices) {
            sortedIds.add(fIds.get(idx));
            sortedNames.add(fNames.get(idx));
            sortedSlots.add(fTotalSlots.get(idx));
            sortedCount.add(fCount.get(idx));
        }

        fIds.clear();
        fIds.addAll(sortedIds);
        fNames.clear();
        fNames.addAll(sortedNames);
        fTotalSlots.clear();
        fTotalSlots.addAll(sortedSlots);
        fCount.clear();
        fCount.addAll(sortedCount);
    }

    private static final Pattern NATURAL_PATTERN = Pattern.compile("(\\D*)(\\d*)");
    
    private static int compareNatural(String s1, String s2) {
        Matcher m1 = NATURAL_PATTERN.matcher(s1);
        Matcher m2 = NATURAL_PATTERN.matcher(s2);

        while (m1.find() && m2.find()) {
            int cmp = m1.group(1).compareTo(m2.group(1));
            if (cmp != 0) return cmp;

            String num1 = m1.group(2);
            String num2 = m2.group(2);
            if (!num1.isEmpty() || !num2.isEmpty()) {
                int n1 = num1.isEmpty() ? 0 : Integer.parseInt(num1);
                int n2 = num2.isEmpty() ? 0 : Integer.parseInt(num2);
                if (n1 != n2) return Integer.compare(n1, n2);
            }
        }
        return s1.length() - s2.length();
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        entryButtons.clear();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 70;

        // 搜索框
        if (searchBox == null) {
            searchBox = new EditBox(this.font, centerX - 120, startY - 25, 240, 18, Component.literal("搜索"));
        } else {
            searchBox.setX(centerX - 120);
            searchBox.setY(startY - 25);
            searchBox.setWidth(240);
        }
        searchBox.setValue(query);
        searchBox.setResponder(text -> {
            if (Objects.equals(text, query)) return;
            query = text;
            page = 0;
            applyFilter();
            refreshButtons();
        });
        this.addRenderableWidget(searchBox);

        // 供应器按钮池
        int buttonWidth = 240;
        int buttonHeight = 20;
        int gap = 5;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int btnIdx = i;
            Button btn = Button.builder(Component.literal(""), b -> {
                int actualIdx = buttonIndexMap[btnIdx];
                if (actualIdx >= 0 && actualIdx < fIds.size()) {
                    onChoose(actualIdx);
                }
            }).bounds(centerX - buttonWidth / 2, startY + i * (buttonHeight + gap), buttonWidth, buttonHeight).build();
            entryButtons.add(btn);
            buttonIndexMap[i] = -1;
            this.addRenderableWidget(btn);
        }

        // 翻页按钮
        int navY = startY + PAGE_SIZE * (buttonHeight + gap) + 10;
        prevButton = Button.builder(Component.literal("<"), b -> changePage(-1))
                .bounds(centerX - 60, navY, 20, 20).build();
        nextButton = Button.builder(Component.literal(">"), b -> changePage(1))
                .bounds(centerX + 40, navY, 20, 20).build();
        this.addRenderableWidget(prevButton);
        this.addRenderableWidget(nextButton);

        // 映射管理区域
        int btnWidth2 = 80;
        int inputWidth = 120;
        int btnGap = 5;
        int totalWidth = btnWidth2 + btnGap + inputWidth + btnGap + btnWidth2 * 3 + btnGap * 2;
        int startX = centerX - totalWidth / 2;

        // 重载按钮
        Button reload = Button.builder(Component.literal("重载"), b -> reloadMapping())
                .bounds(startX, navY + 30, btnWidth2, 20).build();
        this.addRenderableWidget(reload);

        // 中文名输入框
        if (cnInput == null) {
            cnInput = new EditBox(this.font, startX + btnWidth2 + btnGap, navY + 30, inputWidth, 20, Component.literal("映射名称"));
        } else {
            cnInput.setX(startX + btnWidth2 + btnGap);
            cnInput.setY(navY + 30);
            cnInput.setWidth(inputWidth);
        }
        this.addRenderableWidget(cnInput);

        // 关闭按钮
        Button close = Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(startX + btnWidth2 + btnGap + inputWidth + btnGap, navY + 30, btnWidth2, 20).build();
        this.addRenderableWidget(close);

        // 添加映射按钮
        Button addMap = Button.builder(Component.literal("添加"), b -> addMapping())
                .bounds(startX + btnWidth2 + btnGap + inputWidth + btnGap + btnWidth2 + btnGap, navY + 30, btnWidth2, 20).build();
        this.addRenderableWidget(addMap);

        // 删除映射按钮
        Button delMap = Button.builder(Component.literal("删除"), b -> deleteMapping())
                .bounds(startX + btnWidth2 + btnGap + inputWidth + btnGap + btnWidth2 * 2 + btnGap * 2, navY + 30, btnWidth2, 20).build();
        this.addRenderableWidget(delMap);

        refreshButtons();
    }

    private void changePage(int delta) {
        int newPage = page + delta;
        if (newPage < 0 || newPage * PAGE_SIZE >= fIds.size()) return;
        page = newPage;
        refreshButtons();
    }

    private void refreshButtons() {
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, fIds.size());
        for (int i = 0; i < PAGE_SIZE; i++) {
            Button btn = entryButtons.get(i);
            int idx = start + i;
            if (idx < end) {
                btn.visible = true;
                btn.active = true;
                btn.setMessage(Component.literal(buildLabel(idx)));
                buttonIndexMap[i] = idx;
            } else {
                btn.visible = false;
                btn.active = false;
                buttonIndexMap[i] = -1;
            }
        }
        if (prevButton != null) prevButton.active = page > 0;
        if (nextButton != null) nextButton.active = fIds.size() > (page + 1) * PAGE_SIZE;
    }

    private String buildLabel(int idx) {
        String name = fNames.get(idx);
        int totalSlots = fTotalSlots.get(idx);
        int count = fCount.get(idx);
        return name + "  (" + totalSlots + ")  x" + count;
    }

    private void onChoose(int idx) {
        if (idx < 0 || idx >= fIds.size()) return;
        long providerId = fIds.get(idx);
        ModNetwork.CHANNEL.sendToServer(new UploadPatternPacket(providerId));
        this.onClose();
    }

    private void addMapping() {
        String key = query == null ? "" : query.trim();
        String value = cnInput == null ? "" : cnInput.getValue().trim();
        if (key.isEmpty()) {
            sendMessage("请输入搜索关键字");
            return;
        }
        if (value.isEmpty()) {
            sendMessage("请输入映射名称");
            return;
        }
        if (RecipeTypeNameConfig.addOrUpdateAliasMapping(key, value)) {
            sendMessage("已添加映射: " + key + " -> " + value);
            this.query = value;
            if (this.searchBox != null) {
                this.searchBox.setValue(value);
            }
            try {
                RecipeTypeNameConfig.loadRecipeTypeNames();
            } catch (Exception ignored) {}
            applyFilter();
            page = 0;
            refreshButtons();
        } else {
            sendMessage("添加映射失败");
        }
    }

    private void reloadMapping() {
        try {
            RecipeTypeNameConfig.loadRecipeTypeNames();
            sendMessage("映射已重载");
        } catch (Exception e) {
            sendMessage("重载失败: " + e.getMessage());
        }
    }

    private void deleteMapping() {
        String value = cnInput == null ? "" : cnInput.getValue().trim();
        if (value.isEmpty()) {
            sendMessage("请输入要删除的映射名称");
            return;
        }
        int removed = RecipeTypeNameConfig.removeMappingsByCnValue(value);
        if (removed > 0) {
            sendMessage("已删除 " + removed + " 个映射");
            try {
                RecipeTypeNameConfig.loadRecipeTypeNames();
            } catch (Exception ignored) {}
            applyFilter();
            page = 0;
            refreshButtons();
        } else {
            sendMessage("未找到映射: " + value);
        }
    }

    private void sendMessage(String msg) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(Component.literal(msg), true);
        }
    }

    @Override
    public void onClose() {
        if (parent != null) {
            Minecraft.getInstance().setScreen(parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 右键点击搜索框清空
        if (button == 1 && this.searchBox != null) {
            int x = this.searchBox.getX();
            int y = this.searchBox.getY();
            int w = this.searchBox.getWidth();
            int h = this.searchBox.getHeight();
            if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
                if (!this.searchBox.getValue().isEmpty()) {
                    this.searchBox.setValue("");
                }
                this.query = "";
                this.page = 0;
                applyFilter();
                refreshButtons();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        super.tick();
        if (searchBox != null) {
            searchBox.tick();
        }
        if (cnInput != null) {
            cnInput.tick();
        }

        String currentLang = Minecraft.getInstance().options.languageCode;
        if (!currentLang.equals(lastLanguage)) {
            lastLanguage = currentLang;
            componentCache.clear();
            refreshButtons();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 100, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class Group {
        long bestId;
        int bestSlots;
        int totalSlots;
        int count;

        Group(long id, int slots) {
            this.bestId = id;
            this.bestSlots = slots;
            this.totalSlots = Math.max(0, slots);
            this.count = 1;
        }

        void merge(long id, int slots) {
            count++;
            totalSlots += Math.max(0, slots);
            if (slots > bestSlots) {
                bestSlots = slots;
                bestId = id;
            }
        }
    }
}
