package com.gali.ae2_auto_pattern_upload.compat;

import com.gali.ae2_auto_pattern_upload.network.PatternUploadUtil;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.gui.input.IClickableIngredientInternal;
import mezz.jei.gui.overlay.IngredientListOverlay;
import mezz.jei.gui.overlay.bookmarks.BookmarkOverlay;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
            List<IRecipeType<?>> candidates = current.getJeiHelpers().getAllRecipeTypes()
                    .filter(type -> type.getRecipeClass().isInstance(recipe))
                    .toList();
            IRecipeType<?> matched = candidates.size() == 1 ? candidates.getFirst()
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

    public static Optional<ITypedIngredient<?>> getIngredientUnderMouse() {
        IJeiRuntime current = runtime;
        if (current == null) {
            return Optional.empty();
        }
        try {
            Optional<ITypedIngredient<?>> ingredient = current.getIngredientListOverlay().getIngredientUnderMouse();
            return ingredient.isPresent() ? ingredient : current.getBookmarkOverlay().getIngredientUnderMouse();
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    /** 按 EAEP 顺序读取鼠标坐标下的 JEI 列表，其次读取普通书签/历史书签覆盖层。 */
    public static Optional<ITypedIngredient<?>> getIngredientUnderMouse(double mouseX, double mouseY) {
        IJeiRuntime current = runtime;
        if (current == null) {
            return Optional.empty();
        }
        Optional<ITypedIngredient<?>> ingredient = getIngredientUnderMouse(
                current.getIngredientListOverlay(), mouseX, mouseY);
        return ingredient.isPresent()
                ? ingredient
                : getIngredientUnderMouse(current.getBookmarkOverlay(), mouseX, mouseY);
    }

    private static Optional<ITypedIngredient<?>> getIngredientUnderMouse(Object overlay, double mouseX, double mouseY) {
        if (overlay instanceof IngredientListOverlay ingredientListOverlay) {
            return ingredientListOverlay.getIngredientUnderMouse(mouseX, mouseY)
                    .<ITypedIngredient<?>>map(IClickableIngredientInternal::getTypedIngredient)
                    .findFirst();
        }
        if (overlay instanceof BookmarkOverlay bookmarkOverlay) {
            return bookmarkOverlay.getIngredientUnderMouse(mouseX, mouseY)
                    .<ITypedIngredient<?>>map(IClickableIngredientInternal::getTypedIngredient)
                    .findFirst();
        }
        return Optional.empty();
    }

    public static String getTypedIngredientDisplayName(Object typed) {
        if (!(typed instanceof ITypedIngredient<?> ingredient) || runtime == null) {
            return "";
        }
        try {
            return getDisplayName(ingredient);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static <T> String getDisplayName(ITypedIngredient<T> ingredient) {
        IIngredientHelper<T> helper = runtime.getIngredientManager().getIngredientHelper(ingredient.getType());
        return helper.getDisplayName(ingredient.getIngredient());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean contains(IRecipeManager manager, IRecipeType<?> type, Object recipe) {
        return manager.createRecipeLookup((IRecipeType) type).includeHidden().get()
                .anyMatch(candidate -> candidate == recipe || Objects.equals(candidate, recipe));
    }
}
