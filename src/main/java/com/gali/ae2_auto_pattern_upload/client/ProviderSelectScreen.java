package com.gali.ae2_auto_pattern_upload.client;

import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.StyleManager;
import appeng.client.gui.widgets.AE2Button;
import com.gali.ae2_auto_pattern_upload.network.PatternUploadUtil;
import com.gali.ae2_auto_pattern_upload.network.UploadEncodedPatternC2SPacket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** EAEP 风格的供应器上传界面，包含置顶、快捷映射和映射管理入口。 */
public class ProviderSelectScreen extends Screen {
    private static final int MIN_PAGE_SIZE = 2;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ENTRY_GAP = 5;
    private static final int ENTRY_WIDTH = 240;
    private static final int AE_TEXT_FIELD_WIDTH = 128;
    private static final int AE_SEARCH_FIELD_HEIGHT = 20;
    private static final String UI_CONFIG = "ae2_auto_pattern_upload/provider_screen.json";
    private static final String PINNED_KEY = "pinned";
    private static final String AUTO_UPLOAD_KEY = "auto_upload_unique_match";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Set<String> pinnedProviders = new HashSet<>();
    private static final Pattern NATURAL_PATTERN = Pattern.compile("(\\D*)(\\d*)");
    private static boolean autoUploadUniqueMatchEnabled = true;

    static {
        loadUiConfig();
    }

    private final Screen parent;
    private final List<Long> ids;
    private final List<Component> names;
    private final List<Integer> emptySlots;
    private final List<Group> groups = new ArrayList<>();
    private final List<Group> filteredGroups = new ArrayList<>();
    private final List<Button> entryButtons = new ArrayList<>();
    private final ScreenStyle aeStyle;
    private ResizableAETextField searchBox;
    private ResizableAETextField mappingValueInput;
    private Button autoUploadToggleButton;
    private String query;
    private int page;
    private int pageSize = 6;
    private boolean refreshPending;
    private boolean autoUploadRequestedFromPresetSearch;
    private boolean autoUploadAttempted;
    private int lastMatchCount;

    public ProviderSelectScreen(Screen parent, List<Long> ids, List<Component> names, List<Integer> emptySlots) {
        super(Component.translatable("ae2_auto_pattern_upload.screen.choose_provider.title"));
        this.parent = parent;
        this.ids = ids;
        this.names = names;
        this.emptySlots = emptySlots;
        this.aeStyle = StyleManager.loadStyleDoc("/screens/common/common.json");
        String preset = PatternUploadUtil.consumeLastProviderSearchKey();
        this.query = preset == null ? "" : preset;
        this.autoUploadRequestedFromPresetSearch = preset != null && !preset.isBlank();
        buildGroups();
        applyFilter();
    }

    private void buildGroups() {
        Map<String, Group> grouped = new LinkedHashMap<>();
        int size = Math.min(ids.size(), Math.min(names.size(), emptySlots.size()));
        for (int i = 0; i < size; i++) {
            String name = names.get(i).getString();
            Group group = grouped.computeIfAbsent(name, ignored -> new Group(name));
            int slots = Math.max(0, emptySlots.get(i));
            group.totalSlots += slots;
            group.count++;
            if (slots > group.representativeSlots) {
                group.representativeId = ids.get(i);
                group.representativeSlots = slots;
            }
        }
        groups.addAll(grouped.values());
    }

