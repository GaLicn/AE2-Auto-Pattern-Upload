package com.gali.ae2_auto_pattern_upload.network.upload;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;

import com.gali.ae2_auto_pattern_upload.network.ModNetwork;
import com.gali.ae2_auto_pattern_upload.util.RecipeNameUtil;
import com.gali.ae2_auto_pattern_upload.util.UploadUtil;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.Upgrades;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IMachineSet;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.IInterfaceViewable;
import appeng.container.slot.SlotRestrictedInput;
import appeng.helpers.IInterfaceHost;
import appeng.parts.AEBasePart;
import appeng.parts.automation.UpgradeInventory;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 自动上传样板数据包 - 根据配方名称映射自动匹配供应器
 * 客户端发送映射后的名称和目标供应器名称列表，服务器根据列表进行匹配
 */
public class AutoUploadPatternPacket implements IMessage {

    private String mappedName; // 客户端查找后的映射名称

    public AutoUploadPatternPacket() {}

    public AutoUploadPatternPacket(String mappedName) {
        this.mappedName = mappedName;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        // 读取映射后的名称
        int len1 = buf.readShort();
        byte[] bytes1 = new byte[len1];
        buf.readBytes(bytes1);
        this.mappedName = new String(bytes1, StandardCharsets.UTF_8);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // 写入映射后的名称
        byte[] bytes1 = mappedName.getBytes(StandardCharsets.UTF_8);
        buf.writeShort(bytes1.length);
        buf.writeBytes(bytes1);
    }

    public static class Handler implements IMessageHandler<AutoUploadPatternPacket, IMessage> {

        @Override
        public IMessage onMessage(AutoUploadPatternPacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) {
                return null;
            }

            Container container = player.openContainer;
            IActionHost terminal = UploadUtil.resolveTerminal(container);
            if (terminal == null) {
                return null;
            }

            SlotRestrictedInput outputSlot = UploadUtil.resolveOutputSlot(container);
            if (outputSlot == null) {
                return null;
            }

            ItemStack encodedPattern = outputSlot.getStack();
            if (encodedPattern == null || encodedPattern.stackSize <= 0) {
                return null;
            }

            if (!UploadUtil.isSupportedPattern(encodedPattern)) {
                return null;
            }

            try {
                IGridNode node = terminal.getActionableNode();
                if (node == null) {
                    return null;
                }
                IGrid grid = node.getGrid();
                if (grid == null) {
                    return null;
                }

                // 直接使用客户端发送的映射名称
                String mappedName = message.mappedName;
                if (mappedName == null || mappedName.isEmpty()) {
                    return null;
                }

                if (UploadUtil.hasSamePatternInNetwork(grid, encodedPattern, player.worldObj)) {
                    UploadUtil.returnBlankPatternToNetwork(grid, player, terminal, outputSlot, encodedPattern);
                    sendMessageWithArgs(player, "ae2_auto_pattern_upload.info.pattern_already_exists");
                    return null;
                }

                ProviderMatchResult matchResult = findMatchingProvider(grid, mappedName);

                // Process based on match result
                if (matchResult.matchType == MatchType.EXACT_MATCH) {
                    if (!UploadUtil.canProviderAcceptPattern(matchResult.provider, encodedPattern)) {
                        sendMessageWithArgs(player, "ae2_auto_pattern_upload.info.pattern_not_valid_for_provider");
                        return null;
                    }

                    // Exact match, execute upload
                    boolean placedInProvider = UploadUtil
                        .insertPatternIntoProvider(matchResult.provider, encodedPattern.copy());

                    if (placedInProvider) {
                        UploadUtil.rememberLastUploadedPattern(player, matchResult.provider, encodedPattern);
                        outputSlot.putStack(null);
                        if (terminal instanceof AEBasePart part) {
                            part.saveChanges();
                        }
                        // 发送成功消息
                        sendSuccessMessage(player, mappedName);
                        // 通知客户端清除配方名称
                        ModNetwork.CHANNEL.sendTo(new ClearRecipeNamePacket(), player);
                    } else if (matchResult.canInstallCard) {
                        // 上传失败但可以安装容量卡，尝试自动安装
                        boolean installedAndPlaced = tryInstallCardAndPlacePattern(
                            player,
                            grid,
                            matchResult.provider,
                            encodedPattern.copy());
                        if (installedAndPlaced) {
                            UploadUtil.rememberLastUploadedPattern(player, matchResult.provider, encodedPattern);
                            outputSlot.putStack(null);
                            if (terminal instanceof AEBasePart part) {
                                part.saveChanges();
                            }
                            // 发送成功消息
                            sendSuccessMessage(player, mappedName);
                            // 通知客户端清除配方名称
                            ModNetwork.CHANNEL.sendTo(new ClearRecipeNamePacket(), player);
                        }
                    }
                }
                // PARTIAL_MATCH and NO_MATCH do nothing
            } catch (Throwable ignored) {}

