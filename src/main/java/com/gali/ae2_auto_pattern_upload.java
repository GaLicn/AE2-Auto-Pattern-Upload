package com.gali;

import com.gali.network.ModNetwork;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ae2_auto_pattern_upload.MODID)
public class ae2_auto_pattern_upload {

    public static final String MODID = "ae2_auto_pattern_upload";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ae2_auto_pattern_upload() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        modEventBus.addListener(this::commonSetup);
        
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("AE2 Auto Pattern Upload 正在初始化...");
        
        event.enqueueWork(() -> {
            ModNetwork.register();
            LOGGER.info("网络包已注册");
        });
    }
}
