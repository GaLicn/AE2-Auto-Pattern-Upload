package com.gali.ae2_auto_pattern_upload.client.handler;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.gali.ae2_auto_pattern_upload.network.ModNetwork;
import com.gali.ae2_auto_pattern_upload.network.upload.AutoUploadPatternPacket;
import com.gali.ae2_auto_pattern_upload.util.RecipeNameUtil;

/**
 * 自动上传处理器 - 通过 Mixin 在客户端编码按钮点击后自动上传
 */
public class AutoUploadHandler {

    private static final AutoUploadHandler INSTANCE = new AutoUploadHandler();
    private static final ScheduledExecutorService DELAY_EXECUTOR = Executors.newScheduledThreadPool(1);

    /**
     * 当编码按钮被点击时调用（由 Mixin 注入）
     * 这是在客户端执行的，此时配方名称已经被 NEI Mixin 捕获
     */
    public static void onEncodeButtonClicked() {
        INSTANCE.handleEncodeClick();
    }

    private void handleEncodeClick() {
        // 获取最后捕获的配方名称（由 NEI Mixin 在客户端设置）
        String recipeKey = RecipeNameUtil.getLastRecipeName();
        String rawRecipeId = RecipeNameUtil.getLastRawRecipeId();

        if (recipeKey == null || recipeKey.isEmpty()) {
            return;
        }

        // 在客户端查找映射
        String mappedName = findMappedName(recipeKey, rawRecipeId);

        if (mappedName == null || mappedName.isEmpty()) {
            return;
        }

        // 延迟 100ms 后异步发送自动上传请求到服务器
        // 这样可以确保服务器已经处理完编码操作
        DELAY_EXECUTOR.schedule(
            () -> { ModNetwork.CHANNEL.sendToServer(new AutoUploadPatternPacket(mappedName)); },
            100,
            TimeUnit.MILLISECONDS);
    }

    /**
     * 在客户端查找映射名称
     */
    private String findMappedName(String recipeKey, String rawRecipeId) {
        Map<String, String> mappings = RecipeNameUtil.getMappingsView();

        // 首先尝试 rawRecipeId
        if (rawRecipeId != null && !rawRecipeId.isEmpty()) {
            String mapped = mappings.get(rawRecipeId);
            if (mapped != null && !mapped.isEmpty()) {
                return mapped;
            }
        }

        // 然后尝试 recipeKey
        if (recipeKey != null && !recipeKey.isEmpty()) {
            String mapped = mappings.get(recipeKey);
            if (mapped != null && !mapped.isEmpty()) {
                return mapped;
            }
        }

        return null;
    }
}
