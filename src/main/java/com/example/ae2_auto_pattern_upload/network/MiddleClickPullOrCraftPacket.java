package com.example.ae2_auto_pattern_upload.network;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.container.implementations.ContainerCraftAmount;
import appeng.core.sync.GuiBridge;
import appeng.me.helpers.PlayerSource;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import com.example.ae2_auto_pattern_upload.util.FindWirelessTerminal;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.IOException;
import java.util.Collection;

public class MiddleClickPullOrCraftPacket implements IMessage {
    private ItemStack stack = ItemStack.EMPTY;

    public MiddleClickPullOrCraftPacket() {
    }

    public MiddleClickPullOrCraftPacket(ItemStack stack) {
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        try {
            this.stack = packetBuffer.readItemStack();
        } catch (IOException e) {
            this.stack = ItemStack.EMPTY;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        packetBuffer.writeItemStack(this.stack == null ? ItemStack.EMPTY : this.stack);
    }

    public static class Handler implements IMessageHandler<MiddleClickPullOrCraftPacket, IMessage> {
        @Override
        public IMessage onMessage(MiddleClickPullOrCraftPacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) {
                return null;
            }

            player.getServerWorld().addScheduledTask(() -> handle(player, message));
            return null;
        }

        private static void handle(EntityPlayerMP player, MiddleClickPullOrCraftPacket message) {
            if (message.stack == null || message.stack.isEmpty()) {
                return;
            }

            FindWirelessTerminal.WirelessTerminalContext terminalContext =
                    FindWirelessTerminal.findOpenableWirelessTerminal(player);
            if (terminalContext == null) {
                notifyPlayer(player, "message.ae2_auto_pattern_upload.bookmark_pull.no_terminal");
                return;
            }

            ItemStack terminalStack = terminalContext.getTerminalStack();
            IWirelessTermHandler handler = AEApi.instance().registries().wireless().getWirelessTerminalHandler(terminalStack);
            if (handler == null) {
                notifyPlayer(player, "message.ae2_auto_pattern_upload.bookmark_pull.no_terminal");
                return;
            }

            String encryptionKey = handler.getEncryptionKey(terminalStack);
            if (encryptionKey == null || encryptionKey.isEmpty()) {
                notifyPlayer(player, "message.ae2_auto_pattern_upload.bookmark_pull.not_linked");
                return;
            }

            if (!handler.hasPower(player, 0.5, terminalStack)) {
                notifyPlayer(player, "message.ae2_auto_pattern_upload.bookmark_pull.no_power");
                return;
            }

            IGrid grid = FindWirelessTerminal.getWirelessTerminalGrid(terminalStack);
            IStorageGrid storageGrid = grid == null ? null : grid.getCache(IStorageGrid.class);
            if (storageGrid == null) {
                notifyPlayer(player, "message.ae2_auto_pattern_upload.bookmark_pull.not_linked");
                return;
            }

            IMEMonitor<IAEItemStack> monitor = storageGrid.getInventory(
                    AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class));
            if (monitor == null) {
                notifyPlayer(player, "message.ae2_auto_pattern_upload.bookmark_pull.not_found");
                return;
            }

            IAEItemStack storedStack = findStoredStack(monitor.getStorageList(), message.stack);
            if (storedStack == null || storedStack.getStackSize() <= 0) {
                openCraftAmount(player, terminalContext, grid, message.stack);
                return;
            }

            ItemStack previewStack = storedStack.createItemStack();
            if (previewStack.isEmpty()) {
                openCraftAmount(player, terminalContext, grid, message.stack);
                return;
            }

            int requestAmount = (int) Math.min(previewStack.getMaxStackSize(), storedStack.getStackSize());
            previewStack.setCount(requestAmount);

            int fitAmount = getInventoryFitAmount(player, previewStack);
            if (fitAmount <= 0) {
                notifyPlayer(player, "message.ae2_auto_pattern_upload.bookmark_pull.inventory_full");
                return;
            }

            IActionHost actionHost = FindWirelessTerminal.getWirelessTerminalActionHost(terminalStack);
            if (actionHost == null) {
                notifyPlayer(player, "message.ae2_auto_pattern_upload.bookmark_pull.not_linked");
                return;
            }

            IAEItemStack request = storedStack.copy();
            request.setStackSize(fitAmount);
            IAEItemStack extracted = monitor.extractItems(request, Actionable.MODULATE, new PlayerSource(player, actionHost));
            if (extracted == null || extracted.getStackSize() <= 0) {
                openCraftAmount(player, terminalContext, grid, message.stack);
                return;
            }

            if (!handler.usePower(player, 0.5, terminalStack)) {
                monitor.injectItems(extracted, Actionable.MODULATE, new PlayerSource(player, actionHost));
                notifyPlayer(player, "message.ae2_auto_pattern_upload.bookmark_pull.no_power");
                return;
            }

            ItemStack remaining = insertIntoInventory(player, extracted.createItemStack());
            if (!remaining.isEmpty()) {
                monitor.injectItems(AEItemStack.fromItemStack(remaining), Actionable.MODULATE, new PlayerSource(player, actionHost));
            }

            player.inventoryContainer.detectAndSendChanges();
        }

