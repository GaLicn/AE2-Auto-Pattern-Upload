package com.gali.ae2_auto_pattern_upload.network;

import java.lang.reflect.Field;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import com.glodblock.github.client.gui.container.ContainerFluidPatternTerminal;
import com.glodblock.github.client.gui.container.ContainerFluidPatternTerminalEx;
import com.glodblock.github.client.gui.container.base.FCContainerEncodeTerminal;
import com.glodblock.github.common.item.ItemFluidEncodedPattern;
import com.glodblock.github.inventory.item.IItemPatternTerminal;

import appeng.api.AEApi;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IMachineSet;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.implementations.ContainerPatternTermEx;
import appeng.container.slot.SlotRestrictedInput;
import appeng.helpers.IInterfaceHost;
import appeng.parts.AEBasePart;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class UploadPatternPacket implements IMessage {

    private long providerId;

    public UploadPatternPacket() {}

    public UploadPatternPacket(long providerId) {
        this.providerId = providerId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.providerId = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.providerId);
    }

    public static class Handler implements IMessageHandler<UploadPatternPacket, IMessage> {

        @Override
        public IMessage onMessage(UploadPatternPacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) {
                return null;
            }

            Container container = player.openContainer;
            IActionHost terminal = resolveTerminal(container);
            if (terminal == null) {
                return null;
            }

            SlotRestrictedInput outputSlot = resolveOutputSlot(container);
            if (outputSlot == null) {
                return null;
            }

            ItemStack encodedPattern = outputSlot.getStack();
            if (encodedPattern == null || encodedPattern.stackSize <= 0) {
                return null;
            }

            if (!isSupportedPattern(encodedPattern)) {
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

                ICraftingProvider target = findProvider(grid, message.providerId);
                if (target == null) {
                    return null;
                }

                boolean placedInProvider = insertPatternIntoProvider(target, encodedPattern.copy());
                if (placedInProvider) {
                    outputSlot.putStack(null);
                    if (terminal instanceof AEBasePart part) {
                        part.saveChanges();
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }

            return null;
        }

        private boolean isSupportedPattern(ItemStack stack) {
            if (stack == null) {
                return false;
            }
            if (AEApi.instance()
                .definitions()
                .items()
                .encodedPattern()
                .isSameAs(stack)) {
                return true;
            }
            return stack.getItem() instanceof ItemFluidEncodedPattern;
        }

        private IActionHost resolveTerminal(Container container) {
            if (container instanceof ContainerPatternTerm term) {
                return term.getPatternTerminal();
            }
            if (container instanceof ContainerPatternTermEx termEx) {
                return termEx.getPatternTerminal();
            }
            if (container instanceof ContainerFluidPatternTerminal fluidTerm) {
                return fromPatternTerminal(fluidTerm.getPatternTerminal());
            }
            if (container instanceof ContainerFluidPatternTerminalEx fluidTermEx) {
                return fromPatternTerminal(fluidTermEx.getPatternTerminal());
            }
            return null;
        }

        private IActionHost fromPatternTerminal(IItemPatternTerminal terminal) {
            if (terminal instanceof IActionHost actionHost) {
                return actionHost;
            }
            return null;
        }

        private SlotRestrictedInput resolveOutputSlot(Container container) {
            try {
                if (container instanceof ContainerPatternTerm term) {
                    Field field = ContainerPatternTerm.class.getDeclaredField("patternSlotOUT");
                    field.setAccessible(true);
                    return (SlotRestrictedInput) field.get(term);
                }
                if (container instanceof ContainerPatternTermEx termEx) {
                    Field field = ContainerPatternTermEx.class.getDeclaredField("patternSlotOUT");
                    field.setAccessible(true);
                    return (SlotRestrictedInput) field.get(termEx);
                }
                if (container instanceof FCContainerEncodeTerminal fcContainer) {
                    Field field = FCContainerEncodeTerminal.class.getDeclaredField("patternSlotOUT");
                    field.setAccessible(true);
                    return (SlotRestrictedInput) field.get(fcContainer);
                }
            } catch (Exception ignored) {}
            return null;
        }

        private ICraftingProvider findProvider(IGrid grid, long providerId) {
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
                    if (System.identityHashCode(machine) == providerId) {
                        return (ICraftingProvider) machine;
                    }
                }
            }
            return null;
        }

        private boolean insertPatternIntoProvider(ICraftingProvider provider, ItemStack pattern) {
            // 优先处理接口（IInterfaceHost），确保插入到编码样板槽（patterns）
            if (provider instanceof IInterfaceHost host) {
                IInventory patterns = host.getPatterns();
                if (patterns != null && insertIntoPatternInventory(patterns, pattern)) {
                    host.saveChanges();
                    return true;
                }
            }

            if (provider instanceof IInventory inventory) {
                return insertIntoInventory(inventory, pattern);
            }

            return false;
        }

        /**
         * 将样板插入到编码样板槽（patterns）中
         * 确保只插入到允许放置编码样板的槽位
         */
        private boolean insertIntoPatternInventory(IInventory patterns, ItemStack pattern) {
            if (patterns == null) {
                return false;
            }

            for (int i = 0; i < patterns.getSizeInventory(); i++) {
                ItemStack slot = patterns.getStackInSlot(i);
                if (slot == null || slot.stackSize <= 0) {
                    // 检查该槽位是否允许放置编码样板
                    if (patterns.isItemValidForSlot(i, pattern)) {
                        ItemStack copy = pattern.copy();
                        copy.stackSize = 1;
                        patterns.setInventorySlotContents(i, copy);
                        patterns.markDirty();
                        return true;
                    }
                }
            }

            return false;
        }

        private boolean insertIntoInventory(IInventory inventory, ItemStack pattern) {
            if (inventory == null) {
                return false;
            }

            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                ItemStack slot = inventory.getStackInSlot(i);
                if (slot == null || slot.stackSize <= 0) {
                    ItemStack copy = pattern.copy();
                    copy.stackSize = 1;
                    inventory.setInventorySlotContents(i, copy);
                    inventory.markDirty();
                    return true;
                }
            }

            return false;
        }
    }
}
