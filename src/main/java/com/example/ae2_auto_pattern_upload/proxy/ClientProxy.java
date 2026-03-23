package com.example.ae2_auto_pattern_upload.proxy;

import com.example.ae2_auto_pattern_upload.client.gui.GuiProviderSelect;
import com.example.ae2_auto_pattern_upload.network.ProvidersListS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
    @Override
    public void handleProvidersListS2C(ProvidersListS2CPacket message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            mc.displayGuiScreen(new GuiProviderSelect(mc.currentScreen, message.getIds(), message.getNames(), message.getEmptySlots()));
        });
    }
}
