package com.gali.ae2_auto_pattern_upload.network;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionHost;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.menu.AEBaseMenu;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.gali.ae2_auto_pattern_upload.mixin.PatternEncodingTermMenuAccessor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class PatternUploadUtil {
    private static volatile String lastProviderSearchKey;

    private PatternUploadUtil() {
    }

    public static void setLastProviderSearchKey(String key) {
        lastProviderSearchKey = key == null || key.isBlank() ? null : key.trim();
    }

    public static String consumeLastProviderSearchKey() {
        String key = lastProviderSearchKey;
        lastProviderSearchKey = null;
        return key;
    }

    public static List<ProviderEntry> listAvailableProviders(PatternEncodingTermMenu menu) {
        List<ProviderEntry> result = new ArrayList<>();
        IGrid grid = getGrid(menu);
        if (grid == null) {
            return result;
        }

        // 按 AE2 当前网络中的机器顺序生成稳定索引，客户端只回传这个索引。
        for (Class<?> machineClass : grid.getMachineClasses()) {
            if (!PatternContainer.class.isAssignableFrom(machineClass)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Class<? extends PatternContainer> providerClass = (Class<? extends PatternContainer>) machineClass;
            for (PatternContainer provider : grid.getActiveMachines(providerClass)) {
                if (provider == null || !provider.isVisibleInTerminal()) {
                    continue;
                }
                InternalInventory inventory = provider.getTerminalPatternInventory();
                int emptySlots = countEmptySlots(inventory);
                if (emptySlots > 0) {
                    result.add(new ProviderEntry(provider, providerName(provider), emptySlots));
                }
            }
        }
        return result;
    }

    public static boolean uploadByIndex(ServerPlayer player, PatternEncodingTermMenu menu, int index) {
        if (player == null || menu == null || index < 0) {
            return false;
        }
        List<ProviderEntry> providers = listAvailableProviders(menu);
        if (index >= providers.size()) {
            return false;
        }

        ItemStack encodedPattern = getEncodedPattern(menu);
        if (encodedPattern.isEmpty() || !PatternDetailsHelper.isEncodedPattern(encodedPattern)) {
            return false;
        }

        PatternContainer provider = providers.get(index).provider();
        InternalInventory inventory = provider.getTerminalPatternInventory();
        if (inventory == null) {
            return false;
        }
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (!inventory.getStackInSlot(slot).isEmpty()) {
                continue;
            }
            // 服务端再次校验编码样板，避免客户端伪造普通物品上传。
            ItemStack copy = encodedPattern.copyWithCount(1);
            inventory.setItemDirect(slot, copy);
            ((PatternEncodingTermMenuAccessor) menu).ae2apu$getEncodedPatternSlot().set(ItemStack.EMPTY);
            return true;
        }
        return false;
    }

    private static ItemStack getEncodedPattern(PatternEncodingTermMenu menu) {
        return ((PatternEncodingTermMenuAccessor) menu).ae2apu$getEncodedPatternSlot().getItem();
    }

    private static IGrid getGrid(PatternEncodingTermMenu menu) {
        try {
            if (menu instanceof AEBaseMenu baseMenu
                    && baseMenu.getTarget() instanceof IActionHost host
                    && host.getActionableNode() != null) {
                return host.getActionableNode().getGrid();
            }
        } catch (Throwable ignored) {
            // 菜单关闭或网络断开时，按无可用供应器处理。
        }
        return null;
    }

    private static int countEmptySlots(InternalInventory inventory) {
        if (inventory == null) {
            return 0;
        }
        int empty = 0;
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (inventory.getStackInSlot(slot).isEmpty()) {
                empty++;
            }
        }
        return empty;
    }

    private static Component providerName(PatternContainer provider) {
        try {
            if (provider.getTerminalGroup() != null) {
                return provider.getTerminalGroup().name();
            }
        } catch (Throwable ignored) {
            // 某些供应器没有终端分组，使用通用名称。
        }
        return Component.translatable("ae2_auto_pattern_upload.provider.default");
    }

    public record ProviderEntry(PatternContainer provider, Component name, int emptySlots) {
    }
}
