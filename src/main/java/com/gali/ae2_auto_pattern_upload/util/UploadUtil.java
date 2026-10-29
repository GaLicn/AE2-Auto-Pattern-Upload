package com.gali.ae2_auto_pattern_upload.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.gali.ae2_auto_pattern_upload.mixin.ae2.accessor.AEBaseContainerAccessor;
import com.gali.ae2_auto_pattern_upload.mixin.ae2.accessor.ContainerPatternTermAccessor;
import com.glodblock.github.client.gui.container.ContainerFluidPatternEncoder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IMachineSet;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.IInterfaceViewable;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.implementations.ContainerPatternTermEx;
import appeng.container.slot.SlotRestrictedInput;
import appeng.helpers.IInterfaceHost;
import appeng.items.misc.ItemEncodedPattern;
import appeng.util.Platform;

public class UploadUtil {

    private static final Map<UUID, LastUploadedPattern> LAST_UPLOADED_PATTERNS = new HashMap<>();

    /**
     * 判断传入的物品是否为受支持的已编码样板。
     * 兼容 AE2 原版样板以及继承自 ItemEncodedPattern 的扩展样板（如终极样板）。
     *
     * @param stack 需要检查的物品栈
     * @return 如果是受支持的样板则返回 true，否则返回 false
     */
    public static boolean isSupportedPattern(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        if (AEApi.instance()
            .definitions()
            .items()
            .encodedPattern()
            .isSameAs(stack)) {
            return true;
        }
        return stack.getItem() instanceof ItemEncodedPattern;
    }

    /**
     * 从容器中解析出对应的终端（IActionHost）。
     * 支持原版样板终端、扩展样板终端以及流体样板终端。
     *
     * @param container 当前打开的容器
     * @return 解析出的终端实例，如果不支持该容器则返回 null
     */
    public static IActionHost resolveTerminal(Container container) {
        if (container instanceof ContainerPatternTerm term) {
            return term.getPatternTerminal();
        }
        if (container instanceof ContainerPatternTermEx termEx) {
            return termEx.getPatternTerminal();
        }
        if (container instanceof ContainerFluidPatternEncoder) {
            return ((AEBaseContainerAccessor) container).invokeGetActionHost();
        }
        return null;
    }

    /**
     * 从容器中解析出样板输出槽位。
     *
     * @param container 当前打开的容器
     * @return 解析出的输出槽位，如果不支持该容器则返回 null
     */
    public static SlotRestrictedInput resolveOutputSlot(Container container) {
        if (container instanceof ContainerPatternTerm term) {
            return ((ContainerPatternTermAccessor) term).getPatternSlotOUT();
        }
        if (container instanceof ContainerPatternTermEx termEx) {
            return ((ContainerPatternTermAccessor) termEx).getPatternSlotOUT();
        }
        if (container instanceof ContainerFluidPatternEncoder fluidEncoder) {
            IInventory inventory = fluidEncoder.getTile()
                .getInventory();
            for (Object slotObject : fluidEncoder.inventorySlots) {
                if (slotObject instanceof SlotRestrictedInput slot && slot.inventory == inventory
                    && slot.getSlotIndex() == 1) {
                    return slot;
                }
            }
        }
        return null;
    }

    /**
     * 尝试将样板插入到指定的合成供应器（如 ME 接口）中。
     * 兼容 AE2 原版接口、GT5/Programmable Hatches 接口以及普通容器。
     *
     * @param provider 目标合成供应器
     * @param pattern  需要插入的样板
     * @return 如果成功插入则返回 true，否则返回 false
     */
    public static boolean insertPatternIntoProvider(ICraftingProvider provider, ItemStack pattern) {
        // 优先处理 AE2 标准接口（IInterfaceHost），确保只插入到编码样板槽（patterns）
        if (provider instanceof IInterfaceHost host) {
            IInventory patterns = host.getPatterns();
            if (patterns != null) {
                // 计算实际可用的槽位数量（基于升级卡）
                int availableSlots = host.rows() * host.rowSize();
                if (insertIntoPatternInventory(patterns, pattern, availableSlots)) {
                    host.saveChanges();
                    return true;
                }
            }
            // 接口的样板槽满了，直接返回 false，不要尝试放到物品槽
            return false;
        }

        // 处理 GT5 和 Programmable Hatches 的接口（IInterfaceViewable）
        if (provider instanceof IInterfaceViewable viewable) {
            IInventory patterns = viewable.getPatterns();
            if (patterns != null) {
                // 计算实际可用的槽位数量
                int availableSlots = viewable.rows() * viewable.rowSize();
                if (insertIntoPatternInventory(patterns, pattern, availableSlots)) {
                    // IInterfaceViewable 没有 saveChanges 方法，直接标记脏数据
                    patterns.markDirty();
                    return true;
                }
            }
            // 样板槽满了，直接返回 false
            return false;
        }

        if (provider instanceof IInventory inventory) {
            return insertIntoInventory(inventory, pattern);
        }

        return false;
    }

