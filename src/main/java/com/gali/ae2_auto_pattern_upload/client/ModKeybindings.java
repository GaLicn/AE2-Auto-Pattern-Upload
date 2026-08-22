package com.gali.ae2_auto_pattern_upload.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** 当前工程的客户端快捷键定义。 */
public final class ModKeybindings {
    public static final KeyMapping FILL_SEARCH_KEY = new KeyMapping(
            "key.ae2_auto_pattern_upload.fill_search",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            new KeyMapping.Category(Identifier.fromNamespaceAndPath("ae2_auto_pattern_upload", "category")));

    private ModKeybindings() {
    }
}
