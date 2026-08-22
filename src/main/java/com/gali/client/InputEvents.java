package com.gali.client;

import appeng.client.gui.me.common.MEStorageScreen;
import com.gali.compat.JeiRuntimeCompat;
import com.gali.mixin.client.accessor.MEStorageScreenAccessor;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;

import java.util.Optional;

/** 将 JEI 物品列表、普通书签和历史书签中的物品填入 AE2 搜索框。 */
@Mod.EventBusSubscriber(modid = "ae2_auto_pattern_upload", value = Dist.CLIENT)
public final class InputEvents {
    private InputEvents() {}

    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!ModList.get().isLoaded("jei") || !ModKeybindings.FILL_SEARCH_KEY.matches(event.getKeyCode(), event.getScanCode())) {
            return;
        }
        if (!(Minecraft.getInstance().screen instanceof MEStorageScreen<?> screen)) {
            return;
        }
        try {
            Optional<ITypedIngredient<?>> hovered = JeiRuntimeCompat.getIngredientUnderMouse();
            if (hovered.isEmpty()) return;
            String name = JeiRuntimeCompat.getTypedIngredientDisplayName(hovered.get());
            if (name == null || name.isBlank()) return;
            MEStorageScreenAccessor accessor = (MEStorageScreenAccessor) (Object) screen;
            accessor.ae2apu$getSearchField().setValue(name);
            accessor.ae2apu$setSearchText(name);
            event.setCanceled(true);
        } catch (Throwable ignored) {
            // JEI 或 AE2 内部接口变化时保持原版按键行为。
        }
    }
}
