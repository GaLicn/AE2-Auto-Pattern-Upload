package com.gali.util;

import com.gali.ae2_auto_pattern_upload;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** 管理配方类型 UID、JEI 分类标题与供应器搜索词之间的映射。 */
public final class RecipeTypeNameConfig {
    private static final String CONFIG_PATH = "ae2_auto_pattern_upload/recipe_type_names.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<ResourceLocation, String> CUSTOM_NAMES = new ConcurrentHashMap<>();
    private static final Map<String, String> CUSTOM_ALIASES = new ConcurrentHashMap<>();
    public record RecipeTypeMapping(String key, String value) {}

    public static final String DEFAULT_CRAFTING_SEARCH_KEY = "crafting";
    private static volatile String lastProviderSearchKey;

    static {
        try {
            loadRecipeTypeNames();
        } catch (Throwable error) {
            ae2_auto_pattern_upload.LOGGER.warn("配方类型映射加载失败: {}", error.getMessage());
        }
    }

    private RecipeTypeNameConfig() {}

    public static void setLastProcessingName(String name) { setLastProviderSearchKey(name); }

    public static void setLastProviderSearchKey(String name) {
        if (name == null) {
            lastProviderSearchKey = null;
            return;
        }
        String resolved = resolveProviderSearchKey(name.trim());
        lastProviderSearchKey = resolved.isBlank() ? null : resolved;
    }

    public static void presetCraftingProviderSearchKey() {
        setLastProviderSearchKey(resolveSearchKeyAlias(DEFAULT_CRAFTING_SEARCH_KEY));
    }

    public static String consumeLastProviderSearchKey() {
        String value = lastProviderSearchKey;
        lastProviderSearchKey = null;
        return value;
    }

    private static JsonObject loadJsonConfig(Path path) throws IOException {
        if (!Files.exists(path)) return new JsonObject();
        JsonObject object = GSON.fromJson(Files.readString(path), JsonObject.class);
        return object == null ? new JsonObject() : object;
    }

