package com.gali.ae2_auto_pattern_upload;

import com.gali.ae2_auto_pattern_upload.network.ModNetwork;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** AE2 自动上传样板的模组入口，仅负责注册网络负载。 */
@Mod(AE2AutoPatternUpload.MODID)
public class AE2AutoPatternUpload {
    public static final String MODID = "ae2_auto_pattern_upload";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AE2AutoPatternUpload(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(ModNetwork::registerPayloadHandlers);
    }
}
