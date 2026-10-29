package com.gali.ae2_auto_pattern_upload.crafting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.gali.ae2_auto_pattern_upload.network.ModNetwork;
import com.gali.ae2_auto_pattern_upload.network.crafting.RequestCraftingItemsPacket;

import appeng.api.storage.data.IAEItemStack;

/**
 * 缓存当前网络中CPU正在合成的物品
 * 正在合成的物品一直显示前置
 * 合成完成的物品延迟10秒后才取消前置显示
 */
public class CraftingItemsCache {

    // 使用物品哈希值作为key
    private static final Map<Integer, CraftingItemInfo> craftingItemsMap = new HashMap<>();
    private static long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 1000; // 每1000ms向服务器请求更新一次
    private static final long COMPLETED_ITEM_DELAY = 10000; // 合成完成后延迟10秒才取消显示

    /**
     * 存储物品信息的内部类
     */
    private static class CraftingItemInfo {

        final IAEItemStack stack;
        long addedTime; // 首次发现该物品合成的时间（或重新开始合成的时间）
        long completedTime; // 合成完成的时间（0表示仍在合成中）
        boolean isCompleted; // 是否已完成合成

        CraftingItemInfo(IAEItemStack stack, long addedTime) {
            this.stack = stack;
            this.addedTime = addedTime;
            this.completedTime = 0;
            this.isCompleted = false;
        }

        /**
         * 标记物品为已完成合成
         */
        void markCompleted(long time) {
            if (!this.isCompleted) {
                this.isCompleted = true;
                this.completedTime = time;
            }
        }

        /**
         * 重置物品为正在合成状态（当物品重新出现在合成列表中时）
         */
        void resetCrafting(long time) {
            this.isCompleted = false;
            this.completedTime = 0;
            this.addedTime = time;
        }

        /**
         * 检查是否应该显示前置
         * 正在合成的：一直显示
         * 已完成的：10秒内显示
         */
        boolean shouldShow(long currentTime) {
            if (!isCompleted) {
                // 正在合成中，一直显示
                return true;
            }
            // 已完成的，10秒内继续显示
            return currentTime - completedTime < COMPLETED_ITEM_DELAY;
        }
    }

    /**
     * 获取物品的唯一哈希值
     */
    private static int getItemHash(IAEItemStack stack) {
        if (stack == null) return 0;
        return stack.hashCode();
    }

    /**
     * 检查物品是否应该前置显示
     * O(1) 时间复杂度
     */
    public static boolean isCrafting(IAEItemStack stack) {
        if (stack == null) return false;
        requestUpdateIfNeeded();
        return isItemActive(stack);
    }

    /**
     * 从服务器更新数据
     * 单次遍历完成：标记完成 + 移除过期物品
     */
    public static void updateFromServer(Set<IAEItemStack> items) {
        long currentTime = System.currentTimeMillis();

        // 创建新物品的哈希集合，用于快速查找
        Set<Integer> newItemHashes = new HashSet<>(items.size() * 2);
        for (IAEItemStack item : items) {
            newItemHashes.add(getItemHash(item));
        }

        // 单次遍历：标记已完成 + 移除过期物品 + 重置重新合成的物品
        Iterator<Map.Entry<Integer, CraftingItemInfo>> iterator = craftingItemsMap.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, CraftingItemInfo> entry = iterator.next();
            CraftingItemInfo info = entry.getValue();
            int hash = entry.getKey();

            if (newItemHashes.contains(hash)) {
                // 物品在合成列表中
                if (info.isCompleted) {
                    // 如果之前被标记为完成，但现在又重新合成了，重置状态
                    info.resetCrafting(currentTime);
                }
                // 从newItemHashes中移除，剩下的就是新物品
                newItemHashes.remove(hash);
            } else {
                // 物品不再在合成列表中
                if (info.isCompleted) {
                    // 已标记完成的，检查是否超过10秒
                    if (currentTime - info.completedTime >= COMPLETED_ITEM_DELAY) {
                        iterator.remove();
                    }
                } else {
                    // 未标记完成的，标记为完成
                    info.markCompleted(currentTime);
                }
            }
        }

        // 添加新发现的物品（newItemHashes中剩下的就是新物品）
        for (int hash : newItemHashes) {
            // 找到对应的物品
            for (IAEItemStack item : items) {
                if (getItemHash(item) == hash) {
                    craftingItemsMap.put(hash, new CraftingItemInfo(item, currentTime));
                    break;
                }
            }
        }

        lastUpdateTime = currentTime;
    }

    /**
     * 请求服务器更新数据
     */
    private static void requestUpdateIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime > UPDATE_INTERVAL) {
            ModNetwork.INSTANCE.sendToServer(new RequestCraftingItemsPacket());
            lastUpdateTime = currentTime;
        }
    }

    /**
     * 获取当前应该显示为"正在合成"的物品
     */
    private static Set<IAEItemStack> getActiveCraftingItems() {
        long currentTime = System.currentTimeMillis();
        Set<IAEItemStack> activeItems = new HashSet<>();

        for (CraftingItemInfo info : craftingItemsMap.values()) {
            if (info.shouldShow(currentTime)) {
                activeItems.add(info.stack);
            }
        }

        return activeItems;
    }

    /**
     * 检查物品是否应该显示为活跃状态
     * O(1) 时间复杂度
     */
    private static boolean isItemActive(IAEItemStack stack) {
        int hash = getItemHash(stack);
        CraftingItemInfo info = craftingItemsMap.get(hash);

        if (info == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        if (!info.shouldShow(currentTime)) {
            craftingItemsMap.remove(hash);
            return false;
        }
        return true;
    }

    /**
     * 清空缓存
     */
    public static void clear() {
        craftingItemsMap.clear();
        lastUpdateTime = 0;
    }

    /**
     * 当玩家打开终端时调用
     * 只重置仍在显示窗口内的已完成物品，已经过期的物品直接清除。
     */
    public static void onTerminalOpened() {
        long currentTime = System.currentTimeMillis();

        Iterator<Map.Entry<Integer, CraftingItemInfo>> iterator = craftingItemsMap.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            CraftingItemInfo info = iterator.next()
                .getValue();
            if (info.isCompleted) {
                if (!info.shouldShow(currentTime)) {
                    iterator.remove();
                } else {
                    // 仍在10秒显示窗口内，重新打开终端后让玩家有完整时间看到它。
                    info.completedTime = currentTime;
                }
            }
        }
    }
}
