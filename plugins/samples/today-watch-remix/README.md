# Today Watch Remix 示例插件（中文）

这是一个面向 BiliPai Plugin SDK 的最小外部 Kotlin 推荐插件示例。它演示如何声明能力、实现 `RecommendationPluginApi`、编译插件载荷，并打包成可由 BiliPai 预览和授权的 `.bpplugin`。

> [!IMPORTANT]
> 当前宿主行为：BiliPai 可以预览、保存并授权生成的 `.bpplugin`，但尚不执行外部 Dex。因此这个示例用于开发适配、包格式验证和授权流程验证，不代表插件会立即参与线上推荐排序。

## 目录结构

```text
plugins/samples/today-watch-remix/
├── plugin-manifest.json
├── settings.gradle.kts
├── build.gradle.kts
└── plugin/
    ├── build.gradle.kts
    └── src/main/java/dev/example/todaywatchremix/TodayWatchRemixPlugin.kt
```

## 这个示例做了什么？

- 声明插件 ID：`dev.example.today_watch_remix`
- 申请能力：`RECOMMENDATION_CANDIDATES`、`LOCAL_HISTORY_READ`
- 读取 `RecommendationRequest.candidateVideos`
- 排除已消费视频
- 按点赞数优先、播放量其次排序
- 返回带 `score`、`confidence`、`explanation` 的推荐队列

核心实现位于：

```text
plugin/src/main/java/dev/example/todaywatchremix/TodayWatchRemixPlugin.kt
```

## 构建

本示例目录没有独立 Gradle wrapper。请在本目录调用仓库根 wrapper，并显式提供 Android SDK 路径：

```bash
ANDROID_HOME=/Users/yiyang/Library/Android/sdk ../../../gradlew -p . packageBpPlugin --no-daemon
```

输出文件：

```text
build/distributions/today-watch-remix.bpplugin
```

如果复制到仓库外独立开发，也可以在示例根目录创建 `local.properties`：

```properties
sdk.dir=/path/to/android/sdk
```

## 本仓库内开发和外部复制的差异

示例放在 BiliPai 仓库内时，`settings.gradle.kts` 使用复合构建，把 JitPack 坐标替换为本地 `:plugin-sdk`：

```kotlin
includeBuild("../../..") {
    dependencySubstitution {
        substitute(module("com.github.jay3-yy.BiliPai:plugin-sdk"))
            .using(project(":plugin-sdk"))
    }
}
```

如果你把示例复制到仓库外独立开发，需要：

1. 删除或调整 `includeBuild("../../..")`。
2. 在 `plugin/build.gradle.kts` 中把 `0.1.0-SNAPSHOT` 改成目标 tag 或 commit。
3. 保留 `google()`、`mavenCentral()`、`maven("https://jitpack.io")`。

## 修改成自己的插件

1. 修改 `plugin-manifest.json`：
   - `pluginId` 改成自己的反向域名 ID。
   - `displayName` 改成插件显示名称。
   - `entryClassName` 改成你的入口类完整类名。
   - `capabilities` 只保留实际需要的能力。
2. 修改 `TodayWatchRemixPlugin.kt`：
   - 类名和 package 与 `entryClassName` 保持一致。
   - `capabilityManifest` 与 `plugin-manifest.json` 保持一致。
   - 在 `buildRecommendations` 中实现自己的排序、过滤或分组逻辑。
3. 执行 `ANDROID_HOME=/Users/yiyang/Library/Android/sdk ../../../gradlew -p . packageBpPlugin --no-daemon`，或使用你自己工程内的 wrapper。
4. 在 BiliPai 插件中心选择生成的 `.bpplugin`，检查 manifest、SHA-256、签名状态和能力授权提示。

## 打包内容

生成的 `.bpplugin` 包含：

- `plugin-manifest.json`
- 编译后的示例类：`classes.jar`

`classes.jar` 当前只作为预览阶段的编译载荷保留。宿主 Dex 加载和运行能力尚未启用。

## 常见问题

**Q: 为什么安装后没有实际改变推荐结果？**

A: 当前宿主只支持 `.bpplugin` 的预览、保存和授权，不执行外部 Dex。要立即改变推荐排序，请先在源码内实现原生插件，或等待外部 Dex 执行能力开放。

**Q: manifest 和代码里的 `capabilityManifest` 都要写吗？**

A: 目前建议两处保持一致。文件用于包预览和授权，代码声明用于后续运行时入口和能力校验。

**Q: 可以申请 `NETWORK` 吗？**

A: 可以声明，但它是敏感能力，安装前需要用户明确批准。只有确实需要访问远程服务时才应申请。

**Q: 如何验证包结构？**

A: `.bpplugin` 是 ZIP。确认解压后根目录能直接看到 `plugin-manifest.json`，而不是多包了一层目录。
