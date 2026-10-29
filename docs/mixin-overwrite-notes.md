# Mixin Overwrite 记录

## NEIShortcutInputHandlerMixin

- 文件: [NEIShortcutInputHandlerMixin.java](../src/main/java/com/gali/ae2_auto_pattern_upload/mixin/nei/NEIShortcutInputHandlerMixin.java)
- 目标类: `codechicken.nei.api.ShortcutInputHandler`
- 重写方法:
  - `saveRecipeInBookmark(ItemStack stackover, boolean saveIngredients, boolean saveStackSize)`
- 目的:
  - 让每个通过 Shift+A 收藏的配方拥有独立书签分组，而不是复用默认分组
- 说明:
  - 当前实现会在保存配方时调用 `generateNewGroupId()` 创建新的书签组
  - 对仅保存物品的逻辑仍保留原有默认分组行为
