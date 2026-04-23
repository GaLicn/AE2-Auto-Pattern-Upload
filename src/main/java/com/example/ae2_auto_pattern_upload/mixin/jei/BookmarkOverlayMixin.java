package com.example.ae2_auto_pattern_upload.mixin.jei;

import com.example.ae2_auto_pattern_upload.network.ModNetwork;
import com.example.ae2_auto_pattern_upload.network.OpenCraftAmountFromBookmarkPacket;
import com.example.ae2_auto_pattern_upload.network.PullBookmarkItemPacket;
import mezz.jei.gui.overlay.bookmarks.BookmarkOverlay;
import mezz.jei.input.IClickedIngredient;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 直接拦截 HEI 书签覆盖层的点击处理，避免经过更外层分发器时出现命中丢失。
 */
@Mixin(value = BookmarkOverlay.class, remap = false)
public abstract class BookmarkOverlayMixin {

    @Shadow
    public abstract IClickedIngredient<?> getIngredientUnderMouse(int mouseX, int mouseY);

    @Inject(method = "handleMouseClicked", at = @At("HEAD"), cancellable = true)
    private void ae2_auto_pattern_upload$handleBookmarkActions(int mouseX,
                                                               int mouseY,
                                                               int mouseButton,
                                                               CallbackInfoReturnable<Boolean> cir) {
        if (!isBookmarkAction(mouseButton)) {
            return;
        }

        IClickedIngredient<?> clicked = this.getIngredientUnderMouse(mouseX, mouseY);
        if (clicked == null) {
            return;
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
            return;
        }

        if (mouseButton == 2) {
            ModNetwork.CHANNEL.sendToServer(new OpenCraftAmountFromBookmarkPacket(stack));
        } else {
            ModNetwork.CHANNEL.sendToServer(new PullBookmarkItemPacket(stack));
        }

        clicked.onClickHandled();
        cir.setReturnValue(true);
    }

    private static boolean isBookmarkAction(int mouseButton) {
        return mouseButton == 2 || (mouseButton == 0 && GuiScreen.isShiftKeyDown());
    }
}
