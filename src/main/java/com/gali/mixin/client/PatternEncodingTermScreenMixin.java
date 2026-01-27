package com.gali.mixin.client;

import appeng.client.gui.Icon;
import appeng.client.gui.me.items.PatternEncodingTermScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.WidgetStyle;
import appeng.client.gui.widgets.IconButton;
import com.gali.mixin.client.accessor.AEBaseScreenAccessor;
import com.gali.mixin.client.accessor.AbstractContainerScreenAccessor;
import com.gali.mixin.client.accessor.ScreenAccessor;
import com.gali.network.ModNetwork;
import com.gali.network.RequestProvidersListPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在样板编码终端界面添加上传按钮
 */
@Mixin(PatternEncodingTermScreen.class)
public abstract class PatternEncodingTermScreenMixin {

    @Unique
    private IconButton ae2apu$uploadBtn;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ae2apu$createUploadButton(CallbackInfo ci) {
        if (ae2apu$uploadBtn == null) {
            ae2apu$uploadBtn = ae2apu$createUploadButton();
        }
        ae2apu$addButtonToScreen();
    }

    @Inject(method = "updateBeforeRender", at = @At("TAIL"), remap = false)
    private void ae2apu$ensureUploadButton(CallbackInfo ci) {
        if (ae2apu$uploadBtn == null) return;
        ae2apu$updateUploadButtonPosition();
        ae2apu$addButtonToScreen();
    }

    @Unique
    private IconButton ae2apu$createUploadButton() {
        IconButton btn = new IconButton(button ->
                ModNetwork.CHANNEL.sendToServer(new RequestProvidersListPacket())
        ) {
            private final float ae2apu$scale = 0.75f;

            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
                if (!this.visible) return;

                var icon = this.getIcon();
                var blitter = icon.getBlitter();
                if (!this.active) blitter.opacity(0.5f);

                this.width = Math.round(16 * ae2apu$scale);
                this.height = Math.round(16 * ae2apu$scale);

                RenderSystem.disableDepthTest();
                RenderSystem.enableBlend();

                if (isFocused()) {
                    guiGraphics.fill(getX() - 1, getY() - 1, getX() + width + 1, getY(), 0xFFFFFFFF);
                    guiGraphics.fill(getX() - 1, getY(), getX(), getY() + height, 0xFFFFFFFF);
                    guiGraphics.fill(getX() + width, getY(), getX() + width + 1, getY() + height, 0xFFFFFFFF);
                    guiGraphics.fill(getX() - 1, getY() + height, getX() + width + 1, getY() + height + 1, 0xFFFFFFFF);
                }

                var pose = guiGraphics.pose();
                pose.pushPose();
                pose.translate(getX(), getY(), 0.0F);
                pose.scale(ae2apu$scale, ae2apu$scale, 1.f);
                if (!this.isDisableBackground()) {
                    Icon.TOOLBAR_BUTTON_BACKGROUND.getBlitter().dest(0, 0).blit(guiGraphics);
                }
                blitter.dest(0, 0).blit(guiGraphics);
                pose.popPose();

                RenderSystem.enableDepthTest();
            }

            @Override
            public Rect2i getTooltipArea() {
                return new Rect2i(getX(), getY(), Math.round(16 * ae2apu$scale), Math.round(16 * ae2apu$scale));
            }

            @Override
            protected Icon getIcon() {
                return Icon.ARROW_UP;
            }
        };
        btn.setTooltip(Tooltip.create(Component.literal("上传样板到供应器")));
        return btn;
    }

    @Unique
    private void ae2apu$updateUploadButtonPosition() {
        if (ae2apu$uploadBtn == null) return;

        AbstractContainerScreenAccessor<?> screen = (AbstractContainerScreenAccessor<?>) this;
        try {
            ScreenStyle style = ((AEBaseScreenAccessor<?>) this).ae2apu$getStyle();
            WidgetStyle ws = style.getWidget("encodePattern");

            var bounds = new Rect2i(
                    screen.ae2apu$getLeftPos(),
                    screen.ae2apu$getTopPos(),
                    screen.ae2apu$getImageWidth(),
                    screen.ae2apu$getImageHeight()
            );

            var pos = ws.resolve(bounds);
            int baseW = ws.getWidth() > 0 ? ws.getWidth() : 16;
            int baseH = ws.getHeight() > 0 ? ws.getHeight() : 16;

            int targetW = Math.max(10, Math.round(baseW * 0.75f));
            int targetH = Math.max(10, Math.round(baseH * 0.75f));

            ae2apu$uploadBtn.setWidth(targetW);
            ae2apu$uploadBtn.setHeight(targetH);
            ae2apu$uploadBtn.setX(pos.getX() - targetW);
            ae2apu$uploadBtn.setY(pos.getY());
        } catch (Throwable t) {
            int leftPos = screen.ae2apu$getLeftPos();
            int topPos = screen.ae2apu$getTopPos();
            int imageWidth = screen.ae2apu$getImageWidth();
            ae2apu$uploadBtn.setWidth(12);
            ae2apu$uploadBtn.setHeight(12);
            ae2apu$uploadBtn.setX(leftPos + imageWidth - 14);
            ae2apu$uploadBtn.setY(topPos + 88);
        }
    }

    @Unique
    private void ae2apu$addButtonToScreen() {
        var accessor = (ScreenAccessor) this;
        var renderables = accessor.ae2apu$getRenderables();
        var children = accessor.ae2apu$getChildren();
        if (!renderables.contains(ae2apu$uploadBtn)) renderables.add(ae2apu$uploadBtn);
        if (!children.contains(ae2apu$uploadBtn)) children.add(ae2apu$uploadBtn);
    }
}
