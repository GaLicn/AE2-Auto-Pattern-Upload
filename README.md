# AE2 Auto Pattern Upload

*English · 中文双语简述*

AE2 Auto Pattern Upload is a lightweight addon for Applied Energistics 2 that adds a one-click upload button to the Pattern Terminal, sending encoded patterns straight to connected pattern providers.  
AE2 Auto Pattern Upload 是一个 Applied Energistics 2 的轻量级附属模组，在样板终端中添加一个一键上传按钮，可把编码完成的样板直接送到已连接的样板供应器。

## Features · 特性
- **One-click pattern upload · 一键上传样板** — Encode a pattern and send it to providers with a single button press,无需手动搬运。
- **Non-intrusive UI integration · 无侵入界面整合** — Reuses the native AE2 Pattern Terminal layout，只新增一个小按钮。
- **Network-aware delivery · 感知网络的投递** — Targets providers/storage blocks on the same AE2 network，减少 GUI 切换与 Shift+点击。
- **Standalone from ExtendedAE-Plus · 源自 ExtendedAE-Plus** — Extracted from the EAEP feature set for players who only need auto-upload.

## Requirements · 前置
- Minecraft 1.12.2
- Applied Energistics 2 / AE2 Extended Life (modid: `appliedenergistics2`)

## Installation · 安装
1. Download the latest release jar from the Releases tab.
2. Drop it into your `mods/` folder alongside Applied Energistics 2.
3. Launch the game (Forge 1.12.2) and ensure both mods are enabled.

## Usage · 使用方法
1. Open an AE2 Pattern Terminal connected to your network.  
   打开已接入网络的 AE2 样板终端。
2. Encode your pattern as usual.  
   按常规流程完成样板编码。
3. Click the new upload button next to the encode button—the pattern is routed automatically to connected providers.  
   点击编码按钮旁的上传按钮，样板将自动传送到网络中的供应器或存储方块。

## Compatibility & Notes · 兼容与说明
- Designed to be safe for existing worlds; no progression changes.  
  对现有存档安全，不改变 AE2 的核心进程。
- Fully client–server aware: GUI runs client-side, transfers are processed server-side.  
  客户端展示按钮，服务器负责实际样板传输。

## Project Notes · 项目说明
- Created and maintained by **GaLi**.  
  由 **GaLi** 开发与维护。
- This mod is a standalone extraction of the auto-upload workflow originally implemented in my other project **ExtendedAE-Plus (EAEP)**.  
  本模组源自我个人的项目 **ExtendedAE-Plus (EAEP)** 中的自动上传功能，并以独立形式发布。
