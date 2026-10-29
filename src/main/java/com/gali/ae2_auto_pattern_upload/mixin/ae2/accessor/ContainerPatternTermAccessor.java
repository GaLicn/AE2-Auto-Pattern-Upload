package com.gali.ae2_auto_pattern_upload.mixin.ae2.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.slot.SlotRestrictedInput;

@Mixin(value = ContainerPatternTerm.class, remap = false)
public interface ContainerPatternTermAccessor {

    @Accessor("patternSlotOUT")
    SlotRestrictedInput getPatternSlotOUT();
}
