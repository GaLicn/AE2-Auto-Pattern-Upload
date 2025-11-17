package com.example.ae2_auto_pattern_upload.mixin;

import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 通过 MixinBooter 的 ILateMixinLoader 接口注册本模组的 mixin 配置，
 */
public class AE2AutoPatternUploadMixinLoader implements ILateMixinLoader {

    @Override
    public List<String> getMixinConfigs() {
        // 仅注册一个配置文件：resources 根目录下的 mixins.ae2_auto_pattern_upload.json
        return Collections.singletonList("mixins.ae2_auto_pattern_upload.json");
    }

    @Override
    public boolean shouldMixinConfigQueue(String mixinConfig) {
        // 本模组不做条件判断，始终加载上面的配置即可
        return true;
    }
}
