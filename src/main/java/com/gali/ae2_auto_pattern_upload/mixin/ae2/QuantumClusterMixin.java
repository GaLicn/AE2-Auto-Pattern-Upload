package com.gali.ae2_auto_pattern_upload.mixin.ae2;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import appeng.me.cluster.implementations.QuantumCluster;

/**
 * Mixin修改AE2量子环集群的激活机制
 * 实现效果：量子环只需要有量子纠缠奇点即可激活，不需要检查网络供电
 * 这样即使跨维度时某一端暂时无电，只要结构完整就能保持连接
 * <p>
 * 参考AE2 15.0.0-Alpha版本的实现：
 * <a href=
 * "https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/main/src/main/java/appeng/me/cluster/implementations/QuantumCluster.java">QuantumCluster.java</a>
 */
@Mixin(value = QuantumCluster.class, remap = false)
public abstract class QuantumClusterMixin {

    @Shadow
    private boolean isDestroyed;

    @Shadow
    private boolean registered;

    @Shadow
    private long thisSide;

    /**
     * 覆盖isActive方法的逻辑
     * 原版需要本端供电才返回true，现在只要有量子纠缠奇点即可返回true
     * 实现"无需供电检查，只检查结构完整性"的效果
     *
     * @reason 移除供电检查，实现类似AE2 15.0.0-Alpha版本的行为
     * @author C-H716
     */
    @Overwrite
    private boolean isActive() {
        if (this.isDestroyed || !this.registered) {
            return false;
        }

        // 只检查是否有量子纠缠奇点（thisSide != 0），不检查供电
        return this.thisSide != 0;
    }
}
