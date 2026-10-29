package com.gali.ae2_auto_pattern_upload.util;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 工具类，用于识别编程器电路
 */
public class CircuitUtils {

    /**
     * 检查物品是否是编程器电路（PH虚拟电路或GT配置电路）
     */
    public static boolean isProgrammingCircuit(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }

        // 检查 PH 的编程电路
        String itemId = stack.getItem()
            .getClass()
            .getName();
        if (itemId.contains("prog_circuit") || itemId.contains("ItemProgrammingCircuit")) {
            return true;
        }

        // 检查 GT 配置电路 (ID 10407)
        String unlocalizedName = stack.getItem()
            .getUnlocalizedName();
        if (unlocalizedName != null && unlocalizedName.contains("integrated_circuit")) {
            return true;
        }

        // 检查物品ID
        String registryName = getRegistryName(stack);
        if (registryName != null) {
            if (registryName.contains("integrated_circuit") || registryName.equals("10407")
                || registryName.equals("10407s")
                || registryName.equals("11124")
                || registryName.equals("11124s")
                || registryName.equals("gregtech:gt.integrated_circuit")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 从 NBT 标签检查是否是编程器电路
     * 尝试多种方式识别，包括从NBT创建临时ItemStack检查
     */
    public static boolean isProgrammingCircuitFromNBT(NBTTagCompound tag) {
        if (tag == null) {
            return false;
        }

        String itemId = tag.getString("id");
        if (itemId == null || itemId.isEmpty()) {
            return false;
        }

        // PH 的编程电路
        if (itemId.contains("proghatches") && itemId.contains("prog_circuit")) {
            return true;
        }

        // GT 配置电路 - 支持客户端(10407)和服务器端(11124)的不同ID
        if (itemId.contains("integrated_circuit") || itemId.equals("10407")
            || itemId.equals("10407s")
            || itemId.equals("11124")
            || itemId.equals("11124s")
            || itemId.equals("gregtech:gt.integrated_circuit")) {
            return true;
        }

        // 尝试从NBT创建ItemStack进行更准确的检查
        // 这可以处理ID不同但unlocalizedName相同的情况
        try {
            ItemStack tempStack = createItemStackFromNBT(tag);
            if (tempStack != null && isProgrammingCircuit(tempStack)) {
                return true;
            }
        } catch (Exception e) {
            // 如果创建失败，忽略错误
        }

        return false;
    }

    /**
     * 从NBT创建临时ItemStack用于检查
     */
    private static ItemStack createItemStackFromNBT(NBTTagCompound tag) {
        try {
            // 获取物品ID
            String itemId = tag.getString("id");
            if (itemId == null || itemId.isEmpty()) {
                return null;
            }

            // 解析物品 - 1.7.10使用Item.itemRegistry
            Item item = null;
            if (Item.itemRegistry != null) {
                item = (Item) Item.itemRegistry.getObject(itemId);
            }
            // 如果字符串ID找不到，尝试解析为数字ID
            if (item == null) {
                try {
                    int id = Integer.parseInt(itemId);
                    item = Item.getItemById(id);
                } catch (NumberFormatException ignored) {}
            }
            if (item == null) {
                return null;
            }

            // 获取meta/damage值
            int meta = tag.getShort("Damage");
            if (tag.hasKey("tag", 10)) {
                NBTTagCompound itemTag = tag.getCompoundTag("tag");
                if (itemTag.hasKey("GT.ItemConfig")) {
                    // GT配置电路的特殊处理
                    meta = itemTag.getInteger("GT.ItemConfig");
                }
            }

            // 创建ItemStack
            ItemStack stack = new ItemStack(item, 1, meta);

            // 如果有tag，复制过去
            if (tag.hasKey("tag", 10)) {
                stack.setTagCompound(tag.getCompoundTag("tag"));
            }

            return stack;
        } catch (Exception e) {
            return null;
        }
    }

    private static String getRegistryName(ItemStack stack) {
        try {
            return stack.getItem().delegate.name();
        } catch (Exception e) {
            return null;
        }
    }
}
