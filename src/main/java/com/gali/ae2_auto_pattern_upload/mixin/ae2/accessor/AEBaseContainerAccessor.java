package com.gali.ae2_auto_pattern_upload.mixin.ae2.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import appeng.api.networking.security.IActionHost;
import appeng.container.AEBaseContainer;

@Mixin(AEBaseContainer.class)
public interface AEBaseContainerAccessor {

    @Invoker(value = "getActionHost", remap = false)
    IActionHost invokeGetActionHost();
}
