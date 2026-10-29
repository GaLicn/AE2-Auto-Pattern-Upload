package com.gali.ae2_auto_pattern_upload;

import com.gali.ae2_auto_pattern_upload.client.event.GuiUploadButtonHandler;
import com.gali.ae2_auto_pattern_upload.client.event.KeyInputHandler;
import com.gali.ae2_auto_pattern_upload.client.event.MouseInputHandler;
import com.gali.ae2_auto_pattern_upload.util.RecipeNameUtil;

import cpw.mods.fml.common.event.FMLInitializationEvent;

public class ClientProxy extends CommonProxy {

    // Override CommonProxy methods here, if you want a different behaviour on the client (e.g. registering renders).
    // Don't forget to call the super methods as well.

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        // 初始化配方名称映射工具（仅在客户端）
        RecipeNameUtil.initClient();

        GuiUploadButtonHandler.register();
        KeyInputHandler.register();
        MouseInputHandler.register();
    }
}
