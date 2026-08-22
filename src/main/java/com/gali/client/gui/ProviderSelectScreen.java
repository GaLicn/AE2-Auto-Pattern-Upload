package com.gali.client.gui;

import com.gali.network.ModNetwork;
import com.gali.network.UploadPatternPacket;
import com.gali.util.RecipeTypeNameConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** EAEP 1.20.1 风格的供应器选择界面。 */
public class ProviderSelectScreen extends Screen {
    private static final int MIN_PAGE_SIZE = 2;
    private static final String UI_CONFIG = "ae2_auto_pattern_upload/provider_screen.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Set<String> PINNED_PROVIDERS = new HashSet<>();
    private static boolean autoUploadUniqueMatchEnabled = true;
    private static final Pattern NATURAL_PATTERN = Pattern.compile("(\\D*)(\\d*)");

    static { loadUiConfig(); }

    private final Screen parent;
    private final List<Long> ids;
    private final List<String> names;
    private final List<Integer> emptySlots;
    private final List<Group> groups = new ArrayList<>();
    private final List<Group> filteredGroups = new ArrayList<>();
    private final List<Button> entryButtons = new ArrayList<>();
    private EditBox searchBox;
    private EditBox mappingValueInput;
    private Button autoUploadButton;
    private String query = "";
    private int page;
    private int pageSize = 6;
    private boolean autoUploadRequested;
    private boolean autoUploadAttempted;
    private boolean filterUsedFallback;

    public ProviderSelectScreen(Screen parent, List<Long> ids, List<String> names, List<Integer> emptySlots) {
        super(Component.translatable("gali.gui.provider_select"));
        this.parent = parent;
        this.ids = ids;
        this.names = names;
        this.emptySlots = emptySlots;
        String preset = RecipeTypeNameConfig.consumeLastProviderSearchKey();
        if (preset != null && !preset.isBlank()) {
            query = RecipeTypeNameConfig.resolveProviderSearchKey(preset);
            autoUploadRequested = true;
        }
        buildGroups();
        applyFilter();
    }

    private void buildGroups() {
        Map<String, Group> grouped = new LinkedHashMap<>();
        int size = Math.min(ids.size(), Math.min(names.size(), emptySlots.size()));
        for (int i = 0; i < size; i++) {
            String name = deserializeName(names.get(i));
            Group group = grouped.computeIfAbsent(name, Group::new);
            int slots = Math.max(0, emptySlots.get(i));
            group.totalSlots += slots;
            group.count++;
            if (slots > group.representativeSlots) {
                group.representativeSlots = slots;
                group.representativeId = ids.get(i);
            }
        }
        groups.addAll(grouped.values());
    }

    private void applyFilter() {
        filteredGroups.clear();
        filterUsedFallback = false;
        String searchKey = RecipeTypeNameConfig.resolveProviderSearchKey(query);
        String lower = searchKey.toLowerCase(Locale.ROOT);
        for (Group group : groups) {
            if (searchKey.isEmpty() || group.name.toLowerCase(Locale.ROOT).contains(lower)) filteredGroups.add(group);
        }
        // EAEP 在无匹配时保留列表，避免用户被空页面误导；自动上传不会使用这个回退结果。
        if (!searchKey.isEmpty() && filteredGroups.isEmpty()) {
            filterUsedFallback = true;
            filteredGroups.addAll(groups);
        }
        filteredGroups.sort((first, second) -> {
            boolean firstPinned = PINNED_PROVIDERS.contains(first.name);
            boolean secondPinned = PINNED_PROVIDERS.contains(second.name);
            if (firstPinned != secondPinned) return firstPinned ? -1 : 1;
            return compareNatural(first.name, second.name);
        });
        int maxPage = Math.max(0, (filteredGroups.size() - 1) / Math.max(1, pageSize));
        page = Math.min(page, maxPage);
    }

