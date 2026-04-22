package com.example.ae2_auto_pattern_upload.init;

import com.example.ae2_auto_pattern_upload.item.ItemBlockWirelessTransceiver;
import com.example.ae2_auto_pattern_upload.item.ItemBlockLabeledWirelessTransceiver;

public final class ModItems {
    public static final ItemBlockWirelessTransceiver WIRELESS_TRANSCEIVER =
            new ItemBlockWirelessTransceiver(ModBlocks.WIRELESS_TRANSCEIVER);
    public static final ItemBlockLabeledWirelessTransceiver LABELED_WIRELESS_TRANSCEIVER =
            new ItemBlockLabeledWirelessTransceiver(ModBlocks.LABELED_WIRELESS_TRANSCEIVER);

    private ModItems() {
    }
}