            return null;
        }

        private void sendSuccessMessage(EntityPlayerMP player, String providerName) {
            if (player != null) {
                player.addChatMessage(
                    new ChatComponentTranslation("ae2_auto_pattern_upload.info.auto_upload_success", providerName));
            }
        }

        private void sendMessageWithArgs(EntityPlayerMP player, String key, Object... args) {
            if (player != null && key != null && !key.isEmpty()) {
                player.addChatMessage(new ChatComponentTranslation(key, args));
            }
        }

        private ProviderMatchResult findMatchingProvider(IGrid grid, String mappedName) {
            String normalizedMappedName = RecipeNameUtil.normalizeKey(mappedName);

            // Collect all providers
            List<ProviderInfo> allProviders = new ArrayList<>();
            for (Class<? extends IGridHost> hostClass : grid.getMachinesClasses()) {
                if (!ICraftingProvider.class.isAssignableFrom(hostClass)) {
                    continue;
                }
                IMachineSet machines = grid.getMachines(hostClass);
                if (machines == null) {
                    continue;
                }
                for (IGridNode machineNode : machines) {
                    if (machineNode == null) {
                        continue;
                    }
                    Object machine = machineNode.getMachine();
                    if (!(machine instanceof ICraftingProvider provider)) {
                        continue;
                    }

                    String providerName = resolveProviderName(machine);
                    String normalizedProviderName = RecipeNameUtil.normalizeKey(providerName);
                    boolean hasEmpty = hasEmptySlot(provider);
                    boolean canInstall = canInstallCapacityCard(provider);

                    ProviderInfo info = new ProviderInfo(
                        provider,
                        providerName,
                        normalizedProviderName,
                        hasEmpty,
                        canInstall);
                    allProviders.add(info);
                }
            }

            if (allProviders.isEmpty()) {
                return new ProviderMatchResult(null, MatchType.NO_MATCH);
            }

            // Step 1: Check for exact match
            List<ProviderInfo> exactMatches = new ArrayList<>();
            for (ProviderInfo info : allProviders) {
                if (info.normalizedName.equals(normalizedMappedName)) {
                    exactMatches.add(info);
                }
            }

            if (!exactMatches.isEmpty()) {
                // Check for variants (other providers starting with mapped name)
                for (ProviderInfo info : allProviders) {
                    if (!info.normalizedName.equals(normalizedMappedName)
                        && info.normalizedName.startsWith(normalizedMappedName)) {
                        // Variants exist, skip upload
                        return new ProviderMatchResult(null, MatchType.PARTIAL_MATCH);
                    }
                }

                // Select from exact matches with empty slots
                for (ProviderInfo info : exactMatches) {
                    if (info.hasEmptySlot) {
                        return new ProviderMatchResult(info.provider, MatchType.EXACT_MATCH);
                    }
                }

                // No exact match providers have empty slots, but check if can install card
                for (ProviderInfo info : exactMatches) {
                    if (info.canInstallCard) {
                        return new ProviderMatchResult(info.provider, MatchType.EXACT_MATCH, true);
                    }
                }

                // No exact match providers have empty slots or can install card
                return new ProviderMatchResult(null, MatchType.NO_MATCH);
            }

            // Step 2: Try fuzzy match (when no exact match)
            // Fuzzy match rule: provider name must start with mapped name
            List<ProviderInfo> fuzzyMatches = new ArrayList<>();
            for (ProviderInfo info : allProviders) {
                boolean isFuzzyMatch = info.normalizedName.startsWith(normalizedMappedName);

                if (isFuzzyMatch) {
                    fuzzyMatches.add(info);
                }
            }

            // If more than 1 fuzzy match, variants exist, skip upload
            if (fuzzyMatches.size() > 1) {
                return new ProviderMatchResult(null, MatchType.PARTIAL_MATCH);
            }

            // Only one fuzzy match, check for empty slot
            if (fuzzyMatches.size() == 1) {
                ProviderInfo info = fuzzyMatches.get(0);
                if (info.hasEmptySlot) {
                    return new ProviderMatchResult(info.provider, MatchType.EXACT_MATCH);
                } else if (info.canInstallCard) {
                    return new ProviderMatchResult(info.provider, MatchType.EXACT_MATCH, true);
                } else {
                    return new ProviderMatchResult(null, MatchType.NO_MATCH);
                }
            }

            return new ProviderMatchResult(null, MatchType.NO_MATCH);
        }

