package com.gali.client;

import appeng.client.gui.me.items.PatternEncodingTermScreen;
import com.gali.network.ModNetwork;
import com.gali.network.RequestProvidersListPacket;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;

/**
 * 在样板编码终端添加上传按钮
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PatternTerminalButtonHandler {
    
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PatternEncodingTermScreen screen)) {
            return;
        }
        
        try {
            // 通过反射获取GUI的基础位置信息
            int guiLeft = getFieldValue(screen, "leftPos");
            int guiTop = getFieldValue(screen, "topPos");
            int ySize = getFieldValue(screen, "imageHeight");
            
            // 计算上传按钮位置（在编码按钮旁边）
            int encodeButtonX = guiLeft + 147;
            int encodeButtonY = guiTop + ySize - 142;
            int uploadBtnX = encodeButtonX - 14;
            int uploadBtnY = encodeButtonY + 2;
            
            // 创建上传按钮
            Button uploadButton = Button.builder(
                Component.literal("↑"),
                btn -> ModNetwork.CHANNEL.sendToServer(new RequestProvidersListPacket())
            ).bounds(uploadBtnX, uploadBtnY, 12, 12).build();
            
            event.addListener(uploadButton);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static int getFieldValue(AbstractContainerScreen<?> screen, String fieldName) throws Exception {
        Field field = AbstractContainerScreen.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getInt(screen);
    }
}
