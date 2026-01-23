package com.gali.ae2_auto_pattern_upload.init;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import appeng.api.AEApi;
import appeng.api.definitions.IItemDefinition;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * 配方注册
 */
public class ModRecipes {

    public static void init() {
        registerLabeledWirelessTransceiverRecipe();
    }

    private static void registerLabeledWirelessTransceiverRecipe() {
        // 获取AE2的赛特斯石英
        IItemDefinition certusQuartzCrystal = AEApi.instance().definitions().materials().certusQuartzCrystal();
        ItemStack certusQuartz = certusQuartzCrystal.maybeStack(1).orNull();
        
        if (certusQuartz != null) {
            // 配方: 玻璃, 末影珍珠, 玻璃
            //       赛特斯石英, 铁锭, 赛特斯石英
            //       赛特斯石英, 赛特斯石英, 赛特斯石英
            GameRegistry.addRecipe(
                new ItemStack(ModBlocks.labeledWirelessTransceiver),
                "GPG",
                "CIC",
                "CCC",
                'G', Blocks.glass,
                'P', Items.ender_pearl,
                'C', certusQuartz,
                'I', Items.iron_ingot
            );
        }
    }
}
