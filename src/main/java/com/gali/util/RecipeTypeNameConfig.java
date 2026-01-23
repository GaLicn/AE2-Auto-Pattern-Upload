package com.gali.util;

import com.google.gson.*;
import com.gali.ae2_auto_pattern_upload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 负责配置文件 ae2_auto_pattern_upload/recipe_type_names.json 的加载与写入，
 * 以及 recipeType -> 中文名称 / 搜索关键字 的映射逻辑。
 */
public final class RecipeTypeNameConfig {
    private static final String CONFIG_PATH = "ae2_auto_pattern_upload/recipe_type_names.json";
    private static final Map<ResourceLocation, String> CUSTOM_NAMES = new ConcurrentHashMap<>();
    private static final Map<String, String> CUSTOM_ALIASES = new ConcurrentHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    static {
        try {
            loadRecipeTypeNames();
        } catch (Throwable t) {
            ae2_auto_pattern_upload.LOGGER.warn("映射文件解析失败: {}", t.getMessage());
        }
    }

    private RecipeTypeNameConfig() {}

    // 最近一次通过 JEI 填充到编码终端的"处理配方"的中文名称
    public static volatile String lastProcessingName = null;
    
    public static void setLastProcessingName(String name) {
        lastProcessingName = name;
    }

    /**
     * 生成默认的配方类型映射
     */
    private static Map<String, String> getDefaultMappings() {
        Map<String, String> mappings = new HashMap<>();
        mappings.put("minecraft:smelting", "熔炉");
        mappings.put("minecraft:blasting", "高炉");
        mappings.put("minecraft:smoking", "烟熏");
        mappings.put("minecraft:campfire_cooking", "营火");
        mappings.put("smelting", "熔炉");
        mappings.put("blasting", "高炉");
        mappings.put("smoking", "烟熏");
        return mappings;
    }

    /**
     * 创建默认配置文件模板
     */
    private static JsonObject createDefaultTemplate() {
        JsonObject tmpl = new JsonObject();
        getDefaultMappings().forEach(tmpl::addProperty);
        return tmpl;
    }

    /**
     * 加载 JSON 配置文件
     */
    private static JsonObject loadJsonConfig(Path cfgPath) throws IOException, JsonSyntaxException {
        if (!Files.exists(cfgPath)) return new JsonObject();
        String json = Files.readString(cfgPath);
        JsonObject obj = GSON.fromJson(json, JsonObject.class);
        return obj != null ? obj : new JsonObject();
    }

    /**
     * 保存 JSON 配置到文件
     */
    private static void saveJsonConfig(Path cfgPath, JsonObject config) throws IOException {
        Files.createDirectories(cfgPath.getParent());
        Files.writeString(cfgPath, GSON.toJson(config));
    }

    /**
     * 加载配方类型名称映射
     */
    public static synchronized void loadRecipeTypeNames() throws IOException {
        Path cfgPath = FMLPaths.CONFIGDIR.get().resolve(CONFIG_PATH);
        JsonObject config = loadJsonConfig(cfgPath);
        
        if (config.entrySet().isEmpty()) {
            config = createDefaultTemplate();
            saveJsonConfig(cfgPath, config);
        }

        Map<ResourceLocation, String> nameMap = new HashMap<>();
        Map<String, String> alias = new HashMap<>();
        
        for (Map.Entry<String, JsonElement> entry : config.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (value != null && value.isJsonPrimitive()) {
                String name = value.getAsString();
                if (name == null || name.isBlank()) continue;
                
                if (key.contains(":")) {
                    try {
                        ResourceLocation rl = ResourceLocation.parse(key);
                        nameMap.put(rl, name);
                    } catch (Exception ignored) {}
                } else {
                    alias.put(key.toLowerCase(), name);
                }
            }
        }

        CUSTOM_NAMES.clear();
        CUSTOM_NAMES.putAll(nameMap);
        CUSTOM_ALIASES.clear();
        CUSTOM_ALIASES.putAll(alias);
    }

