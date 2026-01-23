package com.gali.ae2_auto_pattern_upload.init;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;

import com.gali.ae2_auto_pattern_upload.MyMod;
import com.gali.ae2_auto_pattern_upload.block.BlockLabeledWirelessTransceiver;
import com.gali.ae2_auto_pattern_upload.tile.TileLabeledWirelessTransceiver;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * 方块注册
 */
public class ModBlocks {

    public static Block labeledWirelessTransceiver;

    public static void init() {
        labeledWirelessTransceiver = new BlockLabeledWirelessTransceiver().setCreativeTab(CreativeTabs.tabRedstone);

        GameRegistry.registerBlock(labeledWirelessTransceiver, "labeledWirelessTransceiver");
        GameRegistry
            .registerTileEntity(TileLabeledWirelessTransceiver.class, MyMod.MODID + ":labeledWirelessTransceiver");
    }
}
