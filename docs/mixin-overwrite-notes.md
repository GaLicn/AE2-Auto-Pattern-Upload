# Mixin Overwrite 记录

## ContainerPatternTermMixin

- 文件: [ContainerPatternTermMixin.java](file:///c:/Users/C-H716/Desktop/mods/AE2-Auto-Pattern-Upload/src/main/java/com/gali/ae2_auto_pattern_upload/mixin/ae2/ContainerPatternTermMixin.java)
- 目标类: `appeng.container.implementations.ContainerPatternTerm`
- 重写方法:
  - `canMultiplyOrDivide(IAEStackInventory inventory, int mult)`
  - `multiplyOrDivideStacksInternal(IAEStackInventory inventory, int mult)`
- 目的:
  - 在样板终端进行批量乘除判断和实际修改时跳过编程电路
- 说明:
  - 当前实现已对齐新版 AE2 对应方法的整体结构
  - 仅额外插入了 `CircuitUtils.isProgrammingCircuit(...)` 判断，用于忽略编程电路

## PatternMultiplierHelperMixin

- 文件: [PatternMultiplierHelperMixin.java](file:///c:/Users/C-H716/Desktop/mods/AE2-Auto-Pattern-Upload/src/main/java/com/gali/ae2_auto_pattern_upload/mixin/ae2/PatternMultiplierHelperMixin.java)
- 目标类: `appeng.util.PatternMultiplierHelper`
- 重写方法:
  - `getMaxBitDivider(ICraftingPatternDetails details)`
  - `applyModification(ItemStack stack, int bitMultiplier)`
- 目的:
  - 在接口样板倍增与缩减时跳过编程电路，避免电路数量被一同修改
- 说明:
  - 当前实现已对齐新版 AE2 的位运算与 NBT 处理方式
  - 额外保留了 `CircuitUtils.isProgrammingCircuit(...)` 与 `CircuitUtils.isProgrammingCircuitFromNBT(...)` 判断

## QuantumClusterMixin

- 文件: [QuantumClusterMixin.java](file:///c:/Users/C-H716/Desktop/mods/AE2-Auto-Pattern-Upload/src/main/java/com/gali/ae2_auto_pattern_upload/mixin/ae2/QuantumClusterMixin.java)
- 目标类: `appeng.me.cluster.implementations.QuantumCluster`
- 重写方法:
  - `isActive()`
- 目的:
  - 修改量子环激活判定逻辑，使其不再依赖本端供电检查
- 说明:
  - 当前逻辑仅在结构未销毁、已注册且存在量子纠缠奇点时返回激活状态
  - 目标效果是跨维度场景下即使一端暂时无电也保持连接判定

## NEIShortcutInputHandlerMixin

- 文件: [NEIShortcutInputHandlerMixin.java](file:///c:/Users/C-H716/Desktop/mods/AE2-Auto-Pattern-Upload/src/main/java/com/gali/ae2_auto_pattern_upload/mixin/nei/NEIShortcutInputHandlerMixin.java)
- 目标类: `codechicken.nei.api.ShortcutInputHandler`
- 重写方法:
  - `saveRecipeInBookmark(ItemStack stackover, boolean saveIngredients, boolean saveStackSize)`
- 目的:
  - 让每个通过 Shift+A 收藏的配方拥有独立书签分组，而不是复用默认分组
- 说明:
  - 当前实现会在保存配方时调用 `generateNewGroupId()` 创建新的书签组
  - 对仅保存物品的逻辑仍保留原有默认分组行为
