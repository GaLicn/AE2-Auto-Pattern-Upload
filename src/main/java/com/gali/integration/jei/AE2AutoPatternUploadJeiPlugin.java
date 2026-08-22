package com.gali.integration.jei;

import com.gali.ae2_auto_pattern_upload;
import com.gali.compat.JeiRuntimeCompat;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

/** 将 JEI Runtime 交给客户端兼容层，避免在模组初始化阶段访问 JEI 内部对象。 */
@JeiPlugin
public final class AE2AutoPatternUploadJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(
            ae2_auto_pattern_upload.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        JeiRuntimeCompat.setRuntime(runtime);
    }
}
