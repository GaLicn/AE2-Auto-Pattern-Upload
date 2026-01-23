package com.gali.ae2_auto_pattern_upload;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gali.ae2_auto_pattern_upload.init.ModBlocks;
import com.gali.ae2_auto_pattern_upload.network.ModNetwork;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;

@Mod(
    modid = MyMod.MODID,
    version = Tags.VERSION,
    name = "AE2 Auto Pattern Upload",
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:appliedenergistics2")
public class MyMod {

    public static final String MODID = "ae2_auto_pattern_upload";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.Instance(MODID)
    public static MyMod instance;

    @SidedProxy(
        clientSide = "com.gali.ae2_auto_pattern_upload.ClientProxy",
        serverSide = "com.gali.ae2_auto_pattern_upload.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
        ModNetwork.registerPackets();
        ModBlocks.init();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }
}
