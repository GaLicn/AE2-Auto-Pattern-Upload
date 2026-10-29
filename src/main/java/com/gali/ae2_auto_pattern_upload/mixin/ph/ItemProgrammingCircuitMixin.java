package com.gali.ae2_auto_pattern_upload.mixin.ph;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import gregtech.api.util.GTUtility;
import reobf.proghatches.item.ItemProgrammingCircuit;

/**
 * 调整编程电路物品名称显示
 */
@Mixin(value = ItemProgrammingCircuit.class)
public class ItemProgrammingCircuitMixin {

    /**
     * 在显示名称时为编程电路补充编号信息
     */
    @Inject(method = "getItemStackDisplayName", at = @At("HEAD"), cancellable = true)
    private void onGetItemStackDisplayName(ItemStack stack, CallbackInfoReturnable<String> cir) {
        ItemProgrammingCircuit.getCircuit(stack)
            .ifPresent(circuitStack -> {
                if (GTUtility
                    .areStacksEqual(circuitStack, GTUtility.getIntegratedCircuit(circuitStack.getItemDamage()))) {
                    int circuitNumber = circuitStack.getItemDamage();
                    String newName = String.format("编程器电路(编程电路%d)", circuitNumber);
                    cir.setReturnValue(newName);
                }
            });
    }
}
