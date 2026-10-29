package com.gali.ae2_auto_pattern_upload.network.inventory;

import java.util.HashMap;
import java.util.Map;

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
 * 处理鼠标中键点击的物品提取到玩家手上的请求
 * 1. 如果AE中有物品，提取到玩家手上（处理手中已有物品的情况）
 * 2. 如果AE中没有但有合成配方，弹出合成界面（默认64个）
 */
public class PacketMiddleClickExtract implements IMessage {

    private ItemStack itemStack;

    public PacketMiddleClickExtract() {}

    public PacketMiddleClickExtract(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.itemStack = ByteBufUtils.readItemStack(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeItemStack(buf, itemStack);
    }

    public static class Handler implements IMessageHandler<PacketMiddleClickExtract, IMessage> {

        // 服务器端冷却机制，防止重复请求
        private static final Map<String, Long> playerLastRequestTime = new HashMap<>();
        private static final Map<String, ItemStack> playerLastRequestItem = new HashMap<>();
        private static final long COOLDOWN_MS = 800;

        @Override
        public IMessage onMessage(PacketMiddleClickExtract message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) {
                return null;
            }

            // 服务器端冷却验证，防止重复提取
            String playerName = player.getCommandSenderName();
            long currentTime = System.currentTimeMillis();
            Long lastTime = playerLastRequestTime.get(playerName);
            ItemStack lastItem = playerLastRequestItem.get(playerName);

            if (lastTime != null && currentTime - lastTime < COOLDOWN_MS) {
                if (lastItem != null && message.itemStack != null && lastItem.isItemEqual(message.itemStack)) {
                    // 冷却时间内，相同物品的重复请求，忽略
                    return null;
                }
            }

            // 更新最后请求时间和物品
            playerLastRequestTime.put(playerName, currentTime);
            if (message.itemStack != null) {
                playerLastRequestItem.put(playerName, message.itemStack.copy());
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

                // 创建AE物品堆栈
                IAEItemStack aeStack = AEUtil.createAEStack(message.itemStack);
                if (aeStack == null) {
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

                // 检查网络中是否有该物品（使用精确匹配）
                IAEItemStack stored = itemStorage.getStorageList()
                    .findPrecise(aeStack);

                // 如果精确匹配失败，尝试模糊匹配
                if (stored == null || stored.getStackSize() <= 0) {
                    java.util.Collection<IAEItemStack> fuzzyList = itemStorage.getStorageList()
                        .findFuzzy(aeStack, appeng.api.config.FuzzyMode.IGNORE_ALL);
                    if (fuzzyList != null && !fuzzyList.isEmpty()) {
                        for (IAEItemStack fuzzy : fuzzyList) {
                            if (fuzzy.getStackSize() > 0) {
                                stored = fuzzy;
                                break;
                            }
                        }
                    }
                }

                boolean hasStock = stored != null && stored.getStackSize() > 0;

                if (hasStock) {
                    // 有库存，执行提取到玩家手上的操作
                    extractItemToHand(player, grid, stored, itemStorage);
                } else if (AEUtil.isCraftable(grid, aeStack)) {
                    // 无库存但可合成，检查合成权限后打开合成下单界面
                    if (AEUtil.hasGridPermission(player, grid, SecurityPermissions.CRAFT)) {
                        // 设置默认合成数量为1
                        IAEItemStack craftStack = aeStack.copy();
                        craftStack.setStackSize(1);
                        AEUtil.openCraftingAmountGui(player, craftStack);
                    }
                }

            } catch (Throwable e) {
                MyMod.LOG.warn("Error handling middle click extraction request", e);
            }

            return null;
        }

        /**
         * 从网络提取物品到玩家快捷栏
         * 1. 优先放到快捷栏(0-8)的空位，然后切换到该栏位
         * 2. 如果没有空位，替换9号位(索引8)，然后切换到9号栏位
         */
        private void extractItemToHand(EntityPlayerMP player, IGrid grid, IAEItemStack actualStack,
            IMEMonitor<IAEItemStack> itemStorage) {
            try {
                if (actualStack == null || actualStack.getStackSize() <= 0) {
                    return;
                }

                // 准备提取物品（提取一组）
                IAEItemStack toExtract = actualStack.copy();
                ItemStack itemStack = toExtract.getItemStack();
                int maxStackSize = itemStack.getMaxStackSize();
                toExtract.setStackSize(maxStackSize);

                // 获取ActionHost
                Container container = player.openContainer;
                IActionHost actionHost = null;

                if (container instanceof AEBaseContainer) {
                    actionHost = AEUtil.getActionHost((AEBaseContainer) container);
                } else {
                    actionHost = AEUtil.findWirelessActionHost(player);
                }

                // 执行提取
                IAEItemStack extracted = itemStorage
                    .extractItems(toExtract, Actionable.MODULATE, new PlayerSource(player, actionHost));

                if (extracted != null && extracted.getStackSize() > 0) {
                    ItemStack extractedStack = extracted.getItemStack();

                    // 查找快捷栏中的空位(0-8)
                    int targetSlot = -1;
                    for (int i = 0; i < 9; i++) {
                        if (player.inventory.mainInventory[i] == null
                            || player.inventory.mainInventory[i].stackSize == 0) {
                            targetSlot = i;
                            break;
                        }
                    }

                    // 如果没有空位，使用9号位(索引8)
                    if (targetSlot == -1) {
                        targetSlot = 8;
                        // 如果9号位有物品，先尝试移动到背包空位
                        ItemStack slot8Stack = player.inventory.mainInventory[8];
                        if (slot8Stack != null && slot8Stack.stackSize > 0) {
                            // 检查背包是否有空位（9-35号槽位是背包主存储）
                            int emptySlot = findEmptySlotInMainInventory(player);
                            if (emptySlot != -1) {
                                // 背包有空位，将9号位物品移动过去
                                player.inventory.mainInventory[emptySlot] = slot8Stack.copy();
                                player.inventory.mainInventory[8] = null;
                            } else {
                                // 背包没有空位，取消提取，将提取的物品返还到AE
                                itemStorage
                                    .injectItems(extracted, Actionable.MODULATE, new PlayerSource(player, actionHost));
                                return;
                            }
                        }
                    }

                    // 将提取的物品放到目标槽位
                    player.inventory.mainInventory[targetSlot] = extractedStack;

                    // 同步玩家背包状态到客户端
                    player.inventory.markDirty();

                    // 切换到目标槽位 - 需要使用数据包同步到客户端
                    player.inventory.currentItem = targetSlot;
                    // 发送槽位切换数据包到客户端
                    net.minecraft.network.play.server.S09PacketHeldItemChange packet = new net.minecraft.network.play.server.S09PacketHeldItemChange(
                        targetSlot);
                    player.playerNetServerHandler.sendPacket(packet);
                }

            } catch (Throwable e) {
                MyMod.LOG.warn("Error extracting item to hand", e);
            }
        }

        /**
         * 查找背包主存储区（9-35号槽位）的空位
         *
         * @return 空位索引，如果没有则返回-1
         */
        private int findEmptySlotInMainInventory(EntityPlayerMP player) {
            // 9-35是背包主存储区（不包括快捷栏0-8）
            for (int i = 9; i < 36; i++) {
                if (player.inventory.mainInventory[i] == null || player.inventory.mainInventory[i].stackSize == 0) {
                    return i;
                }
            }
            return -1;
        }

        /**
         * 尝试将玩家手中的物品移动到背包
         * 如果背包满了，尝试返还到AE网络
         *
         * @return 是否成功处理（物品已不在手上）
         */
        private boolean moveHeldItemToInventory(EntityPlayerMP player, ItemStack heldStack) {
            try {
                // 首先尝试放入玩家背包
                InventoryAdaptor playerInv = InventoryAdaptor.getAdaptor(player, ForgeDirection.UNKNOWN);
                ItemStack remaining = playerInv.simulateAdd(heldStack);

                if (remaining == null || remaining.stackSize == 0) {
                    // 背包可以容纳全部，执行添加
                    playerInv.addItems(heldStack);
                    return true;
                }

                // 背包只能容纳部分，先添加能容纳的部分
                int canAdd = heldStack.stackSize - remaining.stackSize;
                if (canAdd > 0) {
                    ItemStack toAdd = heldStack.copy();
                    toAdd.stackSize = canAdd;
                    playerInv.addItems(toAdd);

                    // 更新剩余数量
                    heldStack.stackSize = remaining.stackSize;
                }

                // 还有剩余，尝试返还到AE网络
                return returnItemToNetwork(player, heldStack);

            } catch (Throwable e) {
                MyMod.LOG.warn("Error moving held item to inventory", e);
                return false;
            }
        }

        /**
         * 将物品返还到AE网络
         *
         * @return 是否成功返还全部物品
         */
        private boolean returnItemToNetwork(EntityPlayerMP player, ItemStack stack) {
            try {
                // 获取玩家网格
                IGrid grid = AEUtil.getPlayerGrid(player);
                if (grid == null) {
                    return false;
                }

                // 获取存储网格
                IStorageGrid storage = grid.getCache(IStorageGrid.class);
                if (storage == null) {
                    return false;
                }

                IMEMonitor<IAEItemStack> itemStorage = storage.getItemInventory();
                if (itemStorage == null) {
                    return false;
                }

                // 检查注入权限
                if (!AEUtil.hasGridPermission(player, grid, SecurityPermissions.INJECT)) {
                    return false;
                }

                // 获取ActionHost
                Container container = player.openContainer;
                IActionHost actionHost = null;

                if (container instanceof AEBaseContainer) {
                    actionHost = AEUtil.getActionHost((AEBaseContainer) container);
                } else {
                    actionHost = AEUtil.findWirelessActionHost(player);
                }

                // 执行注入
                IAEItemStack toInject = AEItemStack.create(stack);
                IAEItemStack remaining = itemStorage
                    .injectItems(toInject, Actionable.MODULATE, new PlayerSource(player, actionHost));

                // 检查是否全部注入成功
                return remaining == null || remaining.getStackSize() == 0;

            } catch (Throwable e) {
                MyMod.LOG.warn("Error returning item to network", e);
                return false;
            }
        }
    }
}
