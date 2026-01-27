package com.gali.mixin.client.accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor<T extends AbstractContainerMenu> {
    @Accessor("leftPos") int ae2apu$getLeftPos();
    @Accessor("topPos") int ae2apu$getTopPos();
    @Accessor("imageWidth") int ae2apu$getImageWidth();
    @Accessor("imageHeight") int ae2apu$getImageHeight();
}
