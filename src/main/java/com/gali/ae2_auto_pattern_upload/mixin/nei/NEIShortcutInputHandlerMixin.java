package com.gali.ae2_auto_pattern_upload.mixin.nei;

import java.awt.Point;
import java.util.Collections;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.ItemPanels;
import codechicken.nei.ItemQuantityField;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.api.ShortcutInputHandler;
import codechicken.nei.bookmark.BookmarkGrid;
import codechicken.nei.recipe.Recipe;
import codechicken.nei.recipe.Recipe.RecipeId;
import codechicken.nei.recipe.StackInfo;

/**
 * 为 NEI 书签配方保存功能创建独立分组
 */
@Mixin(value = ShortcutInputHandler.class, remap = false)
public abstract class NEIShortcutInputHandlerMixin {

    @Shadow
    private static Recipe getFocusedRecipe(ItemStack stackover, int mousex, int mousey, boolean useFavorites) {
        return null;
    }

    /**
     * @author C-H716
     * @reason 为每个Shift+A收藏的配方创建独立分组
     */
    @Overwrite
    // 保存书签配方时为每个配方创建独立分组
    private static boolean saveRecipeInBookmark(ItemStack stackover, boolean saveIngredients, boolean saveStackSize) {
        final Point mousePos = GuiDraw.getMousePosition();

        // If no slot was removed from the bookmark panel on click:
        if (!ItemPanels.bookmarkPanel.removeSlot(mousePos.x, mousePos.y, saveIngredients)) {
            final Recipe recipe = getFocusedRecipe(stackover, mousePos.x, mousePos.y, saveIngredients);

            // --- Case 1: saving a recipe (with ingredients) ---
            if (recipe != null && saveIngredients) {
                // 为每个配方生成新的组ID
                // 生成新的书签分组并返回组标识
                int groupId = generateNewGroupId();

                // If the recipe is not already bookmarked, add it
                if (!ItemPanels.bookmarkPanel.removeRecipe(recipe.getRecipeId(), groupId)) {
                    final Recipe singleRecipe = Recipe.of(
                        Collections.singletonList(recipe.getResult(stackover)),
                        recipe.getHandlerName(),
                        recipe.getIngredients());
                    singleRecipe.setCustomRecipeId(recipe.getRecipeId());

                    ItemPanels.bookmarkPanel.addRecipe(
                        singleRecipe,
                        // Determine if stack size should be saved
                        saveStackSize
                            || NEIClientConfig.getBooleanSetting("inventory.bookmarks.bookmarkRecipeWithCount") ? 1 : 0,
                        groupId); // 使用新组ID
                }

                // --- Case 2: saving an item only ---
            } else {
                final RecipeId recipeId = recipe != null ? recipe.getRecipeId() : null;

                // Determine if the item is associated with a recipe and should be saved with it
                final boolean existsRecipe = recipeId != null
                    && ItemPanels.bookmarkPanel.existsRecipe(recipeId, BookmarkGrid.DEFAULT_GROUP_ID)
                    || NEIClientConfig.getBooleanSetting("inventory.bookmarks.bookmarkItemsWithRecipe");

                // If the item was not removed from the bookmarks, add it
                if (!ItemPanels.bookmarkPanel
                    .removeItem(stackover, existsRecipe ? recipeId : null, BookmarkGrid.DEFAULT_GROUP_ID)) {

                    // Determine whether to preserve the item count
                    saveStackSize = saveStackSize || saveIngredients
                        || ItemPanels.bookmarkPanel.existsRecipe(recipeId, BookmarkGrid.DEFAULT_GROUP_ID);

                    if (!saveStackSize) {
                        stackover = StackInfo.withAmount(stackover, 0);
                    } else if (ItemPanels.itemPanel.containsWithSubpanels(mousePos.x, mousePos.y)) {
                        stackover = ItemQuantityField.prepareStackWithQuantity(stackover, 0);
                    }

                    ItemPanels.bookmarkPanel
                        .addItem(stackover, existsRecipe ? recipeId : null, BookmarkGrid.DEFAULT_GROUP_ID);
                }

            }
        }

        return true;
    }

    /**
     * 生成新的组ID并创建组
     */
    private static int generateNewGroupId() {
        // 创建新的书签组并返回组ID
        return ItemPanels.bookmarkPanel.getGrid()
            .addGroup(
                new codechicken.nei.bookmark.BookmarkGroup(codechicken.nei.BookmarkPanel.BookmarkViewMode.DEFAULT));
    }
}
