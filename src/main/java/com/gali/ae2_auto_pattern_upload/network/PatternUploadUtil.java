package com.gali.ae2_auto_pattern_upload.network;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionHost;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.menu.AEBaseMenu;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.gali.ae2_auto_pattern_upload.mixin.PatternEncodingTermMenuAccessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class PatternUploadUtil {
    private static final String MAPPING_CONFIG = "ae2_auto_pattern_upload/recipe_type_names.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<String, String> RECIPE_TYPE_MAPPINGS = new ConcurrentHashMap<>();
    private static volatile String lastProviderSearchKey;

    public record RecipeTypeMapping(String key, String value) {
    }

    private PatternUploadUtil() {
    }

    static {
        loadRecipeTypeMappings();
    }

    public static void setLastProviderSearchKey(String key) {
        String normalized = key == null ? "" : key.trim();
        String mapped = resolveProviderSearchKey(normalized);
        lastProviderSearchKey = mapped.isBlank() ? null : mapped;
    }

    public static String consumeLastProviderSearchKey() {
        String key = lastProviderSearchKey;
        lastProviderSearchKey = null;
        return key;
    }

    public static synchronized void loadRecipeTypeMappings() {
        try {
            Path configPath = FMLPaths.CONFIGDIR.get().resolve(MAPPING_CONFIG);
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath.getParent());
                Files.writeString(configPath, GSON.toJson(new JsonObject()));
            }

            JsonObject object = GSON.fromJson(Files.readString(configPath), JsonObject.class);
            Map<String, String> loaded = new java.util.HashMap<>();
            if (object != null) {
                for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                    if (entry.getValue() != null && entry.getValue().isJsonPrimitive()
                            && !entry.getValue().getAsString().isBlank()) {
                        loaded.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            }
            RECIPE_TYPE_MAPPINGS.clear();
            RECIPE_TYPE_MAPPINGS.putAll(loaded);
        } catch (IOException | RuntimeException ignored) {
            // 配置损坏或不可写时保持空映射，不阻断上传流程。
        }
    }

    public static synchronized boolean addOrUpdateRecipeTypeMapping(String key, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            return false;
        }
        try {
            Path configPath = FMLPaths.CONFIGDIR.get().resolve(MAPPING_CONFIG);
            loadRecipeTypeMappings();
            JsonObject object = Files.exists(configPath)
                    ? GSON.fromJson(Files.readString(configPath), JsonObject.class) : new JsonObject();
            if (object == null) {
                object = new JsonObject();
            }
            String normalizedKey = key.trim();
            object.addProperty(normalizedKey, value.trim());
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(object));
            loadRecipeTypeMappings();
            return true;
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }

    public static List<RecipeTypeMapping> getRecipeTypeMappings() {
        return RECIPE_TYPE_MAPPINGS.entrySet().stream()
                .map(entry -> new RecipeTypeMapping(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(RecipeTypeMapping::key, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public static synchronized boolean removeRecipeTypeMapping(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        try {
            Path configPath = FMLPaths.CONFIGDIR.get().resolve(MAPPING_CONFIG);
            if (!Files.exists(configPath)) {
                return false;
            }
            JsonObject object = GSON.fromJson(Files.readString(configPath), JsonObject.class);
            if (object == null) {
                return false;
            }
            String requested = key.trim();
            String storedKey = object.keySet().stream()
                    .filter(entry -> mappingKeysEqual(entry, requested))
                    .findFirst().orElse(null);
            if (storedKey == null) {
                return false;
            }
            object.remove(storedKey);
            Files.writeString(configPath, GSON.toJson(object));
            loadRecipeTypeMappings();
            return true;
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }

    public static String resolveProviderSearchKey(String rawKey) {
        String normalized = rawKey == null ? "" : rawKey.trim();
        if (normalized.isEmpty()) {
            return normalized;
        }
        String alias = findMapping(normalized);
        if (alias != null && !alias.isBlank()) {
            return alias;
        }
        ResourceLocation recipeType = ResourceLocation.tryParse(normalized);
        if (recipeType != null) {
            String mapped = findMapping(recipeType.toString());
            if (mapped != null && !mapped.isBlank()) {
                return mapped;
            }
        }
        return normalized;
    }

    /** 按 EAEP 顺序解析配方类型：完整 UID 映射、路径别名，最后回退 JEI 分类标题。 */
    public static String resolveRecipeTypeSearchKey(ResourceLocation recipeType, String displayName) {
        if (recipeType == null) {
            return displayName;
        }
        String mapped = findMapping(recipeType.toString());
        if (mapped == null) {
            mapped = findMapping(recipeType.getPath());
        }
        if (mapped != null && !mapped.isBlank()) {
            return mapped;
        }
        return displayName == null || displayName.isBlank() ? recipeType.getPath() : displayName;
    }

    private static String findMapping(String key) {
        String direct = RECIPE_TYPE_MAPPINGS.get(key);
        if (direct != null) {
            return direct;
        }
        return RECIPE_TYPE_MAPPINGS.entrySet().stream()
                .filter(entry -> mappingKeysEqual(entry.getKey(), key))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
    }

    private static boolean mappingKeysEqual(String first, String second) {
        if (first.contains(":") || second.contains(":")) {
            return Objects.equals(ResourceLocation.tryParse(first), ResourceLocation.tryParse(second));
        }
        return first.equalsIgnoreCase(second);
    }

    public static List<ProviderEntry> listAvailableProviders(PatternEncodingTermMenu menu) {
        List<ProviderEntry> result = new ArrayList<>();
        IGrid grid = getGrid(menu);
        if (grid == null) {
            return result;
        }

        // 按 AE2 当前网络中的机器顺序生成稳定索引，客户端只回传这个索引。
        for (Class<?> machineClass : grid.getMachineClasses()) {
            if (!PatternContainer.class.isAssignableFrom(machineClass)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Class<? extends PatternContainer> providerClass = (Class<? extends PatternContainer>) machineClass;
            for (PatternContainer provider : grid.getActiveMachines(providerClass)) {
                if (provider == null || !provider.isVisibleInTerminal()) {
                    continue;
                }
                InternalInventory inventory = provider.getTerminalPatternInventory();
                int emptySlots = countEmptySlots(inventory);
                if (emptySlots > 0) {
                    result.add(new ProviderEntry(provider, providerName(provider), emptySlots));
                }
            }
        }
        return result;
    }

    public static boolean uploadByIndex(ServerPlayer player, PatternEncodingTermMenu menu, int index) {
        if (player == null || menu == null || index < 0) {
            return false;
        }
        List<ProviderEntry> providers = listAvailableProviders(menu);
        if (index >= providers.size()) {
            return false;
        }

        ItemStack encodedPattern = getEncodedPattern(menu);
        if (encodedPattern.isEmpty() || !PatternDetailsHelper.isEncodedPattern(encodedPattern)) {
            return false;
        }

        PatternContainer provider = providers.get(index).provider();
        InternalInventory inventory = provider.getTerminalPatternInventory();
        if (inventory == null) {
            return false;
        }
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (!inventory.getStackInSlot(slot).isEmpty()) {
                continue;
            }
            // 服务端再次校验编码样板，避免客户端伪造普通物品上传。
            ItemStack copy = encodedPattern.copyWithCount(1);
            inventory.setItemDirect(slot, copy);
            ((PatternEncodingTermMenuAccessor) menu).ae2apu$getEncodedPatternSlot().set(ItemStack.EMPTY);
            return true;
        }
        return false;
    }

    private static ItemStack getEncodedPattern(PatternEncodingTermMenu menu) {
        return ((PatternEncodingTermMenuAccessor) menu).ae2apu$getEncodedPatternSlot().getItem();
    }

    private static IGrid getGrid(PatternEncodingTermMenu menu) {
        try {
            if (menu instanceof AEBaseMenu baseMenu
                    && baseMenu.getTarget() instanceof IActionHost host
                    && host.getActionableNode() != null) {
                return host.getActionableNode().getGrid();
            }
        } catch (Throwable ignored) {
            // 菜单关闭或网络断开时，按无可用供应器处理。
        }
        return null;
    }

    private static int countEmptySlots(InternalInventory inventory) {
        if (inventory == null) {
            return 0;
        }
        int empty = 0;
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (inventory.getStackInSlot(slot).isEmpty()) {
                empty++;
            }
        }
        return empty;
    }

    private static Component providerName(PatternContainer provider) {
        try {
            if (provider.getTerminalGroup() != null) {
                return provider.getTerminalGroup().name();
            }
        } catch (Throwable ignored) {
            // 某些供应器没有终端分组，使用通用名称。
        }
        return Component.translatable("ae2_auto_pattern_upload.provider.default");
    }

    public record ProviderEntry(PatternContainer provider, Component name, int emptySlots) {
    }
}
