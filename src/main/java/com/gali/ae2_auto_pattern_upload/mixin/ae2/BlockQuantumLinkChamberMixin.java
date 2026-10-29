package com.gali.ae2_auto_pattern_upload.mixin.ae2;

import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gali.ae2_auto_pattern_upload.mixin.ae2.accessor.QuantumClusterAccessor;

import appeng.api.AEApi;
import appeng.api.features.ILocatable;
import appeng.block.qnb.BlockQuantumLinkChamber;
import appeng.me.cluster.implementations.QuantumCluster;
import appeng.tile.qnb.TileQuantumBridge;
import appeng.util.Platform;

/**
 * 为量子链接仓添加潜行空手查看对端量子环坐标的交互
 */
@Mixin(value = BlockQuantumLinkChamber.class)
public class BlockQuantumLinkChamberMixin {

    /**
     * 在潜行空手右键时拦截激活逻辑，并向玩家发送另一端量子环坐标
     */
    @Inject(method = "onActivated", at = @At("HEAD"), cancellable = true, remap = false)
    private void onBlockActivated(World w, int x, int y, int z, net.minecraft.entity.player.EntityPlayer p, int side,
        float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
        if (!p.isSneaking()) {
            return;
        }

        if (p.getCurrentEquippedItem() != null) {
            return;
        }

        if (!Platform.isServer()) {
            cir.setReturnValue(true);
            return;
        }

        TileQuantumBridge tg = ((BlockQuantumLinkChamber) (Object) this).getTileEntity(w, x, y, z);
        if (tg == null || !tg.isFormed()) {
            p.addChatMessage(new ChatComponentText("§c量子链接仓未成型"));
            cir.setReturnValue(true);
            return;
        }

        if (tg.getStackInSlot(0) == null) {
            p.addChatMessage(new ChatComponentText("§c量子链接仓中没有量子纠缠奇点"));
            cir.setReturnValue(true);
            return;
        }

        QuantumCluster cluster = (QuantumCluster) tg.getCluster();
        if (cluster == null) {
            p.addChatMessage(new ChatComponentText("§c量子链接仓未连接"));
            cir.setReturnValue(true);
            return;
        }

        long otherSide = ((QuantumClusterAccessor) (Object) cluster).getOtherSide();
        if (otherSide == 0) {
            p.addChatMessage(new ChatComponentText("§c未找到连接的另一个量子环"));
            cir.setReturnValue(true);
            return;
        }

        ILocatable otherLocatable = AEApi.instance()
            .registries()
            .locatable()
            .getLocatableBy(otherSide);
        if (!(otherLocatable instanceof QuantumCluster otherCluster)) {
            p.addChatMessage(new ChatComponentText("§c无法获取另一个量子环的信息"));
            cir.setReturnValue(true);
            return;
        }

        TileQuantumBridge otherCenter = ((QuantumClusterAccessor) (Object) otherCluster).getCenter();
        if (otherCenter == null) {
            p.addChatMessage(new ChatComponentText("§c另一个量子环不可用"));
            cir.setReturnValue(true);
            return;
        }

        World otherWorld = otherCenter.getWorldObj();
        if (otherWorld == null) {
            p.addChatMessage(new ChatComponentText("§c另一个量子环所在维度未加载"));
            cir.setReturnValue(true);
            return;
        }

        String dimensionName = otherWorld.provider.getDimensionName();
        int dimId = otherWorld.provider.dimensionId;
        int otherX = otherCenter.xCoord;
        int otherY = otherCenter.yCoord;
        int otherZ = otherCenter.zCoord;

        IChatComponent message = new ChatComponentText("§a另一个量子环位置：");

        IChatComponent dimInfo = new ChatComponentText(String.format("维度 %s (ID: %d)", dimensionName, dimId));
        dimInfo.getChatStyle()
            .setColor(EnumChatFormatting.GOLD);
        message.appendSibling(dimInfo);

        message.appendText(", ");

        IChatComponent coordText = new ChatComponentText(String.format("坐标 (%d, %d, %d)", otherX, otherY, otherZ));
        coordText.getChatStyle()
            .setColor(EnumChatFormatting.YELLOW);
        coordText.getChatStyle()
            .setUnderlined(true);

        // 点击坐标后可直接执行传送命令
        coordText.getChatStyle()
            .setChatClickEvent(
                new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    String.format("/ae2tp %d %d %d %d", otherX, otherY + 2, otherZ, dimId)));

        String hoverText = String
            .format("§a点击传送到：%s (ID: %d)\n坐标：(%d, %d, %d)", dimensionName, dimId, otherX, otherY + 2, otherZ);
        coordText.getChatStyle()
            .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(hoverText)));

        message.appendSibling(coordText);

        p.addChatMessage(message);

        cir.setReturnValue(true);
    }
}
