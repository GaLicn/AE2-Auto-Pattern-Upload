package com.gali.ae2_auto_pattern_upload.integration.jei;

import com.gali.ae2_auto_pattern_upload.AE2AutoPatternUpload;
import com.gali.ae2_auto_pattern_upload.compat.JeiRuntimeCompat;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@JeiPlugin
public class AE2AutoPatternUploadJeiPlugin implements IModPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath(
            AE2AutoPatternUpload.MODID, "jei_plugin");

    @Override
    public @NotNull Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime runtime) {
        JeiRuntimeCompat.setRuntime(runtime);
    }
}
