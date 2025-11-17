package com.example.ae2_auto_pattern_upload.client.event;

import com.example.ae2_auto_pattern_upload.network.ModNetwork;
import com.example.ae2_auto_pattern_upload.network.RequestProvidersListPacket;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 使用事件监听器在编码终端添加上传按钮
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(Side.CLIENT)
public class GuiScreenEventHandler {

    private static GuiButton uploadBtn = null;

    @SubscribeEvent
    public static void onGuiOpen(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen gui = event.getGui();
        if (gui == null) {
            uploadBtn = null;
            return;
        }
        // 检查是否是样板终端 GUI（使用类名检查避免编译时依赖）
        String guiClassName = gui.getClass().getSimpleName();
        if (!guiClassName.equals("GuiPatternTerm")) {
            uploadBtn = null;
            return;
        }

        try {
            //使用GuiContainer中get方法获取gui基础信息
            if (!(gui instanceof GuiContainer)) {
                uploadBtn = null;
                return;
            }
            GuiContainer container = (GuiContainer) gui;
            int guiLeft = container.getGuiLeft();
            int guiTop = container.getGuiTop();
            int ySize = container.getYSize();

            // - 按钮尺寸：12x12（缩放为 0.75 倍）
            int encodeButtonX = guiLeft + 147;
            int encodeButtonY = guiTop + ySize - 142;
            int uploadBtnWidth = 12;
            int uploadBtnHeight = 12;
            int uploadBtnX = encodeButtonX - uploadBtnWidth;  // 紧挨着编码按钮的左边
            int uploadBtnY = encodeButtonY + 2;
            
            // 创建上传按钮
            uploadBtn = new GuiButton(999, uploadBtnX, uploadBtnY, uploadBtnWidth, uploadBtnHeight, "↑");
            
            gui.buttonList.add(uploadBtn);

        } catch (Exception e) {
            System.err.println("[上传按钮] 添加失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @SubscribeEvent
    public static void onGuiButtonClick(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        // 检查是否点击了上传按钮
        if (uploadBtn != null && event.getButton() == uploadBtn) {
            // 发送请求供应器列表的包
            ModNetwork.CHANNEL.sendToServer(new RequestProvidersListPacket());
            event.setCanceled(true);
        }
    }

}
