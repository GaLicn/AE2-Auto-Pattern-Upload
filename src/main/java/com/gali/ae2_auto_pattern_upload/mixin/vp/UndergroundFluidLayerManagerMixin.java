package com.gali.ae2_auto_pattern_upload.mixin.vp;

import java.util.regex.Pattern;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.moecraft.nechar.NecharUtils;
import net.vfyjxf.nechar.utils.Match;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.sinthoras.visualprospecting.integration.model.layers.UndergroundFluidLayerManager;

import cpw.mods.fml.common.Loader;
import gregtech.api.enums.UndergroundFluidNames;
import it.unimi.dsi.fastutil.objects.ObjectSet;

/**
 * 为 VisualProspecting 的 GT 地下流体搜索补充拼音匹配支持
 */
@Mixin(value = UndergroundFluidLayerManager.class, remap = false)
public abstract class UndergroundFluidLayerManagerMixin {

    @Final
    @Shadow
    private static ObjectSet<Fluid> highlightedFluids;

    /**
     * 接管地下流体搜索结果计算，清空旧高亮后重新按普通匹配与拼音匹配计算
     */
    @Inject(method = "computeSearch", at = @At("HEAD"), cancellable = true)
    /**
     * 接管地下流体搜索结果计算，清空旧高亮后重新按普通匹配与拼音匹配计算
     */
    private void ae2_auto_pattern_upload$computeSearchWithNechar(Pattern filterPattern, CallbackInfo ci) {
        highlightedFluids.clear();
        if (filterPattern != null) {
            final String query = filterPattern.pattern()
                .replace("\\Q", "")
                .replace("\\E", "");
            for (UndergroundFluidNames fluidName : UndergroundFluidNames.values()) {
                Fluid fluid = FluidRegistry.getFluid(fluidName.name);
                if (fluid == null) continue;
                String name = fluid.getLocalizedName();
                /**
                 * 先执行原有文本匹配，再对中文本地化名称执行拼音匹配
                 */
                if (name != null && ae2_auto_pattern_upload$matchesSearch(name, query, filterPattern)) {
                    highlightedFluids.add(fluid);
                }
            }
        }
        ci.cancel();
    }

    /**
     * 先执行原有文本匹配，再对中文本地化名称执行拼音匹配
     */
    private static boolean ae2_auto_pattern_upload$matchesSearch(String name, String query, Pattern filterPattern) {
        if (filterPattern.matcher(name.toLowerCase())
            .find()) {
            return true;
        }
        if (Loader.isModLoaded("nechar") && Match.isChinese(name)) {
            try {
                return NecharUtils.contain(name, query, false);
            } catch (Throwable ignored) {}
        }
        return false;
    }
}
