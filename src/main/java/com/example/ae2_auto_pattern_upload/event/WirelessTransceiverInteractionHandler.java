package com.example.ae2_auto_pattern_upload.event;

import com.example.ae2_auto_pattern_upload.Tags;
import com.example.ae2_auto_pattern_upload.block.BlockWirelessTransceiver;
import com.example.ae2_auto_pattern_upload.tile.TileWirelessTransceiver;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class WirelessTransceiverInteractionHandler {
    private WirelessTransceiverInteractionHandler() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        if (world.isRemote) {
            return;
        }

        if (event.getHand() != EnumHand.MAIN_HAND) {
            return;
        }

        EntityPlayer player = event.getEntityPlayer();
        if (player == null || !player.isSneaking()) {
            return;
        }

        BlockPos pos = event.getPos();
        if (!(world.getBlockState(pos).getBlock() instanceof BlockWirelessTransceiver)) {
            return;
        }

        if (!(world.getTileEntity(pos) instanceof TileWirelessTransceiver)) {
            return;
        }

        TileWirelessTransceiver transceiver = (TileWirelessTransceiver) world.getTileEntity(pos);
        long step = getFrequencyStep(player.getHeldItemMainhand());
        transceiver.setFrequency(transceiver.getFrequency() + step);
        player.sendStatusMessage(
                new TextComponentTranslation(
                        "message.ae2_auto_pattern_upload.wireless_transceiver.frequency",
                        transceiver.getFrequency()),
                true);

        event.setUseBlock(net.minecraftforge.event.entity.player.PlayerInteractEvent.Result.DENY);
        event.setUseItem(net.minecraftforge.event.entity.player.PlayerInteractEvent.Result.DENY);
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
    }

    private static long getFrequencyStep(ItemStack stack) {
        if (!stack.isEmpty() && (stack.getItem() == Item.getItemFromBlock(net.minecraft.init.Blocks.REDSTONE_TORCH)
                || stack.getItem() == net.minecraft.init.Items.STICK)) {
            return 10L;
        }

        return 1L;
    }
}
