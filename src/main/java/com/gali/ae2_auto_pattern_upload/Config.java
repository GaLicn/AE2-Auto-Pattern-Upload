package com.gali.ae2_auto_pattern_upload;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static String greeting = "Hello World";

    // 无线收发器配置
    public static double wirelessTransceiverIdlePower = 1.0;
    public static boolean wirelessCrossDimEnable = false;
    public static double wirelessMaxRange = 256.0;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        greeting = configuration.getString("greeting", Configuration.CATEGORY_GENERAL, greeting, "How shall I greet?");

        // 无线收发器配置
        wirelessTransceiverIdlePower = configuration.getFloat(
            "wirelessTransceiverIdlePower",
            "wireless",
            1.0F,
            0.0F,
            100.0F,
            "Idle power usage of wireless transceiver (AE/t)");

        wirelessCrossDimEnable = configuration
            .getBoolean("wirelessCrossDimEnable", "wireless", false, "Enable cross-dimension wireless connection");

        wirelessMaxRange = configuration.getFloat(
            "wirelessMaxRange",
            "wireless",
            256.0F,
            0.0F,
            10000.0F,
            "Maximum range for wireless connection (blocks)");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
