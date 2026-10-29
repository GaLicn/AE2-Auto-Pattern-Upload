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
 * 处理 Shift + 左键点击的物品拉取/下单请求
 */
public class PacketExtractItem implements IMessage {

    private ItemStack itemStack;
    private boolean openCraftingGui;

    public PacketExtractItem() {}

    public PacketExtractItem(ItemStack itemStack, boolean openCraftingGui) {
        this.itemStack = itemStack;
        this.openCraftingGui = openCraftingGui;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.itemStack = ByteBufUtils.readItemStack(buf);
        this.openCraftingGui = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeItemStack(buf, itemStack);
        buf.writeBoolean(openCraftingGui);
    }

    public static class Handler implements IMessageHandler<PacketExtractItem, IMessage> {

        @Override
        public IMessage onMessage(PacketExtractItem message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) {
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

                // 检查网络中是否有该物品
                IAEItemStack stored = itemStorage.getStorageList()
                    .findPrecise(aeStack);
                boolean hasStock = stored != null && stored.getStackSize() > 0;

                if (hasStock) {
                    // 有库存，执行拉取操作（整组）
                    extractItemToPlayer(player, grid, aeStack);
                } else if (message.openCraftingGui && AEUtil.isCraftable(grid, aeStack)) {
                    // 无库存但可合成，尝试打开合成下单界面
                    AEUtil.openCraftingAmountGui(player, aeStack);
                }

            } catch (Throwable e) {
                MyMod.LOG.warn("Error handling item extraction request", e);
                // 静默失败
            }

            return null;
        }

        /**
         * 从网络提取物品到玩家背包
         */
        private void extractItemToPlayer(EntityPlayerMP player, IGrid grid, IAEItemStack stack) {
            try {
                // 复制物品堆栈并设置为最大堆叠数量
                IAEItemStack toExtract = stack.copy();
                ItemStack itemStack = toExtract.getItemStack();
                int maxStackSize = itemStack.getMaxStackSize();
                toExtract.setStackSize(maxStackSize);

                // 检查玩家背包是否有空间
                InventoryAdaptor playerInv = InventoryAdaptor.getAdaptor(player, ForgeDirection.UNKNOWN);
                ItemStack remaining = playerInv.simulateAdd(itemStack);

                if (remaining != null && remaining.stackSize == maxStackSize) {
                    // 背包已满，无法添加任何物品，静默失败
                    return;
                }

                // 调整提取数量
                if (remaining != null) {
                    toExtract.setStackSize(maxStackSize - remaining.stackSize);
                }

                // 从网格获取存储和能源接口
                IStorageGrid storageGrid = grid.getCache(IStorageGrid.class);
                if (storageGrid == null) {
                    return;
                }

                // 获取 ME 库存监控器
                IMEMonitor<IAEItemStack> itemInventory = storageGrid.getItemInventory();
                if (itemInventory == null) {
                    return;
                }

                // 获取ActionHost - 尝试从当前容器获取，如果不在AE界面则搜索无线终端
                Container container = player.openContainer;
                IActionHost actionHost = null;

                if (container instanceof AEBaseContainer) {
                    actionHost = AEUtil.getActionHost((AEBaseContainer) container);
                } else {
                    // 如果不在AE界面，尝试从无线终端获取ActionHost
                    actionHost = AEUtil.findWirelessActionHost(player);
                }

                // 执行提取
                IAEItemStack extracted = itemInventory
                    .extractItems(toExtract, Actionable.MODULATE, new PlayerSource(player, actionHost));

                if (extracted != null && extracted.getStackSize() > 0) {
                    // 添加到玩家背包
                    ItemStack added = playerInv.addItems(extracted.getItemStack());

                    // 如果有剩余，尝试返还到网络
                    if (added != null) {
                        IAEItemStack remainder = AEItemStack.create(added);
                        itemInventory.injectItems(remainder, Actionable.MODULATE, new PlayerSource(player, actionHost));
                    }
                }

            } catch (Throwable e) {
                MyMod.LOG.warn("Error extracting item to player", e);
            }
        }
    }
}
