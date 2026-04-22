package com.example.ae2_auto_pattern_upload.init;

import com.example.ae2_auto_pattern_upload.block.BlockWirelessTransceiver;
import com.example.ae2_auto_pattern_upload.block.BlockLabeledWirelessTransceiver;

public final class ModBlocks {
    public static final BlockWirelessTransceiver WIRELESS_TRANSCEIVER = new BlockWirelessTransceiver();
    public static final BlockLabeledWirelessTransceiver LABELED_WIRELESS_TRANSCEIVER =
            new BlockLabeledWirelessTransceiver();

    private ModBlocks() {
    }
}
