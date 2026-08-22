package com.gali.compat;

import com.gali.util.RecipeTypeNameConfig;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IBookmarkOverlay;
import mezz.jei.api.runtime.IIngredientListOverlay;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.gui.input.IClickableIngredientInternal;
import mezz.jei.gui.overlay.IngredientListOverlay;
import mezz.jei.gui.overlay.bookmarks.BookmarkOverlay;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** 延迟保存 JEI Runtime，兼容物品列表、普通书签和历史书签覆盖层。 */
public final class JeiRuntimeCompat {
    private static volatile IJeiRuntime runtime;

    private JeiRuntimeCompat() {}

    public static void setRuntime(IJeiRuntime value) { runtime = value; }

    public static String getRecipeCategoryTitle(Object recipe) {
        IJeiRuntime current = runtime;
        if (current == null || recipe == null) return null;
        try {
            IRecipeManager manager = current.getRecipeManager();
            List<RecipeType<?>> candidates = current.getJeiHelpers().getAllRecipeTypes()
                    .filter(type -> type.getRecipeClass().isInstance(recipe)).toList();
            RecipeType<?> matched = candidates.size() == 1 ? candidates.get(0)
                    : candidates.stream().filter(type -> contains(manager, type, recipe)).findFirst().orElse(null);
            if (matched == null) return null;
            String title = manager.getRecipeCategory(matched).getTitle().getString();
            return RecipeTypeNameConfig.resolveRecipeTypeSearchKey(matched.getUid(), title);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Optional<ITypedIngredient<?>> getIngredientUnderMouse() {
        try {
            Class<?> mouseUtil = Class.forName("mezz.jei.gui.input.MouseUtil");
            double x = ((Number) mouseUtil.getMethod("getX").invoke(null)).doubleValue();
            double y = ((Number) mouseUtil.getMethod("getY").invoke(null)).doubleValue();
            return getIngredientUnderMouse(x, y);
        } catch (Throwable ignored) {}

        IJeiRuntime current = runtime;
        if (current == null) return Optional.empty();
        try {
            IIngredientListOverlay list = current.getIngredientListOverlay();
            if (list != null && list.getIngredientUnderMouse().isPresent()) {
                return list.getIngredientUnderMouse().map(value -> (ITypedIngredient<?>) value);
            }
            IBookmarkOverlay bookmarks = current.getBookmarkOverlay();
            if (bookmarks != null && bookmarks.getIngredientUnderMouse().isPresent()) {
                return bookmarks.getIngredientUnderMouse().map(value -> (ITypedIngredient<?>) value);
            }
        } catch (Throwable ignored) {}
        return Optional.empty();
    }

    /** 优先查询 JEI 物品列表，再查询普通书签及历史书签覆盖层。 */
    public static Optional<ITypedIngredient<?>> getIngredientUnderMouse(double x, double y) {
        IJeiRuntime current = runtime;
        if (current == null) return Optional.empty();
        Optional<ITypedIngredient<?>> result = getOverlayIngredient(current.getIngredientListOverlay(), x, y);
        return result.isPresent() ? result : getOverlayIngredient(current.getBookmarkOverlay(), x, y);
    }

    private static Optional<ITypedIngredient<?>> getOverlayIngredient(Object overlay, double x, double y) {
        if (overlay instanceof IngredientListOverlay list) {
            return list.getIngredientUnderMouse(x, y)
                    .<ITypedIngredient<?>>map(IClickableIngredientInternal::getTypedIngredient).findFirst();
        }
        if (overlay instanceof BookmarkOverlay bookmarks) {
            return bookmarks.getIngredientUnderMouse(x, y)
                    .<ITypedIngredient<?>>map(IClickableIngredientInternal::getTypedIngredient).findFirst();
        }
        return Optional.empty();
    }

    public static String getTypedIngredientDisplayName(ITypedIngredient<?> ingredient) {
        if (runtime == null || ingredient == null) return "";
        try {
            IIngredientHelper<Object> helper = (IIngredientHelper<Object>) runtime.getIngredientManager()
                    .getIngredientHelper(ingredient.getType());
            return helper.getDisplayName(ingredient.getIngredient());
        } catch (Throwable ignored) {
            return "";
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean contains(IRecipeManager manager, RecipeType<?> type, Object recipe) {
        return manager.createRecipeLookup((RecipeType) type).includeHidden().get()
                .anyMatch(candidate -> candidate == recipe || Objects.equals(candidate, recipe));
    }
}