    public static void rememberLastUploadedPattern(EntityPlayer player, ICraftingProvider provider, ItemStack pattern) {
        if (player == null || provider == null || pattern == null) {
            return;
        }

        LAST_UPLOADED_PATTERNS
            .put(player.getUniqueID(), new LastUploadedPattern(System.identityHashCode(provider), pattern.copy()));
    }

    public static ItemStack takeLastUploadedPattern(EntityPlayer player, IGrid grid) {
        if (player == null || grid == null) {
            return null;
        }

        LastUploadedPattern record = LAST_UPLOADED_PATTERNS.get(player.getUniqueID());
        if (record == null) {
            return null;
        }

        ICraftingProvider provider = findProviderById(grid, record.providerId);
        if (provider == null) {
            LAST_UPLOADED_PATTERNS.remove(player.getUniqueID());
            return null;
        }

        ItemStack removed = removePatternFromProvider(provider, record.pattern);
        if (removed != null) {
            LAST_UPLOADED_PATTERNS.remove(player.getUniqueID());
        }
        return removed;
    }

    public static ICraftingProvider findProviderById(IGrid grid, long providerId) {
        if (grid == null) {
            return null;
        }

        for (Class<? extends IGridHost> hostClass : grid.getMachinesClasses()) {
            if (!ICraftingProvider.class.isAssignableFrom(hostClass)) {
                continue;
            }
            IMachineSet machines = grid.getMachines(hostClass);
            if (machines == null) {
                continue;
            }
            for (IGridNode machineNode : machines) {
                if (machineNode == null) {
                    continue;
                }
                Object machine = machineNode.getMachine();
                if (machine instanceof ICraftingProvider provider && System.identityHashCode(machine) == providerId) {
                    return provider;
                }
            }
        }
        return null;
    }

    public static ItemStack removePatternFromProvider(ICraftingProvider provider, ItemStack pattern) {
        IInventory patterns = getPatternInventory(provider);
        if (patterns == null || pattern == null) {
            return null;
        }

        for (int i = 0; i < patterns.getSizeInventory(); i++) {
            ItemStack stack = patterns.getStackInSlot(i);
            if (isSamePatternIgnoringAuthor(stack, pattern)) {
                ItemStack removed = stack.copy();
                patterns.setInventorySlotContents(i, null);
                patterns.markDirty();
                saveProvider(provider);
                return removed;
            }
        }
        return null;
    }

