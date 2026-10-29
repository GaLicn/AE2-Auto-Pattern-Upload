package com.gali.ae2_auto_pattern_upload.network.inventory;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.gali.ae2_auto_pattern_upload.MyMod;
import com.gali.ae2_auto_pattern_upload.util.AEUtil;

import appeng.api.config.Actionable;
import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.AEBaseContainer;
import appeng.util.InventoryAdaptor;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 处理鼠标滚轮在AE终端物品上的滚动操作
 * 向下滚动(negative): 从AE取出物品到玩家背包
 * 向上滚动(positive): 从玩家背包存入物品到AE
 */
public class PacketScrollTransfer implements IMessage {

    private ItemStack itemStack;
    private int scrollDirection; // 正数表示向上滚动(存入)，负数表示向下滚动(取出)
    private int transferAmount; // 传输数量：1个、4个或1组

    public PacketScrollTransfer() {}

    public PacketScrollTransfer(ItemStack itemStack, int scrollDirection, int transferAmount) {
        this.itemStack = itemStack;
        this.scrollDirection = scrollDirection;
        this.transferAmount = transferAmount;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.itemStack = ByteBufUtils.readItemStack(buf);
        this.scrollDirection = buf.readInt();
        this.transferAmount = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeItemStack(buf, itemStack);
        buf.writeInt(scrollDirection);
        buf.writeInt(transferAmount);
    }

    public static class Handler implements IMessageHandler<PacketScrollTransfer, IMessage> {

        @Override
        public IMessage onMessage(PacketScrollTransfer message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null || message.itemStack == null) {
                return null;
            }

            // 获取玩家网格
            IGrid grid = AEUtil.getPlayerGrid(player);
            if (grid == null) {
                return null;
            }

            try {
                // 检查权限
                if (!AEUtil.hasGridPermission(player, grid, SecurityPermissions.EXTRACT)) {
                    return null;
                }

                // 获取存储网格
                IStorageGrid storage = grid.getCache(IStorageGrid.class);
                if (storage == null) {
                    return null;
                }

                IMEMonitor<IAEItemStack> itemStorage = storage.getItemInventory();
                if (itemStorage == null) {
                    return null;
                }

                // 获取ActionHost
                Container container = player.openContainer;
                IActionHost actionHost = null;

                if (container instanceof AEBaseContainer) {
                    actionHost = AEUtil.getActionHost((AEBaseContainer) container);
                } else {
                    actionHost = AEUtil.findWirelessActionHost(player);
                }

                if (message.scrollDirection < 0) {
                    // 向下滚动 - 从AE取出物品到玩家背包
                    extractItemToPlayer(
                        player,
                        grid,
                        itemStorage,
                        actionHost,
                        message.itemStack,
                        message.transferAmount);
                } else if (message.scrollDirection > 0) {
                    // 向上滚动 - 从玩家背包存入物品到AE
                    depositItemToAE(player, itemStorage, actionHost, message.itemStack, message.transferAmount);
                }

            } catch (Throwable e) {
                MyMod.LOG.warn("Error handling scroll transfer request", e);
            }

            return null;
        }

