package com.example.ae2_auto_pattern_upload.client;

import com.example.ae2_auto_pattern_upload.Tags;
import com.example.ae2_auto_pattern_upload.init.ModItems;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public final class ClientRegistrationHandler {
    private ClientRegistrationHandler() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(
                ModItems.WIRELESS_TRANSCEIVER,
                0,
                new ModelResourceLocation(ModItems.WIRELESS_TRANSCEIVER.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(
                ModItems.LABELED_WIRELESS_TRANSCEIVER,
                0,
                new ModelResourceLocation(ModItems.LABELED_WIRELESS_TRANSCEIVER.getRegistryName(), "inventory"));
    }
}
