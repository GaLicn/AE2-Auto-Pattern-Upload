package com.gali.ae2_auto_pattern_upload.mixin.vp;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.minecraft.util.EnumChatFormatting;
import net.moecraft.nechar.NecharUtils;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;

import cpw.mods.fml.common.Loader;

/**
 * 为 VisualProspecting 的 GT 矿脉搜索补充拼音匹配支持
 */
@Mixin(value = VeinTypeCaching.class, remap = false)
public abstract class VeinTypeCachingMixin {

    /**
     * 接管矿脉搜索高亮计算，在原有正则匹配基础上追加拼音匹配
     */
    @Inject(method = "recalculateSearch", at = @At("HEAD"), cancellable = true)
    /**
     * 接管矿脉搜索高亮计算，在原有正则匹配基础上追加拼音匹配
     */
    private static void ae2_auto_pattern_upload$recalculateSearchWithNechar(Pattern filterPattern, CallbackInfo ci) {
        for (VeinType veinType : VeinTypeCaching.getVeinTypes()) {
            if (veinType == VeinType.NO_VEIN) continue;
            if (filterPattern != null) {
                List<String> searchableStrings = new ArrayList<String>(veinType.getOreMaterialNames());
                searchableStrings.add(veinType.getVeinName());
                final String query = filterPattern.pattern()
                    .replace("\\Q", "")
                    .replace("\\E", "");
                final boolean match = searchableStrings.stream()
                    .map(EnumChatFormatting::getTextWithoutFormattingCodes)
                    .filter(searchableString -> searchableString != null && !searchableString.isEmpty())
                    /**
                     * 先执行原有文本匹配，再在安装 NEChar 时回退到拼音匹配
                     */
                    .anyMatch(
                        searchableString -> ae2_auto_pattern_upload$matchesSearch(
                            searchableString,
                            query,
                            filterPattern));

                veinType.setNEISearchHighlight(match);
            } else {
                veinType.setNEISearchHighlight(true);
            }
        }
        ci.cancel();
    }

    /**
     * 先执行原有文本匹配，再在安装 NEChar 时回退到拼音匹配
     */
    private static boolean ae2_auto_pattern_upload$matchesSearch(String name, String query, Pattern filterPattern) {
        if (filterPattern.matcher(name.toLowerCase())
            .find()) {
            return true;
        }
        if (Loader.isModLoaded("nechar")) {
            try {
                return NecharUtils.contain(name, query, true);
            } catch (Throwable ignored) {}
        }
        return false;
    }
}
