package com.gali.client.gui;

import com.gali.util.RecipeTypeNameConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** EAEP 1.20.1 风格的配方类型映射管理界面。 */
public class RecipeTypeMappingScreen extends Screen {
    private static final int ROW_HEIGHT = 22;
    private final Screen parent;
    private final List<RecipeTypeNameConfig.RecipeTypeMapping> mappings = new ArrayList<>();
    private final List<RecipeTypeNameConfig.RecipeTypeMapping> filteredMappings = new ArrayList<>();
    private EditBox filterInput;
    private EditBox keyInput;
    private EditBox valueInput;
    private String selectedKey;
    private Component status = Component.empty();
    private int statusColor = 0xFFAAAAAA;
    private int page;
    private int pageSize = 6;
    private boolean refreshPending;

    public RecipeTypeMappingScreen(Screen parent) {
        super(Component.translatable("gali.gui.mapping.title"));
        this.parent = parent;
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

        filterInput = input(filterInput, panelX + (panelWidth - 128) / 2, panelY + 28, 128,
                Component.translatable("gali.gui.mapping.filter"));
        filterInput.setResponder(value -> { page = 0; applyFilter(); refreshPending = true; });
        addRenderableWidget(filterInput);
        if (filterFocused) setFocused(filterInput);

        int gap = 5;
        int saveWidth = 76;
        int inputWidth = Math.min(128, Math.max(70, (innerWidth - saveWidth - gap * 2) / 2));
        int formWidth = inputWidth * 2 + saveWidth + gap * 2;
        int formX = panelX + (panelWidth - formWidth) / 2;
        keyInput = input(keyInput, formX, panelY + 52, inputWidth,
                Component.translatable("gali.gui.mapping.key"));
        valueInput = input(valueInput, formX + inputWidth + gap, panelY + 52, inputWidth,
                Component.translatable("gali.gui.mapping.value"));
        addRenderableWidget(keyInput);
        addRenderableWidget(valueInput);
        if (keyFocused) setFocused(keyInput); else if (valueFocused) setFocused(valueInput);
        addRenderableWidget(Button.builder(Component.translatable("gali.gui.mapping.save"), button -> saveMapping())
                .bounds(formX + inputWidth * 2 + gap * 2, panelY + 52, saveWidth, 20).build());

        int rowsY = panelY + 82;
        int start = page * pageSize;
        int end = Math.min(start + pageSize, filteredMappings.size());
        for (int index = start; index < end; index++) {
            RecipeTypeNameConfig.RecipeTypeMapping mapping = filteredMappings.get(index);
            String prefix = mapping.key().equals(selectedKey) ? "▶ " : "";
            addRenderableWidget(Button.builder(Component.literal(prefix + mapping.key() + "  →  " + mapping.value()),
                            button -> selectMapping(mapping))
                    .bounds(innerX, rowsY + (index - start) * ROW_HEIGHT, innerWidth, 20).build());
        }

        int navigationY = panelY + panelHeight - 54;
        Button previous = Button.builder(Component.literal("<"), button -> changePage(-1))
                .bounds(panelX + panelWidth / 2 - 102, navigationY, 24, 20).build();
        Button next = Button.builder(Component.literal(">"), button -> changePage(1))
                .bounds(panelX + panelWidth / 2 + 78, navigationY, 24, 20).build();
        previous.active = page > 0;
        next.active = (page + 1) * pageSize < filteredMappings.size();
        addRenderableWidget(previous);
        addRenderableWidget(next);

        int actionY = panelY + panelHeight - 28;
        int actionWidth = (innerWidth - 15) / 4;
        addRenderableWidget(Button.builder(Component.translatable("gali.gui.mapping.new"), button -> clearSelection())
                .bounds(innerX, actionY, actionWidth, 20).build());
        Button delete = Button.builder(Component.translatable("gali.gui.mapping.delete"), button -> deleteSelectedMapping())
                .bounds(innerX + actionWidth + 5, actionY, actionWidth, 20).build();
        delete.active = selectedKey != null;
        addRenderableWidget(delete);
        addRenderableWidget(Button.builder(Component.translatable("gali.gui.mapping.reload"), button -> reloadMappings(true))
                .bounds(innerX + (actionWidth + 5) * 2, actionY, actionWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(innerX + (actionWidth + 5) * 3, actionY, actionWidth, 20).build());
    }

    private EditBox input(EditBox previous, int x, int y, int width, Component hint) {
        EditBox input = new EditBox(font, x, y, width, 20, hint);
        input.setMaxLength(256);
        if (previous != null) input.setValue(previous.getValue());
        return input;
    }

    private void reloadMappings(boolean showStatus) {
        try { RecipeTypeNameConfig.loadRecipeTypeNames(); } catch (Exception ignored) {}
        mappings.clear();
        mappings.addAll(RecipeTypeNameConfig.getRecipeTypeMappings());
        applyFilter();
        refreshPending = true;
        if (showStatus) setStatus("gali.gui.mapping.reloaded", 0xFF55FF55);
    }

    private void applyFilter() {
        String query = filterInput == null ? "" : filterInput.getValue().trim().toLowerCase(Locale.ROOT);
        filteredMappings.clear();
        for (RecipeTypeNameConfig.RecipeTypeMapping mapping : mappings) {
            if (query.isEmpty() || mapping.key().toLowerCase(Locale.ROOT).contains(query)
                    || mapping.value().toLowerCase(Locale.ROOT).contains(query)) filteredMappings.add(mapping);
        }
    }

    private void selectMapping(RecipeTypeNameConfig.RecipeTypeMapping mapping) {
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
        if (key.isEmpty() || value.isEmpty()) { setStatus("gali.gui.mapping.required", 0xFFFF5555); return; }
        String previous = selectedKey;
        if (!RecipeTypeNameConfig.addOrUpdateRecipeTypeMapping(key, value)) {
            setStatus("gali.gui.mapping.save_failed", 0xFFFF5555);
            return;
        }
        if (previous != null && !previous.equalsIgnoreCase(key)) RecipeTypeNameConfig.removeRecipeTypeMapping(previous);
        selectedKey = key;
        reloadMappings(false);
        setStatus("gali.gui.mapping.saved", 0xFF55FF55);
    }

    private void deleteSelectedMapping() {
        if (selectedKey == null) return;
        if (!RecipeTypeNameConfig.removeRecipeTypeMapping(selectedKey)) {
            setStatus("gali.gui.mapping.delete_failed", 0xFFFF5555);
            return;
        }
        clearSelection();
        reloadMappings(false);
        setStatus("gali.gui.mapping.deleted", 0xFF55FF55);
    }

    private void changePage(int delta) {
        int next = page + delta;
        if (next >= 0 && next * pageSize < filteredMappings.size()) { page = next; refreshPending = true; }
    }

    private void setStatus(String key, int color) { status = Component.translatable(key); statusColor = color; }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (filterInput != null && filterInput.isFocused() && filterInput.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (keyInput != null && keyInput.isFocused() && keyInput.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (valueInput != null && valueInput.isFocused() && valueInput.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean charTyped(char codePoint, int modifiers) {
        if (filterInput != null && filterInput.charTyped(codePoint, modifiers)) return true;
        if (keyInput != null && keyInput.charTyped(codePoint, modifiers)) return true;
        if (valueInput != null && valueInput.charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override public void tick() {
        super.tick();
        if (refreshPending) { refreshPending = false; init(); }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = Math.min(600, width - 20);
        int panelHeight = Math.min(390, height - 20);
        int panelY = (height - panelHeight) / 2;
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, panelY + 9, 0xFFFFFFFF);
        int totalPages = Math.max(1, (filteredMappings.size() + pageSize - 1) / pageSize);
        graphics.drawCenteredString(font, Component.translatable("gali.gui.mapping.page",
                filteredMappings.isEmpty() ? 0 : page + 1, totalPages, filteredMappings.size()),
                width / 2, panelY + panelHeight - 48, 0xFFB0B0B0);
        if (!status.getString().isEmpty()) graphics.drawString(font, status,
                (width - panelWidth) / 2 + 12, panelY + panelHeight - 66, statusColor, false);
    }

    @Override public void onClose() { Minecraft.getInstance().setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
