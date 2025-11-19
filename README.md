# AE2 Auto Pattern Upload (GTNH 1.7.10 Branch)

专为 GT New Horizons 1.7.10 客户端打造的 AE2 自动样板上传模组：在 NEI/NEE 中查看配方时自动抓取配方名称。

## 功能

1. **样板终端上传按钮**：在 AE2 标准与扩展样板终端注入上传按钮，直接把当前 NEI 配方推送至服务器。
2. **网络协议**：自定义数据包同步配方输入、输出以及名称，确保客户端与服务器保持一致。

## 依赖

- `com.github.GTNewHorizons:Applied-Energistics-2-Unofficial:rv3-beta-691-GTNH:dev`
- `com.github.GTNewHorizons:NotEnoughItems:2.8.19-GTNH:dev`
- `codechicken:CodeChickenLib:1.7.10-1.1.3.136:dev@jar`
- `libs/NotEnoughEnergistics-1.7.14.jar`

如需切换版本，请编辑 `dependencies.gradle`。

## 构建与调试

```bash
./gradlew build       # 构建模组
./gradlew runClient   # 在 GTNH Dev 环境调试
```

## 目录速览

- `src/main/java/com/gali/...`：核心逻辑、网络包、mixin
- `src/main/resources/mixins.ae2_auto_pattern_upload.json`：mixin 注册表

