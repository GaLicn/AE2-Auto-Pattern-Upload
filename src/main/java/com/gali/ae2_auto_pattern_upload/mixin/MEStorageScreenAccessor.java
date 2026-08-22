package com.gali.ae2_auto_pattern_upload.mixin;

import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.widgets.AETextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = MEStorageScreen.class, remap = false)
public interface MEStorageScreenAccessor {
    @Accessor("searchField")
    AETextField ae2apu$getSearchField();

    @Invoker("setSearchText")
    void ae2apu$setSearchText(String text);
}
