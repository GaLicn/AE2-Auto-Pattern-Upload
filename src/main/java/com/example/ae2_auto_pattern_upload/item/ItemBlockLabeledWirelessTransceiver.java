package com.example.ae2_auto_pattern_upload.item;

import com.example.ae2_auto_pattern_upload.Tags;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class ItemBlockLabeledWirelessTransceiver extends ItemBlock {
    public ItemBlockLabeledWirelessTransceiver(Block block) {
        super(block);
        this.setRegistryName(block.getRegistryName());
        this.setTranslationKey(Tags.MOD_ID + ".labeled_wireless_transceiver");
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY + I18n.format("tooltip.ae2_auto_pattern_upload.labeled_wireless_transceiver.1"));
        tooltip.add(TextFormatting.GRAY + I18n.format("tooltip.ae2_auto_pattern_upload.labeled_wireless_transceiver.2"));
        tooltip.add(TextFormatting.GRAY + I18n.format("tooltip.ae2_auto_pattern_upload.labeled_wireless_transceiver.3"));
        tooltip.add(TextFormatting.DARK_GRAY
                + I18n.format("tooltip.ae2_auto_pattern_upload.labeled_wireless_transceiver.4"));
    }
}
