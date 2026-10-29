package com.gali.ae2_auto_pattern_upload.mixin.ae2;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.gali.ae2_auto_pattern_upload.util.CircuitUtils;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.PatternMultiplierHelper;

/**
 * 调整样板倍增逻辑，使编程电路不参与接口样板的乘除计算
 */
@Mixin(value = PatternMultiplierHelper.class, remap = false)
public abstract class PatternMultiplierHelperMixin {

    /**
     * @author C-H716
     * @reason 计算最大可除位数时跳过编程电路
     */
    @Overwrite
    public static int getMaxBitDivider(ICraftingPatternDetails details) {
        int maxDiv = 62;
        for (IAEItemStack input : details.getInputs()) {
            if (input == null) continue;

            // 跳过编程器电路
            ItemStack stack = input.getItemStack();
            if (CircuitUtils.isProgrammingCircuit(stack)) {
                continue;
            }

            long size = input.getStackSize();
            if (size <= 0) continue;
            int tz = Math.min(Long.numberOfTrailingZeros(size), 62);
            if (tz < maxDiv) maxDiv = tz;
        }
        for (IAEItemStack out : details.getOutputs()) {
            if (out == null) continue;

            // 跳过编程器电路
            ItemStack stack = out.getItemStack();
            if (CircuitUtils.isProgrammingCircuit(stack)) {
                continue;
            }

            long size = out.getStackSize();
            if (size <= 0) continue;
            int tz = Math.min(Long.numberOfTrailingZeros(size), 62);
            if (tz < maxDiv) maxDiv = tz;
        }

        return maxDiv;
    }

    /**
     * @author C-H1716
     * @reason 应用样板数量修改时跳过编程电路
     */
    @Overwrite
    public static void applyModification(ItemStack stack, int bitMultiplier) {
        if (bitMultiplier == 0) return;

        boolean isDividing = false;
        if (bitMultiplier < 0) {
            isDividing = true;
            bitMultiplier = -bitMultiplier;
        }

        NBTTagCompound encodedValue = stack.stackTagCompound;
        if (encodedValue == null) return;

        final NBTTagList inTag = encodedValue.getTagList("in", NBT.TAG_COMPOUND);
        final NBTTagList outTag = encodedValue.getTagList("out", NBT.TAG_COMPOUND);

        // 处理输入物品
        for (int x = 0; x < inTag.tagCount(); x++) {
            final NBTTagCompound tag = inTag.getCompoundTagAt(x);
            if (tag.hasNoTags()) continue;

            // 跳过编程器电路
            if (CircuitUtils.isProgrammingCircuitFromNBT(tag)) {
                continue;
            }

            // 处理 Count 字段
            if (tag.hasKey("Count")) {
                tag.setInteger(
                    "Count",
                    isDividing ? tag.getInteger("Count") >> bitMultiplier : tag.getInteger("Count") << bitMultiplier);
            }
            // 处理 Cnt 字段
            if (tag.hasKey("Cnt", NBT.TAG_LONG)) {
                tag.setLong(
                    "Cnt",
                    isDividing ? tag.getLong("Cnt") >> bitMultiplier : tag.getLong("Cnt") << bitMultiplier);
            }
        }

        // 处理输出物品
        for (int x = 0; x < outTag.tagCount(); x++) {
            final NBTTagCompound tag = outTag.getCompoundTagAt(x);
            if (tag.hasNoTags()) continue;

            // 跳过编程器电路
            if (CircuitUtils.isProgrammingCircuitFromNBT(tag)) {
                continue;
            }

            // 处理 Count 字段
            if (tag.hasKey("Count")) {
                tag.setInteger(
                    "Count",
                    isDividing ? tag.getInteger("Count") >> bitMultiplier : tag.getInteger("Count") << bitMultiplier);
            }
            // 处理 Cnt 字段
            if (tag.hasKey("Cnt", NBT.TAG_LONG)) {
                tag.setLong(
                    "Cnt",
                    isDividing ? tag.getLong("Cnt") >> bitMultiplier : tag.getLong("Cnt") << bitMultiplier);
            }
        }
    }
}
