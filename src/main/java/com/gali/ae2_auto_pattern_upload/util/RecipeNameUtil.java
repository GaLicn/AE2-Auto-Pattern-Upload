package com.gali.ae2_auto_pattern_upload.util;

import com.gali.ae2_auto_pattern_upload.MyMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import codechicken.nei.recipe.IRecipeHandler;
import cpw.mods.fml.common.Loader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.util.StatCollector;

/**
 * 配方名称映射工具，兼容 1.7.10 环境。
 */
public final class RecipeNameUtil {

    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Map<String, String> RAW_MAPPINGS = new HashMap<String, String>();
    private static final Map<String, String> LOOKUP_MAPPINGS = new HashMap<String, String>();

    private static final Path CONFIG_FILE;

    private static final Pattern CAMEL_CASE_SPLITTER = Pattern.compile("(?<!^)([A-Z])");

    private static String lastRecipeName = null;

    static {
        Path configDir = Loader.instance().getConfigDir().toPath();
        CONFIG_FILE = configDir.resolve("ae2_auto_pattern_upload").resolve("recipe_names.json");
        loadMappings();
    }

    private RecipeNameUtil() {}

    public static synchronized void setLastRecipeName(String name) {
        lastRecipeName = name;
    }

    public static synchronized String getLastRecipeName() {
        return lastRecipeName;
    }

    public static synchronized void clearLastRecipeName() {
        lastRecipeName = null;
    }

    public static synchronized boolean addOrUpdateMapping(String key, String value) {
        if (key == null || key.trim().isEmpty() || value == null || value.trim().isEmpty()) {
            return false;
        }
        RAW_MAPPINGS.put(key.trim(), value.trim());
        LOOKUP_MAPPINGS.put(normalizeKey(key), value.trim());
        saveMappings();
        return true;
    }

