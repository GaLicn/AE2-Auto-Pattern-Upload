package com.example.ae2_auto_pattern_upload;

import com.example.ae2_auto_pattern_upload.init.ModTileEntities;
import com.example.ae2_auto_pattern_upload.gui.ModGuiHandler;
import com.example.ae2_auto_pattern_upload.network.ModNetwork;
import com.example.ae2_auto_pattern_upload.proxy.CommonProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION,
        dependencies = "required-after:appliedenergistics2")
public class ExampleMod {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);
    @Mod.Instance(Tags.MOD_ID)
    public static ExampleMod INSTANCE;
    @SidedProxy(
        clientSide = "com.example.ae2_auto_pattern_upload.proxy.ClientProxy",
        serverSide = "com.example.ae2_auto_pattern_upload.proxy.CommonProxy")
    public static CommonProxy PROXY;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Hello From {}!", Tags.MOD_NAME);
        ModTileEntities.register();
        ModNetwork.registerPackets();
        NetworkRegistry.INSTANCE.registerGuiHandler(INSTANCE, new ModGuiHandler());
        PROXY.preInit();
    }

}
