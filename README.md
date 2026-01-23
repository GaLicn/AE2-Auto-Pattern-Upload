# AE2 Auto Pattern Upload

*English · 中文双语简述*

AE2 Auto Pattern Upload 是一个 Applied Energistics 2 的轻量级附属模组，在样板终端中添加一个一键上传按钮，可把编码完成的样板直接送到已连接的样板供应器。

AE2 Auto Pattern Upload is a lightweight addon for Applied Energistics 2 that adds a one-click upload button to the Pattern Terminal, sending encoded patterns straight to connected pattern providers.

## 特性 · Features

- **一键上传样板 · One-click pattern upload** — 编码样板后点击上传按钮，直接发送到供应器，无需手动搬运。
- **无侵入界面整合 · Non-intrusive UI integration** — 在原版 AE2 样板编码终端旁边添加一个小按钮。
- **网络感知投递 · Network-aware delivery** — 自动识别同一 AE2 网络中的样板供应器，减少 GUI 切换。
- **独立模组 · Standalone mod** — 从 ExtendedAE-Plus 项目中提取的自动上传功能，适合只需要此功能的玩家。

## 前置 · Requirements

- Minecraft 1.20.1
- Forge 47.4.16+
- Applied Energistics 2 (modid: `ae2`)

## 安装 · Installation

1. 从 Releases 页面下载最新版本的 jar 文件
2. 将其放入 `mods/` 文件夹，确保 Applied Energistics 2 也已安装
3. 启动游戏（Forge 1.20.1）并确保两个模组都已启用

## 使用方法 · Usage

1. 打开已接入网络的 AE2 样板编码终端  
   Open an AE2 Pattern Encoding Terminal connected to your network
2. 按常规流程完成样板编码  
   Encode your pattern as usual
3. 点击编码按钮旁的上传按钮（↑），样板将自动传送到网络中的供应器  
   Click the upload button (↑) next to the encode button—the pattern is routed automatically to connected providers

## 兼容与说明 · Compatibility & Notes

- 对现有存档安全，不改变 AE2 的核心进程  
  Designed to be safe for existing worlds; no progression changes
- 客户端展示按钮，服务器负责实际样板传输  
  Fully client–server aware: GUI runs client-side, transfers are processed server-side

## 项目说明 · Project Notes

- 由 **GaLi** 开发与维护  
  Created and maintained by **GaLi**
- 本模组源自我个人的项目 **ExtendedAE-Plus (EAEP)** 中的自动上传功能，并以独立形式发布  
  This mod is a standalone extraction of the auto-upload workflow originally implemented in my other project **ExtendedAE-Plus (EAEP)**

## 版本历史 · Version History

### 1.0.0 (1.20.1)
- 首次发布 1.20.1 版本
- 从 1.12.2 版本移植核心功能
- 支持 AE2 样板编码终端的一键上传功能
- 优化了供应器选择界面

## 开源协议 · License

GNU LGPL 3.0

## 链接 · Links

- GitHub: https://github.com/GaLicn/AE2-Auto-Pattern-Upload
- Issues: https://github.com/GaLicn/AE2-Auto-Pattern-Upload/issues
