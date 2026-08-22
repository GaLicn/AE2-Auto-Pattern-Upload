package com.gali.ae2_auto_pattern_upload.client;

import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.StyleManager;
import appeng.client.gui.widgets.AE2Button;
import com.gali.ae2_auto_pattern_upload.network.PatternUploadUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 管理配方类型（或分类标题）到供应器搜索词的映射。 */
public class RecipeTypeMappingScreen extends Screen {
    private static final int ROW_HEIGHT = 22;
    private static final int FIELD_HEIGHT = 20;

    private final Screen parent;
    private final ScreenStyle aeStyle;
    private final List<PatternUploadUtil.RecipeTypeMapping> mappings = new ArrayList<>();
    private final List<PatternUploadUtil.RecipeTypeMapping> filteredMappings = new ArrayList<>();
    private ResizableAETextField filterInput;
    private ResizableAETextField keyInput;
    private ResizableAETextField valueInput;
    private String selectedKey;
    private Component status = Component.empty();
    private int statusColor = 0xFFAAAAAA;
    private int page;
    private int pageSize = 6;
    private boolean refreshPending;

    public RecipeTypeMappingScreen(Screen parent) {
        super(Component.translatable("ae2_auto_pattern_upload.screen.mapping.title"));
        this.parent = parent;
        this.aeStyle = StyleManager.loadStyleDoc("/screens/common/common.json");
        reloadMappings(false);
    }

    @Override
    protected void init() {
        boolean filterFocused = filterInput != null && filterInput.isFocused();
        boolean keyFocused = keyInput != null && keyInput.isFocused();
        boolean valueFocused = valueInput != null && valueInput.isFocused();
        clearWidgets();

        int panelWidth = Math.min(600, width - 20);
        int panelHeight = Math.min(390, height - 20);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;
        int innerX = panelX + 12;
        int innerWidth = panelWidth - 24;
        pageSize = Math.max(1, (panelHeight - 166) / ROW_HEIGHT);
        page = Math.min(page, Math.max(0, (filteredMappings.size() - 1) / pageSize));

        filterInput = prepareInput(filterInput, panelX + (panelWidth - 128) / 2,
                panelY + 28, 128, FIELD_HEIGHT,
                "ae2_auto_pattern_upload.screen.mapping.filter");
        filterInput.setResponder(value -> {
            page = 0;
            applyFilter();
            refreshPending = true;
        });
        addRenderableWidget(filterInput);
        if (filterFocused) {
            setFocused(filterInput);
        }

        int gap = 5;
        int saveWidth = 76;
        int inputWidth = Math.min(128, Math.max(70, (innerWidth - saveWidth - gap * 2) / 2));
        int formWidth = inputWidth * 2 + saveWidth + gap * 2;
        int formX = panelX + (panelWidth - formWidth) / 2;
        int inputY = panelY + 52;
        keyInput = prepareInput(keyInput, formX, inputY, inputWidth, FIELD_HEIGHT,
                "ae2_auto_pattern_upload.screen.mapping.key");
        valueInput = prepareInput(valueInput, formX + inputWidth + gap, inputY, inputWidth, FIELD_HEIGHT,
                "ae2_auto_pattern_upload.screen.mapping.value");
        addRenderableWidget(keyInput);
        addRenderableWidget(valueInput);
        if (keyFocused) {
            setFocused(keyInput);
        } else if (valueFocused) {
            setFocused(valueInput);
        }
        addRenderableWidget(new AE2Button(formX + inputWidth * 2 + gap * 2, inputY, saveWidth, FIELD_HEIGHT,
                Component.translatable("ae2_auto_pattern_upload.screen.mapping.save"), ignored -> saveMapping()));

        int rowsY = panelY + 82;
        int start = page * pageSize;
        int end = Math.min(start + pageSize, filteredMappings.size());
        for (int index = start; index < end; index++) {
            PatternUploadUtil.RecipeTypeMapping mapping = filteredMappings.get(index);
            String prefix = mapping.key().equals(selectedKey) ? "▶ " : "";
            addRenderableWidget(new AE2Button(innerX, rowsY + (index - start) * ROW_HEIGHT, innerWidth,
                    FIELD_HEIGHT, Component.literal(prefix + mapping.key() + "  →  " + mapping.value()),
                    ignored -> selectMapping(mapping)));
        }

        int navigationY = panelY + panelHeight - 54;
        Button previous = new AE2Button(panelX + panelWidth / 2 - 102, navigationY, 24, FIELD_HEIGHT,
                Component.literal("<"), ignored -> changePage(-1));
        Button next = new AE2Button(panelX + panelWidth / 2 + 78, navigationY, 24, FIELD_HEIGHT,
                Component.literal(">"), ignored -> changePage(1));
        previous.active = page > 0;
        next.active = (page + 1) * pageSize < filteredMappings.size();
        addRenderableWidget(previous);
        addRenderableWidget(next);

        int actionY = panelY + panelHeight - 28;
        int actionGap = 5;
        int actionWidth = (innerWidth - actionGap * 3) / 4;
        addRenderableWidget(new AE2Button(innerX, actionY, actionWidth, FIELD_HEIGHT,
                Component.translatable("ae2_auto_pattern_upload.screen.mapping.new"), ignored -> clearSelection()));
        Button delete = new AE2Button(innerX + actionWidth + actionGap, actionY, actionWidth, FIELD_HEIGHT,
                Component.translatable("ae2_auto_pattern_upload.screen.mapping.delete"), ignored -> deleteSelectedMapping());
        delete.active = selectedKey != null;
        addRenderableWidget(delete);
        addRenderableWidget(new AE2Button(innerX + (actionWidth + actionGap) * 2, actionY, actionWidth, FIELD_HEIGHT,
                Component.translatable("ae2_auto_pattern_upload.screen.mapping.reload"), ignored -> reloadMappings(true)));
        addRenderableWidget(new AE2Button(innerX + (actionWidth + actionGap) * 3, actionY, actionWidth, FIELD_HEIGHT,
                Component.translatable("gui.back"), ignored -> onClose()));
    }