        private static void openCraftAmount(EntityPlayerMP player,
                                            FindWirelessTerminal.WirelessTerminalContext terminalContext,
                                            IGrid grid,
                                            ItemStack stack) {
            IAEItemStack craftableStack = findCraftableStack(grid, stack);
            if (craftableStack == null || !craftableStack.isCraftable()) {
                notifyPlayer(player, "message.ae2_auto_pattern_upload.bookmark_autocraft.not_craftable");
                return;
            }

            Platform.openGUI(player, terminalContext.getSlot(), GuiBridge.GUI_CRAFTING_AMOUNT, terminalContext.isBaubleSlot());
            if (!(player.openContainer instanceof ContainerCraftAmount)) {
                return;
            }

            ContainerCraftAmount container = (ContainerCraftAmount) player.openContainer;
            container.getCraftingItem().putStack(craftableStack.asItemStackRepresentation());
            container.setItemToCraft(craftableStack.copy());
            container.detectAndSendChanges();
        }

        private static IAEItemStack findStoredStack(IItemList<IAEItemStack> storageList, ItemStack stack) {
            if (storageList == null) {
                return null;
            }

            IAEItemStack search = AEItemStack.fromItemStack(stack);
            if (search == null) {
                return null;
            }

            if (stack.getItem().isDamageable() || Platform.isGTDamageableItem(stack.getItem())) {
                Collection<IAEItemStack> fuzzyMatches = storageList.findFuzzy(search, FuzzyMode.IGNORE_ALL);
                for (IAEItemStack candidate : fuzzyMatches) {
                    if (candidate == null || candidate.getStackSize() <= 0) {
                        continue;
                    }

                    if (Platform.isGTDamageableItem(stack.getItem())
                            && stack.getMetadata() != candidate.getDefinition().getMetadata()) {
                        continue;
                    }

                    return candidate.copy();
                }
                return null;
            }

            IAEItemStack precise = storageList.findPrecise(search);
            return precise != null && precise.getStackSize() > 0 ? precise.copy() : null;
        }

        private static IAEItemStack findCraftableStack(IGrid grid, ItemStack stack) {
            IStorageGrid storageGrid = grid.getCache(IStorageGrid.class);
            if (storageGrid == null) {
                return null;
            }

            IItemList<IAEItemStack> storageList = storageGrid
                    .getInventory(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class))
                    .getStorageList();
            if (storageList == null) {
                return null;
            }

            IAEItemStack search = AEItemStack.fromItemStack(stack);
            if (search == null) {
                return null;
            }

            if (stack.getItem().isDamageable() || Platform.isGTDamageableItem(stack.getItem())) {
                Collection<IAEItemStack> fuzzyMatches = storageList.findFuzzy(search, FuzzyMode.IGNORE_ALL);
                for (IAEItemStack candidate : fuzzyMatches) {
                    if (candidate == null || !candidate.isCraftable()) {
                        continue;
                    }

                    if (Platform.isGTDamageableItem(stack.getItem())
                            && stack.getMetadata() != candidate.getDefinition().getMetadata()) {
                        continue;
                    }

                    return candidate.copy();
                }
                return null;
            }

