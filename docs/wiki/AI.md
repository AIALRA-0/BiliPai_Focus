# AI Source Map / AI 事实导航

最后核对：2026-08-23。本文只提供仓库路径与事实优先级，不替代源码检查。

## 推荐入口

| 需求 | 文件 |
| --- | --- |
| 项目总览 | [`../../README.md`](../../README.md) / [`../../README_EN.md`](../../README_EN.md) |
| 最新完整发布记录 | [`../../CHANGELOG.md`](../../CHANGELOG.md) |
| 当前开发优先级 | [`ROADMAP.md`](ROADMAP.md) |
| 架构 | [`ARCHITECTURE.md`](ARCHITECTURE.md) |
| 功能状态 | [`FEATURE_MATRIX.md`](FEATURE_MATRIX.md) |
| UI 设计与验收 | [`ui-design/README.md`](ui-design/README.md) |
| QA 与回归 | [`QA.md`](QA.md) |
| 发布流程 | [`RELEASE_WORKFLOW.md`](RELEASE_WORKFLOW.md) |
| 版本规范 | [`VERSIONING.md`](VERSIONING.md) |
| JSON 与外部插件 | [`../PLUGIN_DEVELOPMENT.md`](../PLUGIN_DEVELOPMENT.md) |
| 源码级原生插件 | [`../NATIVE_PLUGIN_DEVELOPMENT.md`](../NATIVE_PLUGIN_DEVELOPMENT.md) |
| 插件 SDK | [`../../plugins/sdk/README.md`](../../plugins/sdk/README.md) |
| 结构约束 | [`../../STRUCTURE_GUIDELINES.adoc`](../../STRUCTURE_GUIDELINES.adoc) |

## 仓库结构

| 路径 | 职责 |
| --- | --- |
| `app/` | Android 主应用、功能界面、播放器、Navigation3 编排和测试 |
| `design-system/` | iOS、Material 3、Miuix 共用的主题、组件与视觉策略 |
| `settings-core/` | 可复用设置与偏好逻辑 |
| `network-core/` | 网络策略与底层网络支持 |
| `plugin-sdk/` | 外部插件可依赖的稳定接口 |
| `baselineprofile/` | 启动与帧性能基准配置 |
| `danmaku-engine/` | 项目内维护的弹幕渲染引擎与中立接口实现 |
| `dolby-ffmpeg-decoder/` | Media3 FFmpeg Dolby 音频解码扩展 |
| `plugins/` | 插件示例、SDK 文档、工具与社区目录 |
| `scripts/` | 发布、性能和辅助脚本 |

主源码位于 `app/src/main/java/com/android/purebilibili/`，其中：

- `app/`：应用入口与启动装配。
- `core/`：跨业务公共能力。
- `data/`：数据模型与仓库实现。
- `domain/`：稳定业务规则与 UseCase。
- `feature/`：按业务场景组织的界面与交互。
- `navigation/`：路由兼容、入口策略与顶层导航装配；`navigation3/`：当前 NavKey、返回栈、Entry/Scene、预测返回与整卡会话实现。

## 事实优先级

出现冲突时按以下顺序判断：

1. 当前源码和构建配置。
2. `CHANGELOG.md` 与 GitHub Releases。
3. `docs/wiki/ROADMAP.md`（仅用于优先级和状态，不代表已发布）。
4. Wiki 与插件开发文档。
5. `README.md` / `README_EN.md`。

当前 `app/build.gradle.kts` 声明构建 `0.2.3-beta.13 / versionCode 317`；`CHANGELOG.md` 最新记录为 `v0.2.3-beta.13`。公开发布状态仍以 GitHub / Telegram 为准。

当前构建基线为 minSdk 26、targetSdk 37、compileSdk 37、AGP 9.3.1、Gradle 9.5、Kotlin 2.4、JDK 21；Navigation3 runtime/UI 使用官方同版 `1.2.0-alpha07`，Miuix `0.9.4-4f86de92-SNAPSHOT` 用于主题、组件与视觉能力。

## 任务路由补充

- UI、主题、自适应布局或无障碍：先读 [`ui-design/README.md`](ui-design/README.md)，再定位 `design-system/` 与对应 `feature/`。
- 播放、转场、PiP 或 MediaSession：先读 [`ARCHITECTURE.md`](ARCHITECTURE.md) 与 [`QA.md`](QA.md)，再定位 `feature/video/` 和播放器 owner。
- 插件格式或能力边界：JSON/外部包读 [`../PLUGIN_DEVELOPMENT.md`](../PLUGIN_DEVELOPMENT.md)，源码插件读 [`../NATIVE_PLUGIN_DEVELOPMENT.md`](../NATIVE_PLUGIN_DEVELOPMENT.md)，稳定 API 读 [`../../plugins/sdk/README.md`](../../plugins/sdk/README.md)。
- 启动、滚动、转场或帧性能：定位 `baselineprofile/`、`docs/perf/` 与 `scripts/*perf*`，使用现有最小验证路径。
- 模块归属或新文件位置：先读 [`../../STRUCTURE_GUIDELINES.adoc`](../../STRUCTURE_GUIDELINES.adoc) 和模块 `build.gradle.kts`。

## 操作约束

- 遵循仓库根目录 `AGENTS.md`：用户明确要求时才编译；默认不打包、不安装 APK。
- 修改前检查工作区，保留用户已有改动；验证使用覆盖改动面的最小命令。
- 路线图描述的是优先级和完成条件，不能单独作为功能已发布的证据。

`AI.txt` 是兼容入口，主入口为 [`../../llms.txt`](../../llms.txt)。
