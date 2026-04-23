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

    public static ItemStack findWirelessTerminalItem(EntityPlayer player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }

        IWirelessTermRegistry registry = AEApi.instance().registries().wireless();
        if (registry == null) {
            return ItemStack.EMPTY;
        }

        ItemStack mainInventoryStack = findInList(player.inventory.mainInventory, registry);
        if (!mainInventoryStack.isEmpty()) {
            return mainInventoryStack;
        }

        ItemStack offHandStack = findInList(player.inventory.offHandInventory, registry);
        if (!offHandStack.isEmpty()) {
            return offHandStack;
        }

        return findInBaubles(player, registry);
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

    private static ItemStack findInList(NonNullList<ItemStack> stacks, IWirelessTermRegistry registry) {
        for (ItemStack stack : stacks) {
            if (isWirelessTerminal(stack, registry)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack findInBaubles(EntityPlayer player, IWirelessTermRegistry registry) {
        if (!Loader.isModLoaded("baubles")) {
            return ItemStack.EMPTY;
        }

        IBaublesItemHandler baublesHandler = BaublesApi.getBaublesHandler(player);
        if (baublesHandler == null) {
            return ItemStack.EMPTY;
        }

        for (int slot = 0; slot < baublesHandler.getSlots(); slot++) {
            ItemStack stack = baublesHandler.getStackInSlot(slot);
            if (isWirelessTerminal(stack, registry)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    @Nullable
    private static IActionHost getWirelessTerminalActionHost(ItemStack terminal) {
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

    private static boolean isWirelessTerminal(ItemStack stack, IWirelessTermRegistry registry) {
        return stack != null && !stack.isEmpty() && registry != null && registry.isWirelessTerminal(stack);
    }
}
