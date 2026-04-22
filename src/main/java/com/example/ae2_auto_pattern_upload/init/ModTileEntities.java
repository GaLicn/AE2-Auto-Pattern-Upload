package com.example.ae2_auto_pattern_upload.init;

import appeng.core.features.IStackSrc;
import appeng.tile.AEBaseTile;
import com.example.ae2_auto_pattern_upload.Tags;
import com.example.ae2_auto_pattern_upload.tile.TileLabeledWirelessTransceiver;
import com.example.ae2_auto_pattern_upload.tile.TileWirelessTransceiver;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class ModTileEntities {
    private ModTileEntities() {
    }

    public static void register() {
        GameRegistry.registerTileEntity(
                TileWirelessTransceiver.class,
                new ResourceLocation(Tags.MOD_ID, "wireless_transceiver"));
        GameRegistry.registerTileEntity(
                TileLabeledWirelessTransceiver.class,
                new ResourceLocation(Tags.MOD_ID, "labeled_wireless_transceiver"));
        AEBaseTile.registerTileItem(
                TileWirelessTransceiver.class,
                new IStackSrc() {
                    @Override
                    public ItemStack stack(int i) {
                        return new ItemStack(ModItems.WIRELESS_TRANSCEIVER, i, getDamage());
                    }

                    @Override
                    public Item getItem() {
                        return ModItems.WIRELESS_TRANSCEIVER;
                    }

                    @Override
                    public int getDamage() {
                        return 0;
                    }

                    @Override
                    public boolean isEnabled() {
                        return true;
                    }
                });
        AEBaseTile.registerTileItem(
                TileLabeledWirelessTransceiver.class,
                new IStackSrc() {
                    @Override
                    public ItemStack stack(int i) {
                        return new ItemStack(ModItems.LABELED_WIRELESS_TRANSCEIVER, i, getDamage());
                    }

                    @Override
                    public Item getItem() {
                        return ModItems.LABELED_WIRELESS_TRANSCEIVER;
                    }

                    @Override
                    public int getDamage() {
                        return 0;
                    }

                    @Override
                    public boolean isEnabled() {
                        return true;
                    }
                });
    }
}
