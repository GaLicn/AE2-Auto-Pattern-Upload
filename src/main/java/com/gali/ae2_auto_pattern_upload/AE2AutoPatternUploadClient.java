package com.gali.ae2_auto_pattern_upload;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = AE2AutoPatternUpload.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = AE2AutoPatternUpload.MODID, value = Dist.CLIENT)
public class AE2AutoPatternUploadClient {
    public AE2AutoPatternUploadClient(ModContainer container, IEventBus modEventBus) {
        // 按键注册属于模组事件总线，避免依赖已移除的模板配置界面。
        modEventBus.addListener((RegisterKeyMappingsEvent event) ->
                event.register(com.gali.ae2_auto_pattern_upload.client.ModKeybindings.FILL_SEARCH_KEY));
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        AE2AutoPatternUpload.LOGGER.info("AE2 Auto Pattern Upload client initialized");
    }
}
