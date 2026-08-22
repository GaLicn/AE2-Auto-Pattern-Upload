package com.gali.ae2_auto_pattern_upload.client;

import appeng.client.gui.me.common.MEStorageScreen;
import com.gali.ae2_auto_pattern_upload.compat.JeiRuntimeCompat;
import com.gali.ae2_auto_pattern_upload.mixin.MEStorageScreenAccessor;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import java.util.Optional;

/** 将 JEI 鼠标下的物品名称填入 AE2 终端搜索栏。 */
@EventBusSubscriber(modid = "ae2_auto_pattern_upload", value = Dist.CLIENT)
public final class InputEvents {
    private InputEvents() {
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!ModKeybindings.FILL_SEARCH_KEY.matches(new net.minecraft.client.input.KeyEvent(
                event.getKeyCode(), event.getScanCode(), event.getModifiers()))) {
            return;
        }

        if (!(Minecraft.getInstance().screen instanceof MEStorageScreen<?> screen)) {
            return;
        }
        Optional<?> hovered = JeiRuntimeCompat.getIngredientUnderMouse();
        if (hovered.isEmpty()) {
            return;
        }
        String name = JeiRuntimeCompat.getTypedIngredientDisplayName(hovered.get());
        if (name == null || name.isBlank()) {
            return;
        }
        try {
            MEStorageScreenAccessor accessor = (MEStorageScreenAccessor) (Object) screen;
            accessor.ae2apu$getSearchField().setValue(name);
            accessor.ae2apu$setSearchText(name);
            event.setCanceled(true);
        } catch (Throwable ignored) {
            // 终端版本不兼容时不阻断原版按键处理。
        }
    }
}
