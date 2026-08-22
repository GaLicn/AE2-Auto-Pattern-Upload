package com.gali.ae2_auto_pattern_upload.compat;

import com.gali.ae2_auto_pattern_upload.network.PatternUploadUtil;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;

import java.util.List;
import java.util.Objects;

/** 通过 JEI 运行时读取实际分类标题，避免依赖配方实现类名。 */
public final class JeiRuntimeCompat {
    private static volatile IJeiRuntime runtime;

    private JeiRuntimeCompat() {
    }

    public static void setRuntime(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    public static String getRecipeCategoryTitle(Object recipe) {
        IJeiRuntime current = runtime;
        if (current == null || recipe == null) {
            return null;
        }
        try {
            IRecipeManager manager = current.getRecipeManager();
            List<RecipeType<?>> candidates = current.getJeiHelpers().getAllRecipeTypes()
                    .filter(type -> type.getRecipeClass().isInstance(recipe))
                    .toList();
            RecipeType<?> matched = candidates.size() == 1 ? candidates.getFirst()
                    : candidates.stream().filter(type -> contains(manager, type, recipe)).findFirst().orElse(null);
            if (matched == null) {
                return null;
            }
            String title = manager.getRecipeCategory(matched).getTitle().getString();
            return PatternUploadUtil.resolveRecipeTypeSearchKey(matched.getUid(), title);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean contains(IRecipeManager manager, RecipeType<?> type, Object recipe) {
        return manager.createRecipeLookup((RecipeType) type).includeHidden().get()
                .anyMatch(candidate -> candidate == recipe || Objects.equals(candidate, recipe));
    }
}