            IAEItemStack precise = storageList.findPrecise(search);
            return precise != null && precise.isCraftable() ? precise.copy() : null;
        }

        private static int getInventoryFitAmount(EntityPlayerMP player, ItemStack stack) {
            if (stack.isEmpty()) {
                return 0;
            }

            NonNullList<ItemStack> inventory = player.inventory.mainInventory;
            int selectedSlot = player.inventory.currentItem;
            int remaining = stack.getCount();
            remaining = fillSelectedSlotIfEmpty(inventory, stack, remaining, selectedSlot);
            remaining = fillByRange(inventory, stack, remaining, 0, 9, true, selectedSlot);
            remaining = fillByRange(inventory, stack, remaining, 0, 9, false, selectedSlot);
            remaining = fillByRange(inventory, stack, remaining, 9, inventory.size(), true, -1);
            remaining = fillByRange(inventory, stack, remaining, 9, inventory.size(), false, -1);
            return stack.getCount() - remaining;
        }

        private static ItemStack insertIntoInventory(EntityPlayerMP player, ItemStack stack) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            NonNullList<ItemStack> inventory = player.inventory.mainInventory;
            int selectedSlot = player.inventory.currentItem;
            int remaining = stack.getCount();
            remaining = insertIntoSelectedSlotIfEmpty(inventory, stack, remaining, selectedSlot);
            remaining = insertByRange(inventory, stack, remaining, 0, 9, true, selectedSlot);
            remaining = insertByRange(inventory, stack, remaining, 0, 9, false, selectedSlot);
            remaining = insertByRange(inventory, stack, remaining, 9, inventory.size(), true, -1);
            remaining = insertByRange(inventory, stack, remaining, 9, inventory.size(), false, -1);

            if (remaining <= 0) {
                return ItemStack.EMPTY;
            }

            ItemStack leftover = stack.copy();
            leftover.setCount(remaining);
            return leftover;
        }

        private static int fillSelectedSlotIfEmpty(NonNullList<ItemStack> inventory,
                                                   ItemStack stack,
                                                   int remaining,
                                                   int selectedSlot) {
            if (remaining <= 0 || selectedSlot < 0 || selectedSlot >= inventory.size()) {
                return remaining;
            }

            return inventory.get(selectedSlot).isEmpty()
                    ? remaining - Math.min(stack.getMaxStackSize(), remaining)
                    : remaining;
        }

        private static int insertIntoSelectedSlotIfEmpty(NonNullList<ItemStack> inventory,
                                                         ItemStack stack,
                                                         int remaining,
                                                         int selectedSlot) {
            if (remaining <= 0 || selectedSlot < 0 || selectedSlot >= inventory.size()) {
                return remaining;
            }

            if (!inventory.get(selectedSlot).isEmpty()) {
                return remaining;
            }

            int move = Math.min(stack.getMaxStackSize(), remaining);
            ItemStack inserted = stack.copy();
            inserted.setCount(move);
            inventory.set(selectedSlot, inserted);
            return remaining - move;
        }

        private static int fillByRange(NonNullList<ItemStack> inventory,
                                       ItemStack stack,
                                       int remaining,
                                       int start,
                                       int end,
                                       boolean mergeExisting,
                                       int excludedSlot) {
            for (int slot = start; slot < end && remaining > 0; slot++) {
                if (slot == excludedSlot) {
                    continue;
                }

                ItemStack existing = inventory.get(slot);
                if (mergeExisting) {
                    if (!canMerge(existing, stack)) {
                        continue;
                    }

                    int limit = Math.min(existing.getMaxStackSize(), stack.getMaxStackSize());
                    remaining -= Math.min(limit - existing.getCount(), remaining);
                } else if (existing.isEmpty()) {
                    remaining -= Math.min(stack.getMaxStackSize(), remaining);
                }
            }

            return remaining;
        }

        private static int insertByRange(NonNullList<ItemStack> inventory,
                                         ItemStack stack,
                                         int remaining,
                                         int start,
                                         int end,
                                         boolean mergeExisting,
                                         int excludedSlot) {
            for (int slot = start; slot < end && remaining > 0; slot++) {
                if (slot == excludedSlot) {
                    continue;
                }

                ItemStack existing = inventory.get(slot);
                if (mergeExisting) {
                    if (!canMerge(existing, stack)) {
                        continue;
                    }

                    int limit = Math.min(existing.getMaxStackSize(), stack.getMaxStackSize());
                    int move = Math.min(limit - existing.getCount(), remaining);
                    if (move > 0) {
                        existing.grow(move);
                        remaining -= move;
                    }
                } else if (existing.isEmpty()) {
                    int move = Math.min(stack.getMaxStackSize(), remaining);
                    ItemStack inserted = stack.copy();
                    inserted.setCount(move);
                    inventory.set(slot, inserted);
                    remaining -= move;
                }
            }

            return remaining;
        }

        private static boolean canMerge(ItemStack existing, ItemStack stack) {
            return !existing.isEmpty()
                    && existing.getCount() < Math.min(existing.getMaxStackSize(), stack.getMaxStackSize())
                    && ItemStack.areItemsEqual(existing, stack)
                    && ItemStack.areItemStackTagsEqual(existing, stack);
        }

        private static void notifyPlayer(EntityPlayerMP player, String translationKey) {
            player.sendStatusMessage(new TextComponentTranslation(translationKey), true);
        }
    }
}