    private static void saveJsonConfig(Path path, JsonObject object) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(object));
    }

    public static synchronized void loadRecipeTypeNames() throws IOException {
        Path path = FMLPaths.CONFIGDIR.get().resolve(CONFIG_PATH);
        if (!Files.exists(path)) saveJsonConfig(path, new JsonObject());
        JsonObject config = loadJsonConfig(path);
        Map<ResourceLocation, String> names = new HashMap<>();
        Map<String, String> aliases = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : config.entrySet()) {
            if (!entry.getValue().isJsonPrimitive()) continue;
            String value = entry.getValue().getAsString();
            if (value.isBlank()) continue;
            if (entry.getKey().contains(":")) {
                try { names.put(new ResourceLocation(entry.getKey()), value); } catch (RuntimeException ignored) {}
            } else {
                aliases.put(entry.getKey().toLowerCase(Locale.ROOT), value);
            }
        }
        CUSTOM_NAMES.clear();
        CUSTOM_NAMES.putAll(names);
        CUSTOM_ALIASES.clear();
        CUSTOM_ALIASES.putAll(aliases);
    }

    public static synchronized boolean addOrUpdateRecipeTypeMapping(String key, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) return false;
        try {
            Path path = FMLPaths.CONFIGDIR.get().resolve(CONFIG_PATH);
            JsonObject config = loadJsonConfig(path);
            config.addProperty(key.trim(), value.trim());
            saveJsonConfig(path, config);
            loadRecipeTypeNames();
            return true;
        } catch (IOException | RuntimeException error) {
            ae2_auto_pattern_upload.LOGGER.error("配方类型映射保存失败", error);
            return false;
        }
    }

    public static boolean addOrUpdateAliasMapping(String key, String value) {
        return addOrUpdateRecipeTypeMapping(key, value);
    }

    public static List<RecipeTypeMapping> getRecipeTypeMappings() {
        List<RecipeTypeMapping> result = new ArrayList<>();
        CUSTOM_NAMES.forEach((key, value) -> result.add(new RecipeTypeMapping(key.toString(), value)));
        CUSTOM_ALIASES.forEach((key, value) -> result.add(new RecipeTypeMapping(key, value)));
        result.sort(Comparator.comparing(RecipeTypeMapping::key, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(result);
    }

    public static synchronized boolean removeRecipeTypeMapping(String mappingKey) {
        if (mappingKey == null || mappingKey.isBlank()) return false;
        try {
            Path path = FMLPaths.CONFIGDIR.get().resolve(CONFIG_PATH);
            JsonObject config = loadJsonConfig(path);
            String storedKey = config.keySet().stream()
                    .filter(key -> mappingKeysEqual(key, mappingKey.trim())).findFirst().orElse(null);
            if (storedKey == null) return false;
            config.remove(storedKey);
            saveJsonConfig(path, config);
            loadRecipeTypeNames();
            return true;
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }

    public static synchronized int removeMappingsByCnValue(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            Path path = FMLPaths.CONFIGDIR.get().resolve(CONFIG_PATH);
            JsonObject config = loadJsonConfig(path);
            List<String> keys = config.entrySet().stream()
                    .filter(entry -> entry.getValue().isJsonPrimitive() && value.equals(entry.getValue().getAsString()))
                    .map(Map.Entry::getKey).toList();
            keys.forEach(config::remove);
            if (!keys.isEmpty()) {
                saveJsonConfig(path, config);
                loadRecipeTypeNames();
            }
            return keys.size();
        } catch (IOException | RuntimeException ignored) {
            return 0;
        }
    }

    private static boolean mappingKeysEqual(String first, String second) {
        if (first.contains(":") || second.contains(":")) {
            try { return Objects.equals(new ResourceLocation(first), new ResourceLocation(second)); }
            catch (RuntimeException ignored) { return false; }
        }
        return first.equalsIgnoreCase(second);
    }

    public static String resolveSearchKeyAlias(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) return null;
        String normalized = rawKey.trim();
        return CUSTOM_ALIASES.getOrDefault(normalized.toLowerCase(Locale.ROOT), normalized);
    }

    public static String resolveProviderSearchKey(String rawKey) {
        String normalized = rawKey == null ? "" : rawKey.trim();
        if (normalized.isEmpty()) return normalized;
        String alias = resolveSearchKeyAlias(normalized);
        if (!normalized.equals(alias)) return alias;
        try {
            String mapped = resolveRecipeTypeSearchKey(new ResourceLocation(normalized), null);
            return mapped == null ? normalized : mapped;
        } catch (RuntimeException ignored) {
            return normalized;
        }
    }

    public static String mapRecipeTypeToSearchKey(Recipe<?> recipe) {
        return recipe == null ? null : resolveRecipeTypeSearchKey(resolveRecipeTypeId(recipe.getType()), null);
    }

    public static String resolveRecipeTypeSearchKey(ResourceLocation id, String displayName) {
        if (id == null) return displayName;
        String custom = CUSTOM_NAMES.get(id);
        if (custom != null && !custom.isBlank()) return custom;
        String alias = CUSTOM_ALIASES.get(id.getPath().toLowerCase(Locale.ROOT));
        if (alias != null && !alias.isBlank()) return alias;
        return displayName == null || displayName.isBlank() ? id.getPath() : displayName;
    }

    private static ResourceLocation resolveRecipeTypeId(RecipeType<?> type) {
        if (type == null) return null;
        ResourceLocation id = BuiltInRegistries.RECIPE_TYPE.getKey(type);
        if (id != null) return id;
        try { return new ResourceLocation(type.toString()); } catch (RuntimeException ignored) { return null; }
    }

    public static String mapGTCEuRecipeToSearchKey(Object recipe) {
        try {
            Object type = recipe.getClass().getMethod("getType").invoke(recipe);
            return resolveRecipeTypeSearchKey(new ResourceLocation(String.valueOf(type)), null);
        } catch (Throwable ignored) { return null; }
    }

    public static String deriveSearchKeyFromUnknownRecipe(Object recipe) {
        if (recipe == null) return null;
        try {
            Object type = recipe.getClass().getMethod("getType").invoke(recipe);
            if (type instanceof RecipeType<?> recipeType) {
                String mapped = resolveRecipeTypeSearchKey(resolveRecipeTypeId(recipeType), null);
                if (mapped != null && !mapped.isBlank()) return mapped;
            }
        } catch (Throwable ignored) {}
        String simpleName = recipe.getClass().getSimpleName()
                .replaceAll("Recipe(s)?$|Category$|JEI$", "")
                .replaceAll("(?<!^)([A-Z])", " $")
                .toLowerCase(Locale.ROOT).trim();
        return simpleName.isBlank() ? null : CUSTOM_ALIASES.getOrDefault(simpleName, simpleName);
    }
}
