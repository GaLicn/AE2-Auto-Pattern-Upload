package com.example.ae2_auto_pattern_upload.network;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.networking.IGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
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

public class PullBookmarkItemPacket implements IMessage {
    private ItemStack stack = ItemStack.EMPTY;

    public PullBookmarkItemPacket() {
    }

    public PullBookmarkItemPacket(ItemStack stack) {
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

    public static class Handler implements IMessageHandler<PullBookmarkItemPacket, IMessage> {
        @Override
        public IMessage onMessage(PullBookmarkItemPacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) {
                return null;
            }

            player.getServerWorld().addScheduledTask(() -> handle(player, message));
            return null;
        }

        private static void handle(EntityPlayerMP player, PullBookmarkItemPacket message) {
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

            IActionHost actionHost = FindWirelessTerminal.getWirelessTerminalActionHost(terminalStack);
            if (actionHost == null) {
                notifyPlayer(player, "message.ae2_auto_pattern_upload.bookmark_pull.not_linked");
                return;
            }

            IGrid grid = FindWirelessTerminal.getWirelessTerminalGrid(terminalStack);
            IStorageGrid storageGrid = grid == null ? null : grid.getCache(IStorageGrid.class);
            if (storageGrid == null) {
                notifyPlayer(player, "message.ae2_auto_pattern_upload.bookmark_pull.not_linked");
                return;
            }

            IMEMonitor<IAEItemStack> monitor = storageGrid.getInventory(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class));
            if (monitor == null) {
                notifyPlayer(player, "message.ae2_auto_pattern_upload.bookmark_pull.not_found");
                return;
            }

            IAEItemStack storedStack = findStoredStack(monitor.getStorageList(), message.stack);
            if (storedStack == null || storedStack.getStackSize() <= 0) {
                notifyPlayer(player, "message.ae2_auto_pattern_upload.bookmark_pull.not_found");
                return;
            }

            ItemStack previewStack = storedStack.createItemStack();
            if (previewStack.isEmpty()) {
                notifyPlayer(player, "message.ae2_auto_pattern_upload.bookmark_pull.not_found");
                return;
            }

            int requestAmount = (int) Math.min(previewStack.getMaxStackSize(), storedStack.getStackSize());
            previewStack.setCount(requestAmount);

            int fitAmount = getInventoryFitAmount(player.inventory.mainInventory, previewStack);
            if (fitAmount <= 0) {
                notifyPlayer(player, "message.ae2_auto_pattern_upload.bookmark_pull.inventory_full");
                return;
            }

            if (!handler.hasPower(player, 0.5, terminalStack)) {
                notifyPlayer(player, "message.ae2_auto_pattern_upload.bookmark_pull.no_power");
                return;
            }

            IAEItemStack request = storedStack.copy();
            request.setStackSize(fitAmount);
            IAEItemStack extracted = monitor.extractItems(request, Actionable.MODULATE, new PlayerSource(player, actionHost));
            if (extracted == null || extracted.getStackSize() <= 0) {
                notifyPlayer(player, "message.ae2_auto_pattern_upload.bookmark_pull.not_found");
                return;
            }

            if (!handler.usePower(player, 0.5, terminalStack)) {
                monitor.injectItems(extracted, Actionable.MODULATE, new PlayerSource(player, actionHost));
                notifyPlayer(player, "message.ae2_auto_pattern_upload.bookmark_pull.no_power");
                return;
            }

            ItemStack remaining = insertIntoInventory(player.inventory.mainInventory, extracted.createItemStack());
            if (!remaining.isEmpty()) {
                monitor.injectItems(AEItemStack.fromItemStack(remaining), Actionable.MODULATE, new PlayerSource(player, actionHost));
            }

            player.inventoryContainer.detectAndSendChanges();
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

        private static int getInventoryFitAmount(NonNullList<ItemStack> inventory, ItemStack stack) {
            if (stack.isEmpty()) {
                return 0;
            }

            int remaining = stack.getCount();
            remaining = fillByRange(inventory, stack, remaining, 0, 9, true);
            remaining = fillByRange(inventory, stack, remaining, 0, 9, false);
            remaining = fillByRange(inventory, stack, remaining, 9, inventory.size(), true);
            remaining = fillByRange(inventory, stack, remaining, 9, inventory.size(), false);
            return stack.getCount() - remaining;
        }

        private static ItemStack insertIntoInventory(NonNullList<ItemStack> inventory, ItemStack stack) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            int remaining = stack.getCount();
            remaining = insertByRange(inventory, stack, remaining, 0, 9, true);
            remaining = insertByRange(inventory, stack, remaining, 0, 9, false);
            remaining = insertByRange(inventory, stack, remaining, 9, inventory.size(), true);
            remaining = insertByRange(inventory, stack, remaining, 9, inventory.size(), false);

            if (remaining <= 0) {
                return ItemStack.EMPTY;
            }

            ItemStack leftover = stack.copy();
            leftover.setCount(remaining);
            return leftover;
        }

        private static int fillByRange(NonNullList<ItemStack> inventory,
                                       ItemStack stack,
                                       int remaining,
                                       int start,
                                       int end,
                                       boolean mergeExisting) {
            for (int slot = start; slot < end && remaining > 0; slot++) {
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
                                         boolean mergeExisting) {
            for (int slot = start; slot < end && remaining > 0; slot++) {
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
