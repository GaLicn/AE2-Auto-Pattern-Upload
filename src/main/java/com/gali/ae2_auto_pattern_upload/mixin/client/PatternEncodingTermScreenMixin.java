package com.gali.ae2_auto_pattern_upload.mixin.client;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.me.items.PatternEncodingTermScreen;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.WidgetStyle;
import appeng.client.gui.widgets.IconButton;
import appeng.util.Icon;
import appeng.menu.AEBaseMenu;
import com.gali.ae2_auto_pattern_upload.mixin.AEBaseScreenAccessor;
import com.gali.ae2_auto_pattern_upload.mixin.AbstractContainerScreenAccessor;
import com.gali.ae2_auto_pattern_upload.mixin.ScreenAccessor;
import com.gali.ae2_auto_pattern_upload.network.RequestProvidersListC2SPacket;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 在 AE2 原生样板编码终端中注入上传按钮。 */
@Mixin(value = AEBaseScreen.class, remap = false)
public abstract class PatternEncodingTermScreenMixin<T extends AEBaseMenu> {
    @Unique
    private IconButton ae2apu$uploadButton;

    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private void ae2apu$addUploadButton(CallbackInfo ci) {
        if (!((Object) this instanceof PatternEncodingTermScreen)) {
            return;
        }
        if (ae2apu$uploadButton == null) {
            ae2apu$uploadButton = new IconButton(button -> {
                var connection = Minecraft.getInstance().getConnection();
                if (connection != null) {
                    connection.send(RequestProvidersListC2SPacket.INSTANCE.toVanillaServerbound());
                }
            }) {
                @Override
                protected Icon getIcon() {
                    return Icon.ARROW_UP;
                }

                @Override
                public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                             float partialTick) {
                    if (!visible) {
                        return;
                    }

                    // 保持 1.21.1 的 16x16 图标按 0.75 倍绘制为 12x12。
                    int iconWidth = Math.round(16 * 0.75f);
                    int iconHeight = Math.round(16 * 0.75f);
                    if (isFocused()) {
                        graphics.fill(getX() - 1, getY() - 1, getX() + iconWidth + 1, getY(), 0xFFFFFFFF);
                        graphics.fill(getX() - 1, getY(), getX(), getY() + iconHeight, 0xFFFFFFFF);
                        graphics.fill(getX() + iconWidth, getY(), getX() + iconWidth + 1,
                                getY() + iconHeight, 0xFFFFFFFF);
                        graphics.fill(getX() - 1, getY() + iconHeight, getX() + iconWidth + 1,
                                getY() + iconHeight + 1, 0xFFFFFFFF);
                    }

                    var pose = graphics.pose();
                    pose.pushMatrix();
                    pose.translate(getX(), getY());
                    pose.scale(0.75f, 0.75f);
                    if (!isDisableBackground()) {
                        Blitter.icon(Icon.TOOLBAR_BUTTON_BACKGROUND).dest(0, 0).blit(graphics);
                    }
                    var iconBlitter = Blitter.icon(getIcon());
                    if (!active) {
                        iconBlitter.opacity(0.5f);
                    }
                    iconBlitter.dest(0, 0).blit(graphics);
                    pose.popMatrix();
                }

                @Override
                public Rect2i getTooltipArea() {
                    return new Rect2i(getX(), getY(), Math.round(16 * 0.75f), Math.round(16 * 0.75f));
                }
            };
            ae2apu$uploadButton.setTooltip(Tooltip.create(
                    Component.translatable("ae2_auto_pattern_upload.button.upload")));
        }
        ae2apu$updatePosition();
        ae2apu$attachButton();
    }

    @Inject(method = "containerTick", at = @At("TAIL"), remap = false)
    private void ae2apu$ensureUploadButton(CallbackInfo ci) {
        if (((Object) this instanceof PatternEncodingTermScreen) && ae2apu$uploadButton != null) {
            ae2apu$updatePosition();
            ae2apu$attachButton();
        }
    }

    @Unique
    private void ae2apu$updatePosition() {
        var screen = (AbstractContainerScreenAccessor<?>) this;
        try {
            ScreenStyle style = ((AEBaseScreenAccessor<?>) this).ae2apu$getStyle();
            WidgetStyle encodeWidget = style.getWidget("encodePattern");
            Rect2i bounds = new Rect2i(screen.ae2apu$getLeftPos(), screen.ae2apu$getTopPos(),
                    screen.ae2apu$getImageWidth(), screen.ae2apu$getImageHeight());
            var position = encodeWidget.resolve(bounds);
            int baseWidth = encodeWidget.getWidth() > 0 ? encodeWidget.getWidth() : 12;
            int baseHeight = encodeWidget.getHeight() > 0 ? encodeWidget.getHeight() : 12;
            int targetWidth = Math.max(10, Math.round(baseWidth * 0.75f));
            int targetHeight = Math.max(10, Math.round(baseHeight * 0.75f));
            ae2apu$uploadButton.setWidth(targetWidth);
            ae2apu$uploadButton.setHeight(targetHeight);
            // 按 EAEP 使用原始 encodePattern 宽度计算左侧间距。
            ae2apu$uploadButton.setX(position.getX() - baseWidth - 2);
            ae2apu$uploadButton.setY(position.getY());
        } catch (Throwable ignored) {
            ae2apu$uploadButton.setWidth(12);
            ae2apu$uploadButton.setHeight(12);
            ae2apu$uploadButton.setX(screen.ae2apu$getLeftPos() + screen.ae2apu$getImageWidth() - 18);
            ae2apu$uploadButton.setY(screen.ae2apu$getTopPos() + 88);
        }
    }

    @Unique
    private void ae2apu$attachButton() {
        ScreenAccessor accessor = (ScreenAccessor) this;
        if (!accessor.ae2apu$getRenderables().contains(ae2apu$uploadButton)) {
            accessor.ae2apu$getRenderables().add(ae2apu$uploadButton);
        }
        if (!accessor.ae2apu$getChildren().contains(ae2apu$uploadButton)) {
            accessor.ae2apu$getChildren().add(ae2apu$uploadButton);
        }
    }
}