    /**
     * 新增或更新别名到名称的映射
     */
    public static synchronized boolean addOrUpdateAliasMapping(String aliasKey, String value) {
        if (aliasKey == null || aliasKey.isBlank() || value == null || value.isBlank()) {
            return false;
        }
        try {
            Path cfgPath = FMLPaths.CONFIGDIR.get().resolve(CONFIG_PATH);
            JsonObject config = loadJsonConfig(cfgPath);
            String key = aliasKey.trim();
            config.addProperty(key, value);
            saveJsonConfig(cfgPath, config);

            if (key.contains(":")) {
                try {
                    ResourceLocation rl = ResourceLocation.parse(key);
                    CUSTOM_NAMES.put(rl, value);
                } catch (Exception ignored) {}
            } else {
                CUSTOM_ALIASES.put(key.toLowerCase(), value);
            }
            return true;
        } catch (IOException | JsonSyntaxException e) {
            ae2_auto_pattern_upload.LOGGER.error("配置更新失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 按值精确匹配删除映射
     */
    public static synchronized int removeMappingsByCnValue(String delValue) {
        if (delValue == null || delValue.trim().isEmpty()) return 0;
        try {
            Path cfgPath = FMLPaths.CONFIGDIR.get().resolve(CONFIG_PATH);
            JsonObject config = loadJsonConfig(cfgPath);

            List<String> toRemove = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : config.entrySet()) {
                JsonElement value = entry.getValue();
                if (value != null && value.isJsonPrimitive() && delValue.equals(value.getAsString())) {
                    toRemove.add(entry.getKey());
                }
            }

            if (toRemove.isEmpty()) return 0;

            toRemove.forEach(config::remove);
            saveJsonConfig(cfgPath, config);

            for (String key : toRemove) {
                if (key.contains(":")) {
                    try {
                        ResourceLocation rl = ResourceLocation.parse(key);
                        if (delValue.equals(CUSTOM_NAMES.get(rl))) {
                            CUSTOM_NAMES.remove(rl);
                        }
                    } catch (Exception ignored) {}
                } else {
                    String lower = key.toLowerCase();
                    if (delValue.equals(CUSTOM_ALIASES.get(lower))) {
                        CUSTOM_ALIASES.remove(lower);
                    }
                }
            }
            return toRemove.size();
        } catch (IOException | JsonSyntaxException e) {
            ae2_auto_pattern_upload.LOGGER.error("配置删除失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 映射配方类型到搜索关键字
     */
    public static String mapRecipeTypeToSearchKey(Recipe<?> recipe) {
        if (recipe == null) return null;
        RecipeType<?> type = recipe.getType();
        ResourceLocation key = BuiltInRegistries.RECIPE_TYPE.getKey(type);
        if (key == null) return null;
        String path = key.getPath().toLowerCase();
        return CUSTOM_ALIASES.getOrDefault(path, CUSTOM_NAMES.getOrDefault(key, path));
    }

    /**
     * 从未知配方类推导搜索关键字
     */
    public static String deriveSearchKeyFromUnknownRecipe(Object recipeBase) {
        if (recipeBase == null) return null;
        try {
            Class<?> cls = recipeBase.getClass();
            String simple = cls.getSimpleName();
            String pkg = cls.getName();

            String namespace = null;
            String lower = pkg.toLowerCase();
            if (lower.contains("gtceu")) namespace = "gtceu";
            else if (lower.contains("gregtech")) namespace = "gregtech";
            else if (lower.contains("create")) namespace = "create";

            String token = toSearchToken(simple);
            String key = (namespace != null && token != null && !token.isBlank()) ?
                    namespace + " " + token : token;
            if (key == null || key.isBlank()) return null;
            
            String alias = CUSTOM_ALIASES.get(key.toLowerCase());
            return alias != null && !alias.isBlank() ? alias : key;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * 将类名转换为搜索关键字
     */
    private static String toSearchToken(String simpleName) {
        if (simpleName == null || simpleName.isBlank()) return null;
        String s = simpleName
                .replaceAll("Recipe(s)?$", "")
                .replaceAll("Category$", "")
                .replaceAll("JEI$", "")
                .replaceAll("(?<!^)([A-Z])", " $1")
                .toLowerCase()
                .trim();
        return s.isBlank() ? null : s;
    }
}
