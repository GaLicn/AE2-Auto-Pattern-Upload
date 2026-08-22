package com.gali.ae2_auto_pattern_upload.client;

import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/** EA2 风格可变宽度输入框，保持输入区域与纹理边界一致。 */
public class ResizableAETextField extends EditBox {
    private static final Blitter BLITTER = Blitter.texture("guis/text_field.png", 128, 128);
    private static final int PADDING = 2;
    private static final int EDGE_HEIGHT = 2;

    private final ScreenStyle style;
    private final Rect2i visualBounds;
    @Nullable
    private Component placeholder;

    public ResizableAETextField(ScreenStyle style, Font font, int x, int y, int width, int height) {
        super(font, x + PADDING, y + Math.max(PADDING, (height - font.lineHeight) / 2),
                width - 2 * PADDING - font.width("_"), font.lineHeight, Component.empty());
        this.style = style;
        this.visualBounds = new Rect2i(x, y, width, height);
        setBordered(false);
        setTextColor(style.getColor(PaletteColor.TEXTFIELD_TEXT).toARGB());
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return visualBounds.contains((int) mouseX, (int) mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) {
            return true;
        }
        return isFocused() && canConsumeInput()
                && event.key() != GLFW.GLFW_KEY_TAB && event.key() != GLFW.GLFW_KEY_ESCAPE;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!isVisible()) {
            return;
        }
        Rect2i bounds = visualBounds;
        renderBackground(graphics, bounds, isFocused() ? 24 : 0);
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
        if (placeholder != null && !isFocused() && getValue().isEmpty()) {
            graphics.text(Minecraft.getInstance().font, placeholder, getX(), getY(),
                    style.getColor(PaletteColor.TEXTFIELD_PLACEHOLDER).toARGB(), false);
        }
    }

    private void renderBackground(GuiGraphicsExtractor graphics, Rect2i bounds, int textureY) {
        int centerWidth = Math.max(0, bounds.getWidth() - 2);
        int centerHeight = Math.max(0, bounds.getHeight() - EDGE_HEIGHT * 2);
        int sourceCenterWidth = Math.min(126, centerWidth);
        blit(graphics, 0, textureY, 1, EDGE_HEIGHT, bounds.getX(), bounds.getY(), 1, EDGE_HEIGHT);
        blit(graphics, 1, textureY, sourceCenterWidth, EDGE_HEIGHT,
                bounds.getX() + 1, bounds.getY(), centerWidth, EDGE_HEIGHT);
        blit(graphics, 127, textureY, 1, EDGE_HEIGHT,
                bounds.getX() + bounds.getWidth() - 1, bounds.getY(), 1, EDGE_HEIGHT);
        blit(graphics, 0, textureY + EDGE_HEIGHT, 1, 12 - EDGE_HEIGHT * 2,
                bounds.getX(), bounds.getY() + EDGE_HEIGHT, 1, centerHeight);
        blit(graphics, 1, textureY + EDGE_HEIGHT, sourceCenterWidth, 12 - EDGE_HEIGHT * 2,
                bounds.getX() + 1, bounds.getY() + EDGE_HEIGHT, centerWidth, centerHeight);
        blit(graphics, 127, textureY + EDGE_HEIGHT, 1, 12 - EDGE_HEIGHT * 2,
                bounds.getX() + bounds.getWidth() - 1, bounds.getY() + EDGE_HEIGHT, 1, centerHeight);
        int bottomY = bounds.getY() + bounds.getHeight() - EDGE_HEIGHT;
        blit(graphics, 0, textureY + 12 - EDGE_HEIGHT, 1, EDGE_HEIGHT,
                bounds.getX(), bottomY, 1, EDGE_HEIGHT);
        blit(graphics, 1, textureY + 12 - EDGE_HEIGHT, sourceCenterWidth, EDGE_HEIGHT,
                bounds.getX() + 1, bottomY, centerWidth, EDGE_HEIGHT);
        blit(graphics, 127, textureY + 12 - EDGE_HEIGHT, 1, EDGE_HEIGHT,
                bounds.getX() + bounds.getWidth() - 1, bottomY, 1, EDGE_HEIGHT);
    }

    private void blit(GuiGraphicsExtractor graphics, int sourceX, int sourceY, int sourceWidth, int sourceHeight,
                      int x, int y, int width, int height) {
        if (width > 0 && height > 0 && sourceWidth > 0 && sourceHeight > 0) {
            BLITTER.src(sourceX, sourceY, sourceWidth, sourceHeight).dest(x, y, width, height).blit(graphics);
        }
    }

    public void selectAll() {
        moveCursorTo(0, false);
        setHighlightPos(getValue().length());
    }

    public Rect2i getTooltipArea() {
        return visualBounds;
    }

    public void setPlaceholder(@Nullable Component placeholder) {
        this.placeholder = placeholder;
    }
}
