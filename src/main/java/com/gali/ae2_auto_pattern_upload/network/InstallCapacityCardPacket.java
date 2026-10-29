package com.gali.ae2_auto_pattern_upload.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;

import com.gali.ae2_auto_pattern_upload.mixin.ae2.accessor.AEBaseContainerAccessor;
import com.gali.ae2_auto_pattern_upload.network.provider.ProvidersListS2CPacket;
import com.glodblock.github.client.gui.container.ContainerFluidPatternEncoder;

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
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.implementations.ContainerPatternTermEx;
import appeng.helpers.IInterfaceHost;
import appeng.parts.AEBasePart;
import appeng.parts.automation.UpgradeInventory;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 请求为指定接口安装样板容量卡的数据包
 */
public class InstallCapacityCardPacket implements IMessage {

    private long providerId;

    public InstallCapacityCardPacket() {}

    public InstallCapacityCardPacket(long providerId) {
        this.providerId = providerId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        providerId = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(providerId);
    }

    public static class Handler implements IMessageHandler<InstallCapacityCardPacket, IMessage> {

        @Override
        public IMessage onMessage(InstallCapacityCardPacket message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) {
                return null;
            }

            Container container = player.openContainer;
            if (!(container instanceof ContainerPatternTerm) && !(container instanceof ContainerPatternTermEx)
                && !(container instanceof ContainerFluidPatternEncoder)) {
                return null;
            }

            try {
                IActionHost terminal = resolveTerminal(container);
                if (terminal == null) {
                    return null;
                }

                IGridNode node = terminal.getActionableNode();
                if (node == null) {
                    return null;
                }

                IGrid grid = node.getGrid();
                if (grid == null) {
                    return null;
                }

                // 查找目标接口
                IInterfaceHost targetInterface = findInterfaceById(grid, message.providerId);
                if (targetInterface == null) {
                    sendMessage(player, "ae2_auto_pattern_upload.info.interface_not_found");
                    return null;
                }

                // 检查是否还有升级槽位
                int currentUpgrades = targetInterface.getInstalledUpgrades(Upgrades.PATTERN_CAPACITY);

                // 获取升级槽位Inventory并检查最大可安装数量
                IInventory upgrades = targetInterface.getInterfaceDuality()
                    .getInventoryByName("upgrades");
                int maxUpgrades = 3; // 默认最大3个样板容量卡
                if (upgrades instanceof UpgradeInventory) {
                    maxUpgrades = ((UpgradeInventory) upgrades).getMaxInstalled(Upgrades.PATTERN_CAPACITY);
                }

                if (currentUpgrades >= maxUpgrades) {
                    sendMessage(player, "ae2_auto_pattern_upload.info.max_capacity_cards_reached");
                    return null;
                }

                // 尝试从网络中获取样板容量卡
                IStorageGrid storageGrid = grid.getCache(IStorageGrid.class);
                if (storageGrid == null) {
                    sendMessage(player, "ae2_auto_pattern_upload.info.no_storage_grid");
                    return null;
                }

                // 获取样板容量卡物品
                ItemStack capacityCard = getPatternCapacityCard();
                if (capacityCard == null) {
                    sendMessage(player, "ae2_auto_pattern_upload.info.capacity_card_not_craftable");
                    return null;
                }

                // 检查网络中是否有样板容量卡
                IAEItemStack cardStack = AEApi.instance()
                    .storage()
                    .createItemStack(capacityCard);
                IAEItemStack extracted = storageGrid.getItemInventory()
                    .extractItems(cardStack, Actionable.MODULATE, new PlayerSource(player, terminal));

                if (extracted == null || extracted.getStackSize() <= 0) {
                    sendMessage(player, "ae2_auto_pattern_upload.info.no_capacity_card_in_network");
                    return null;
                }

                // 检查升级槽位是否为null
                if (upgrades == null) {
                    // 返还提取的物品
                    storageGrid.getItemInventory()
                        .injectItems(extracted, Actionable.MODULATE, new PlayerSource(player, terminal));
                    sendMessage(player, "ae2_auto_pattern_upload.info.no_upgrade_slots");
                    return null;
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
                        .injectItems(extracted, Actionable.MODULATE, new PlayerSource(player, terminal));
                    sendMessage(player, "ae2_auto_pattern_upload.info.no_empty_upgrade_slot");
                    return null;
                }

                // 如果有剩余物品，返还到网络
                if (extracted.getStackSize() > 1) {
                    IAEItemStack remaining = extracted.copy();
                    remaining.setStackSize(extracted.getStackSize() - 1);
                    storageGrid.getItemInventory()
                        .injectItems(remaining, Actionable.MODULATE, new PlayerSource(player, terminal));
                }

                // 保存更改
                targetInterface.saveChanges();

                // 发送成功消息
                int newEmptySlots = calculateEmptySlots(targetInterface);
                sendMessageWithArgs(player, "ae2_auto_pattern_upload.info.capacity_card_installed", newEmptySlots);

                // 刷新供应器列表
                refreshProvidersList(player, container, grid);

            } catch (Throwable t) {
                t.printStackTrace();
            }

            return null;
        }

        private IActionHost resolveTerminal(Container container) {
            if (container instanceof ContainerPatternTerm term) {
                return term.getPatternTerminal();
            }
            if (container instanceof ContainerPatternTermEx termEx) {
                return termEx.getPatternTerminal();
            }
            if (container instanceof ContainerFluidPatternEncoder) {
                return ((AEBaseContainerAccessor) container).invokeGetActionHost();
            }
            return null;
        }