    public static synchronized int removeMappingsByCnValue(String cnValue) {
        if (cnValue == null || cnValue.trim().isEmpty()) {
            return 0;
        }
        String target = cnValue.trim();
        int removed = 0;
        Iterator<Map.Entry<String, String>> iterator = RAW_MAPPINGS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            if (Objects.equals(entry.getValue(), target)) {
                iterator.remove();
                LOOKUP_MAPPINGS.remove(normalizeKey(entry.getKey()));
                removed++;
            }
        }
        if (removed > 0) {
            saveMappings();
        }
        return removed;
    }

    public static synchronized void reloadMappings() {
        loadMappings();
    }

    public static synchronized Map<String, String> getMappingsView() {
        return Collections.unmodifiableMap(RAW_MAPPINGS);
    }

    private static synchronized void loadMappings() {
        RAW_MAPPINGS.clear();
        LOOKUP_MAPPINGS.clear();

        if (!Files.exists(CONFIG_FILE)) {
            writeTemplate();
            return;
        }

        try (InputStreamReader reader =
                new InputStreamReader(Files.newInputStream(CONFIG_FILE), StandardCharsets.UTF_8)) {
            JsonObject obj = GSON.fromJson(reader, JsonObject.class);
            if (obj == null) {
                return;
            }
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String key = entry.getKey();
                if (key == null || key.trim().isEmpty()) {
                    continue;
                }
                JsonElement value = entry.getValue();
                if (value != null && value.isJsonPrimitive()) {
                    String mapped = value.getAsString();
                    if (mapped != null && !mapped.trim().isEmpty()) {
                        RAW_MAPPINGS.put(key.trim(), mapped.trim());
                        LOOKUP_MAPPINGS.put(normalizeKey(key), mapped.trim());
                    }
                }
            }
        } catch (IOException e) {
            MyMod.LOG.warn(
                    StatCollector.translateToLocalFormatted(
                            "ae2_auto_pattern_upload.error.read_mappings",
                            e.getMessage()));
        }
    }

    private static void writeTemplate() {
        JsonObject template = new JsonObject();
        template.addProperty("example.crafting", "example_crafting");
        template.addProperty("example.processing", "example_processing");

        try {
            Path parent = CONFIG_FILE.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            try (OutputStreamWriter writer =
                    new OutputStreamWriter(Files.newOutputStream(CONFIG_FILE), StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(template));
            }
        } catch (IOException e) {
            MyMod.LOG.warn(
                    StatCollector.translateToLocalFormatted(
                            "ae2_auto_pattern_upload.error.create_template",
                            e.getMessage()));
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
            try (OutputStreamWriter writer =
                    new OutputStreamWriter(Files.newOutputStream(CONFIG_FILE), StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(obj));
            }
        } catch (IOException e) {
            MyMod.LOG.warn(
                    StatCollector.translateToLocalFormatted(
                            "ae2_auto_pattern_upload.error.write_mappings",
                            e.getMessage()));
        }
    }

    public static String mapCategoryUidToSearchKey(String categoryUid) {
        if (categoryUid == null || categoryUid.isEmpty()) {
            return null;
        }
        String normalized = categoryUid.trim().toLowerCase(Locale.ROOT);
        int colon = normalized.indexOf(':');
        int dot = normalized.indexOf('.');
        String path;
        if (colon >= 0) {
            path = normalized.substring(colon + 1);
        } else if (dot >= 0) {
            path = normalized.substring(dot + 1);
        } else {
            path = normalized;
        }
        String mapped = LOOKUP_MAPPINGS.get(path);
        if (mapped != null && !mapped.isEmpty()) {
            return mapped;
        }
        return toDisplayString(path);
    }

    public static String deriveSearchKeyFromClassName(Object recipeObj) {
        if (recipeObj == null) {
            return null;
        }
        try {
            String simpleName = recipeObj.getClass().getSimpleName();
            String packageName = recipeObj.getClass().getPackage().getName().toLowerCase(Locale.ROOT);

            String token = CAMEL_CASE_SPLITTER.matcher(simpleName)
                    .replaceAll(" $1")
                    .replace("_", " ")
                    .replace("-", " ")
                    .trim()
                    .toLowerCase(Locale.ROOT);

            token = token.replace(" recipe", "").replace(" handler", "").trim();

            String namespace = null;
            if (packageName.contains("gregtech")) {
                namespace = "gregtech";
            } else if (packageName.contains("gtceu")) {
                namespace = "gtceu";
            } else if (packageName.contains("thermal")) {
                namespace = "thermal";
            } else if (packageName.contains("botania")) {
                namespace = "botania";
            } else if (packageName.contains("immersive")) {
                namespace = "immersive";
            }

            if (namespace != null && !token.isEmpty()) {
                return namespace + " " + token;
            }
            if (!token.isEmpty()) {
                return token;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static void captureFromRecipeHandler(IRecipeHandler handler) {
        String keyword = mapRecipeHandlerToSearchKey(handler);
        if (keyword != null && !keyword.isEmpty()) {
            setLastRecipeName(keyword);
        }
    }

    public static String mapRecipeHandlerToSearchKey(IRecipeHandler handler) {
        if (handler == null) {
            return null;
        }
        try {
            String overlayId = safeOverlayIdentifier(handler);
            if (overlayId != null) {
                String mapped = mapStringToMapping(overlayId);
                if (mapped != null) {
                    return mapped;
                }
                return toDisplayString(overlayId);
            }
        } catch (Throwable ignored) {
        }

        try {
            String recipeName = handler.getRecipeName();
            if (recipeName != null && !recipeName.trim().isEmpty()) {
                String mapped = mapStringToMapping(recipeName);
                if (mapped != null) {
                    return mapped;
                }
                return recipeName.trim();
            }
        } catch (Throwable ignored) {
        }

        return toDisplayString(handler.getClass().getSimpleName());
    }

    private static String mapStringToMapping(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String normalized = normalizeKey(raw);
        String mapped = LOOKUP_MAPPINGS.get(normalized);
        if (mapped != null && !mapped.isEmpty()) {
            return mapped;
        }
        return null;
    }

    private static String safeOverlayIdentifier(IRecipeHandler handler) {
        try {
            String id = handler.getOverlayIdentifier();
            if (id != null && !id.trim().isEmpty()) {
                return id;
            }
        } catch (Throwable ignored) {
        }
        try {
            String tabName = handler.getRecipeTabName();
            if (tabName != null && !tabName.trim().isEmpty()) {
                return tabName;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String normalizeKey(String key) {
        return key.trim().toLowerCase(Locale.ROOT);
    }

    private static String toDisplayString(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.replace('_', ' ').replace('-', ' ').replace('.', ' ').replace(':', ' ');
        cleaned = CAMEL_CASE_SPLITTER.matcher(cleaned).replaceAll(" $1");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned;
    }
}
