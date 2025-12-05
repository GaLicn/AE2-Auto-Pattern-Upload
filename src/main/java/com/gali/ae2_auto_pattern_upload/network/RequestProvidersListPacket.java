package com.gali.ae2_auto_pattern_upload.network;

import java.util.ArrayList;
import java.util.List;

import appeng.parts.AEBasePart;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;

import com.glodblock.github.client.gui.container.ContainerFluidPatternTerminal;
import com.glodblock.github.client.gui.container.ContainerFluidPatternTerminalEx;
import com.glodblock.github.inventory.item.IItemPatternTerminal;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IMachineSet;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.implementations.ContainerPatternTermEx;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class RequestProvidersListPacket implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<RequestProvidersListPacket, IMessage> {

        @Override
        public IMessage onMessage(RequestProvidersListPacket message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) {
                return null;
            }

            Container container = player.openContainer;
            if (!(container instanceof ContainerPatternTerm) && !(container instanceof ContainerPatternTermEx)
                && !(container instanceof ContainerFluidPatternTerminal)
                && !(container instanceof ContainerFluidPatternTerminalEx)) {
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

                List<Long> ids = new ArrayList<Long>();
                List<String> names = new ArrayList<String>();
                List<Integer> emptySlots = new ArrayList<Integer>();

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

                        ids.add(id);
                        names.add(name);
                        emptySlots.add(estimateEmptySlots(provider));
                    }
                }

                ModNetwork.CHANNEL.sendTo(new ProvidersListS2CPacket(ids, names, emptySlots), player);
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

        private int estimateEmptySlots(ICraftingProvider provider) {
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
            if(machine instanceof AEBasePart part){
                try {
                    name = part.getCustomName();
                }catch (Throwable ignored){}
            }
            return name;
        }
    }
}
