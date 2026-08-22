package com.gali.ae2_auto_pattern_upload.mixin;

import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.slot.RestrictedInputSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PatternEncodingTermMenu.class)
public interface PatternEncodingTermMenuAccessor {
    @Accessor("encodedPatternSlot")
    RestrictedInputSlot ae2apu$getEncodedPatternSlot();
}