        private IInterfaceHost findInterfaceById(IGrid grid, long providerId) {
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
                    if (!(machine instanceof ICraftingProvider)) {
                        continue;
                    }

                    ICraftingProvider provider = (ICraftingProvider) machine;
                    long id = System.identityHashCode(provider);
                    if (id == providerId && machine instanceof IInterfaceHost) {
                        return (IInterfaceHost) machine;
                    }
                }
            }
            return null;
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

        private int calculateEmptySlots(IInterfaceHost host) {
            IInventory patterns = host.getPatterns();
            if (patterns != null) {
                int availableSlots = host.rows() * host.rowSize();
                int limit = Math.min(availableSlots, patterns.getSizeInventory());
                int empty = 0;
                for (int i = 0; i < limit; i++) {
                    ItemStack slot = patterns.getStackInSlot(i);
                    if (slot == null || slot.stackSize <= 0) {
                        empty++;
                    }
                }
                return empty;
            }
            return 0;
        }

        /**
         * 槽位信息
         */
        private static class SlotInfo {

            int emptySlots;
            boolean canInstallCard;

            SlotInfo(int emptySlots, boolean canInstallCard) {
                this.emptySlots = emptySlots;
                this.canInstallCard = canInstallCard;
            }
        }

        /**
         * 获取接口的槽位信息
         */
        private SlotInfo getSlotInfo(ICraftingProvider provider) {
            if (provider instanceof IInterfaceHost host) {
                int empty = calculateEmptySlots(host);
                boolean canInstall = canInstallCapacityCard(host);
                return new SlotInfo(empty, canInstall);
            }
            if (provider instanceof IInventory inv) {
                int empty = 0;
                for (int i = 0; i < inv.getSizeInventory(); i++) {
                    ItemStack slot = inv.getStackInSlot(i);
                    if (slot == null || slot.stackSize <= 0) {
                        empty++;
                    }
                }
                return new SlotInfo(empty, false);
            }
            return new SlotInfo(0, false);
        }

        /**
         * 检查是否可以安装更多样板容量卡
         */
        private boolean canInstallCapacityCard(IInterfaceHost host) {
            try {
                int currentCards = host.getInstalledUpgrades(Upgrades.PATTERN_CAPACITY);
                IInventory upgrades = host.getInterfaceDuality()
                    .getInventoryByName("upgrades");
                if (upgrades == null) {
                    return false;
                }

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

                int maxCards = 3;
                if (upgrades instanceof UpgradeInventory) {
                    maxCards = ((UpgradeInventory) upgrades).getMaxInstalled(Upgrades.PATTERN_CAPACITY);
                }

                return currentCards < maxCards;
            } catch (Throwable t) {
                return false;
            }
        }

        private void refreshProvidersList(EntityPlayerMP player, Container container, IGrid grid) {
            List<Long> ids = new ArrayList<Long>();
            List<String> names = new ArrayList<String>();
            List<Integer> emptySlots = new ArrayList<Integer>();
            List<Boolean> canInstallCard = new ArrayList<Boolean>();

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
                    if (!(machine instanceof ICraftingProvider)) {
                        continue;
                    }

                    ICraftingProvider provider = (ICraftingProvider) machine;
                    long id = System.identityHashCode(provider);
                    String name = resolveProviderName(machine);

                    // 获取槽位信息和是否可以装卡
                    SlotInfo slotInfo = getSlotInfo(provider);

                    // 只显示有用的接口
                    if (slotInfo.emptySlots > 0 || slotInfo.canInstallCard) {
                        ids.add(id);
                        names.add(name);
                        emptySlots.add(slotInfo.emptySlots);
                        canInstallCard.add(slotInfo.canInstallCard);
                    }
                }
            }

            ModNetwork.CHANNEL.sendTo(new ProvidersListS2CPacket(ids, names, emptySlots, canInstallCard), player);
        }

        private int estimateEmptySlots(ICraftingProvider provider) {
            if (provider instanceof IInterfaceHost host) {
                return calculateEmptySlots(host);
            }
            if (provider instanceof IInventory inv) {
                int empty = 0;
                for (int i = 0; i < inv.getSizeInventory(); i++) {
                    ItemStack slot = inv.getStackInSlot(i);
                    if (slot == null || slot.stackSize <= 0) {
                        empty++;
                    }
                }
                return empty;
            }
            return 0;
        }

        private String resolveProviderName(Object machine) {
            String name = "Crafting Provider";
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
            if (machine instanceof AEBasePart part) {
                try {
                    name = part.getCustomName();
                } catch (Throwable ignored) {}
            }
            return name;
        }

        private void sendMessage(EntityPlayerMP player, String key) {
            if (player != null && key != null && !key.isEmpty()) {
                // 使用ChatComponentTranslation让客户端自行翻译
                player.addChatMessage(new ChatComponentTranslation(key));
            }
        }

        private void sendMessageWithArgs(EntityPlayerMP player, String key, Object... args) {
            if (player != null && key != null && !key.isEmpty()) {
                // 使用ChatComponentTranslation让客户端自行翻译，支持参数
                player.addChatMessage(new ChatComponentTranslation(key, args));
            }
        }

        private String translate(EntityPlayerMP player, String key) {
            return StatCollector.translateToLocal(key);
        }
    }
}