    @Override
    protected void init() {
        String mappingValue = mappingValueInput == null ? "" : mappingValueInput.getValue();
        clearWidgets();
        entryButtons.clear();

        int centerX = width / 2;
        int buttonHeight = 20;
        int gap = 5;
        int reservedHeight = 30 + 30 + 30 + 20 + 40;
        pageSize = Math.max(MIN_PAGE_SIZE, (height - reservedHeight) / (buttonHeight + gap));
        page = Math.min(page, Math.max(0, (filteredGroups.size() - 1) / pageSize));
        int contentHeight = 30 + pageSize * (buttonHeight + gap) + 30 + 30 + 20;
        int startY = (height - contentHeight) / 2 + 30;

        searchBox = new EditBox(font, centerX - 120, startY - 25, 240, 18,
                Component.translatable("gali.gui.search"));
        searchBox.setMaxLength(256);
        searchBox.setValue(query);
        searchBox.setResponder(value -> {
            if (value.equals(query)) return;
            query = value;
            page = 0;
            applyFilter();
            refreshButtons();
        });
        addRenderableWidget(searchBox);

        int start = page * pageSize;
        int end = Math.min(start + pageSize, filteredGroups.size());
        for (int index = start; index < end; index++) {
            Group group = filteredGroups.get(index);
            Button button = Button.builder(Component.literal(group.label()), ignored -> choose(group))
                    .bounds(centerX - 120, startY + (index - start) * (buttonHeight + gap), 240, buttonHeight).build();
            entryButtons.add(button);
            addRenderableWidget(button);
        }

        int navigationY = startY + pageSize * (buttonHeight + gap) + 10;
        Button previous = Button.builder(Component.literal("<"), ignored -> changePage(-1))
                .bounds(centerX - 60, navigationY, 20, 20).build();
        Button next = Button.builder(Component.literal(">"), ignored -> changePage(1))
                .bounds(centerX + 40, navigationY, 20, 20).build();
        previous.active = page > 0;
        next.active = (page + 1) * pageSize < filteredGroups.size();
        addRenderableWidget(previous);
        addRenderableWidget(next);

        int controlsWidth = Math.min(500, Math.max(240, width - 20));
        int controlsX = centerX - controlsWidth / 2;
        int toggleWidth = Math.min(240, controlsWidth);
        int toggleY = navigationY + 30;
        autoUploadButton = Button.builder(autoUploadLabel(), ignored -> toggleAutoUpload())
                .bounds(centerX - toggleWidth / 2, toggleY, toggleWidth, 20).build();
        autoUploadButton.setTooltip(Tooltip.create(Component.translatable("gali.gui.auto_upload_unique.tooltip")));
        addRenderableWidget(autoUploadButton);

        int quickMappingY = navigationY + 55;
        int quickInputWidth = 150;
        mappingValueInput = new EditBox(font, controlsX, quickMappingY, quickInputWidth, 20,
                Component.translatable("gali.gui.mapping_name"));
        mappingValueInput.setMaxLength(256);
        mappingValueInput.setValue(mappingValue);
        addRenderableWidget(mappingValueInput);
        addRenderableWidget(Button.builder(Component.translatable("gali.gui.add"), ignored -> addMapping())
                .bounds(controlsX + quickInputWidth + 5, quickMappingY, 85, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gali.gui.mapping.management"), ignored ->
                        Minecraft.getInstance().setScreen(new RecipeTypeMappingScreen(this)))
                .bounds(controlsX + quickInputWidth + 95, quickMappingY, 155, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), ignored -> onClose())
                .bounds(controlsX + quickInputWidth + 255, quickMappingY, 80, 20).build());

