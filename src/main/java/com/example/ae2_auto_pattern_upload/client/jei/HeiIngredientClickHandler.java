package com.example.ae2_auto_pattern_upload.client.jei;

import com.example.ae2_auto_pattern_upload.network.MiddleClickPullOrCraftPacket;
import com.example.ae2_auto_pattern_upload.network.ModNetwork;
import com.example.ae2_auto_pattern_upload.network.OpenCraftAmountFromBookmarkPacket;
import com.example.ae2_auto_pattern_upload.network.PullBookmarkItemPacket;
import mezz.jei.input.IClickedIngredient;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

public final class HeiIngredientClickHandler {

    private HeiIngredientClickHandler() {
    }

    public static boolean handle(IClickedIngredient<?> clicked, int mouseButton) {
        if (!isBookmarkAction(mouseButton) || clicked == null) {
            return false;
        }

        ItemStack stack = clicked.getCheatItemStack();
        if (stack.isEmpty()) {
            Object value = clicked.getValue();
            if (value instanceof ItemStack) {
                stack = ((ItemStack) value).copy();
            }
        } else {
            stack = stack.copy();
        }

        if (stack.isEmpty()) {
            return false;
        }

        if (mouseButton == 2) {
            ModNetwork.CHANNEL.sendToServer(new OpenCraftAmountFromBookmarkPacket(stack));
        } else {
            ModNetwork.CHANNEL.sendToServer(new MiddleClickPullOrCraftPacket(stack));
        }

        clicked.onClickHandled();
        return true;
    }

    private static boolean isBookmarkAction(int mouseButton) {
        return mouseButton == 2 || (mouseButton == 0 && GuiScreen.isShiftKeyDown());
    }
}
