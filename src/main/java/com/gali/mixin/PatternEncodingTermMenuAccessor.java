package com.gali.mixin;

import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.slot.RestrictedInputSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Mixin访问器，用于访问PatternEncodingTermMenu的私有字段
 */
@Mixin(PatternEncodingTermMenu.class)
public interface PatternEncodingTermMenuAccessor {
    
    @Accessor(value = "encodedPatternSlot", remap = false)
    RestrictedInputSlot getEncodedPatternSlot();
    
    @Accessor(value = "blankPatternSlot", remap = false)
    RestrictedInputSlot getBlankPatternSlot();
}
