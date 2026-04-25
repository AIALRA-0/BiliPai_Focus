# AI Source Map / AI 事实导航

Verified path map for repository reading.
面向仓库阅读的已核实路径映射。

Maintained periodically, but still subject to freshness limits; reference only.
会定期维护，但仍存在时效性限制，仅供参考。

## Repository Paths / 仓库路径

| Path | Verified Note |
| --- | --- |
| `../../AI.txt` | AI compatibility entry / AI 兼容入口 |
| `../../llm.txt` | LLM compatibility entry / LLM 兼容入口 |
| `../../llms.txt` | canonical AI / LLM entry / 主 AI / LLM 入口 |
| `app/` | Android app module / Android 应用模块 |
| `baselineprofile/` | baseline profile module / Baseline Profile 模块 |
| `docs/wiki/` | wiki documents / Wiki 文档 |
| `docs/perf/` | performance notes / 性能记录 |
| `plugins/` | plugin files / 插件文件 |
| `scripts/` | shell scripts / Shell 脚本 |
| `settings.gradle.kts` | module declarations / 模块声明 |
| `build.gradle.kts` | root build config / 根构建配置 |
| `app/build.gradle.kts` | app build config / 应用构建配置 |

## Source Roots / 源码根路径

| Path | Verified Note |
| --- | --- |
| `app/src/main/java/com/android/purebilibili` | main source root / 主源码根路径 |
| `app/src/test/java/com/android/purebilibili` | unit test root / 单元测试根路径 |
| `app/src/androidTest/java` | instrumentation test root / 仪器测试根路径 |

## Primary Module Directories / 主要模块目录

| Path | Verified Note |
| --- | --- |
| `app/src/main/java/com/android/purebilibili/app` | app entry / 应用入口 |
| `app/src/main/java/com/android/purebilibili/core` | shared core / 公共核心层 |
| `app/src/main/java/com/android/purebilibili/data` | data layer / 数据层 |
| `app/src/main/java/com/android/purebilibili/domain` | domain layer / 领域层 |
| `app/src/main/java/com/android/purebilibili/feature` | feature layer / 功能层 |
| `app/src/main/java/com/android/purebilibili/navigation` | navigation layer / 导航层 |

## Task-to-File Map / 任务到文件映射

| Task | File |
| --- | --- |
| AI / LLM entry / AI / LLM 入口 | [`../../llms.txt`](../../llms.txt) |
| AI compatibility alias / AI 兼容入口 | [`../../AI.txt`](../../AI.txt) |
| LLM compatibility alias / LLM 兼容入口 | [`../../llm.txt`](../../llm.txt) |
| project overview / 项目总览 | [`../../README.md`](../../README.md) / [`../../README_EN.md`](../../README_EN.md) |
| release changes / 版本变更 | [`../../CHANGELOG.md`](../../CHANGELOG.md) |
| architecture / 架构 | [`ARCHITECTURE.md`](./ARCHITECTURE.md) |
| QA or regression / QA 与回归 | [`QA.md`](./QA.md) |
| release workflow / 发布流程 | [`RELEASE_WORKFLOW.md`](./RELEASE_WORKFLOW.md) |
| structure constraints / 结构约束 | [`../../STRUCTURE_GUIDELINES.adoc`](../../STRUCTURE_GUIDELINES.adoc) |
| JSON plugin development / JSON 插件开发 | [`../PLUGIN_DEVELOPMENT.md`](../PLUGIN_DEVELOPMENT.md) |
| native plugin development / 原生插件开发 | [`../NATIVE_PLUGIN_DEVELOPMENT.md`](../NATIVE_PLUGIN_DEVELOPMENT.md) |
| plugin examples / 插件示例 | [`../../plugins/samples/`](../../plugins/samples/) |
| community plugins / 社区插件 | [`../../plugins/community/README.md`](../../plugins/community/README.md) |

## Source Priority / 事实优先级

1. Source code and build files
1. 源码与构建文件
2. [`../../CHANGELOG.md`](../../CHANGELOG.md)
2. [`../../CHANGELOG.md`](../../CHANGELOG.md)
3. Files under `docs/wiki/`
3. `docs/wiki/` 下的文件
4. [`../../README.md`](../../README.md) and [`../../README_EN.md`](../../README_EN.md)
4. [`../../README.md`](../../README.md) 与 [`../../README_EN.md`](../../README_EN.md)
5. Files under `docs/plans/`
5. `docs/plans/` 下的文件

## Document Freshness / 文档时效

- `app/build.gradle.kts` currently declares `versionName = "7.8.0"` and `versionCode = 149`
- `app/build.gradle.kts` 当前声明 `versionName = "7.8.0"`、`versionCode = 149`
- `README.md` and `README_EN.md` header text: 2026-04-13 / v7.8.0
- `README.md` 与 `README_EN.md` 页头：2026-04-13 / v7.8.0
- `docs/wiki/README.md` and `docs/wiki/AI.md` refreshed: 2026-04-06
- `docs/wiki/README.md` 与 `docs/wiki/AI.md` 最近刷新：2026-04-06
- `CHANGELOG.md`, `README.md`, and `README_EN.md` align with `versionName = "7.8.0"` in `app/build.gradle.kts`
- `CHANGELOG.md`、`README.md` 与 `README_EN.md` 已与 `app/build.gradle.kts` 中的 `versionName = "7.8.0"` 对齐
- `AI.txt` and `llm.txt` are compatibility aliases that mirror the current AI entry guidance
- `AI.txt` 与 `llm.txt` 是当前 AI 入口说明的兼容别名
- Use code and `CHANGELOG.md` to verify release-specific answers.
- 涉及具体版本发布答案时，使用代码与 `CHANGELOG.md` 共同核验。