    /**
     * 将样板插入到编码样板槽（patterns）中。
     * 确保只插入到允许放置编码样板的槽位。
     *
     * @param patterns 样板槽 Inventory
     * @param pattern  需要插入的样板
     * @param maxSlots 最大可用槽位数量（受升级卡限制）
     * @return 如果成功插入则返回 true，否则返回 false
     */
    public static boolean insertIntoPatternInventory(IInventory patterns, ItemStack pattern, int maxSlots) {
        if (patterns == null) {
            return false;
        }

        int limit = Math.min(maxSlots, patterns.getSizeInventory());
        for (int i = 0; i < limit; i++) {
            ItemStack slot = patterns.getStackInSlot(i);
            if (slot == null || slot.stackSize <= 0) {
                // 检查该槽位是否允许放置编码样板
                if (patterns.isItemValidForSlot(i, pattern)) {
                    ItemStack copy = pattern.copy();
                    copy.stackSize = 1;
                    patterns.setInventorySlotContents(i, copy);
                    patterns.markDirty();
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 判断样板是否可以安全放入目标供应器。
     * 会先按目标样板槽 Inventory 的合法性判断，再拦截普通接口/不支持流体的 PH 总线接收流体样板的场景。
     *
     * @param provider 目标供应器
     * @param pattern  需要上传的样板
     * @return 如果目标可以接收该样板则返回 true，否则返回 false
     */
    public static boolean canProviderAcceptPattern(ICraftingProvider provider, ItemStack pattern) {
        if (provider == null || pattern == null) {
            return false;
        }

        boolean nonItemPattern = isNonItemPattern(pattern);
        if (isFluidInterfaceProvider(provider)) {
            return hasValidPatternSlot(resolvePatternsInventory(provider), pattern, 64);
        }

        if (nonItemPattern && !canProviderAcceptFluidPattern(provider)) {
            return false;
        }

        if (provider instanceof IInterfaceHost host) {
            return hasValidPatternSlot(host.getPatterns(), pattern, host.rows() * host.rowSize());
        }

        if (provider instanceof IInterfaceViewable viewable) {
            return hasValidPatternSlot(viewable.getPatterns(), pattern, viewable.rows() * viewable.rowSize());
        }

        if (provider instanceof IInventory inventory) {
            if (nonItemPattern) {
                return false;
            }
            return hasValidPatternSlot(inventory, pattern, inventory.getSizeInventory());
        }

        return false;
    }

    /**
     * 将当前编码样板退回为空白样板。
     * 用于重复样板或非法上传时恢复终端输出槽状态。
     *
     * @param outputSlot 输出槽中的编码样板
     * @return 空白样板，如果无法创建则返回 null
     */
    public static ItemStack createBlankPatternFromEncoded(ItemStack outputSlot) {
        if (outputSlot == null) {
            return null;
        }
        ItemStack blankPattern = AEApi.instance()
            .definitions()
            .materials()
            .blankPattern()
            .maybeStack(1)
            .orNull();
        if (blankPattern == null) {
            return null;
        }
        blankPattern.stackSize = Math.max(1, outputSlot.stackSize);
        return blankPattern;
    }

    /**
     * 将编码样板退回为空白样板并返还到 AE 网络。
     * 若返还失败，则退回终端输出槽，避免物品丢失。
     *
     * @param grid           当前 AE 网络
     * @param player         当前玩家
     * @param terminal       当前终端
     * @param outputSlot     终端输出槽
     * @param encodedPattern 原始编码样板
     * @return 成功处理后返回 true，否则返回 false
     */
    public static boolean returnBlankPatternToNetwork(IGrid grid, net.minecraft.entity.player.EntityPlayer player,
        IActionHost terminal, SlotRestrictedInput outputSlot, ItemStack encodedPattern) {
        if (grid == null || outputSlot == null || encodedPattern == null) {
            return false;
        }

        ItemStack blankPattern = createBlankPatternFromEncoded(encodedPattern);
        if (blankPattern == null) {
            return false;
        }

        IStorageGrid storageGrid = grid.getCache(IStorageGrid.class);
        if (storageGrid == null) {
            outputSlot.putStack(blankPattern);
            return true;
        }

        IAEItemStack toInject = AEApi.instance()
            .storage()
            .createItemStack(blankPattern);
        BaseActionSource source = new PlayerSource(player, terminal);
        IAEItemStack remaining = storageGrid.getItemInventory()
            .injectItems(toInject, Actionable.MODULATE, source);

        if (remaining == null || remaining.getStackSize() <= 0) {
            outputSlot.putStack(null);
            return true;
        }

        ItemStack fallback = remaining.getItemStack();
        if (fallback != null) {
            outputSlot.putStack(fallback.copy());
            return true;
        }

        outputSlot.putStack(blankPattern);
        return true;
    }

    /**
     * 判断当前网络中是否已经存在相同的样板。
     * 比较使用 AE2 的精确 ItemStack 比较，包含物品、damage 和 NBT。
     *
     * @param grid    当前 AE 网络
     * @param pattern 需要检查的样板
     * @param world   当前世界
     * @return 如果网络中已经存在相同样板则返回 true，否则返回 false
     */
    public static boolean hasSamePatternInNetwork(IGrid grid, ItemStack pattern, World world) {
        if (grid == null || pattern == null || !(pattern.getItem() instanceof ICraftingPatternItem patternItem)) {
            return false;
        }

        ICraftingPatternDetails currentDetails = patternItem.getPatternForItem(pattern, world);
        if (currentDetails == null) {
            return false;
        }

        ICraftingGrid craftingGrid = grid.getCache(ICraftingGrid.class);
        if (craftingGrid == null) {
            return false;
        }

        ImmutableMap<IAEStack<?>, ImmutableList<ICraftingPatternDetails>> patterns = craftingGrid
            .getCraftingMultiPatterns();
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }

        for (ImmutableList<ICraftingPatternDetails> detailsList : patterns.values()) {
            if (detailsList == null) {
                continue;
            }
            for (ICraftingPatternDetails details : detailsList) {
                if (details == null) {
                    continue;
                }
                ItemStack existing = details.getPattern();
                if (isSamePatternIgnoringAuthor(existing, pattern)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isSamePatternIgnoringAuthor(ItemStack left, ItemStack right) {
        if (left == null || right == null) {
            return false;
        }

        ItemStack normalizedLeft = left.copy();
        ItemStack normalizedRight = right.copy();

        removeAuthorTag(normalizedLeft);
        removeAuthorTag(normalizedRight);

        return Platform.isSameItemPrecise(normalizedLeft, normalizedRight);
    }

    private static void removeAuthorTag(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return;
        }

        stack.getTagCompound()
            .removeTag("author");
        if (stack.getTagCompound()
            .hasNoTags()) {
            stack.setTagCompound(null);
        }
    }

    private static boolean hasValidPatternSlot(IInventory patterns, ItemStack pattern, int maxSlots) {
        if (patterns == null || pattern == null) {
            return false;
        }
        int limit = Math.min(maxSlots, patterns.getSizeInventory());
        for (int i = 0; i < limit; i++) {
            if (patterns.isItemValidForSlot(i, pattern)) {
                return true;
            }
        }
        return false;
    }

    private static IInventory getPatternInventory(ICraftingProvider provider) {
        if (provider instanceof IInterfaceHost host) {
            return host.getPatterns();
        }
        if (provider instanceof IInterfaceViewable viewable) {
            return viewable.getPatterns();
        }
        if (provider instanceof IInventory inventory) {
            return inventory;
        }
        return resolvePatternsInventory(provider);
    }

    private static IInventory resolvePatternsInventory(ICraftingProvider provider) {
        try {
            Object result = provider.getClass()
                .getMethod("getPatterns")
                .invoke(provider);
            return result instanceof IInventory ? (IInventory) result : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isNonItemPattern(ItemStack pattern) {
        if (pattern == null || !(pattern.getItem() instanceof ICraftingPatternItem patternItem)) {
            return false;
        }

        ICraftingPatternDetails details = patternItem.getPatternForItem(pattern, null);
        if (details == null) {
            return hasNonItemEntries(pattern);
        }

        return hasNonItemStacks(details.getAEInputs()) || hasNonItemStacks(details.getAEOutputs());
    }

    private static boolean canProviderAcceptFluidPattern(ICraftingProvider provider) {
        if (provider instanceof IInterfaceHost) {
            return false;
        }
        if (provider instanceof IInterfaceViewable) {
            try {
                Object result = provider.getClass()
                    .getMethod("supportsFluids")
                    .invoke(provider);
                return result instanceof Boolean && (Boolean) result;
            } catch (Throwable ignored) {
                return false;
            }
        }
        return true;
    }

    private static boolean isFluidInterfaceProvider(ICraftingProvider provider) {
        if (provider == null) {
            return false;
        }
        String className = provider.getClass()
            .getName();
        return className.contains("TileFluidInterface") || className.contains("PartFluidInterface");
    }

    private static boolean hasNonItemEntries(ItemStack pattern) {
        if (pattern == null || !pattern.hasTagCompound()) {
            return false;
        }
        return hasGenericAeStackEntries(pattern.getTagCompound(), "in")
            || hasGenericAeStackEntries(pattern.getTagCompound(), "out");
    }

    private static boolean hasGenericAeStackEntries(net.minecraft.nbt.NBTTagCompound tag, String key) {
        if (tag == null || !tag.hasKey(key, 9)) {
            return false;
        }
        net.minecraft.nbt.NBTTagList list = tag.getTagList(key, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            net.minecraft.nbt.NBTTagCompound entry = list.getCompoundTagAt(i);
            IAEStack<?> stack = Platform.readStackNBT(entry, true);
            if (stack != null && !(stack instanceof IAEItemStack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNonItemStacks(IAEStack<?>[] stacks) {
        if (stacks == null) {
            return false;
        }
        for (IAEStack<?> stack : stacks) {
            if (stack != null && !(stack instanceof IAEItemStack)) {
                return true;
            }
        }
        return false;
    }

    private static void saveProvider(ICraftingProvider provider) {
        if (provider instanceof IInterfaceHost host) {
            host.saveChanges();
        }
    }

    /**
     * 将样板插入到普通的物品槽（Inventory）中。
     * 遍历所有槽位，寻找空槽位进行插入。
     *
     * @param inventory 目标 Inventory
     * @param pattern   需要插入的样板
     * @return 如果成功插入则返回 true，否则返回 false
     */
    private static boolean insertIntoInventory(IInventory inventory, ItemStack pattern) {
        if (inventory == null) {
            return false;
        }

        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack slot = inventory.getStackInSlot(i);
            if (slot == null || slot.stackSize <= 0) {
                ItemStack copy = pattern.copy();
                copy.stackSize = 1;
                inventory.setInventorySlotContents(i, copy);
                inventory.markDirty();
                return true;
            }
        }

        return false;
    }

    private static class LastUploadedPattern {

        private final long providerId;
        private final ItemStack pattern;

        private LastUploadedPattern(long providerId, ItemStack pattern) {
            this.providerId = providerId;
            this.pattern = pattern;
        }
    }
}