        /**
         * 从网络提取物品到玩家背包
         */
        private void extractItemToPlayer(EntityPlayerMP player, IGrid grid, IMEMonitor<IAEItemStack> itemStorage,
            IActionHost actionHost, ItemStack stack, int transferAmount) {
            try {
                // 创建AE物品堆栈
                IAEItemStack aeStack = AEUtil.createAEStack(stack);
                if (aeStack == null) {
                    return;
                }

                // 设置提取数量
                aeStack.setStackSize(transferAmount);

                // 检查玩家背包是否有空间
                InventoryAdaptor playerInv = InventoryAdaptor.getAdaptor(player, ForgeDirection.UNKNOWN);
                ItemStack simulateStack = stack.copy();
                simulateStack.stackSize = transferAmount;
                ItemStack remaining = playerInv.simulateAdd(simulateStack);

                if (remaining != null && remaining.stackSize == transferAmount) {
                    // 背包已满
                    return;
                }

                // 调整提取数量
                if (remaining != null) {
                    aeStack.setStackSize(transferAmount - remaining.stackSize);
                }

                // 执行提取
                IAEItemStack extracted = itemStorage
                    .extractItems(aeStack, Actionable.MODULATE, new PlayerSource(player, actionHost));

                if (extracted != null && extracted.getStackSize() > 0) {
                    // 添加到玩家背包
                    ItemStack added = playerInv.addItems(extracted.getItemStack());

                    // 如果有剩余，返还到网络
                    if (added != null) {
                        IAEItemStack remainder = AEItemStack.create(added);
                        itemStorage.injectItems(remainder, Actionable.MODULATE, new PlayerSource(player, actionHost));
                    }
                }

            } catch (Throwable e) {
                MyMod.LOG.warn("Error extracting item to player", e);
            }
        }

        /**
         * 从玩家背包存入物品到AE网络
         */
        private void depositItemToAE(EntityPlayerMP player, IMEMonitor<IAEItemStack> itemStorage,
            IActionHost actionHost, ItemStack stack, int transferAmount) {
            try {
                // 在玩家背包中查找匹配的物品，限制数量
                ItemStack toDeposit = findItemInPlayerInventory(player, stack, transferAmount);
                if (toDeposit == null || toDeposit.stackSize <= 0) {
                    return;
                }

                // 创建AE物品堆栈
                IAEItemStack aeStack = AEItemStack.create(toDeposit);
                if (aeStack == null) {
                    return;
                }

                // 存入AE网络
                IAEItemStack notInserted = itemStorage
                    .injectItems(aeStack, Actionable.MODULATE, new PlayerSource(player, actionHost));

                // 计算实际存入的数量
                long insertedCount = aeStack.getStackSize();
                if (notInserted != null) {
                    insertedCount -= notInserted.getStackSize();
                }

                // 从玩家背包移除已存入的物品
                if (insertedCount > 0) {
                    removeItemFromPlayerInventory(player, stack, (int) insertedCount);
                }

            } catch (Throwable e) {
                MyMod.LOG.warn("Error depositing item to AE", e);
            }
        }

        /**
         * 在玩家背包中查找匹配的物品
         */
        private ItemStack findItemInPlayerInventory(EntityPlayerMP player, ItemStack target, int maxAmount) {
            int totalCount = 0;
            ItemStack foundStack = null;

            for (int i = 0; i < player.inventory.mainInventory.length; i++) {
                ItemStack invStack = player.inventory.mainInventory[i];
                if (invStack != null && invStack.isItemEqual(target)
                    && ItemStack.areItemStackTagsEqual(invStack, target)) {
                    if (foundStack == null) {
                        foundStack = invStack.copy();
                    }
                    totalCount += invStack.stackSize;
                    if (totalCount >= maxAmount) {
                        totalCount = maxAmount;
                        break;
                    }
                }
            }

            if (foundStack != null) {
                foundStack.stackSize = totalCount;
                return foundStack;
            }

            return null;
        }

        /**
         * 从玩家背包移除指定数量的物品
         */
        private void removeItemFromPlayerInventory(EntityPlayerMP player, ItemStack target, int amount) {
            int remaining = amount;

            for (int i = 0; i < player.inventory.mainInventory.length && remaining > 0; i++) {
                ItemStack invStack = player.inventory.mainInventory[i];
                if (invStack != null && invStack.isItemEqual(target)
                    && ItemStack.areItemStackTagsEqual(invStack, target)) {

                    int toRemove = Math.min(invStack.stackSize, remaining);
                    invStack.stackSize -= toRemove;
                    remaining -= toRemove;

                    if (invStack.stackSize <= 0) {
                        player.inventory.mainInventory[i] = null;
                    }
                }
            }

            player.inventory.markDirty();
        }
    }
}