    private ResizableAETextField prepareInput(ResizableAETextField previous, int x, int y, int width, int height,
                                              String placeholderKey) {
        String value = previous == null ? "" : previous.getValue();
        ResizableAETextField input = new ResizableAETextField(aeStyle, font, x, y, width, height);
        input.setMaxLength(256);
        input.setValue(value);
        input.setPlaceholder(Component.translatable(placeholderKey));
        return input;
    }

    private void reloadMappings(boolean showStatus) {
        PatternUploadUtil.loadRecipeTypeMappings();
        mappings.clear();
        mappings.addAll(PatternUploadUtil.getRecipeTypeMappings());
        applyFilter();
        refreshPending = true;
        if (showStatus) {
            setStatus("ae2_auto_pattern_upload.screen.mapping.reloaded", 0xFF55FF55);
        }
    }

    private void applyFilter() {
        String query = filterInput == null ? "" : filterInput.getValue().trim().toLowerCase(Locale.ROOT);
        filteredMappings.clear();
        for (PatternUploadUtil.RecipeTypeMapping mapping : mappings) {
            if (query.isEmpty() || mapping.key().toLowerCase(Locale.ROOT).contains(query)
                    || mapping.value().toLowerCase(Locale.ROOT).contains(query)) {
                filteredMappings.add(mapping);
            }
        }
    }

    private void selectMapping(PatternUploadUtil.RecipeTypeMapping mapping) {
        selectedKey = mapping.key();
        keyInput.setValue(mapping.key());
        valueInput.setValue(mapping.value());
        refreshPending = true;
    }

    private void clearSelection() {
        selectedKey = null;
        keyInput.setValue("");
        valueInput.setValue("");
        setFocused(keyInput);
        refreshPending = true;
    }

    private void saveMapping() {
        String key = keyInput.getValue().trim();
        String value = valueInput.getValue().trim();
        if (key.isEmpty() || value.isEmpty()) {
            setStatus("ae2_auto_pattern_upload.screen.mapping.required", 0xFFFF5555);
            return;
        }
        String previousKey = selectedKey;
        if (!PatternUploadUtil.addOrUpdateRecipeTypeMapping(key, value)) {
            setStatus("ae2_auto_pattern_upload.screen.mapping.save_failed", 0xFFFF5555);
            return;
        }
        if (previousKey != null && !previousKey.equalsIgnoreCase(key)) {
            PatternUploadUtil.removeRecipeTypeMapping(previousKey);
        }
        selectedKey = key;
        reloadMappings(false);
        setStatus("ae2_auto_pattern_upload.screen.mapping.saved", 0xFF55FF55);
    }

    private void deleteSelectedMapping() {
        if (selectedKey == null) {
            return;
        }
        if (!PatternUploadUtil.removeRecipeTypeMapping(selectedKey)) {
            setStatus("ae2_auto_pattern_upload.screen.mapping.delete_failed", 0xFFFF5555);
            return;
        }
        selectedKey = null;
        keyInput.setValue("");
        valueInput.setValue("");
        reloadMappings(false);
        setStatus("ae2_auto_pattern_upload.screen.mapping.deleted", 0xFF55FF55);
    }

    private void changePage(int delta) {
        int nextPage = page + delta;
        if (nextPage < 0 || nextPage * pageSize >= filteredMappings.size()) {
            return;
        }
        page = nextPage;
        refreshPending = true;
    }

    private void setStatus(String key, int color) {
        status = Component.translatable(key);
        statusColor = color;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (filterInput != null && filterInput.isFocused() && filterInput.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyInput != null && keyInput.isFocused() && keyInput.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (valueInput != null && valueInput.isFocused() && valueInput.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (filterInput != null && filterInput.charTyped(codePoint, modifiers)) {
            return true;
        }
        if (keyInput != null && keyInput.charTyped(codePoint, modifiers)) {
            return true;
        }
        if (valueInput != null && valueInput.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = Math.min(600, width - 20);
        int panelHeight = Math.min(390, height - 20);
        int panelY = (height - panelHeight) / 2;
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, panelY + 9, 0xFFFFFFFF);
        int totalPages = Math.max(1, (filteredMappings.size() + pageSize - 1) / pageSize);
        graphics.drawCenteredString(font, Component.translatable("ae2_auto_pattern_upload.screen.mapping.page",
                filteredMappings.isEmpty() ? 0 : page + 1, totalPages, filteredMappings.size()),
                width / 2, panelY + panelHeight - 48, 0xFFB0B0B0);
        if (!status.getString().isEmpty()) {
            graphics.drawString(font, status, (width - panelWidth) / 2 + 12,
                    panelY + panelHeight - 66, statusColor, false);
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
}
