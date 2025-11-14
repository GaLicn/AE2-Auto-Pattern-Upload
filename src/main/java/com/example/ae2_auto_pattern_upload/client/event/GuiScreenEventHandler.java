package com.example.ae2_auto_pattern_upload.client.event;

import com.example.ae2_auto_pattern_upload.network.ModNetwork;
import com.example.ae2_auto_pattern_upload.network.RequestProvidersListPacket;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 使用事件监听器在编码终端添加上传按钮
 * 使用反射避免编译时依赖问题
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
            // 使用反射获取 guiLeft, height, guiTop（1.12.2 使用 height 而不是 ySize）
            int guiLeft = (int) gui.getClass().getField("guiLeft").get(gui);
            int height = (int) gui.getClass().getField("height").get(gui);
            int guiTop = (int) gui.getClass().getField("guiTop").get(gui);
            
            // 编码按钮位置：guiLeft + 147, guiTop + height - 142
            // 参照 ExtendedAE Plus 的实现：
            // - 按钮尺寸：12x12（缩放为 0.75 倍）
            // - 位置：放在编码按钮的左边紧挨着，向下平移 16 像素
            int encodeButtonX = guiLeft + 147;
            int encodeButtonY = guiTop + height - 142;
            int uploadBtnWidth = 12;
            int uploadBtnHeight = 12;
            int uploadBtnX = encodeButtonX - uploadBtnWidth;  // 紧挨着编码按钮的左边
            int uploadBtnY = encodeButtonY + 12;  // 向下平移 16 像素
            
            // 创建上传按钮
            uploadBtn = new GuiButton(999, uploadBtnX, uploadBtnY, uploadBtnWidth, uploadBtnHeight, "↑");
            
            gui.buttonList.add(uploadBtn);
            
            // 调试信息
            System.out.println("[上传按钮] 位置: X=" + uploadBtnX + ", Y=" + uploadBtnY + 
                             ", 尺寸: " + uploadBtnWidth + "x" + uploadBtnHeight +
                             ", 编码按钮: (" + encodeButtonX + "," + encodeButtonY + ")");
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
