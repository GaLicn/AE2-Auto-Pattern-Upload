package com.gali.ae2_auto_pattern_upload.network.upload;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;

import com.gali.ae2_auto_pattern_upload.util.UploadUtil;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IMachineSet;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.container.slot.SlotRestrictedInput;
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

            if (UploadUtil.hasSamePatternInNetwork(grid, encodedPattern, player.worldObj)) {
                UploadUtil.returnBlankPatternToNetwork(grid, player, terminal, outputSlot, encodedPattern);
                sendMessage(player, "ae2_auto_pattern_upload.info.pattern_already_exists");
                return null;
            }

            if (!UploadUtil.canProviderAcceptPattern(target, encodedPattern)) {
                sendMessage(player, "ae2_auto_pattern_upload.info.pattern_not_valid_for_provider");
                return null;
            }

            try {
                boolean placedInProvider = UploadUtil.insertPatternIntoProvider(target, encodedPattern.copy());
                if (placedInProvider) {
                    UploadUtil.rememberLastUploadedPattern(player, target, encodedPattern);
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

        private void sendMessage(EntityPlayerMP player, String key) {
            if (player != null && key != null && !key.isEmpty()) {
                player.addChatMessage(new ChatComponentTranslation(key));
            }
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
    }
}