        tryAutoUploadIfUniqueMatch();
    }

    private void choose(Group group) {
        ModNetwork.CHANNEL.sendToServer(new UploadPatternPacket(group.representativeId, false, group.name));
        onClose();
    }

    private void chooseAutomatically(Group group) {
        ModNetwork.CHANNEL.sendToServer(new UploadPatternPacket(group.representativeId, true, group.name));
        onClose();
    }

    private void changePage(int delta) {
        int next = page + delta;
        if (next >= 0 && next * pageSize < filteredGroups.size()) { page = next; init(); }
    }

    private void refreshButtons() {
        // 输入框改变时直接重建控件，确保旧页面按钮不会残留。
        init();
    }

    private void addMapping() {
        String key = query.trim();
        String value = mappingValueInput.getValue().trim();
        if (key.isEmpty() || value.isEmpty()) return;
        if (RecipeTypeNameConfig.addOrUpdateRecipeTypeMapping(key, value)) {
            query = value;
            page = 0;
            applyFilter();
            init();
        }
    }

    private Component autoUploadLabel() {
        return Component.translatable("gali.gui.auto_upload_unique",
                Component.translatable(autoUploadUniqueMatchEnabled ? "gali.gui.on" : "gali.gui.off"));
    }

    private void toggleAutoUpload() {
        autoUploadUniqueMatchEnabled = !autoUploadUniqueMatchEnabled;
        saveUiConfig();
        if (autoUploadButton != null) autoUploadButton.setMessage(autoUploadLabel());
    }

    private void tryAutoUploadIfUniqueMatch() {
        if (!autoUploadUniqueMatchEnabled || !autoUploadRequested || autoUploadAttempted) return;
        autoUploadAttempted = true;
        if (!query.isBlank() && !filterUsedFallback && filteredGroups.size() == 1) {
            chooseAutomatically(filteredGroups.get(0));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && searchBox != null && mouseX >= searchBox.getX() && mouseX <= searchBox.getX() + searchBox.getWidth()
                && mouseY >= searchBox.getY() && mouseY <= searchBox.getY() + searchBox.getHeight()) {
            searchBox.setValue("");
            query = "";
            page = 0;
            applyFilter();
            init();
            return true;
        }
        if (button == 1) {
            for (int index = 0; index < entryButtons.size(); index++) {
                Button entry = entryButtons.get(index);
                if (entry.visible && mouseX >= entry.getX() && mouseX <= entry.getX() + entry.getWidth()
                        && mouseY >= entry.getY() && mouseY <= entry.getY() + entry.getHeight()) {
                    togglePin(page * pageSize + index);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void togglePin(int index) {
        if (index < 0 || index >= filteredGroups.size()) return;
        String name = filteredGroups.get(index).name;
        if (!PINNED_PROVIDERS.add(name)) PINNED_PROVIDERS.remove(name);
        saveUiConfig();
        applyFilter();
        init();
    }

    private static String deserializeName(String name) {
        if (name == null) return "";
        try {
            if (name.startsWith("{") || name.startsWith("\"")) {
                Component component = Component.Serializer.fromJson(name);
                if (component != null) return component.getString();
            }
        } catch (Exception ignored) {}
        return name;
    }

    private static int compareNatural(String first, String second) {
        Matcher firstMatcher = NATURAL_PATTERN.matcher(first);
        Matcher secondMatcher = NATURAL_PATTERN.matcher(second);
        while (firstMatcher.find() && secondMatcher.find()) {
            int textCompare = firstMatcher.group(1).compareTo(secondMatcher.group(1));
            if (textCompare != 0) return textCompare;
            String firstNumber = firstMatcher.group(2);
            String secondNumber = secondMatcher.group(2);
            if (!firstNumber.isEmpty() || !secondNumber.isEmpty()) {
                int firstValue = firstNumber.isEmpty() ? 0 : Integer.parseInt(firstNumber);
                int secondValue = secondNumber.isEmpty() ? 0 : Integer.parseInt(secondNumber);
                if (firstValue != secondValue) return Integer.compare(firstValue, secondValue);
            }
        }
        return first.length() - second.length();
    }

    private static void loadUiConfig() {
        try {
            Path path = FMLPaths.CONFIGDIR.get().resolve(UI_CONFIG);
            if (!Files.exists(path)) return;
            JsonObject object = GSON.fromJson(Files.readString(path), JsonObject.class);
            if (object == null) return;
            if (object.get("pinned") instanceof JsonArray pinned) {
                PINNED_PROVIDERS.clear();
                pinned.forEach(value -> { if (value.isJsonPrimitive()) PINNED_PROVIDERS.add(value.getAsString()); });
            }
            if (object.has("auto_upload_unique_match")) autoUploadUniqueMatchEnabled =
                    object.get("auto_upload_unique_match").getAsBoolean();
        } catch (IOException | RuntimeException ignored) {}
    }

    private static void saveUiConfig() {
        try {
            Path path = FMLPaths.CONFIGDIR.get().resolve(UI_CONFIG);
            Files.createDirectories(path.getParent());
            JsonObject object = new JsonObject();
            JsonArray pinned = new JsonArray();
            PINNED_PROVIDERS.forEach(pinned::add);
            object.add("pinned", pinned);
            object.addProperty("auto_upload_unique_match", autoUploadUniqueMatchEnabled);
            Files.writeString(path, GSON.toJson(object));
        } catch (IOException ignored) {}
    }

    @Override public void onClose() { Minecraft.getInstance().setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (filteredGroups.isEmpty()) graphics.drawCenteredString(font,
                Component.translatable("gali.gui.no_providers"), width / 2, height / 2, 0xFFFFFFFF);
    }

    private static final class Group {
        private final String name;
        private long representativeId;
        private int representativeSlots;
        private int totalSlots;
        private int count;

        private Group(String name) { this.name = name; }

        private String label() {
            return (PINNED_PROVIDERS.contains(name) ? "★ " : "") + name + "  (" + totalSlots + ")  x" + count;
        }
    }
}