        private boolean hasEmptySlot(ICraftingProvider provider) {
            // 优先检查 AE2 标准接口（IInterfaceHost）
            if (provider instanceof IInterfaceHost host) {
                IInventory patterns = host.getPatterns();
                if (patterns != null) {
                    int availableSlots = host.rows() * host.rowSize();
                    int limit = Math.min(availableSlots, patterns.getSizeInventory());
                    for (int i = 0; i < limit; i++) {
                        ItemStack slot = patterns.getStackInSlot(i);
                        if (slot == null || slot.stackSize <= 0) {
                            return true;
                        }
                    }
                }
                return false;
            }
            // 检查 GT5 和 Programmable Hatches 的接口（IInterfaceViewable）
            if (provider instanceof IInterfaceViewable viewable) {
                IInventory patterns = viewable.getPatterns();
                if (patterns != null) {
                    int availableSlots = viewable.rows() * viewable.rowSize();
                    int limit = Math.min(availableSlots, patterns.getSizeInventory());
                    for (int i = 0; i < limit; i++) {
                        ItemStack slot = patterns.getStackInSlot(i);
                        if (slot == null || slot.stackSize <= 0) {
                            return true;
                        }
                    }
                }
                return false;
            }
            if (provider instanceof IInventory inv) {
                for (int i = 0; i < inv.getSizeInventory(); i++) {
                    ItemStack slot = inv.getStackInSlot(i);
                    if (slot == null || slot.stackSize <= 0) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * 检查是否可以安装样板容量卡
         */
        private boolean canInstallCapacityCard(ICraftingProvider provider) {
            if (!(provider instanceof IInterfaceHost host)) {
                return false;
            }

            try {
                // 检查当前已安装的样板容量卡数量
                int currentCards = host.getInstalledUpgrades(Upgrades.PATTERN_CAPACITY);

                // 获取升级槽位 Inventory
                IInventory upgrades = host.getInterfaceDuality()
                    .getInventoryByName("upgrades");
                if (upgrades == null) {
                    return false;
                }

                // 检查是否有空的升级槽位
                boolean hasEmptySlot = false;
                for (int i = 0; i < upgrades.getSizeInventory(); i++) {
                    ItemStack slot = upgrades.getStackInSlot(i);
                    if (slot == null || slot.stackSize <= 0) {
                        hasEmptySlot = true;
                        break;
                    }
                }

                if (!hasEmptySlot) {
                    return false;
                }

                // 获取最大可安装数量
                int maxCards = 3; // 默认最大 3 个
                if (upgrades instanceof UpgradeInventory) {
                    maxCards = ((UpgradeInventory) upgrades).getMaxInstalled(Upgrades.PATTERN_CAPACITY);
                }

                // 检查是否还能安装更多
                return currentCards < maxCards;
            } catch (Throwable t) {
                return false;
            }
        }

        /**
         * 尝试安装样板容量卡并放入样板
         */
        private boolean tryInstallCardAndPlacePattern(EntityPlayerMP player, IGrid grid, ICraftingProvider provider,
            ItemStack pattern) {
            if (!(provider instanceof IInterfaceHost host)) {
                return false;
            }

            try {
                // 获取样板容量卡物品
                ItemStack capacityCard = getPatternCapacityCard();
                if (capacityCard == null) {
                    return false;
                }

                // 检查网络中是否有样板容量卡
                IStorageGrid storageGrid = grid.getCache(IStorageGrid.class);
                if (storageGrid == null) {
                    return false;
                }

                IAEItemStack cardStack = AEApi.instance()
                    .storage()
                    .createItemStack(capacityCard);
                IAEItemStack extracted = storageGrid.getItemInventory()
                    .extractItems(cardStack, Actionable.MODULATE, new PlayerSource(player, host));

                if (extracted == null || extracted.getStackSize() <= 0) {
                    return false;
                }

                // 获取升级槽位 Inventory
                IInventory upgrades = host.getInterfaceDuality()
                    .getInventoryByName("upgrades");
                if (upgrades == null) {
                    // 返还提取的物品
                    storageGrid.getItemInventory()
                        .injectItems(extracted, Actionable.MODULATE, new PlayerSource(player, host));
                    return false;
                }

                // 找到空的升级槽位并放入卡片
                boolean inserted = false;
                for (int i = 0; i < upgrades.getSizeInventory(); i++) {
                    if (upgrades.getStackInSlot(i) == null || upgrades.getStackInSlot(i).stackSize <= 0) {
                        ItemStack cardToInsert = extracted.getItemStack()
                            .copy();
                        cardToInsert.stackSize = 1;
                        upgrades.setInventorySlotContents(i, cardToInsert);
                        inserted = true;
                        break;
                    }
                }

                if (!inserted) {
                    // 返还提取的物品
                    storageGrid.getItemInventory()
                        .injectItems(extracted, Actionable.MODULATE, new PlayerSource(player, host));
                    return false;
                }

                // 如果有剩余物品，返还到网络
                if (extracted.getStackSize() > 1) {
                    IAEItemStack remaining = extracted.copy();
                    remaining.setStackSize(extracted.getStackSize() - 1);
                    storageGrid.getItemInventory()
                        .injectItems(remaining, Actionable.MODULATE, new PlayerSource(player, host));
                }

                // 保存更改
                host.saveChanges();

                // 发送成功消息
                sendMessageWithArgs(player, "ae2_auto_pattern_upload.info.auto_install_card_success");

                // 尝试放入样板
                IInventory patterns = host.getPatterns();
                if (patterns != null) {
                    int availableSlots = host.rows() * host.rowSize();
                    return UploadUtil.insertIntoPatternInventory(patterns, pattern, availableSlots);
                }

                return false;
            } catch (Throwable t) {
                t.printStackTrace();
                return false;
            }
        }

        private ItemStack getPatternCapacityCard() {
            try {
                return AEApi.instance()
                    .definitions()
                    .materials()
                    .cardPatternCapacity()
                    .maybeStack(1)
                    .orNull();
            } catch (Throwable t) {
                t.printStackTrace();
                return null;
            }
        }

        private String resolveProviderName(Object machine) {
            String name = null;

            // 优先检查 IInterfaceViewable 的 getName() 方法 (GT5 和 Programmable Hatches 使用)
            if (machine instanceof IInterfaceViewable viewable) {
                try {
                    name = resolveDisplayName(viewable);
                } catch (Throwable ignored) {}
            }

            if (name == null || name.isEmpty()) {
                if (machine instanceof TileEntity tile) {
                    try {
                        if (tile.getBlockType() != null) {
                            name = tile.getBlockType()
                                .getLocalizedName();
                        }
                    } catch (Throwable ignored) {}

                    if (machine instanceof IInventory inv) {
                        try {
                            if (inv.hasCustomInventoryName()) {
                                name = inv.getInventoryName();
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            }
            if (machine instanceof AEBasePart part) {
                try {
                    String customName = part.getCustomName();
                    if (isValidProviderName(customName)) {
                        name = customName;
                    }
                } catch (Throwable ignored) {}
            }
            if (name == null || name.isEmpty()) {
                name = "ME Interface";
            }
            return name;
        }

        private String resolveDisplayName(IInterfaceViewable viewable) {
            String rawName = viewable.getRawName();
            String suffix = viewable.getNameSuffix();
            if (isValidProviderName(rawName)) {
                String translated = translateProviderName(rawName);
                if (translated != null && !translated.isEmpty()) {
                    return suffix == null ? translated : translated + suffix;
                }
                return suffix == null ? rawName : rawName + suffix;
            }

            String name = viewable.getName();
            if (isValidProviderName(name)) {
                String translated = translateProviderName(name);
                return translated == null || translated.isEmpty() ? name : translated;
            }
            return null;
        }

        private String translateProviderName(String name) {
            String translated = StatCollector.translateToLocal(name);
            if (translated == null || translated.isEmpty() || translated.equals(name)) {
                translated = StatCollector.translateToLocal(name + ".name");
            }
            return translated == null || translated.equals(name + ".name") ? name : translated;
        }

        private boolean isValidProviderName(String name) {
            if (name == null) {
                return false;
            }
            String trimmed = name.trim();
            if (trimmed.isEmpty() || "nothing".equalsIgnoreCase(trimmed)) {
                return false;
            }
            String lower = trimmed.toLowerCase(java.util.Locale.ROOT);
            return !lower.startsWith("appeng.") && !lower.startsWith("gregtech.")
                && !lower.startsWith("com.")
                && !lower.contains(".tile")
                && !lower.contains(".part");
        }

        private enum MatchType {
            NO_MATCH, // 没有匹配
            PARTIAL_MATCH, // 部分匹配（如"压印器-1"）
            EXACT_MATCH // 完全匹配
        }

        private static class ProviderMatchResult {

            final ICraftingProvider provider;
            final MatchType matchType;
            final boolean canInstallCard; // 是否可以安装样板容量卡

            ProviderMatchResult(ICraftingProvider provider, MatchType matchType) {
                this(provider, matchType, false);
            }

            ProviderMatchResult(ICraftingProvider provider, MatchType matchType, boolean canInstallCard) {
                this.provider = provider;
                this.matchType = matchType;
                this.canInstallCard = canInstallCard;
            }
        }

        private static class ProviderInfo {

            final ICraftingProvider provider;
            final String name;
            final String normalizedName;
            final boolean hasEmptySlot;
            final boolean canInstallCard; // 是否可以安装样板容量卡

            ProviderInfo(ICraftingProvider provider, String name, String normalizedName, boolean hasEmptySlot,
                boolean canInstallCard) {
                this.provider = provider;
                this.name = name;
                this.normalizedName = normalizedName;
                this.hasEmptySlot = hasEmptySlot;
                this.canInstallCard = canInstallCard;
            }
        }
    }
}
