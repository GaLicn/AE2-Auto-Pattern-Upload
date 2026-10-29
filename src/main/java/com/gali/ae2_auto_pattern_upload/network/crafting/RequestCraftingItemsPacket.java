package com.gali.ae2_auto_pattern_upload.network.crafting;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;

import com.gali.ae2_auto_pattern_upload.util.AEUtil;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.AEBaseContainer;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 客户端请求正在合成的物品列表
 */
public class RequestCraftingItemsPacket implements IMessage {

    public RequestCraftingItemsPacket() {}

    @Override
    public void fromBytes(ByteBuf buf) {
        // 无数据需要读取
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // 无数据需要写入
    }

    /**
     * 服务器处理器
     */
    public static class ServerHandler implements IMessageHandler<RequestCraftingItemsPacket, IMessage> {

        @Override
        public IMessage onMessage(RequestCraftingItemsPacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) {
                return null;
            }

            IGrid grid = null;
            Container container = player.openContainer;

            // 尝试从有线终端获取网格
            if (container instanceof AEBaseContainer aeContainer) {
                grid = AEUtil.getGrid(aeContainer);
            }

            // 如果是有线终端且获取不到网格，或者可能是无线终端
            if (grid == null) {
                // 尝试从玩家背包中的无线终端获取网格
                grid = AEUtil.getGridFromWirelessTerminal(player);
            }

            if (grid == null) {
                return null;
            }

            ICraftingGrid craftingGrid = grid.getCache(ICraftingGrid.class);
            if (craftingGrid == null) {
                return null;
            }

            // 收集所有正在合成的物品
            Set<IAEItemStack> craftingItems = new HashSet<>();
            for (ICraftingCPU cpu : craftingGrid.getCpus()) {
                if (cpu.isBusy()) {
                    IAEItemStack finalOutput = cpu.getFinalOutput();
                    if (finalOutput != null) {
                        craftingItems.add(finalOutput.copy());
                    }
                }
            }

            // 发送回客户端
            return new PacketCraftingItemsUpdate(craftingItems);
        }
    }
}
