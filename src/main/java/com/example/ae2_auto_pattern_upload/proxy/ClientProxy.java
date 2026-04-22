package com.example.ae2_auto_pattern_upload.proxy;

import com.example.ae2_auto_pattern_upload.init.ModItems;
import com.example.ae2_auto_pattern_upload.client.gui.GuiLabeledWirelessTransceiver;
import com.example.ae2_auto_pattern_upload.client.gui.GuiProviderSelect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import com.example.ae2_auto_pattern_upload.network.LabelNetworkListS2CPacket;
import com.example.ae2_auto_pattern_upload.network.ProvidersListS2CPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        // Models are registered in ModelRegistryEvent.
    }

    @Override
    public void handleProvidersListS2C(ProvidersListS2CPacket message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            mc.displayGuiScreen(new GuiProviderSelect(mc.currentScreen, message.getIds(), message.getNames(), message.getEmptySlots()));
        });
	    }

    @Override
    public void handleLabelNetworkListS2C(LabelNetworkListS2CPacket message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            GuiScreen currentScreen = mc.currentScreen;
            if (currentScreen instanceof GuiLabeledWirelessTransceiver) {
                GuiLabeledWirelessTransceiver gui = (GuiLabeledWirelessTransceiver) currentScreen;
                if (gui.isFor(message.getPos())) {
                    gui.updateData(
                            message.getLabels(),
                            message.getChannels(),
                            message.getCurrentLabel(),
                            message.getCurrentOwner(),
                            message.getOnlineCount(),
                            message.getUsedChannels(),
                            message.getMaxChannels());
                }
            }
        });
    }
}
