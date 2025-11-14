package com.example.ae2_auto_pattern_upload.util;

/**
 * 配方名称映射工具
 * 根据配方类型获取搜索关键字
 */
public class RecipeNameUtil {
    
    // 配方类型到搜索关键字的映射
    private static final java.util.Map<String, String> RECIPE_TYPE_MAPPING = new java.util.HashMap<>();
    
    static {
        // 原版配方类型映射
        RECIPE_TYPE_MAPPING.put("crafting", "合成");
        RECIPE_TYPE_MAPPING.put("smelting", "烧炼");
        RECIPE_TYPE_MAPPING.put("smoking", "烟熏");
        RECIPE_TYPE_MAPPING.put("campfire_cooking", "篝火");
        RECIPE_TYPE_MAPPING.put("stonecutting", "切石");

        // AE2 配方类型映射
        RECIPE_TYPE_MAPPING.put("ae2:inscriber", "铭刻");
        RECIPE_TYPE_MAPPING.put("ae2:grindstone", "研磨");
        RECIPE_TYPE_MAPPING.put("ae2:charger", "充能");
        RECIPE_TYPE_MAPPING.put("ae2:matter_cannon", "物质炮");

        // GTCEu 配方类型映射
        RECIPE_TYPE_MAPPING.put("assembler", "装配");
        RECIPE_TYPE_MAPPING.put("macerator", "粉碎");
        RECIPE_TYPE_MAPPING.put("extractor", "提取");
        RECIPE_TYPE_MAPPING.put("compressor", "压缩");
        RECIPE_TYPE_MAPPING.put("bender", "弯曲");
        RECIPE_TYPE_MAPPING.put("cutter", "切割");
        RECIPE_TYPE_MAPPING.put("chemical_reactor", "化学反应");
        RECIPE_TYPE_MAPPING.put("chemical_bath", "化学浴");
        RECIPE_TYPE_MAPPING.put("ore_washer", "矿洗");
        RECIPE_TYPE_MAPPING.put("electrolyzer", "电解");
        RECIPE_TYPE_MAPPING.put("centrifuge", "离心");
        RECIPE_TYPE_MAPPING.put("thermal_centrifuge", "热离心");
        RECIPE_TYPE_MAPPING.put("arc_furnace", "电弧炉");
        RECIPE_TYPE_MAPPING.put("blast_furnace", "高炉");
        RECIPE_TYPE_MAPPING.put("vacuum_freezer", "真空冷冻");
        RECIPE_TYPE_MAPPING.put("implosion_compressor", "内爆压缩");
        RECIPE_TYPE_MAPPING.put("autoclave", "高压釜");
        RECIPE_TYPE_MAPPING.put("forming_press", "成型机");
        RECIPE_TYPE_MAPPING.put("wiremill", "拉丝机");
        RECIPE_TYPE_MAPPING.put("laser_engraver", "激光刻印");
        RECIPE_TYPE_MAPPING.put("mixer", "混合");
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
        String mapped = RECIPE_TYPE_MAPPING.get(path);
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

        String mapped = RECIPE_TYPE_MAPPING.get(path);
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
}
