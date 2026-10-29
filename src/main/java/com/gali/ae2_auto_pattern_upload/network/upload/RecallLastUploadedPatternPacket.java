package com.gali.ae2_auto_pattern_upload.network.upload;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;

import com.gali.ae2_auto_pattern_upload.util.UploadUtil;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.container.slot.SlotRestrictedInput;
import appeng.parts.AEBasePart;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class RecallLastUploadedPatternPacket implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<RecallLastUploadedPatternPacket, IMessage> {

        @Override
        public IMessage onMessage(RecallLastUploadedPatternPacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) {
                return null;
            }

            Container container = player.openContainer;
            IActionHost terminal = UploadUtil.resolveTerminal(container);
            SlotRestrictedInput outputSlot = UploadUtil.resolveOutputSlot(container);
            if (terminal == null || outputSlot == null) {
                return null;
            }

            ItemStack currentOutput = outputSlot.getStack();
            if (currentOutput != null && currentOutput.stackSize > 0) {
                sendMessage(player, "ae2_auto_pattern_upload.info.recall_output_not_empty");
                return null;
            }

            IGridNode node = terminal.getActionableNode();
            if (node == null || node.getGrid() == null) {
                return null;
            }

            IGrid grid = node.getGrid();
            ItemStack recalled = UploadUtil.takeLastUploadedPattern(player, grid);
            if (recalled == null) {
                sendMessage(player, "ae2_auto_pattern_upload.info.recall_not_found");
                return null;
            }

            recalled.stackSize = 1;
            outputSlot.putStack(recalled);
            if (terminal instanceof AEBasePart part) {
                part.saveChanges();
            }
            sendMessage(player, "ae2_auto_pattern_upload.info.recall_success");
            return null;
        }

        private void sendMessage(EntityPlayerMP player, String key) {
            if (player != null && key != null && !key.isEmpty()) {
                player.addChatMessage(new ChatComponentTranslation(key));
            }
        }
    }
}
