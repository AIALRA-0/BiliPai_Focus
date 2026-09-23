# 🔌 BiliPai 插件开发指南

本文档面向想要为 BiliPai 创建自定义插件的开发者。BiliPai 提供了一个灵活的插件系统，当前主要支持四种开发路径：

| 类型 | 难度 | 适用场景 |
|------|------|----------|
| **JSON 规则插件** | ⭐ 简单 | 内容过滤、弹幕净化、关键词屏蔽 |
| **外部 `.bpskin` 皮肤包** | ⭐ 预览 | 首页顶部氛围、搜索框、底栏饰面等数据型 UI 美化 |
| **外部 `.bpplugin` Kotlin 包** | ⭐⭐ 预览 | 推荐算法、播放器/弹幕接口适配、能力授权流程验证 |
| **源码级原生 Kotlin 插件** | ⭐⭐⭐ 进阶 | 复杂功能、API 集成、自定义 UI、立即运行的深度集成 |

> [!CAUTION]
> 当前仓库注册了 10 个内置插件，并支持通过 URL 导入外部 JSON 规则插件；但插件生态仍处于早期阶段。
> `plugins/community/` 目前仅包含 1 个演示插件，社区规模和兼容性样本都还有限。
> 外部 `.bpplugin` Kotlin 包当前支持预览、签名/哈希展示和能力授权记录，宿主尚不执行外部 Dex。
> 外部 `.bpskin` 皮肤包是数据型资源包，只能提供资源、颜色和适用界面声明，不能替换 Compose 组件或执行代码。
> 引入第三方插件前请自行审阅规则内容、验证兼容性，并假设规则能力与导入体验会继续随版本迭代。

---

## 📋 目录

