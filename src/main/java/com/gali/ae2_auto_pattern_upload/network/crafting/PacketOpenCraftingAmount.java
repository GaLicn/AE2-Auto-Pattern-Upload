package com.gali.ae2_auto_pattern_upload.network.crafting;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.gali.ae2_auto_pattern_upload.MyMod;
import com.gali.ae2_auto_pattern_upload.util.AEUtil;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.storage.data.IAEItemStack;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 处理鼠标中键点击的合成下单请求
 */
public class PacketOpenCraftingAmount implements IMessage {

    private ItemStack itemStack;

    public PacketOpenCraftingAmount() {}

    public PacketOpenCraftingAmount(ItemStack itemStack) {
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

    public static class Handler implements IMessageHandler<PacketOpenCraftingAmount, IMessage> {

        @Override
        public IMessage onMessage(PacketOpenCraftingAmount message, MessageContext ctx) {
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
                if (!AEUtil.hasGridPermission(player, grid, SecurityPermissions.CRAFT)) {
                    return null;
                }

                // 创建AE物品堆栈
                IAEItemStack aeStack = AEUtil.createAEStack(message.itemStack);
                if (aeStack == null) {
                    return null;
                }

                // 检查是否可合成
                if (!AEUtil.isCraftable(grid, aeStack)) {
                    // 不可合成，静默失败
                    return null;
                }

                // 打开合成界面
                AEUtil.openCraftingAmountGui(player, aeStack);

            } catch (Throwable e) {
                MyMod.LOG.warn("Error handling crafting request", e);
                // 静默失败
            }

            return null;
        }
    }
}
