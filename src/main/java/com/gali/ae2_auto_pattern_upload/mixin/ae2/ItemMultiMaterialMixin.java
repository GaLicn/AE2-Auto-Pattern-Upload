package com.gali.ae2_auto_pattern_upload.mixin.ae2;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.items.materials.ItemMultiMaterial;
import appeng.items.materials.MaterialType;
import appeng.util.Platform;

/**
 * 为量子纠缠奇点提示补充频率信息显示
 */
@Mixin(value = ItemMultiMaterial.class, remap = false)
public class ItemMultiMaterialMixin {

    /**
     * 在物品提示信息末尾追加量子纠缠频率和十六进制表示
     */
    @Inject(method = "addCheckedInformation", at = @At("RETURN"))
    private void addCheckedInformation(ItemStack stack, EntityPlayer player, List<String> lines,
        boolean displayMoreInfo, CallbackInfo ci) {
        MaterialType mt = ((ItemMultiMaterial) (Object) this).getTypeByStack(stack);
        if (mt == MaterialType.QESingularity) {
            NBTTagCompound nbt = Platform.openNbtData(stack);
            if (nbt != null && nbt.hasKey("freq")) {
                long freq = nbt.getLong("freq");
                // 使用大写十六进制格式，不显示前导零
                String freqHex = String.format("%X", freq);

                lines.add(EnumChatFormatting.AQUA + "量子纠缠频率：" + EnumChatFormatting.WHITE + freq);
                lines.add(EnumChatFormatting.GRAY + "十六进制：" + EnumChatFormatting.GOLD + "0x" + freqHex);
            }
        }
    }
}