    private void applyFilter() {
        filteredGroups.clear();
        lastMatchCount = 0;
        String searchKey = PatternUploadUtil.resolveProviderSearchKey(query);
        for (Group group : groups) {
            if (searchKey.isEmpty() || group.name.toLowerCase(Locale.ROOT).contains(searchKey.toLowerCase(Locale.ROOT))) {
                filteredGroups.add(group);
                lastMatchCount++;
            }
        }
        filteredGroups.sort((first, second) -> {
            boolean firstPinned = pinnedProviders.contains(first.name);
            boolean secondPinned = pinnedProviders.contains(second.name);
            if (firstPinned != secondPinned) {
                return firstPinned ? -1 : 1;
            }
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
        int entryUnitHeight = BUTTON_HEIGHT + ENTRY_GAP;
        int reservedHeight = 30 + 30 + 30 + 20 + 40;
        pageSize = Math.max(MIN_PAGE_SIZE, (height - reservedHeight) / entryUnitHeight);
        page = Math.min(page, Math.max(0, (filteredGroups.size() - 1) / pageSize));

        int totalEntriesHeight = pageSize * entryUnitHeight;
        int contentHeight = 30 + totalEntriesHeight + 30 + 30 + 20;
        int startY = (height - contentHeight) / 2 + 30;
        int searchX = centerX - AE_TEXT_FIELD_WIDTH / 2;
        searchBox = new ResizableAETextField(aeStyle, font, searchX, startY - 25,
                AE_TEXT_FIELD_WIDTH, AE_SEARCH_FIELD_HEIGHT);
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

        int start = page * pageSize;
        int end = Math.min(start + pageSize, filteredGroups.size());
        for (int index = start; index < end; index++) {
            Group group = filteredGroups.get(index);
            Button button = new AE2Button(centerX - ENTRY_WIDTH / 2,
                    startY + (index - start) * entryUnitHeight, ENTRY_WIDTH, BUTTON_HEIGHT,
                    Component.literal(group.label()), ignored -> choose(group));
            entryButtons.add(button);
            addRenderableWidget(button);
        }

        int navigationY = startY + pageSize * entryUnitHeight + 10;
        Button previous = new AE2Button(centerX - 60, navigationY, 20, BUTTON_HEIGHT,
                Component.literal("<"), ignored -> changePage(-1));
        Button next = new AE2Button(centerX + 40, navigationY, 20, BUTTON_HEIGHT,
                Component.literal(">"), ignored -> changePage(1));
        previous.active = page > 0;
        next.active = (page + 1) * pageSize < filteredGroups.size();
        addRenderableWidget(previous);
        addRenderableWidget(next);

        int controlsWidth = Math.min(480, Math.max(240, width - 20));
        int controlsX = centerX - controlsWidth / 2;
        int toggleWidth = Math.min(240, controlsWidth);
        int toggleY = navigationY + 30;
        autoUploadToggleButton = new AE2Button(centerX - toggleWidth / 2, toggleY,
                toggleWidth, BUTTON_HEIGHT, autoUploadLabel(), ignored -> toggleAutoUpload());
        autoUploadToggleButton.setTooltip(autoUploadTooltip());
        addRenderableWidget(autoUploadToggleButton);

        int quickMappingY = navigationY + 55;
        int quickInputWidth = AE_TEXT_FIELD_WIDTH;
        mappingValueInput = new ResizableAETextField(aeStyle, font, controlsX, quickMappingY,
                quickInputWidth, AE_SEARCH_FIELD_HEIGHT);
        mappingValueInput.setMaxLength(256);
        mappingValueInput.setPlaceholder(Component.translatable("ae2_auto_pattern_upload.screen.mapping.value"));
        mappingValueInput.setValue(mappingValue);
        addRenderableWidget(mappingValueInput);
        addRenderableWidget(new AE2Button(controlsX + quickInputWidth + 5, quickMappingY, 85, BUTTON_HEIGHT,
                Component.translatable("ae2_auto_pattern_upload.screen.mapping.add"), ignored -> addMappingFromUi()));
        addRenderableWidget(new AE2Button(controlsX + quickInputWidth + 95, quickMappingY, 155, BUTTON_HEIGHT,
                Component.translatable("ae2_auto_pattern_upload.screen.mapping.management"),
                ignored -> Minecraft.getInstance().setScreen(new RecipeTypeMappingScreen(this))));
        Button close = new AE2Button(controlsX + quickInputWidth + 255, quickMappingY, 80, BUTTON_HEIGHT,
                Component.translatable("gui.cancel"), ignored -> onClose());
        close.setTooltip(Tooltip.create(Component.translatable("ae2_auto_pattern_upload.screen.cancel.tooltip")));
        addRenderableWidget(close);
        tryAutoUploadIfUniqueMatch();
    }

    private void choose(Group group) {
        PacketDistributor.sendToServer(new UploadEncodedPatternC2SPacket(group.representativeId));
        onClose();
    }

    private void changePage(int delta) {
        int nextPage = page + delta;
        if (nextPage < 0 || nextPage * pageSize >= filteredGroups.size()) {
            return;
        }
        page = nextPage;
        refreshPending = true;
    }

    private void addMappingFromUi() {
        String key = query == null ? "" : query.trim();
        String value = mappingValueInput == null ? "" : mappingValueInput.getValue().trim();
        if (key.isEmpty() || value.isEmpty()) {
            return;
        }
        if (PatternUploadUtil.addOrUpdateRecipeTypeMapping(key, value)) {
            query = value;
            page = 0;
            applyFilter();
            refreshPending = true;
        }
    }

    private Component autoUploadLabel() {
        return Component.translatable("ae2_auto_pattern_upload.screen.auto_upload_unique",
                Component.translatable(autoUploadUniqueMatchEnabled
                        ? "ae2_auto_pattern_upload.state.on" : "ae2_auto_pattern_upload.state.off"));
    }

    private Tooltip autoUploadTooltip() {
        return Tooltip.create(Component.translatable("ae2_auto_pattern_upload.screen.auto_upload_unique.tooltip"));
    }

    private void toggleAutoUpload() {
        autoUploadUniqueMatchEnabled = !autoUploadUniqueMatchEnabled;
        saveUiConfig();
        autoUploadToggleButton.setMessage(autoUploadLabel());
        autoUploadToggleButton.setTooltip(autoUploadTooltip());
    }

    private void tryAutoUploadIfUniqueMatch() {
        if (!autoUploadUniqueMatchEnabled || !autoUploadRequestedFromPresetSearch || autoUploadAttempted) {
            return;
        }
        autoUploadAttempted = true;
        if (!query.isBlank() && lastMatchCount == 1) {
            choose(filteredGroups.get(0));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && searchBox != null && searchBox.getTooltipArea().contains((int) mouseX, (int) mouseY)) {
            searchBox.setValue("");
            query = "";
            page = 0;
            applyFilter();
            refreshPending = true;
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
        if (index < 0 || index >= filteredGroups.size()) {
            return;
        }
        String name = filteredGroups.get(index).name;
        if (!pinnedProviders.add(name)) {
            pinnedProviders.remove(name);
        }
        saveUiConfig();
        applyFilter();
        refreshPending = true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox != null && searchBox.isFocused() && searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (mappingValueInput != null && mappingValueInput.isFocused()
                && mappingValueInput.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchBox != null && searchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        if (mappingValueInput != null && mappingValueInput.charTyped(codePoint, modifiers)) {
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
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (filteredGroups.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("ae2_auto_pattern_upload.screen.no_providers"),
                    width / 2, height / 2, 0xFFFFFFFF);
        }
    }

    private static void loadUiConfig() {
        try {
            Path path = FMLPaths.CONFIGDIR.get().resolve(UI_CONFIG);
            if (!Files.exists(path)) {
                return;
            }
            JsonObject object = GSON.fromJson(Files.readString(path), JsonObject.class);
            if (object == null) {
                return;
            }
            JsonElement pinned = object.get(PINNED_KEY);
            if (pinned != null && pinned.isJsonArray()) {
                pinnedProviders.clear();
                for (JsonElement entry : pinned.getAsJsonArray()) {
                    if (entry.isJsonPrimitive() && !entry.getAsString().isBlank()) {
                        pinnedProviders.add(entry.getAsString());
                    }
                }
            }
            if (object.has(AUTO_UPLOAD_KEY)) {
                autoUploadUniqueMatchEnabled = object.get(AUTO_UPLOAD_KEY).getAsBoolean();
            }
        } catch (IOException | RuntimeException ignored) {
            // 配置读取失败时使用默认界面设置。
        }
    }

    private static void saveUiConfig() {
        try {
            Path path = FMLPaths.CONFIGDIR.get().resolve(UI_CONFIG);
            Files.createDirectories(path.getParent());
            JsonObject object = new JsonObject();
            JsonArray pinned = new JsonArray();
            pinnedProviders.forEach(pinned::add);
            object.add(PINNED_KEY, pinned);
            object.addProperty(AUTO_UPLOAD_KEY, autoUploadUniqueMatchEnabled);
            Files.writeString(path, GSON.toJson(object));
        } catch (IOException ignored) {
            // 配置保存失败不影响本次界面操作。
        }
    }

    private static int compareNatural(String first, String second) {
        Matcher firstMatcher = NATURAL_PATTERN.matcher(first);
        Matcher secondMatcher = NATURAL_PATTERN.matcher(second);
        while (firstMatcher.find() && secondMatcher.find()) {
            int textCompare = firstMatcher.group(1).compareTo(secondMatcher.group(1));
            if (textCompare != 0) {
                return textCompare;
            }
            String firstNumber = firstMatcher.group(2);
            String secondNumber = secondMatcher.group(2);
            if (!firstNumber.isEmpty() || !secondNumber.isEmpty()) {
                int firstValue = firstNumber.isEmpty() ? 0 : Integer.parseInt(firstNumber);
                int secondValue = secondNumber.isEmpty() ? 0 : Integer.parseInt(secondNumber);
                if (firstValue != secondValue) {
                    return Integer.compare(firstValue, secondValue);
                }
            }
        }
        return first.length() - second.length();
    }

    private static final class Group {
        private final String name;
        private long representativeId;
        private int representativeSlots;
        private int totalSlots;
        private int count;

        private Group(String name) {
            this.name = name;
        }

        private String label() {
            String prefix = pinnedProviders.contains(name) ? "★ " : "";
            return prefix + name + "  (" + totalSlots + ")  x" + count;
        }
    }
}
