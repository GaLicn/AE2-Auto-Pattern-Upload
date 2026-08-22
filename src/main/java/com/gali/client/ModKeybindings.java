package com.gali.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import org.lwjgl.glfw.GLFW;

/** 定义与 EAEP 一致的 F 键搜索快捷键。 */
@Mod.EventBusSubscriber(modid = "ae2_auto_pattern_upload", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModKeybindings {
    public static final KeyMapping FILL_SEARCH_KEY = new KeyMapping(
            "key.ae2_auto_pattern_upload.fill_search", KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F,
            "key.categories.ae2_auto_pattern_upload");

    private ModKeybindings() {}

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(FILL_SEARCH_KEY);
    }
}
