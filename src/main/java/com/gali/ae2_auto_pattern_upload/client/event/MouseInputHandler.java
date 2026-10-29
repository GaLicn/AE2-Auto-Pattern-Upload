package com.gali.ae2_auto_pattern_upload.client.event;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.MouseEvent;

import com.gali.ae2_auto_pattern_upload.network.ModNetwork;
import com.gali.ae2_auto_pattern_upload.network.inventory.PacketMiddleClickExtract;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 处理游戏中的鼠标输入事件
 * 主要用于检测玩家中键点击方块/物品
 */
@SideOnly(Side.CLIENT)
public class MouseInputHandler {

    private static final int MOUSE_MIDDLE = 2;
    private static final long COOLDOWN_MS = 500;
    private static long lastMiddleClickTime = 0;
    private static ItemStack lastTargetStack = null;

    public static void register() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new MouseInputHandler());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouseEvent(MouseEvent event) {
        // 检查是否是中键点击
        if (event.button != MOUSE_MIDDLE) {
            return;
        }

        // 只在按键按下时触发，不处理松开
        if (!event.buttonstate) {
            return;
        }

        // 获取玩家
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null) {
            return;
        }

        // 检查手持物品是否是 GT 的无限喷漆罐
        // 如果是，完全不干预事件，让 GT 自己处理中键事件（如吸取颜色功能）
        ItemStack heldItem = player.getHeldItem();
        if (isInfiniteSprayCan(heldItem)) {
            return;
        }

        // 检查玩家是否处于生存模式
        // 通过capabilities判断：创造模式和旁观模式的玩家可以飞行
        if (player.capabilities.isCreativeMode) {
            return;
        }

        // 获取准星指向的对象
        MovingObjectPosition target = mc.objectMouseOver;
        if (target == null) {
            return;
        }

        // 处理不同类型的目标
        ItemStack targetStack = null;

        if (target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            // 指向方块，使用 Forge 原生方法获取 pick block
            Block block = mc.theWorld.getBlock(target.blockX, target.blockY, target.blockZ);
            if (block != null) {
                // 使用 Forge 的 getPickBlock 方法，传递 player 参数以支持 AE 线缆等复杂方块
                targetStack = block
                    .getPickBlock(target, mc.theWorld, target.blockX, target.blockY, target.blockZ, player);
            }
        }

        if (targetStack == null || targetStack.getItem() == null) {
            return;
        }

        // 防抖动：检查冷却时间和目标物品是否相同
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMiddleClickTime < COOLDOWN_MS && lastTargetStack != null
            && lastTargetStack.isItemEqual(targetStack)) {
            event.setCanceled(true);
            return;
        }
        lastMiddleClickTime = currentTime;
        lastTargetStack = targetStack.copy();

        // 检查玩家背包或快捷栏中是否已有该物品（包括手中的物品）
        // 如果背包中有，让原版逻辑处理（不取消事件）
        // 如果背包中没有，发送到服务器从AE提取
        if (hasItemInInventoryOrHand(player, targetStack)) {
            // 背包中有该物品，不取消事件，让原版处理
            return;
        }

        // 背包中没有该物品，取消事件并从AE提取
        event.setCanceled(true);
        ModNetwork.INSTANCE.sendToServer(new PacketMiddleClickExtract(targetStack));
    }

    /**
     * 检查物品是否是 GregTech 的无限喷漆罐
     * 用于让 GT 处理自己的中键事件（如无限喷漆罐的吸取颜色）
     */
    private boolean isInfiniteSprayCan(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }

        // 使用 unlocalizedName 检测，这是固定的
        // 无限喷漆罐属于 gt.metaitem.01，metadata 是 32468
        String unlocalizedName = stack.getItem()
            .getUnlocalizedName();
        if (unlocalizedName == null) {
            return false;
        }

        int damage = stack.getItemDamage();
        // 兼容两种格式：可能带或不带 item. 前缀
        return (unlocalizedName.equals("gt.metaitem.01") || unlocalizedName.equals("item.gt.metaitem.01"))
            && damage == 32468;
    }

    /**
     * 检查玩家背包或手中是否有指定物品
     */
    private boolean hasItemInInventoryOrHand(EntityClientPlayerMP player, ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }

        // 检查手中持有的物品
        ItemStack heldItem = player.getHeldItem();
        if (heldItem != null && heldItem.getItem() == stack.getItem()
            && heldItem.getItemDamage() == stack.getItemDamage()) {
            return true;
        }

        // 检查背包中的所有物品（包括快捷栏）
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            ItemStack invStack = player.inventory.mainInventory[i];
            if (invStack != null && invStack.getItem() == stack.getItem()) {
                // 检查metadata是否匹配（对于红石粉等需要精确匹配）
                if (invStack.getItemDamage() == stack.getItemDamage()) {
                    return true;
                }
            }
        }
        return false;
    }
}
