package com.example.ae2_auto_pattern_upload.util;

import appeng.api.AEApi;
import appeng.api.features.ILocatable;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.features.IWirelessTermRegistry;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;

public class FindWirelessTerminal {
    private FindWirelessTerminal() {
    }

    public static final class WirelessTerminalContext {
        private final ItemStack terminalStack;
        private final int slot;
        private final boolean baubleSlot;

        private WirelessTerminalContext(ItemStack terminalStack, int slot, boolean baubleSlot) {
            this.terminalStack = terminalStack;
            this.slot = slot;
            this.baubleSlot = baubleSlot;
        }

        public ItemStack getTerminalStack() {
            return terminalStack;
        }

        public int getSlot() {
            return slot;
        }

        public boolean isBaubleSlot() {
            return baubleSlot;
        }
    }

    @Nullable
    public static WirelessTerminalContext findOpenableWirelessTerminal(EntityPlayer player) {
        if (player == null) {
            return null;
        }

        IWirelessTermRegistry registry = AEApi.instance().registries().wireless();
        if (registry == null) {
            return null;
        }

        WirelessTerminalContext mainInventoryContext = findContextInList(player.inventory.mainInventory, registry, false);
        if (mainInventoryContext != null) {
            return mainInventoryContext;
        }

        return findContextInBaubles(player, registry);
    }

    public static ItemStack findWirelessTerminalItem(EntityPlayer player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }

        IWirelessTermRegistry registry = AEApi.instance().registries().wireless();
        if (registry == null) {
            return ItemStack.EMPTY;
        }

        WirelessTerminalContext openableContext = findOpenableWirelessTerminal(player);
        if (openableContext != null) {
            return openableContext.getTerminalStack();
        }

        ItemStack offHandStack = findInList(player.inventory.offHandInventory, registry);
        if (!offHandStack.isEmpty()) {
            return offHandStack;
        }

        return ItemStack.EMPTY;
    }

    public static boolean hasWirelessTerminalItem(EntityPlayer player) {
        return !findWirelessTerminalItem(player).isEmpty();
    }

    @Nullable
    public static IGridNode getWirelessTerminalNode(ItemStack terminal) {
        IActionHost actionHost = getWirelessTerminalActionHost(terminal);
        return actionHost == null ? null : actionHost.getActionableNode();
    }

    @Nullable
    public static IGrid getWirelessTerminalGrid(ItemStack terminal) {
        IGridNode node = getWirelessTerminalNode(terminal);
        return node == null ? null : node.getGrid();
    }

    @Nullable
    public static IActionHost getWirelessTerminalActionHost(ItemStack terminal) {
        IWirelessTermRegistry registry = AEApi.instance().registries().wireless();
        if (!isWirelessTerminal(terminal, registry)) {
            return null;
        }

        IWirelessTermHandler handler = registry.getWirelessTerminalHandler(terminal);
        if (handler == null) {
            return null;
        }

        String encryptionKey = handler.getEncryptionKey(terminal);
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            return null;
        }

        try {
            long parsedKey = Long.parseLong(encryptionKey);
            ILocatable locatable = AEApi.instance().registries().locatable().getLocatableBy(parsedKey);
            return locatable instanceof IActionHost ? (IActionHost) locatable : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static ItemStack findInList(NonNullList<ItemStack> stacks, IWirelessTermRegistry registry) {
        for (ItemStack stack : stacks) {
            if (isWirelessTerminal(stack, registry)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    @Nullable
    private static WirelessTerminalContext findContextInList(NonNullList<ItemStack> stacks,
                                                             IWirelessTermRegistry registry,
                                                             boolean baubleSlot) {
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            if (isWirelessTerminal(stack, registry)) {
                return new WirelessTerminalContext(stack, slot, baubleSlot);
            }
        }

        return null;
    }

    @Nullable
    private static WirelessTerminalContext findContextInBaubles(EntityPlayer player, IWirelessTermRegistry registry) {
        if (!Loader.isModLoaded("baubles")) {
            return null;
        }

        IBaublesItemHandler baublesHandler = BaublesApi.getBaublesHandler(player);
        if (baublesHandler == null) {
            return null;
        }

        for (int slot = 0; slot < baublesHandler.getSlots(); slot++) {
            ItemStack stack = baublesHandler.getStackInSlot(slot);
            if (isWirelessTerminal(stack, registry)) {
                return new WirelessTerminalContext(stack, slot, true);
            }
        }

        return null;
    }

    private static boolean isWirelessTerminal(ItemStack stack, IWirelessTermRegistry registry) {
        return stack != null && !stack.isEmpty() && registry != null && registry.isWirelessTerminal(stack);
    }
}
