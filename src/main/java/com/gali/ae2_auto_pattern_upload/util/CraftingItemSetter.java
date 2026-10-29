package com.gali.ae2_auto_pattern_upload.util;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.gali.ae2_auto_pattern_upload.MyMod;

import appeng.api.storage.data.IAEItemStack;
import appeng.container.implementations.ContainerCraftAmount;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * 延迟设置合成界面的物品（确保容器已打开）
 */
public class CraftingItemSetter {

    private int ticks = 0;
    private final EntityPlayerMP targetPlayer;
    private final IAEItemStack targetStack;

    public CraftingItemSetter(EntityPlayerMP player, IAEItemStack aeStack) {
        this.targetPlayer = player;
        this.targetStack = aeStack;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        ticks++;
        // 延迟2tick确保容器已打开
        if (ticks >= 2) {
            try {
                // 直接设置合成物品
                if (targetPlayer.openContainer instanceof ContainerCraftAmount craftAmount) {
                    ItemStack stack = targetStack.getItemStack()
                        .copy();
                    stack.stackSize = (int) Math.min(targetStack.getStackSize(), Integer.MAX_VALUE);
                    craftAmount.getCraftingItem()
                        .putStack(stack);
                    craftAmount.setItemToCraft(targetStack);
                    // 设置默认合成数量为64
                    craftAmount.setInitialCraftAmount(64);
                    craftAmount.detectAndSendChanges();
                }
            } catch (Throwable e) {
                MyMod.LOG.warn("Error setting crafting item in scheduled task", e);
            }
            // 取消注册
            cpw.mods.fml.common.FMLCommonHandler.instance()
                .bus()
                .unregister(this);
        }
    }
}
