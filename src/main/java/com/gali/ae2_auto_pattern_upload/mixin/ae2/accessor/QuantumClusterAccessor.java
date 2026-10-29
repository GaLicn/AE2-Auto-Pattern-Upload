package com.gali.ae2_auto_pattern_upload.mixin.ae2.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import appeng.me.cluster.implementations.QuantumCluster;
import appeng.tile.qnb.TileQuantumBridge;

@Mixin(QuantumCluster.class)
public interface QuantumClusterAccessor {

    @Accessor(value = "otherSide", remap = false)
    long getOtherSide();

    @Accessor(value = "center", remap = false)
    TileQuantumBridge getCenter();
}
