package com.example.ae2_auto_pattern_upload.network;

import appeng.api.AEApi;
import appeng.api.config.FuzzyMode;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.networking.IGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.container.implementations.ContainerCraftAmount;
import appeng.core.sync.GuiBridge;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import com.example.ae2_auto_pattern_upload.util.FindWirelessTerminal;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.IOException;
import java.util.Collection;

public class OpenCraftAmountFromBookmarkPacket implements IMessage {
    private ItemStack stack = ItemStack.EMPTY;

    public OpenCraftAmountFromBookmarkPacket() {
    }

    public OpenCraftAmountFromBookmarkPacket(ItemStack stack) {
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

    public static class Handler implements IMessageHandler<OpenCraftAmountFromBookmarkPacket, IMessage> {
        @Override
        public IMessage onMessage(OpenCraftAmountFromBookmarkPacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) {
                return null;
            }

            player.getServerWorld().addScheduledTask(() -> handle(player, message));
            return null;
        }

        private static void handle(EntityPlayerMP player, OpenCraftAmountFromBookmarkPacket message) {
            if (message.stack == null || message.stack.isEmpty()) {
                return;
            }

            FindWirelessTerminal.WirelessTerminalContext terminalContext =
                    FindWirelessTerminal.findOpenableWirelessTerminal(player);
            if (terminalContext == null) {
                notifyPlayer(player, "message.ae2_auto_pattern_upload.bookmark_autocraft.no_terminal");
                return;
            }

            ItemStack terminalStack = terminalContext.getTerminalStack();
            IWirelessTermHandler handler = AEApi.instance().registries().wireless().getWirelessTerminalHandler(terminalStack);
            if (handler == null) {
                notifyPlayer(player, "message.ae2_auto_pattern_upload.bookmark_autocraft.no_terminal");
                return;
            }

            String encryptionKey = handler.getEncryptionKey(terminalStack);
            if (encryptionKey == null || encryptionKey.isEmpty()) {
                notifyPlayer(player, "message.ae2_auto_pattern_upload.bookmark_autocraft.not_linked");
                return;
            }

            if (!handler.hasPower(player, 0.5, terminalStack)) {
                notifyPlayer(player, "message.ae2_auto_pattern_upload.bookmark_autocraft.no_power");
                return;
            }

            IGrid grid = FindWirelessTerminal.getWirelessTerminalGrid(terminalStack);
            if (grid == null) {
                notifyPlayer(player, "message.ae2_auto_pattern_upload.bookmark_autocraft.not_linked");
                return;
            }

            IAEItemStack craftableStack = findCraftableStack(grid, message.stack);
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

        private static void notifyPlayer(EntityPlayerMP player, String translationKey) {
            player.sendStatusMessage(new TextComponentTranslation(translationKey), true);
        }
    }
}
