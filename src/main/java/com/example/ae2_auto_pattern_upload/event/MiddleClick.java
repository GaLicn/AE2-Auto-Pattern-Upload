package com.example.ae2_auto_pattern_upload.event;

import com.example.ae2_auto_pattern_upload.network.ModNetwork;
import com.example.ae2_auto_pattern_upload.network.MiddleClickPullOrCraftPacket;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(Side.CLIENT)
public final class MiddleClick {
    private MiddleClick(){}

    @SubscribeEvent
    public static void onMouseClick(MouseEvent mouseEvent){
        if (mouseEvent.getButton()!=2 || !mouseEvent.isButtonstate()){
            return;
        }
        Minecraft mc =Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) {
            return;
        }
        EntityPlayerSP player = mc.player;
        RayTraceResult ray = player.rayTrace(5.0D,1.0F);
        if (ray == null || ray.typeOfHit != RayTraceResult.Type.BLOCK)return;

        BlockPos pos =ray.getBlockPos();
        IBlockState state = mc.world.getBlockState(pos);

        Block block = state.getBlock();
        Item item = Item.getItemFromBlock(block);
        ItemStack itemStack = new ItemStack(item,1,block.getMetaFromState(state));
        if (itemStack.isEmpty()) {
            return;
        }

        ModNetwork.CHANNEL.sendToServer(new MiddleClickPullOrCraftPacket(itemStack));
    }
}
