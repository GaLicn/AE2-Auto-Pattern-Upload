package com.gali.ae2_auto_pattern_upload.network.crafting;

import java.util.ArrayList;
import java.util.List;

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
 * 处理从AE网络提取配方材料到玩家背包的请求
 * 当玩家在NEI配方界面Shift+左键点击主合成物时触发
 */
public class PacketExtractIngredients implements IMessage {

    private List<ItemStack> ingredients;

    public PacketExtractIngredients() {
        this.ingredients = new ArrayList<>();
    }

    public PacketExtractIngredients(List<ItemStack> ingredients) {
        this.ingredients = ingredients;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        ingredients = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ItemStack stack = ByteBufUtils.readItemStack(buf);
            if (stack != null) {
                ingredients.add(stack);
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(ingredients.size());
        for (ItemStack stack : ingredients) {
            ByteBufUtils.writeItemStack(buf, stack);
        }
    }

    public static class Handler implements IMessageHandler<PacketExtractIngredients, IMessage> {

        @Override
        public IMessage onMessage(PacketExtractIngredients message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null || message.ingredients.isEmpty()) {
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

                // 获取玩家背包适配器
                InventoryAdaptor playerInv = InventoryAdaptor.getAdaptor(player, ForgeDirection.UNKNOWN);

                // 提取每种材料
                for (ItemStack ingredient : message.ingredients) {
                    extractIngredient(player, grid, itemStorage, playerInv, actionHost, ingredient);
                }

            } catch (Throwable e) {
                MyMod.LOG.warn("Error handling ingredients extraction request", e);
            }

            return null;
        }

        /**
         * 从网络提取单个材料到玩家背包
         */
        private void extractIngredient(EntityPlayerMP player, IGrid grid, IMEMonitor<IAEItemStack> itemStorage,
            InventoryAdaptor playerInv, IActionHost actionHost, ItemStack ingredient) {
            try {
                // 创建AE物品堆栈
                IAEItemStack aeStack = AEUtil.createAEStack(ingredient);
                if (aeStack == null) {
                    return;
                }

                // 检查网络中是否有该物品
                IAEItemStack stored = itemStorage.getStorageList()
                    .findPrecise(aeStack);
                if (stored == null || stored.getStackSize() <= 0) {
                    return; // 网络中没有该物品，跳过
                }

                // 设置提取数量（配方需要的数量）
                IAEItemStack toExtract = aeStack.copy();
                toExtract.setStackSize(ingredient.stackSize);

                // 检查玩家背包是否有空间
                ItemStack remaining = playerInv.simulateAdd(ingredient);
                if (remaining != null && remaining.stackSize == ingredient.stackSize) {
                    return; // 背包已满，跳过
                }

                // 调整提取数量
                if (remaining != null) {
                    toExtract.setStackSize(ingredient.stackSize - remaining.stackSize);
                }

                // 执行提取
                IAEItemStack extracted = itemStorage
                    .extractItems(toExtract, Actionable.MODULATE, new PlayerSource(player, actionHost));

                if (extracted != null && extracted.getStackSize() > 0) {
                    // 添加到玩家背包
                    ItemStack added = playerInv.addItems(extracted.getItemStack());

                    // 如果有剩余，返还到网络
                    if (added != null && added.stackSize > 0) {
                        IAEItemStack remainder = AEItemStack.create(added);
                        itemStorage.injectItems(remainder, Actionable.MODULATE, new PlayerSource(player, actionHost));
                    }
                }

            } catch (Throwable e) {
                MyMod.LOG.debug("Error extracting ingredient: {}", ingredient, e);
            }
        }
    }
}