## Constraints / 约束

- `docs/plans/*.md` are planning records, not default evidence.
- `docs/plans/*.md` 是计划记录，不是默认事实证据。
- Current behavior must be verified in Kotlin or Gradle files.
- 当前行为必须在 Kotlin 或 Gradle 文件中核验。
- If a linked path changes, update this file after the tree changes.
- 如果链接路径变更，应在仓库树变更后更新本文件。

## BiliPai Skill Defaults / BiliPai 默认 Skill 组合

Use the lowest-overhead skill set that matches the task.
默认使用覆盖任务且开销最低的 skill 组合。

### Recommended Always-On Set / 推荐常驻组合

| Skill | Default Role | When to Prefer |
| --- | --- | --- |
| `android-jetpack-compose-expert` | primary UI architecture skill | Compose screen structure, state hoisting, navigation, recomposition, list performance |
| `android-native-dev` | Android platform and build fallback | Material 3, adaptive layout, accessibility, Gradle, build or device troubleshooting |
| `android-emulator-qa` | verification skill, enable on demand | emulator flow checks, screenshot capture, adb-driven UI validation, regression reproduction |

### Selection Rules / 选择规则

1. If the task changes a Compose screen, start with `android-jetpack-compose-expert`.
1. 如果任务修改 Compose 页面，优先使用 `android-jetpack-compose-expert`。
2. If the task also touches build config, resources, adaptive behavior, accessibility, or Android platform behavior, add `android-native-dev`.
2. 如果任务还涉及构建配置、资源、自适应布局、无障碍或 Android 平台行为，再补充 `android-native-dev`。
3. If the task changes gestures, overlays, player UI, settings flows, or any interaction that should be checked on device or emulator, add `android-emulator-qa` for verification.
3. 如果任务涉及手势、浮层、播放器 UI、设置流程，或任何需要在设备/模拟器上确认的交互，再补充 `android-emulator-qa` 做验证。
4. Do not default to heavy multi-phase workflow skills in this repository unless explicitly requested by the user.
4. 本仓库默认不要启用高开销多阶段 workflow skill，除非用户明确要求。

### Task Mapping / 任务映射

| Task Type | Preferred Skill |
| --- | --- |
| screen refactor / 页面重构 | `android-jetpack-compose-expert` |
| state or ViewModel flow cleanup / 状态流与 ViewModel 梳理 | `android-jetpack-compose-expert` |
| navigation or route wiring / 导航与路由接线 | `android-jetpack-compose-expert` |
| Material 3 or MIUIX style alignment / Material 3 或 MIUIX 风格对齐 | `android-native-dev` |
| accessibility or touch target review / 无障碍与触控目标检查 | `android-native-dev` |
| Gradle, manifest, install, build failure / Gradle、Manifest、安装、构建故障 | `android-native-dev` |
| emulator reproduction / 模拟器复现 | `android-emulator-qa` |
| player overlay, feed scroll, gesture regression / 播放器浮层、信息流滚动、手势回归 | `android-emulator-qa` |

### BiliPai-Specific Triggers / BiliPai 专用触发词

- Use `android-jetpack-compose-expert` for prompts like:
- 对以下提示优先使用 `android-jetpack-compose-expert`：
  `优化这个 Compose 页面`
  `拆分这个大 Screen`
  `减少重组`
  `整理 ViewModel 到无状态 UI`
  `修这个列表卡顿`
- Use `android-native-dev` for prompts like:
- 对以下提示优先使用 `android-native-dev`：
  `修复 assembleDebug`
  `检查 Material 3`
  `适配平板`
  `看一下无障碍`
  `处理 edge-to-edge / IME / insets`
- Use `android-emulator-qa` for prompts like:
- 对以下提示优先使用 `android-emulator-qa`：
  `帮我在模拟器走一遍流程`
  `复现这个 UI bug`
  `截一张当前页面图`
  `验证播放器返回前后台`
  `确认设置页交互`

### Scope Notes / 范围说明

- `android-jetpack-compose-expert` should lead feature UI work under `app/src/main/java/com/android/purebilibili/feature/`.
- `android-jetpack-compose-expert` 主要负责 `app/src/main/java/com/android/purebilibili/feature/` 下的功能 UI 工作。
- `android-native-dev` is the default secondary skill for module, build, manifest, resource, and platform-level concerns.
- `android-native-dev` 是模块、构建、Manifest、资源和平台级问题的默认辅助 skill。
- `android-emulator-qa` is verification-oriented; do not use it as the primary implementation skill.
- `android-emulator-qa` 偏验证，不应作为主要实现 skill。
