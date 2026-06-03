# BiliPai Plugin SDK（中文）

`plugin-sdk` 是 BiliPai 面向外部 Kotlin 插件作者暴露的公共 API。它只包含插件可依赖的数据结构和接口，不暴露 Compose、Android `Context`、账号凭证、宿主仓库或 BiliPai 内部单例。

> [!IMPORTANT]
> 当前状态：预览与授权阶段。BiliPai 可以解析 `.bpplugin`，展示 manifest、SHA-256、签名状态和申请能力，并保存插件包与授权记录；当前宿主版本尚不执行外部 Dex。也就是说，你现在可以开发、打包、预览和验证授权流程，但外部 Kotlin 插件还不会在宿主内真正运行。

## 适合谁使用？

| 场景 | 推荐方式 |
| --- | --- |
| 只想过滤推荐流、屏蔽关键词或处理弹幕 | 使用 [JSON / `.bp` 规则插件](../../docs/PLUGIN_DEVELOPMENT.md) |
| 只想做首页顶部、搜索框、底栏饰面等 UI 美化 | 使用数据型 `.bpskin` 皮肤包 |
| 需要写 Kotlin 排序逻辑、推荐算法、播放器或弹幕扩展 | 使用本 SDK 打包 `.bpplugin` |
| 需要完整 UI、深度接入宿主生命周期或立即运行 | 暂时使用源码级原生插件，参考 [原生插件开发](../../docs/NATIVE_PLUGIN_DEVELOPMENT.md) |

## 开发流程总览

1. 新建一个 Android Library / Kotlin 插件工程。
2. 依赖 `com.github.jay3-yy.BiliPai:plugin-sdk:<tag-or-commit>`。
3. 选择要实现的接口，例如 `RecommendationPluginApi`。
4. 在插件类里声明 `PluginCapabilityManifest` 和所需能力。
5. 在包根目录放置 `plugin-manifest.json`，字段与代码里的 manifest 保持一致。
6. 编译插件模块，生成 `classes.jar`、`classes.dex` 或后续版本支持的载荷。
7. 将 `plugin-manifest.json`、可选签名文件、编译载荷打包为 `.bpplugin`。
8. 在 BiliPai 插件中心选择 `.bpplugin`，检查预览、签名、能力和授权状态。

## Gradle 依赖

通过 JitPack 依赖 SDK：

```kotlin
repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.jay3-yy.BiliPai:plugin-sdk:<tag-or-commit>")
}
```

如果插件样例放在 BiliPai 仓库内，可以使用复合构建直接替换成本地 `:plugin-sdk`，参考 [`plugins/samples/today-watch-remix/settings.gradle.kts`](../samples/today-watch-remix/settings.gradle.kts)。

## Manifest

每个 `.bpplugin` 必须在 ZIP 根目录包含 `plugin-manifest.json`：

```json
{
  "pluginId": "dev.example.today_watch_remix",
  "displayName": "Today Watch Remix",
  "version": "1.0.0",
  "apiVersion": 1,
  "entryClassName": "dev.example.todaywatchremix.TodayWatchRemixPlugin",
  "capabilities": [
    "RECOMMENDATION_CANDIDATES",
    "LOCAL_HISTORY_READ"
  ]
}
```

字段说明：

| 字段 | 说明 |
| --- | --- |
| `pluginId` | 全局唯一 ID，建议使用反向域名，例如 `dev.example.my_plugin` |
| `displayName` | 插件中心展示名称 |
| `version` | 插件版本，建议语义化版本 |
| `apiVersion` | SDK API 版本；当前为 `1` |
| `entryClassName` | 插件入口类完整类名 |
| `capabilities` | 插件需要的能力列表，安装前会展示并要求授权 |

`plugin-manifest.json` 会映射到 SDK 内的 `PluginCapabilityManifest`。代码里的 `capabilityManifest` 应与文件内容保持一致，避免预览信息与运行入口不一致。

## 能力声明

插件必须提前声明能力，宿主会在安装前展示：

| 能力 | 含义 |
| --- | --- |
| `RECOMMENDATION_CANDIDATES` | 读取推荐候选，用于排序或筛选 |
| `LOCAL_HISTORY_READ` | 读取本地历史摘要和偏好画像 |
| `LOCAL_FEEDBACK_READ` | 读取“不感兴趣”等本地反馈 |
| `NETWORK` | 访问远程服务 |
| `PLUGIN_STORAGE` | 读写插件自己的本地设置或缓存 |
| `PLAYER_STATE` | 读取播放器当前状态 |
| `PLAYER_CONTROL` | 控制跳转、跳过片段或调整播放行为 |
| `DANMAKU_STREAM` | 读取弹幕流 |
| `DANMAKU_MUTATION` | 过滤、高亮或改写弹幕 |

敏感能力即使来自可信签名，也仍需要用户显式批准。开发时应只申请当前版本实际使用的能力，不要一次性申请全部能力。

## 推荐插件接口

当前最完整的外部接口是 `RecommendationPluginApi`：

