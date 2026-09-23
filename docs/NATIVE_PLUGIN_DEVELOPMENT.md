# 🔧 BiliPai 原生插件开发指南

本文档面向有 Kotlin/Android 开发经验的开发者，详细介绍如何开发 BiliPai 原生插件。

> ⚠️ **注意**：原生插件需要修改源码并重新编译 APK。如果你只需要简单的内容过滤，请使用 [JSON 规则插件](PLUGIN_DEVELOPMENT.md)。
>
> 当前插件生态仍处于早期阶段：主仓库以内置插件与示例为主，第三方原生插件并未形成稳定生态。
> 编写或接入原生插件时，请以 `core/plugin/`、`core/plugin/json/` 与 `PureApplication.kt` 的当前实现为准，不要假设接口在后续版本中保持不变。

---

## 📋 目录

- [开发环境](#-开发环境)
- [插件架构概述](#-插件架构概述)
- [快速开始](#-快速开始)
- [Plugin 基础接口](#-plugin-基础接口)
- [插件类型详解](#-插件类型详解)
  - [PlayerPlugin 播放器插件](#playerplugin-播放器插件)
  - [FeedPlugin 推荐流插件](#feedplugin-推荐流插件)
  - [DanmakuPlugin 弹幕插件](#danmakuplugin-弹幕插件)
- [配置持久化](#-配置持久化)
- [插件 UI 开发](#-插件-ui-开发)
- [完整示例](#-完整示例)
- [注册插件](#-注册插件)
- [最佳实践](#-最佳实践)
- [调试技巧](#-调试技巧)

---

## 🛠️ 开发环境

### 必备条件

- Android Studio 2024.1 或更高版本
- Kotlin 1.9+
- Gradle 8.0+
- Android SDK 37（Compile SDK）

### 克隆项目

```bash
git clone https://github.com/jay3-yy/BiliPai.git
cd BiliPai
```

### 项目结构

```
app/src/main/java/com/android/purebilibili/
├── core/plugin/               # 插件核心框架
│   ├── Plugin.kt             # 基础接口
│   ├── PlayerPlugin.kt       # 播放器插件接口
│   ├── FeedPlugin.kt         # 推荐流插件接口
│   ├── DanmakuPlugin.kt      # 弹幕插件接口
│   ├── CastPluginApi.kt      # 投屏插件接口
│   ├── RecommendationPluginApi.kt # 推荐插件接口
│   ├── TypedPluginApis.kt    # 类型化 API 分发
│   ├── PluginCapabilityManifest.kt # 能力声明
│   ├── PluginManager.kt      # 插件管理器
│   ├── PluginStore.kt        # 配置持久化
│   ├── json/                 # JSON 规则插件引擎
│   ├── js/                   # JS 插件运行时
│   ├── kotlinpkg/            # 外部 Kotlin 包解析/授权
│   └── skin/                 # 皮肤包解析
└── feature/plugin/            # 内置插件实现
    └── SponsorBlockPlugin.kt # 示例：空降助手
```

---

## 🏗️ 插件架构概述

```
┌─────────────────────────────────────────────────────────────┐
│                       PluginManager                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐│
│  │ register │ │ enable   │ │ disable  │ │ getEnabledPlugins││
│  └──────────┘ └──────────┘ └──────────┘ └──────────────────┘│
└─────────────────────────────────────────────────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         ▼                    ▼                    ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│  PlayerPlugin   │ │   FeedPlugin    │ │  DanmakuPlugin  │
│ ─────────────── │ │ ─────────────── │ │ ─────────────── │
│ • onVideoLoad   │ │ • shouldShowItem│ │ • filterDanmaku │
│ • onPositionUpd │ │                 │ │ • styleDanmaku  │
│ • onVideoEnd    │ │                 │ │                 │
└─────────────────┘ └─────────────────┘ └─────────────────┘
         │                    │                    │
         └────────────────────┴────────────────────┘
                              │
                              ▼
                     ┌─────────────────┐
                     │   PluginStore   │
                     │ (DataStore 持久化)│
                     └─────────────────┘
```

---

## 🚀 快速开始

### 1. 创建插件文件

在 `feature/plugin/` 目录下创建新文件：

```kotlin
// MyPlugin.kt
package com.android.purebilibili.feature.plugin

import com.android.purebilibili.core.plugin.Plugin

class MyPlugin : Plugin {
    override val id = "my_plugin"
    override val name = "我的插件"
    override val description = "这是一个示例插件"
    override val version = "1.0.0"
    override val author = "YourName"
}
```

### 2. 注册插件

在 `PureApplication.kt` 的插件初始化区域中添加：

```kotlin
private fun initPluginStackNow() {
    PluginManager.initialize(this)
    PluginManager.register(MyPlugin())
}
```

### 3. 编译运行

```bash
./gradlew :app:assembleDev
```

---

## 📦 Plugin 基础接口

所有插件必须实现 `Plugin` 接口：

```kotlin
interface Plugin {
    /** 唯一标识符，如 "sponsorblock" */
    val id: String
    
    /** 显示名称，如 "空降助手" */
    val name: String
    
    /** 插件描述 */
    val description: String
    
    /** 版本号，如 "1.0.0" */
    val version: String
    
    /** 插件作者（可选，默认 "Unknown"） */
    val author: String
        get() = "Unknown"
    
    /** 插件图标（可选） */
    val icon: ImageVector?
        get() = null
    
    /** 是否暂不可用（用于标识功能尚未完成） */
    val unavailable: Boolean
        get() = false

    /** 不可用原因描述 */
    val unavailableReason: String
        get() = "功能开发中"

    /** 插件能力声明，用于安装前授权和插件中心展示 */
    val capabilityManifest: PluginCapabilityManifest
        get() = PluginCapabilityManifest(
            pluginId = id,
            displayName = name,
            version = version,
            apiVersion = 1,
            entryClassName = this::class.java.name,
            capabilities = emptySet()
        )
    
    /** 插件启用时调用 */
    suspend fun onEnable() {}
    
    /** 插件禁用时调用 */
    suspend fun onDisable() {}
    
    /** 插件配置界面（可选） */
    @Composable
    fun SettingsContent(): Unit = Unit

    /** Modifier-aware 配置入口；无参版本保留给旧/实验插件，根布局需要消费调用方约束时可重载此版本 */
    @Composable
    fun SettingsContent(modifier: Modifier) {
        Box(modifier = modifier) { SettingsContent() }
    }
}
```

### 属性说明

| 属性 | 类型 | 必需 | 说明 |
|------|------|:----:|------|
| `id` | String | ✅ | 唯一标识符，用于存储配置 |
| `name` | String | ✅ | 在设置页显示的名称 |
| `description` | String | ✅ | 插件功能描述 |
| `version` | String | ✅ | 语义化版本号 |
| `author` | String | ❌ | 作者名称 |
| `icon` | ImageVector | ❌ | Material Icons 图标 |
| `unavailable` | Boolean | ❌ | 是否暂不可用（功能未完成），默认 false |
| `unavailableReason` | String | ❌ | 不可用原因，默认 "功能开发中" |
| `capabilityManifest` | PluginCapabilityManifest | ❌ | 插件能力声明，用于安装前授权与插件中心展示 |

---

## 🎬 插件类型详解

### PlayerPlugin 播放器插件

用于控制视频播放行为，如自动跳过片段。

```kotlin
interface PlayerPlugin : Plugin {
    /**
     * 视频加载时回调
     * @param bvid 视频 BV 号
     * @param cid 视频 cid（分P）
     */
    suspend fun onVideoLoad(bvid: String, cid: Long)
    
    /**
     * 播放位置更新回调（约每 500ms 调用一次）
     * @param positionMs 当前播放位置（毫秒）
     * @return 跳过动作
     */
    suspend fun onPositionUpdate(positionMs: Long): SkipAction?

    /** 用户拖动进度条或点击章节时调用 */
    fun onUserSeek(positionMs: Long) {}
    
    /**
     * 视频播放结束时回调
     */
    fun onVideoEnd() {}
}
```

#### SkipAction 跳过动作

```kotlin
sealed class SkipAction {
    /** 不执行跳过 */
    object None : SkipAction()
    
    /** 自动跳转到指定位置 */
    data class SkipTo(
        val positionMs: Long,        // 跳转目标位置
        val reason: String,          // 跳过原因（用于 Toast 提示）
        val segmentId: String? = null,
        val startMs: Long? = null,
        val categoryName: String? = null,
        val showToast: Boolean = true,
    ) : SkipAction()
    
    /** 显示跳过按钮（手动跳过模式） */
    data class ShowButton(
        val skipToMs: Long,    // 点击后跳转位置
        val label: String,     // 按钮文字，如 "跳过广告"
        val segmentId: String  // 片段唯一 ID，防止重复显示
    ) : SkipAction()

    /** 临时静音（用于 SponsorBlock 的 mute 类型片段） */
    data class Mute(
        val untilMs: Long,
        val reason: String,
        val segmentId: String? = null,
        val showToast: Boolean = true,
    ) : SkipAction()
}
```

#### 示例：自动跳过片头

```kotlin
class SkipIntroPlugin : PlayerPlugin {
    override val id = "skip_intro"
    override val name = "跳过片头"
    override val description = "自动跳过视频前 10 秒"
    override val version = "1.0.0"
    
    private var hasSkipped = false
    
    override suspend fun onVideoLoad(bvid: String, cid: Long) {
        hasSkipped = false
    }
    
    override suspend fun onPositionUpdate(positionMs: Long): SkipAction? {
        if (!hasSkipped && positionMs < 10_000) {
            hasSkipped = true
            return SkipAction.SkipTo(10_000, "跳过片头")
        }
        return SkipAction.None
    }
}
```

---

### FeedPlugin 推荐流插件

用于过滤首页推荐视频。

```kotlin
interface FeedPlugin : Plugin {
    /**
     * 判断是否显示该推荐项
     * @param item 视频数据
     * @return true 显示，false 隐藏
     */
    fun shouldShowItem(item: VideoItem): Boolean
}
```

#### VideoItem 数据结构

```kotlin
data class VideoItem(
    val bvid: String,           // BV 号
    val title: String,          // 标题
    val duration: Int,          // 时长（秒）
    val owner: Owner,          // UP 主信息
    val stat: Stat,            // 统计数据
    // ...
)

data class Owner(
    val mid: Long,              // UP 主 UID
    val name: String,           // UP 主名称
)

data class Stat(
    val view: Int,              // 播放量
    val like: Int,              // 点赞数
    val danmaku: Int,           // 弹幕数
)
```

#### 示例：过滤低播放量视频

```kotlin
class LowViewFilter : FeedPlugin {
    override val id = "low_view_filter"
    override val name = "低播放过滤"
    override val description = "隐藏播放量低于 1000 的视频"
    override val version = "1.0.0"
    
    override fun shouldShowItem(item: VideoItem): Boolean {
        return (item.stat?.view ?: 0) >= 1000
    }
}
```

---

### DanmakuPlugin 弹幕插件

用于过滤或美化弹幕。

```kotlin
interface DanmakuPlugin : Plugin {
    /**
     * 过滤弹幕
     * @param danmaku 原始弹幕
     * @return 处理后的弹幕，返回 null 表示屏蔽
     */
    fun filterDanmaku(danmaku: DanmakuItem): DanmakuItem?
    
    /**
     * 获取弹幕样式
     * @return 自定义样式，返回 null 使用默认样式
     */
    fun styleDanmaku(danmaku: DanmakuItem): DanmakuStyle? = null
}
```

#### DanmakuItem 数据结构

```kotlin
data class DanmakuItem(
    val id: Long,               // 弹幕 ID
    val content: String,        // 弹幕内容
    val timeMs: Long,           // 出现时间（毫秒）
    val type: Int,              // 类型：1=滚动, 4=底部, 5=顶部
    val color: Int,             // 颜色（RGB）
    val userId: String          // 发送者 UID
)
```

#### DanmakuStyle 样式

```kotlin
data class DanmakuStyle(
    val textColor: Color? = null,       // 文字颜色
    val borderColor: Color? = null,     // 描边颜色
    val backgroundColor: Color? = null, // 背景色
    val bold: Boolean = false,          // 粗体
    val scale: Float = 1.0f             // 缩放
)
```

#### 示例：高亮同传弹幕

```kotlin
class SubtitleHighlight : DanmakuPlugin {
    override val id = "subtitle_highlight"
    override val name = "同传高亮"
    override val description = "高亮显示翻译弹幕"
    override val version = "1.0.0"
    
    override fun filterDanmaku(danmaku: DanmakuItem): DanmakuItem? = danmaku
    
    override fun styleDanmaku(danmaku: DanmakuItem): DanmakuStyle? {
        if (danmaku.content.startsWith("【") && danmaku.content.endsWith("】")) {
            return DanmakuStyle(
                textColor = Color(0xFFFFD700),  // 金色
                bold = true,
                scale = 1.2f
            )
        }
        return null
    }
}
```

---

## 💾 配置持久化

使用 `PluginStore` 存储插件配置：

```kotlin
class MyPlugin : Plugin {
    // 定义配置数据类
    @Serializable
    data class Config(
        val threshold: Int = 1000,
        val enabled: Boolean = true
    )
    
    private var config = Config()
    
    override suspend fun onEnable() {
        loadConfig()
    }
    
    private suspend fun loadConfig() {
        val context = PluginManager.getContext()
        val json = PluginStore.getConfigJson(context, id)
        if (json != null) {
            config = Json.decodeFromString(json)
        }
    }
    
    private suspend fun saveConfig() {
        val context = PluginManager.getContext()
        val json = Json.encodeToString(config)
        PluginStore.setConfigJson(context, id, json)
    }
}
```

---

## 🎨 插件 UI 开发

通过 `SettingsContent()` 提供配置界面：

```kotlin
@Composable
override fun SettingsContent() {
    val context = LocalContext.current
    var threshold by remember { mutableStateOf(config.threshold) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "阈值设置",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("播放量阈值")
            Spacer(Modifier.weight(1f))
            OutlinedTextField(
                value = threshold.toString(),
                onValueChange = { 
                    threshold = it.toIntOrNull() ?: 0
                    // 保存配置
                    scope.launch {
                        config = config.copy(threshold = threshold)
                        saveConfig()
                    }
                },
                modifier = Modifier.width(100.dp)
            )
        }
        
        // 开关示例
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("启用过滤")
            Spacer(Modifier.weight(1f))
            CupertinoSwitch(
                checked = config.enabled,
                onCheckedChange = { enabled ->
                    scope.launch {
                        config = config.copy(enabled = enabled)
                        saveConfig()
                    }
                }
            )
        }
    }
}
```

---

## 📝 完整示例

查看内置的 **空降助手** 插件作为完整参考：

📄 [SponsorBlockPlugin.kt](../app/src/main/java/com/android/purebilibili/feature/plugin/SponsorBlockPlugin.kt)

该插件展示了：

- ✅ 完整的 `PlayerPlugin` 实现
- ✅ 异步数据加载
- ✅ 配置持久化
- ✅ Compose UI 配置界面
- ✅ 自动跳过和按钮跳过两种模式

---

## 📋 注册插件

在 `PureApplication.kt` 的插件初始化区域注册：

```kotlin
// 文件: PureApplication.kt
private fun initPluginStackNow() {
    PluginManager.initialize(this)

    // 注册你的插件
    PluginManager.register(MyCustomPlugin())
}
```

---

## ✨ 最佳实践

### 1. 性能优化

```kotlin
// ❌ 不好：在 UI 线程执行耗时操作
override fun shouldShowItem(item: VideoItem): Boolean {
    val result = heavyComputation(item)  // 阻塞 UI
    return result
}

// ✅ 好：预计算或缓存结果
private val cache = mutableMapOf<String, Boolean>()

override fun shouldShowItem(item: VideoItem): Boolean {
    return cache.getOrPut(item.bvid) {
        // 轻量级判断
        item.duration > 60
    }
}
```

### 2. 异常处理

```kotlin
override suspend fun onVideoLoad(bvid: String, cid: Long) {
    try {
        // 网络请求
        val data = api.fetchData(bvid)
        processData(data)
    } catch (e: Exception) {
        Logger.e(TAG, "加载失败", e)
        // 降级处理，不影响主功能
    }
}
```

### 3. 日志规范

```kotlin
private const val TAG = "MyPlugin"

// 使用项目统一的 Logger
Logger.d(TAG, "调试信息")
Logger.i(TAG, "普通信息")
Logger.w(TAG, "警告信息")
Logger.e(TAG, "错误信息", exception)
```

### 4. 资源清理

```kotlin
override suspend fun onDisable() {
    // 清理缓存
    cache.clear()
    // 取消任务
    job?.cancel()
    // 释放资源
    connection?.close()
}
```

---

## 🐛 调试技巧

### 1. 查看日志

```bash
adb logcat | grep "PluginManager\|MyPlugin"
```

### 2. 检查插件状态

在设置 → 插件中心查看：

- 插件是否注册成功
- 插件是否启用
- 配置是否保存

### 3. 热重载

使用 Android Studio 的 **Apply Changes** 功能加快迭代。

### 4. 单元测试

```kotlin
@Test
fun `filter should hide short videos`() {
    val plugin = MyFilterPlugin()
    
    val shortVideo = VideoItem(duration = 30, ...)
    val longVideo = VideoItem(duration = 120, ...)
    
    assertFalse(plugin.shouldShowItem(shortVideo))
    assertTrue(plugin.shouldShowItem(longVideo))
}
```

---

## 🤝 贡献插件

如果你开发了有用的插件，欢迎提交 PR！

1. Fork 本仓库
2. 创建插件分支：`git checkout -b plugin/my-plugin`
3. 编写代码和测试
4. 提交 PR 并描述插件功能

---

## ❓ 常见问题

**Q: 插件注册后没显示？**

A: 确保在 `PureApplication.kt` 的插件初始化区域中调用了 `PluginManager.register()`

**Q: 配置没保存？**

A: 确保使用了 `PluginStore` 并正确调用了 `setConfigJson()`

**Q: 插件和主程序版本不兼容？**

A: 原生插件需要与主程序一起编译，更新主程序后需要重新编译插件

---

<p align="center">
  <sub>Made with ❤️ by BiliPai Team</sub>
</p>