- [JSON 规则插件（推荐入门）](#-json-规则插件推荐入门)
  - [快速开始](#快速开始)
  - [插件结构](#插件结构)
  - [字段参考](#字段参考)
  - [操作符大全](#操作符大全)
  - [示例插件](#示例插件)
- [外部 `.bpplugin` Kotlin 包（预览）](#-外部-bpplugin-kotlin-包预览)
- [外部 `.bpskin` 皮肤包（预览）](#-外部-bpskin-皮肤包预览)
- [源码级原生 Kotlin 插件](#-源码级原生-kotlin-插件)
- [安装与分发](#-安装与分发)
- [常见问题](#-常见问题)

---

## 📝 JSON 规则插件（推荐入门）

JSON 规则插件是最简单的插件形式，只需编写一个 JSON 文件即可实现内容过滤功能。无需编程基础！

### 快速开始

1. 创建一个 `.json` 文件
2. 按照下面的格式编写规则
3. 上传到任意公开可访问的 URL（如 GitHub Gist、Cloudflare R2）
4. 在 BiliPai 中通过 **设置 → 插件中心 → 导入外部插件** 安装

### 插件结构

```json
{
    "id": "my_plugin",           // 唯一标识符（英文、下划线）
    "name": "我的插件",           // 显示名称
    "description": "插件描述",    // 简短描述
    "version": "1.0.0",          // 版本号
    "author": "你的名字",         // 作者
    "type": "feed",              // 插件类型: "feed" 或 "danmaku"
    "rules": [                   // 规则数组
        {
            "field": "title",    // 匹配字段
            "op": "contains",    // 操作符
            "value": "广告",      // 匹配值
            "action": "hide"     // 动作
        }
    ]
}
```

### 字段参考

#### Feed 插件（推荐流过滤）

| 字段 | 说明 | 示例值 |
|------|------|--------|
| `title` | 视频标题 | `"震惊"` |
| `duration` | 视频时长（秒） | `60` |
| `owner.mid` | UP 主 UID | `12345678` |
| `owner.name` | UP 主名称 | `"某UP主"` |
| `stat.view` | 播放量 | `100000` |
| `stat.like` | 点赞数 | `5000` |

#### Danmaku 插件（弹幕过滤）

| 字段 | 说明 | 示例值 |
|------|------|--------|
| `content` | 弹幕内容 | `"666"` |
| `userId` | 发送者 UID | `12345678` |
| `type` | 弹幕类型 | `1` |

### 操作符大全

| 操作符 | 说明 | 示例 |
|--------|------|------|
| `eq` | 等于 | `"op": "eq", "value": 60` |
| `ne` | 不等于 | `"op": "ne", "value": 0` |
| `lt` | 小于 | `"op": "lt", "value": 60` |
| `le` | 小于等于 | `"op": "le", "value": 60` |
| `gt` | 大于 | `"op": "gt", "value": 100000` |
| `ge` | 大于等于 | `"op": "ge", "value": 100000` |
| `contains` | 包含 | `"op": "contains", "value": "广告"` |
| `startsWith` | 以...开头 | `"op": "startsWith", "value": "【"` |
| `endsWith` | 以...结尾 | `"op": "endsWith", "value": "】"` |
| `regex` | 正则匹配 | `"op": "regex", "value": "^[哈]{5,}$"` |
| `in` | 在列表中 | `"op": "in", "value": [123, 456]` |

### 动作类型

| 动作 | 说明 | 可选参数 |
|------|------|----------|
| `hide` | 隐藏匹配内容 | 无 |
| `highlight` | 高亮显示（仅弹幕） | `style` 对象 |

#### 高亮样式

```json
{
    "action": "highlight",
    "style": {
        "color": "#FFD700",    // 十六进制颜色
        "bold": true,          // 粗体
        "scale": 1.2           // 缩放比例
    }
}
```

### 🆕 复合条件（AND/OR）

当前版本支持使用 `and` 和 `or` 组合多个条件实现更精确的过滤。

#### AND 条件

所有子条件**都必须满足**时才触发动作：

```json
{
    "condition": {
        "and": [
            { "field": "duration", "op": "lt", "value": 60 },
            { "field": "title", "op": "contains", "value": "搬运" }
        ]
    },
    "action": "hide"
}
```

#### OR 条件

**任一**子条件满足时即触发动作：

```json
{
    "condition": {
        "or": [
            { "field": "owner.name", "op": "contains", "value": "营销号" },
            { "field": "title", "op": "regex", "value": "震惊.*必看" }
        ]
    },
    "action": "hide"
}
```

#### 嵌套条件

支持 AND/OR 嵌套实现复杂逻辑：

```json
{
    "condition": {
        "and": [
            { "field": "stat.view", "op": "lt", "value": 1000 },
            {
                "or": [
                    { "field": "title", "op": "contains", "value": "广告" },
                    { "field": "title", "op": "contains", "value": "推广" }
                ]
            }
        ]
    },
    "action": "hide"
}
```

> 💡 **向后兼容**：旧格式 `field/op/value` 仍然有效，无需修改现有插件。

### 示例插件

#### 1️⃣ 短视频过滤器

过滤时长小于 60 秒的短视频：

```json
{
    "id": "short_video_filter",
    "name": "短视频过滤",
    "description": "隐藏时长小于60秒的视频",
    "version": "1.0.0",
    "author": "BiliPai",
    "type": "feed",
    "rules": [
        {
            "field": "duration",
            "op": "lt",
            "value": 60,
            "action": "hide"
        }
    ]
}
```

#### 2️⃣ 标题关键词过滤

过滤标题党视频：

```json
{
    "id": "keyword_filter",
    "name": "标题关键词过滤",
    "description": "过滤包含指定关键词的视频",
    "version": "1.0.0",
    "author": "BiliPai",
    "type": "feed",
    "rules": [
        {
            "field": "title",
            "op": "contains",
            "value": "广告",
            "action": "hide"
        },
        {
            "field": "title",
            "op": "regex",
            "value": "震惊.*必看",
            "action": "hide"
        }
    ]
}
```

#### 3️⃣ 弹幕净化器

过滤刷屏弹幕，高亮同传翻译：

```json
{
    "id": "danmaku_cleaner",
    "name": "弹幕净化",
    "description": "过滤刷屏弹幕，高亮同传翻译",
    "version": "1.0.0",
    "author": "BiliPai",
    "type": "danmaku",
    "rules": [
        {
            "field": "content",
            "op": "regex",
            "value": "^[哈]{5,}$",
            "action": "hide"
        },
        {
            "field": "content",
            "op": "startsWith",
            "value": "【",
            "action": "highlight",
            "style": {
                "color": "#FFD700",
                "bold": true
            }
        }
    ]
}
```

---

## 📦 外部 `.bpplugin` Kotlin 包（预览）

外部 `.bpplugin` 是面向未来插件生态的 Kotlin 包格式。完整的 SDK 接口选择、`plugin-manifest.json` 与能力声明、推荐插件最小示例、打包与构建命令见 [Plugin SDK 指南](../plugins/sdk/README.md)。可打包预览的示例见 [Today Watch Remix](../plugins/samples/today-watch-remix/) 与[观感罗盘](../plugins/samples/watch-compass/)。

> [!IMPORTANT]
> 当前宿主只解析、签名校验、展示和能力授权记录 `.bpplugin`，不会执行外部 Dex。如果你需要插件立即影响应用行为，请使用[源码级原生 Kotlin 插件](#-源码级原生-kotlin-插件)。

---

## 🎨 外部 `.bpskin` 皮肤包（预览）

`.bpskin` 是 ZIP 资源包，用于数据型 UI 美化。它和 `.bpplugin` 分离，不包含 Dex、Jar 或 Compose 入口，宿主只解析、校验、保存资源和启用记录。

仓库内置了一个可打包样例：[`plugins/samples/winter-cloud-skin`](../plugins/samples/winter-cloud-skin)。它演示了截图风格的浅色冬季氛围、云朵底栏饰边、搜索胶囊和底栏图标贴纸声明。

插件中心可以直接选择 `.bpskin`。如果你本地已有 [`KimmyXYC/bilibili-skin`](https://github.com/KimmyXYC/bilibili-skin) 这类公开存档，也可以把单个主题文件夹压成 ZIP 后在插件中心选择，App 会在本地转换成 `.bpskin` 并进入同一个预览、保存、启用流程。主题目录 ZIP 内应包含 `个性装扮.json` 或 `<主题名>.json`，以及 `<主题名>_package.zip`。如果你手上只有内层 `<主题名>_package.zip`，也可以直接导入；这时 App 会使用通用名称和默认色板，仍然会读取 `tail_bg`、`head_bg/head_tab_bg/side_bg` 和 `tail_icon_*` 资源。

桌面批量转换仍可使用仓库工具。转换器会直接使用本地主题 zip 中的 `tail_bg`、`head_bg`、`tail_icon_*` 等素材，但不会把这些素材提交到 BiliPai 仓库：

```bash
python3 plugins/tools/bilibili_skin_to_bpskin.py \
  --theme-dir /path/to/bilibili-skin/萧逸 \
  --output /tmp/xiaoyi.bpskin
```

App 内转换和桌面工具转换出的包都会声明 `containsOfficialAssets=true`、`communityShareable=false`。这类包适合本地私用或在你已获得授权时分享；不要把官方付费主题原图、角色立绘、图标原件或动效资源作为社区包分发。

版本 1 的包根目录必须包含 `skin-manifest.json`，其他资源必须位于 `assets/` 下：

```text
my-skin.bpskin
├── skin-manifest.json
└── assets/
    ├── bottom_trim.png
    └── top_atmosphere.webp
```

最小 manifest 示例：

```json
{
  "formatVersion": 1,
  "skinId": "dev.example.winter_cloud",
  "displayName": "冬日云朵",
  "version": "1.0.0",
  "apiVersion": 1,
  "author": "BiliPai",
  "surfaces": ["HOME_BOTTOM_BAR", "HOME_TOP_CHROME"],
  "assets": {
    "bottomBarTrim": "assets/bottom_trim.png",
    "topAtmosphere": "assets/top_atmosphere.webp"
  },
  "colors": {
    "bottomBarTrimTint": "#EAF8FF",
    "topAtmosphereTint": "#DFF5FF"
  },
  "styleSourceName": "KimmyXYC/bilibili-skin",
  "styleSourceUrl": "https://github.com/KimmyXYC/bilibili-skin",
  "licenseNote": "原创或已授权资源说明；社区可分享包必须填写",
  "communityShareable": true,
  "containsOfficialAssets": false
}
```

当前支持的界面：

- `HOME_BOTTOM_BAR`：首页底栏装饰资源和颜色 token。
- `HOME_TOP_CHROME`：首页顶部氛围资源、搜索胶囊色和顶部标签背景色 token。

安全边界：

- 包内只能包含 `skin-manifest.json` 和 `assets/` 下的 PNG、WebP、JPEG 资源。
- 宿主会拒绝路径穿越、未知界面、未声明资源、重复声明资源、超大 manifest 和超大解压内容。
- 用户导入后，宿主会把已声明资源解压到应用私有目录，并把资源路径作为受控装饰输入传给对应 UI。
- 皮肤只作为装饰输入传给宿主。底栏的 `FrostedBottomBar`、`KernelSuAlignedBottomBar`、`drawBackdrop`、指示器折射、滑动色散和输入层不会被皮肤包替换或重算。
- `styleSourceName`、`styleSourceUrl`、`licenseNote`、`communityShareable`、`containsOfficialAssets` 是可选元数据；旧包不填写也可导入。
- 如果声明 `communityShareable=true`，必须填写 `licenseNote`。
- 宿主只做静态提示，不判断版权归属；社区分享前需要作者确认资源是原创、已授权或公共授权。
- App 内主题目录 ZIP / `_package.zip` 转换只处理本地文件，不联网、不登录、不下载 B 站资源、不解密 App 数据。

---

## 🔧 源码级原生 Kotlin 插件

> ⚠️ 原生插件需要修改源码并重新编译，适合有 Android 开发经验的开发者

完整的 `Plugin` / `PlayerPlugin` / `FeedPlugin` / `DanmakuPlugin` 接口签名、`SkipAction` 与数据结构、配置持久化、UI 开发、调试与最佳实践见[原生插件开发指南](NATIVE_PLUGIN_DEVELOPMENT.md)。示例：[SponsorBlockPlugin](../app/src/main/java/com/android/purebilibili/feature/plugin/SponsorBlockPlugin.kt)。

在 `PureApplication.kt` 的插件初始化区域注册：

```kotlin
private fun initPluginStackNow() {
    PluginManager.initialize(this)
    PluginManager.register(YourCustomPlugin())
}
```

---

## 📤 安装与分发

### JSON 插件分发

1. **GitHub Gist** - 创建一个公开的 Gist，使用 Raw 链接
2. **GitHub 仓库** - 放在仓库中，使用 Raw 文件链接
3. **Cloudflare R2 / S3** - 上传到云存储
4. **个人服务器** - 确保 HTTPS 可访问

### 链接格式要求

- 必须是 http/https 直接下载链接（内容为 JSON，`.json` 或 `.bp` 后缀均可）
- 建议使用 HTTPS

---

## ❓ 常见问题

**Q: 插件安装后为什么没生效？**

A: 确保插件已启用（开关为开），部分插件需要重启应用。

**Q: 如何调试我的 JSON 插件？**

A: 使用在线 JSON 验证器检查语法，确保所有字段都正确。

**Q: 正则表达式不生效？**

A: 确保正则表达式语法正确，可以在 [regex101](https://regex101.com/) 上测试。

**Q: 可以组合多个条件吗？**

A: ✅ 支持！使用 `and` 和 `or` 复合条件可以组合多个条件。参见上方的[复合条件（AND/OR）](#-复合条件andor)章节。

---

## 🤝 社区插件

欢迎分享你的插件！提交 PR 到本仓库的 `plugins/community/` 目录。

---

<p align="center">
  <sub>Made with ❤️ by BiliPai Team</sub>
</p>
