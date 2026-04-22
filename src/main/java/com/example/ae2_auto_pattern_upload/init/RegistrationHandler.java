package com.example.ae2_auto_pattern_upload.init;

import com.example.ae2_auto_pattern_upload.Tags;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class RegistrationHandler {
    private RegistrationHandler() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(ModBlocks.WIRELESS_TRANSCEIVER);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(ModItems.WIRELESS_TRANSCEIVER);
    }
}
