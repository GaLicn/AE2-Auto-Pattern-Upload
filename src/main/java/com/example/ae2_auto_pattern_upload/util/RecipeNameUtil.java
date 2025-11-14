package com.example.ae2_auto_pattern_upload.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraftforge.fml.common.Loader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 配方名称映射工具，支持用户自定义 JEI 分类与搜索关键字的对应关系。
 */
public class RecipeNameUtil {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final Map<String, String> RAW_MAPPINGS = new HashMap<>();
    private static final Map<String, String> LOOKUP_MAPPINGS = new HashMap<>();
    private static final Path CONFIG_FILE;

    static {
        Path configDir = Loader.instance().getConfigDir().toPath();
        CONFIG_FILE = configDir.resolve("ae2_auto_pattern_upload").resolve("recipe_names.json");
        loadMappings();
    }
    
    /**
     * 从类名推导搜索关键字
     */
    public static String deriveSearchKeyFromClassName(Object recipeObj) {
        if (recipeObj == null) return null;
        
        try {
            Class<?> cls = recipeObj.getClass();
            String simpleName = cls.getSimpleName();
            String packageName = cls.getPackage().getName().toLowerCase();
            
            // 去掉常见后缀
            String token = simpleName
                    .replaceAll("Recipe$", "")
                    .replaceAll("Recipes$", "")
                    .replaceAll("Category$", "")
                    .replaceAll("JEI$", "");
            
            // 驼峰转小写
            token = token.replaceAll("(?<!^)([A-Z])", " $1").toLowerCase().trim();
            
            // 根据包名推断命名空间
            String namespace = null;
            if (packageName.contains("gtceu")) namespace = "gtceu";
            else if (packageName.contains("gregtech")) namespace = "gregtech";
            else if (packageName.contains("create")) namespace = "create";
            else if (packageName.contains("immersiveengineering")) namespace = "immersive";
            
            if (namespace != null && !token.isEmpty()) {
                return namespace + " " + token;
            }
            return token;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 从字符串映射获取搜索关键字
     */
    public static String mapRecipeTypeToSearchKey(String recipeType) {
        if (recipeType == null || recipeType.isEmpty()) return null;

        String path = extractPath(recipeType);
        String mapped = LOOKUP_MAPPINGS.get(path.toLowerCase());
        return mapped != null ? mapped : path;
    }

    /**
     * JEI 分类 UID 映射（通常形如 "minecraft.smelting" 或 "appliedenergistics2.grinder"）。
     */
    public static String mapCategoryUidToSearchKey(String categoryUid) {
        if (categoryUid == null || categoryUid.isEmpty()) {
            return null;
        }

        // 统一使用小写
        String normalized = categoryUid.toLowerCase();

        // 部分整合包会使用 “namespace.category” 形式
        String path = normalized;
        int separator = normalized.indexOf(':');
        if (separator >= 0) {
            path = normalized.substring(separator + 1);
        } else {
            separator = normalized.indexOf('.');
            if (separator >= 0) {
                path = normalized.substring(separator + 1);
            }
        }

        String mapped = LOOKUP_MAPPINGS.get(path.toLowerCase());
        if (mapped != null && !mapped.isEmpty()) {
            return mapped;
        }

        return path;
    }

    private static String extractPath(String recipeType) {
        int separator = recipeType.indexOf(':');
        if (separator >= 0 && separator + 1 < recipeType.length()) {
            return recipeType.substring(separator + 1);
        }
        return recipeType;
    }
    
    /**
     * 设置最后一个配方的搜索名称
     */
    private static String lastRecipeName = null;
    
    public static void setLastRecipeName(String name) {
        lastRecipeName = name;
    }
    
    public static String getLastRecipeName() {
        return lastRecipeName;
    }
    
    public static void clearLastRecipeName() {
        lastRecipeName = null;
    }

    private static void loadMappings() {
        RAW_MAPPINGS.clear();
        LOOKUP_MAPPINGS.clear();

        if (!Files.exists(CONFIG_FILE)) {
            writeTemplate();
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(CONFIG_FILE), StandardCharsets.UTF_8)) {
            JsonObject obj = GSON.fromJson(reader, JsonObject.class);
            if (obj == null) {
                return;
            }

            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();
                if (key == null || key.trim().isEmpty()) {
                    continue;
                }
                if (value != null && value.isJsonPrimitive()) {
                    String mapped = value.getAsString();
                    if (mapped != null && !mapped.trim().isEmpty()) {
                        RAW_MAPPINGS.put(key.trim(), mapped.trim());
                        LOOKUP_MAPPINGS.put(key.trim().toLowerCase(), mapped.trim());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[AE2 Auto Pattern Upload] " + net.minecraft.client.resources.I18n.format("ae2_auto_pattern_upload.error.read_mappings", e.getMessage()));
        }
    }

    private static void writeTemplate() {
        JsonObject template = new JsonObject();
        template.addProperty("example.minecraft.smelting", "example_smelting");
        template.addProperty("example.appliedenergistics2.inscriber", "example_inscriber");

        try {
            Path parent = CONFIG_FILE.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            String json = GSON.toJson(template);
            try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(CONFIG_FILE), StandardCharsets.UTF_8)) {
                writer.write(json);
            }
        } catch (IOException e) {
            System.err.println("[AE2 Auto Pattern Upload] " + net.minecraft.client.resources.I18n.format("ae2_auto_pattern_upload.error.create_template", e.getMessage()));
        }
    }

    private static void saveMappings() {
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, String> entry : RAW_MAPPINGS.entrySet()) {
            obj.addProperty(entry.getKey(), entry.getValue());
        }
        try {
            Path parent = CONFIG_FILE.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            String json = GSON.toJson(obj);
            try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(CONFIG_FILE), StandardCharsets.UTF_8)) {
                writer.write(json);
            }
        } catch (IOException e) {
            System.err.println("[AE2 Auto Pattern Upload] " + net.minecraft.client.resources.I18n.format("ae2_auto_pattern_upload.error.write_mappings", e.getMessage()));
        }
    }

    public static boolean addOrUpdateMapping(String key, String value) {
        if (key == null || key.trim().isEmpty() || value == null || value.trim().isEmpty()) {
            return false;
        }
        RAW_MAPPINGS.put(key.trim(), value.trim());
        LOOKUP_MAPPINGS.put(key.trim().toLowerCase(), value.trim());
        saveMappings();
        return true;
    }

    public static int removeMappingsByCnValue(String cnValue) {
        if (cnValue == null || cnValue.trim().isEmpty()) {
            return 0;
        }
        String target = cnValue.trim();
        int removed = 0;
        Iterator<Map.Entry<String, String>> it = RAW_MAPPINGS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            if (target.equals(entry.getValue())) {
                LOOKUP_MAPPINGS.remove(entry.getKey().toLowerCase());
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            saveMappings();
        }
        return removed;
    }

    public static void reloadMappings() {
        loadMappings();
    }

    public static Map<String, String> getMappingsView() {
        return Collections.unmodifiableMap(RAW_MAPPINGS);
    }
}