```kotlin
class TodayWatchRemixPlugin : RecommendationPluginApi {
    override val capabilityManifest = PluginCapabilityManifest(
        pluginId = "dev.example.today_watch_remix",
        displayName = "Today Watch Remix",
        version = "1.0.0",
        apiVersion = 1,
        entryClassName = "dev.example.todaywatchremix.TodayWatchRemixPlugin",
        capabilities = setOf(
            PluginCapability.RECOMMENDATION_CANDIDATES,
            PluginCapability.LOCAL_HISTORY_READ
        )
    )

    override fun buildRecommendations(request: RecommendationRequest): RecommendationResult {
        val ranked = request.candidateVideos
            .filterNot { it.bvid in request.feedbackSignals.consumedBvids }
            .sortedWith(compareByDescending<PluginVideoCandidate> { it.likeCount }.thenByDescending { it.playCount })
            .take(request.queueLimit)
            .mapIndexed { index, candidate ->
                RecommendedVideo(
                    video = candidate,
                    score = 100.0 - index,
                    confidence = 0.7f,
                    explanation = "按点赞优先，其次按播放量排序"
                )
            }

        return RecommendationResult(
            sourcePluginId = capabilityManifest.pluginId,
            mode = request.mode,
            items = ranked,
            historySampleCount = request.historyVideos.size,
            sceneSignals = request.sceneSignals
        )
    }
}
```

`RecommendationRequest` 里常用字段：

| 字段 | 说明 |
| --- | --- |
| `candidateVideos` | 当前可用于排序的候选视频 |
| `historyVideos` | 本地历史样本 |
| `creatorSignals` | UP 主偏好信号 |
| `feedbackSignals` | 已看过、不感兴趣视频、UP、关键词等反馈 |
| `sceneSignals` | 夜间护眼、当前时间等场景信号 |
| `mode` | `RELAX` 或 `LEARN` |
| `queueLimit` | 推荐队列最大数量 |
| `groupLimit` | 分组/榜单数量上限 |

返回的 `RecommendationResult.items` 应控制在 `queueLimit` 内，并为每条 `RecommendedVideo` 填写 `score`、`confidence` 和 `explanation`，方便宿主展示解释标签和后续调试。

## 其他接口

SDK 还定义了播放器和弹幕接口，供后续宿主执行能力开放时使用：

- `PlayerPluginApi`：`onVideoLoad`、`onPositionUpdate`、`onUserSeek`、`onVideoEnd`，可返回 `SkipAction`。
- `DanmakuPluginApi`：`filterDanmaku` 和 `styleDanmaku`，用于过滤或样式化弹幕。

当前外部 Dex 尚不执行，这些接口主要用于提前适配 API 和包格式。需要马上落地运行的播放器/弹幕能力，请先走源码级原生插件。

> UI 美化请不要打包成 `.bpplugin`。首页顶部氛围、搜索框和底栏饰面走 `.bpskin` 资源包；`.bpskin` 不执行代码，也不能替换底栏液态玻璃、指示器或滑动色散链路。可参考 [`plugins/samples/winter-cloud-skin`](../samples/winter-cloud-skin) 打包一个可导入的冬日云朵皮肤；插件中心也可以直接选择单主题目录 ZIP 或内层 `_package.zip`，或用 [`plugins/tools/bilibili_skin_to_bpskin.py`](../tools/bilibili_skin_to_bpskin.py) 将本地 `KimmyXYC/bilibili-skin` 主题目录转换为仅本地使用的 `.bpskin`。

## `.bpplugin` 包格式

`.bpplugin` 本质是 ZIP 文件。版本 1 要求：

- `plugin-manifest.json` 位于 ZIP 根目录。
- 可选 `plugin-signature.json` 位于 ZIP 根目录。
- 可选编译载荷，例如 `classes.jar`、`classes.dex` 或其他资源文件。当前宿主只做预览和授权，不加载执行。

BiliPai 会计算完整插件包 SHA-256。授权记录绑定 `pluginId + packageSha256`，所以只要包内容变化，用户就需要重新确认授权。

一个最小打包任务示例：

```kotlin
val packageBpPlugin by tasks.registering(Zip::class) {
    dependsOn("assembleRelease")
    archiveBaseName.set("today-watch-remix")
    archiveExtension.set("bpplugin")
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("distributions"))

    from(rootProject.layout.projectDirectory.file("plugin-manifest.json"))
    from(layout.buildDirectory.file("intermediates/aar_main_jar/release/syncReleaseLibJars/classes.jar")) {
        rename { "classes.jar" }
    }
}
```

## 签名元数据

可选 `plugin-signature.json`：

```json
{
  "formatVersion": 1,
  "keyId": "official",
  "algorithm": "SHA256withRSA",
  "signatureBase64": "..."
}
```

当前支持 `SHA256withRSA` 和 `SHA256withECDSA`。签名只影响来源可信展示，不会跳过能力授权。

## 示例

参考以下示例：

- [`plugins/samples/today-watch-remix/`](../samples/today-watch-remix/)：最小推荐插件，按点赞和播放量排序。
- [`plugins/samples/watch-compass/`](../samples/watch-compass/)：观感罗盘样例，把推荐拆成“轻松起步 / 深挖正片 / 冷门宝藏”三类。

示例目录没有独立 Gradle wrapper。请从示例目录调用仓库根 wrapper，并提供 Android SDK 路径：

```bash
cd plugins/samples/today-watch-remix
ANDROID_HOME=/Users/yiyang/Library/Android/sdk ../../../gradlew -p . packageBpPlugin --no-daemon
```

输出位置：

```text
build/distributions/today-watch-remix.bpplugin
```

如果复制到仓库外独立开发，也可以在示例根目录创建 `local.properties` 并写入 `sdk.dir=/path/to/android/sdk`。

## 发布前检查清单

- `pluginId` 唯一且稳定，版本升级不随意更换。
- `plugin-manifest.json` 与代码中的 `capabilityManifest` 一致。
- 只声明当前版本需要的能力。
- 推荐结果不超过 `queueLimit`，解释文案和分数可用于调试。
- `.bpplugin` 根目录能直接看到 `plugin-manifest.json`。
- 修改包内容后重新验证 SHA-256 和授权提示。
