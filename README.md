# BiliPai Focus <img src="docs/images/233娘.jpeg" height="80" align="center">

<p align="center">
  <a href="README_EN.md">English</a> | <a href="README.md">简体中文</a>
</p>

<p align="center">
  <strong>原生、纯净、可扩展 —— 在 BiliPai 之上，打造更专注的 B 站体验</strong>
</p>

<p align="center">
  <sub>最后更新：2026-03-22 · 上游基线 v7.1.0 · 当前 Focus 发布 v7.1.0-focus.4</sub>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Focus-7.1.0--focus.4-fb7299?style=flat-square" alt="Focus Version">
  <img src="https://img.shields.io/badge/Upstream-7.1.0-00a1d6?style=flat-square" alt="Upstream Version">
  <img src="https://img.shields.io/github/stars/AIALRA-0/BiliPai_Focus?style=flat-square&color=yellow" alt="Stars">
  <img src="https://img.shields.io/github/forks/AIALRA-0/BiliPai_Focus?style=flat-square&color=green" alt="Forks">
  <img src="https://img.shields.io/github/last-commit/AIALRA-0/BiliPai_Focus?style=flat-square&color=purple" alt="Last Commit">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026)-brightgreen?style=flat-square" alt="Platform">
  <img src="https://img.shields.io/badge/APK-Varies-orange?style=flat-square" alt="Size">
  <img src="https://img.shields.io/badge/License-GPL--3.0-blue?style=flat-square" alt="License">
  <img src="https://img.shields.io/badge/Plugins-5%20Built--in-blueviolet?style=flat-square" alt="Plugins">
</p>

<p align="center">
  <a href="https://t.me/BiliPaii"><img src="https://img.shields.io/badge/Telegram-交流群-2CA5E0?style=flat-square&logo=telegram" alt="Telegram Group"></a>
  <a href="https://t.me/BiliPai"><img src="https://img.shields.io/badge/Telegram-频道-2CA5E0?style=flat-square&logo=telegram" alt="Telegram Channel"></a>
  <a href="https://x.com/YangY_0x00"><img src="https://img.shields.io/badge/X-Follow-000000?style=flat-square&logo=x" alt="X"></a>
</p>

## 🚀 快速导航

| 类别 | 入口 |
| --- | --- |
| 开始使用 | [官方 Releases](https://github.com/jay3-yy/BiliPai/releases) · [Focus Releases](https://github.com/AIALRA-0/BiliPai_Focus/releases) · [Focus Release Notes](docs/releases/focus-7.1.0-focus.4.md) · [更新日志](CHANGELOG.md) · [Focus 变更日志](FOCUS_CHANGLOG.md) |
| 文档导航 | [Wiki 首页](docs/wiki/README.md) · [AI / LLM 入口](llms.txt) · [AI 导航指南](docs/wiki/AI.md) |
| 开发参考 | [JSON 插件开发](docs/PLUGIN_DEVELOPMENT.md) · [原生插件开发](docs/NATIVE_PLUGIN_DEVELOPMENT.md) |

> [!IMPORTANT]
> 应用内默认设置为通用场景，可能不适合所有用户。建议进入 **设置** 按个人习惯手动调整（如外观、动画、播放设置等）。

> [!NOTE]
> 首页推荐流、视频详情/播放页等高频滑动与动效链路正在进行结构性性能重构。近期 `main` 分支会持续出现滚动、模糊、转场、播放控制相关调整；若遇到阶段性波动，请以最新提交与 `CHANGELOG`/Wiki 说明为准。

## 🧩 Focus 定制版

> [!TIP]
> 这是一个面向 Focus 使用场景维护的公开定制 fork。它尽量保留上游核心能力与可同步性，同时把首页入口、搜索热搜、相关推荐与历史管理收口成更克制、更可控的默认体验。

### 核心理念

从注意力心理学和行为设计的角度看，推荐流最擅长利用变动奖励、即时反馈和错失焦虑，把人从原本的目标拉进被动刷取；而 B 站真正高质量的部分，往往出现在你有明确意图时的定点搜索、收藏回看、历史回溯和关注订阅。Focus 的核心不是“少功能”，而是“少诱饵、保留目的”，尽量去掉一切勾引你继续看的入口，只保留你主动寻找、主动订阅、主动决定要看的内容。

### 版本入口

| 版本 | 适合谁 | 入口 |
| --- | --- | --- |
| 官方原版 | 想直接跟随上游默认体验与发布节奏 | [仓库](https://github.com/jay3-yy/BiliPai) · [Releases](https://github.com/jay3-yy/BiliPai/releases) |
| Focus 定制版 | 想保留原生能力，同时获得更克制的默认入口、关注过滤和专注模式开关 | [仓库](https://github.com/AIALRA-0/BiliPai_Focus) · [Releases](https://github.com/AIALRA-0/BiliPai_Focus/releases) · [Release Notes](docs/releases/focus-7.1.0-focus.4.md) · [Focus 变更日志](FOCUS_CHANGLOG.md) |

### 当前 Focus 发布

| 项目 | 内容 |
| --- | --- |
| Focus 版本 | `7.1.0-focus.4` |
| 上游基线 | `7.1.0` |
| 发布标签 | `v7.1.0-focus.4` |
| Release Notes | [docs/releases/focus-7.1.0-focus.4.md](docs/releases/focus-7.1.0-focus.4.md) |
| 主要收口 | Focus 理念与文档说明重写、历史清空展示补齐、维护范围与差异文件清单同步 |
| APK 命名 | `BliPai-Focus-debug-7.1.0-focus.4-debug.apk` · `BliPai-Focus-release-7.1.0-focus.4.apk` |

### 默认定制项

| 项目 | 默认行为 |
| --- | --- |
| 首页推荐 | 默认隐藏 |
| 首页热门 | 默认隐藏 |
| 首页直播 | 默认隐藏 |
| 首页游戏 | 默认隐藏 |
| 首页分区按钮 | 默认隐藏 |
| 搜索热搜 | 默认关闭，但保留搜索建议、搜索发现、搜索结果与搜索历史链路 |
| 观看历史 | 新增“一键清空全部历史记录”，默认开启入口 |
| 视频相关推荐 | 默认隐藏，可在 Focus 设置中恢复 |
| 关注分组过滤 | 支持本地单归属分组、动态页和首页“关注”联动过滤，并可整体开关 |
| 设置入口 | 统一收纳到 `设置 → 常规 → Focus` |

### Focus 特色说明

- 不删除首页/搜索/历史/视频详情的底层实现，只控制默认 UI 暴露与入口优先级。
- 首页顶部分类在 Focus 过滤后会按剩余数量自动回到严格居中对称排布，像只保留“关注”时也会稳定居中。
- 动态页与首页“关注”共用同一套本地关注分组过滤，分组隐藏后，两处内容会一起收敛。
- 关注对象归属改成按分组展开查看，只有点开某个分组后才显示该组成员，关注很多时也不会整页爆开。
- 动态页关注列表会优先使用本地缓存回填，并在页面启动时并行预热，不再必须等主动态先加载完才补全关注对象。
- Focus 开关优先于首页顶部标签最终展示结果，但不会破坏原有顶栏管理/底栏管理页。
- 如果首页顶部项被全部隐藏，会自动回退到 `关注`，避免首页出现空白。

### 维护节奏与测试范围

- Focus 尽量与主项目保持相同主版本号，目前暂定只跟进上游大版本，再在同一上游基线上滚动维护 Focus 子版本。
- 当前测试设备仅覆盖 `真我 Neo 7` 与 `联想 Y700 2023`；如果你在其他设备上遇到兼容性或行为问题，请到仓库提交 [Issue](https://github.com/AIALRA-0/BiliPai_Focus/issues)。

### Focus 功能截图

#### 专注模式入口与设置

<p align="center">
  <img src="docs/images/focus/focus-settings-entry.jpg" alt="Focus 设置入口" height="500">
  <img src="docs/images/focus/focus-settings-1.jpg" alt="Focus 设置页一" height="500">
  <img src="docs/images/focus/focus-settings-2.jpg" alt="Focus 设置页二" height="500">
</p>

<p align="center">
  <sub>展示设置入口、Focus 专注模式总览，以及首页 / 搜索 / 历史 / 详情相关默认开关</sub>
</p>

#### 首页、搜索与详情过滤

<p align="center">
  <img src="docs/images/focus/focus-home-filter.jpg" alt="Focus 首页过滤" height="500">
  <img src="docs/images/focus/focus-search-filter.jpg" alt="Focus 搜索过滤" height="500">
  <img src="docs/images/focus/focus-related-filter.jpg" alt="Focus 相关推荐过滤" height="500">
</p>

<p align="center">
  <sub>首页顶部入口过滤、搜索热搜过滤，以及视频详情页下方相关推荐过滤后的实际效果</sub>
</p>

#### 关注分组管理

<p align="center">
  <img src="docs/images/focus/focus-follow-groups-entry.jpg" alt="关注分组入口" height="500">
  <img src="docs/images/focus/focus-follow-groups-1.jpg" alt="关注分组管理一" height="500">
  <img src="docs/images/focus/focus-follow-groups-2.jpg" alt="关注分组管理二" height="500">
</p>

<p align="center">
  <sub>动态页分组入口、分组管理展开，以及关注对象按单归属分组管理的实际界面，点击动态页面右上角小齿轮进入分组管理</sub>
</p>

#### 历史记录清除

<p align="center">
  <img src="docs/images/focus/focus-history-clear.jpg" alt="Focus 历史一键清空" height="500">
</p>

<p align="center">
  <sub>展示历史记录一键清空后的实际界面与交互入口</sub>
</p>

### 与官方的主要 diff 文件

以下只列 Focus 维护线最核心的新增和改动入口，完整差异以仓库提交历史为准。

| 类型 | 文件 | 作用 |
| --- | --- | --- |
| 新增 | `FOCUS_CHANGLOG.md` | Focus 独立变更日志与维护记录 |
| 新增 | `docs/releases/focus-7.1.0-focus.4.md` | Focus `focus.4` 中文发布说明 |
| 新增 | `docs/releases/focus-7.1.0-focus.4-en.md` | Focus `focus.4` 英文发布说明 |
| 新增 | `docs/images/focus/*` | Focus 专属功能截图与展示资源 |
| 修改 | `app/build.gradle.kts` | Focus 子版本号、应用名称、release 命名与签名出包 |
| 修改 | `app/src/main/java/com/android/purebilibili/core/store/SettingsManager.kt` | Focus 设置持久化、关注过滤与默认值 |
| 修改 | `app/src/main/java/com/android/purebilibili/feature/settings/screen/FocusSettingsScreen.kt` | Focus 设置页入口与专注模式开关 |
| 修改 | `app/src/main/java/com/android/purebilibili/feature/settings/SettingsSearchPolicy.kt` | Focus 设置搜索索引与设置入口文案 |
| 修改 | `app/src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt` | 首页顶部分类过滤、居中对称布局与边界留白 |
| 修改 | `app/src/main/java/com/android/purebilibili/feature/home/components/LiquidIndicator.kt` | 顶部分类指示器偏移与居中补偿 |
| 修改 | `app/src/main/java/com/android/purebilibili/feature/home/HomeViewModel.kt` | 首页“关注”数据过滤与本地缓存投影 |
| 修改 | `app/src/main/java/com/android/purebilibili/feature/dynamic/DynamicViewModel.kt` | 动态页关注对象预热、分组过滤与显示策略 |
| 修改 | `app/src/main/java/com/android/purebilibili/feature/dynamic/components/FocusFollowGroupSheet.kt` | 关注分组管理、分组展开与单归属迁移 |
| 修改 | `app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicTopBar.kt` | 动态页分组入口与交互承接 |
| 修改 | `app/src/main/java/com/android/purebilibili/core/network/ApiClient.kt` | 历史记录清空 API 接入 |
| 修改 | `app/src/main/java/com/android/purebilibili/data/repository/HistoryRepository.kt` | 历史记录清空仓储链路 |
| 修改 | `app/src/main/java/com/android/purebilibili/feature/list/ListViewModel.kt` | 历史记录一键清空状态与交互回写 |
| 修改 | `app/src/main/java/com/android/purebilibili/feature/onboarding/OnboardingBottomSheet.kt` | 首次使用页的官方 / Focus GitHub 入口 |
| 修改 | `app/src/main/java/com/android/purebilibili/feature/settings/update/AppUpdateChecker.kt` | 应用内更新检查切换到 Focus 仓库 |
| 修改 | `README.md` / `README_EN.md` / `CHANGELOG.md` | Focus 文档入口、版本线、截图与发布说明 |

## 📸 官方应用预览

> [!NOTE]
> 下方预览与后续大部分功能说明基本沿用上游官方 README 的原始内容，方便对照 Focus 定制版与官方原版的差异。

<p align="center">
  <img src="docs/images/screenshot_preview_1.png" alt="预览图 1" height="500">
  <img src="docs/images/screenshot_preview_2.png" alt="预览图 2" height="500">
  <img src="docs/images/screenshot_preview_3.png" alt="预览图 3" height="500">
  <img src="docs/images/screenshot_preview_4.png" alt="预览图 4" height="500">
  <img src="docs/images/screenshot_preview_5.png" alt="预览图 5" height="500">
</p>

## ✨ 功能亮点

### 🎬 视频播放

| 功能 | 描述 |
|-----|-----|
| **高清画质** | 支持 4K / 1080P60 / HDR / Dolby Vision (需登录/大会员) |
| **DASH 流媒体** | 自适应码率选择，无缝切换画质，流畅播放体验 |
| **弹幕系统** | 透明度、字体大小、滚动速度可调，支持弹幕密度过滤 |
| **手势控制** | 左侧上下滑动调节亮度，右侧调节音量，左右滑动快进/快退 |
| **倍速播放** | 0.5x / 0.75x / 1.0x / 1.25x / 1.5x / 2.0x，长按倍速支持上滑锁定 |
| **画中画** | 悬浮小窗播放，多任务无缝切换 |
| **听视频模式** | 🆕 专属音频播放界面，支持沉浸式/黑胶唱片模式、歌词、播放列表与定时关闭 |
| **AI 总结** | 🆕 智能生成视频内容摘要，快速获取核心信息 |
| **原地播放** | 长按视频封面直接预览播放，点击即可全屏，无缝衔接 |
| **后台播放** | 锁屏/切后台继续听，通知栏与系统媒体中心前后切歌更稳定 |
| **播放顺序** | 支持播完暂停 / 顺序播放 / 单个循环 / 列表循环 / 自动连播，横竖屏可快捷切换 |
| **播放完成体验** | 关闭“自动播放下一个”后，播完不再弹强干扰操作弹窗 |
| **评论体验** | 支持默认排序偏好（最热/最新），并修复特定排序下 UP 主/置顶评论缺失问题 |
| **评论复制增强** | 长按进入可选择复制面板，支持拖拽选择评论片段（含表情/富文本场景） |
| **横屏信息栏** | 全屏顶部新增时间显示，横屏交互信息更完整 |
| **播放记忆** | 自动记录观看进度，续播提示支持开关与同目标仅提醒一次 |
| **高画质扫码登录** | 支持扫码登录，解锁大会员专属高画质 |
| **插件系统** | 内置空降助手、去广告、弹幕增强、夜间护眼、今日推荐单等插件，可扩展架构 |

### 🔌 插件系统

| 插件 | 描述 |
|-----|-----|
| **空降助手** | 基于 BilibiliSponsorBlock 数据库，自动跳过广告/恰饭片段 |
| **去广告插件** | 智能过滤推荐流中的商业推广内容 |
| **弹幕增强** | 支持关键词 + 用户 UID/hash 过滤与高亮，规则变更支持播放内热更新 |
| **夜间护眼** | 定时护眼、三档预设可 DIY、实时预览、暖色滤镜、关怀提醒（支持稍后提醒） |
| **🆕 今日推荐单** | 本地分析观看历史与反馈，生成“今晚轻松看 / 深度学习看”队列，支持收起/展开、单独刷新、UP 主榜与推荐解释 |
| **插件中心** | 统一管理所有插件，支持独立配置 |
| **🆕 外部插件** | 支持通过 URL 动态加载 JSON 规则插件 |

#### 已实现细节（补充）

- `今日推荐单`：
  - 支持双模式切换（今晚轻松看 / 深度学习看）
  - 支持 UP 主榜、视频队列、推荐理由标签
  - 推荐队列展示 UP 主头像与昵称，优化观感
  - 联动护眼状态：夜间自动偏向短时长、低刺激内容
  - 支持本地负反馈学习（不感兴趣视频/UP/关键词）
  - 支持冷启动首屏曝光策略，避免“已生成但看不到”
  - 插件内可一键清空本地画像与反馈，重新学习
- `夜间护眼`：
  - 三档预设（轻柔/平衡/专注）并支持用户 DIY
  - 实时预览亮度与暖色滤镜强度
  - 定时护眼 + 使用时长关怀提醒 + 稍后提醒
  - 关怀文案与提醒策略支持人性化优化
- `画质切换`：
  - 画质列表优先使用 DASH 实际可切换轨道
  - 缓存切换改为目标画质精确匹配，缺失时回退 API
  - 切换提示文案更明确（目标不可用时清晰反馈）

#### 今日推荐单算法（通俗版）

- **先看你最近看了谁**：统计历史记录里你常看的 UP 主，并结合播放进度和“最近看过”的权重。
- **再给候选视频打分**：每条视频会综合以下信号：
  - 热度（播放量）
  - 与你偏好 UP 主的匹配度
  - 新鲜度（发布时间）
  - 模式偏好（`今晚轻松看` 更偏短、轻松；`深度学习看` 更偏知识、时长适中）
  - 夜间护眼状态（夜间会降低高刺激、超长视频权重）
  - 负反馈（不感兴趣的视频/UP/关键词会被明显降权）
- **最后做“去同质化”排序**：不是只按分数从高到低排，而是避免连续刷到同一个 UP 主，让列表更耐看。

一句话总结：`今日推荐单` 是一个完全本地、可解释的加权排序器，会根据你的观看行为持续微调结果。

#### 今日推荐单界面示例

<p align="center">
  <img src="docs/images/screenshot_today_watch_plan.png" alt="今日推荐单截图" height="560">
</p>

#### 今日推荐单算法原理（详细版）

> 对应实现：`app/src/main/java/com/android/purebilibili/feature/home/TodayWatchPolicy.kt`  
> 画像与反馈存储：`app/src/main/java/com/android/purebilibili/core/store/TodayWatchProfileStore.kt`、`app/src/main/java/com/android/purebilibili/core/store/TodayWatchFeedbackStore.kt`

1. 输入数据

- 历史样本：`historyVideos`（本地历史记录）
- 候选集合：`candidateVideos`（首页推荐流候选）
- 模式：`RELAX`（今晚轻松看）或 `LEARN`（深度学习看）
- 护眼信号：`eyeCareNightActive`（夜间护眼是否激活）
- 画像信号：`creatorSignals`（本地累计的 UP 主偏好）
- 负反馈信号：`penaltySignals`（不感兴趣视频/UP/关键词）

2. 历史预处理与 UP 主亲和度构建

- 仅保留有效历史项：`bvid` 非空且 `owner.mid > 0`
- 按 `view_at` 倒序，统计每位 UP 的聚合分：
  - `creator_score += 1.0 + completion * 1.2 + recencyBonus(view_at)`
  - `completion`：
    - `progress < 0` -> `0.35`
    - `duration <= 0` -> `clamp(progress / 600, 0..1)`
    - 其他 -> `clamp(progress / duration, 0..1)`
  - `recencyBonus(view_at)`：
    - `<=1天:1.0`，`<=3天:0.8`，`<=7天:0.6`，`<=30天:0.35`，其余 `0.15`

3. 跨会话画像融合（Creator Signal）

- 从本地画像仓读取每位 UP 的长期偏好分：
  - `engagementScore = ln(totalWatchSec + 1) * 0.92 + ln(engagementEvents + 1) * 0.66`
  - `recencyScore`：
    - `<=1天:1.15`，`<=3天:0.85`，`<=7天:0.55`，`<=30天:0.2`，其余 `-0.1`
  - `signal.score = engagementScore + recencyScore`
- 合并到当前会话亲和度：`creatorAffinity[mid] += signal.score`

4. 候选视频清洗

- 过滤无效候选：`bvid/title` 非空
- 按 `bvid` 去重
- 标记是否已看过：`alreadySeen = bvid in historySet`

5. 单条候选打分（核心公式）

- 总分：
  - `score = base + creator + freshness + seenPenalty + mode + night + feedback`
- 基础分：
  - `base = ln(view + 1) * 0.45`
  - `creator = ln(creatorAffinity + 1) * 2.1`
  - `freshness(pubdate)`：`<=1天:0.8`，`<=3天:0.55`，`<=7天:0.3`，`<=30天:0.1`，其余 `-0.05`
  - `seenPenalty`：已看过则 `-2.6`
- 强度信号（弹幕密度近似刺激度）：
  - `intensity = danmaku / max(view,1)`
  - `calmScore`：`<0.004:1.0`，`<0.01:0.3`，其余 `-1.0`
- 模式分：
  - `RELAX`：
    - `durationRelaxScore`：`<2:-0.2`，`<=12:1.4`，`<=20:0.6`，`<=35:-0.1`，其余 `-0.9`
    - `keywordBonus(title, RELAX_KEYWORDS, LEARN_KEYWORDS)`
    - `+ calmScore`
  - `LEARN`：
    - `durationLearnScore`：`<5:-0.6`，`<=12:0.5`，`<=35:1.5`，`<=55:0.8`，其余 `-0.2`
    - `keywordBonus(title, LEARN_KEYWORDS, RELAX_KEYWORDS)`
    - 时长补偿：`duration>=10分钟 ? +0.6 : -0.2`
- 夜间护眼调权（仅护眼激活时）：
  - `durationPenalty`：`<=15:+1.2`，`<=25:+0.2`，`>25` 按时长递减到最多 `-3.0`
  - `intensityPenalty`：`<0.006:+0.6`，`<0.012:0.0`，其余 `-1.1`
- 负反馈惩罚：
  - 命中不感兴趣视频：`-3.2`
  - 命中不感兴趣 UP：`-2.4`
  - 不感兴趣关键词：每个 `-0.7`，最低封顶 `-2.8`

6. 关键词加权与限幅

- `keywordBonus = positiveCount * 0.55 - negativeCount * 0.35`
- 限幅区间：`[-1.2, 1.8]`（防止关键词信号压过核心行为信号）

7. UP 主榜与多样化队列

- UP 主榜：按聚合 `creator_score` 取 TopN（默认 5，可配置）
- 视频队列不是直接按总分排序，而是做“多样化贪心”：
  - `adjusted = candidateScore - sameCreatorPenalty - repeatPenalty + noveltyBonus`
  - 同 UP 连续惩罚：`1.15`
  - 重复出现惩罚：`usedCount * 0.75`
  - 首次出现奖励：`+0.35`
- 作用：避免连续刷到同一个 UP，提高耐看度和探索感

8. 可解释输出

- 每条推荐会附带解释标签（如：`学习向 · 中时长 · 夜间友好 · 偏好UP`）
- `偏好UP` 触发阈值：`creatorAffinity > 0.8`

9. 冷启动可见性策略

- 推荐单在冷启动窗口内采用一次性曝光策略：
  - 若插件已启用、推荐单已生成、当前在推荐页且列表不在顶部，则自动回顶一次
  - 避免“推荐单已生成但首屏看不到”

10. 隐私与可控性

- 算法完全在本地运行，不上传历史记录用于个性化训练
- 支持一键清空本地画像与反馈，恢复冷启动推荐状态

<details>
<summary><b>📖 JSON 规则插件快速入门（点击展开）</b></summary>

#### 什么是 JSON 规则插件？

JSON 规则插件是一种**无需编程**的轻量级插件格式，只需编写简单的 JSON 文件即可实现内容过滤功能。

#### 插件结构

```json
{
    "id": "my_plugin",
    "name": "我的插件",
    "description": "插件描述",
    "version": "1.0.0",
    "author": "你的名字",
    "type": "feed",
    "rules": [
        {
            "field": "title",
            "op": "contains",
            "value": "广告",
            "action": "hide"
        }
    ]
}
```

#### 支持的字段

| 类型 | 字段 | 说明 |
|------|------|------|
| **Feed** | `title` | 视频标题 |
| **Feed** | `duration` | 视频时长（秒） |
| **Feed** | `owner.mid` | UP 主 UID |
| **Feed** | `owner.name` | UP 主名称 |
| **Feed** | `stat.view` | 播放量 |
| **Danmaku** | `content` | 弹幕内容 |

#### 操作符

| 操作符 | 说明 | 示例 |
|--------|------|------|
| `contains` | 包含 | `"value": "广告"` |
| `regex` | 正则匹配 | `"value": "震惊.*必看"` |
| `lt` / `gt` | 小于 / 大于 | `"value": 60` |
| `eq` / `ne` | 等于 / 不等于 | `"value": 123456` |
| `startsWith` | 以...开头 | `"value": "【"` |

#### 示例：短视频过滤器

```json
{
    "id": "short_video_filter",
    "name": "短视频过滤",
    "type": "feed",
    "rules": [
        { "field": "duration", "op": "lt", "value": 60, "action": "hide" }
    ]
}
```

#### 安装方式

1. 将 JSON 文件上传到公开可访问的 URL（如 GitHub Gist）
2. 在 BiliPai 中进入 **设置 → 插件中心 → 导入外部插件**
3. 粘贴链接并安装

</details>

> 📚 **完整文档**: [插件开发指南](docs/PLUGIN_DEVELOPMENT.md)
>
> 🧩 **示例插件**: [plugins/samples/](plugins/samples/)

### 📺 番剧追番

| 功能 | 描述 |
|-----|-----|
| **番剧首页** | 热门推荐、新番时间表、分区浏览 |
| **选集面板** | 官方风格底部弹出面板，支持季度/版本切换 |
| **追番管理** | 追番列表、观看进度自动同步 |
| **弹幕支持** | 番剧同样支持完整弹幕功能 |

### 📡 直播功能

| 功能 | 描述 |
|-----|-----|
| **直播列表** | 热门直播、分区浏览、关注直播 |
| **高清直播流** | HLS 自适应码率播放 |
| **直播弹幕** | 实时弹幕显示 |
| **一键跳转** | 动态卡片直接进入直播间 |

### 📱 动态页面

| 功能 | 描述 |
|-----|-----|
| **动态流** | 关注 UP 主的视频/图文/转发动态 |
| **分类筛选** | 全部动态 / 仅视频动态 切换 |
| **GIF 支持** | 完美渲染动态中的 GIF 图片 |
| **图片下载** | 长按预览，一键保存到相册 |
| **图片预览** | 全局 Overlay 预览层 + iOS 风格开关动画，评论场景顶部文案不遮挡图片主体，支持立体过渡切换 |
| **@ 高亮** | 动态中 @用户 自动高亮显示 |

### 💬 私信聊天

| 功能 | 描述 |
|-----|-----|
| **消息列表** | 支持查看历史消息，分页加载 |
| **富文本交互** | 支持表情包、@提醒、图片查看 |
| **链接预览** | 自动识别视频链接 (BV号) 并生成即时预览卡片 |
| **深色适配** | 聊天界面完美适配深色模式 |

### 📥 离线缓存

| 功能 | 描述 |
|-----|-----|
| **视频下载** | 支持选择画质下载，音视频自动合并 |
| **断点续传** | 网络中断后自动恢复下载 |
| **下载管理** | 清晰的下载列表与进度显示 |
| **本地播放** | 离线视频管理与播放 |

### 🔍 智能搜索

| 功能 | 描述 |
|-----|-----|
| **实时建议** | 输入时实时搜索建议 (300ms 防抖优化) |
| **热门榜单** | 展示当前热门搜索词 |
| **历史记录** | 搜索历史自动保存，支持去重 |
| **分类搜索** | 视频 / UP主 / 番剧 分类检索 |
| **视频音乐查找** | 🆕 快速识别并查找视频中的背景音乐 (BGM) |

### 🎨 现代 UI 设计

| 功能 | 描述 |
|-----|-----|
| **Material You** | 动态主题色，根据壁纸自动适配 |
| **深色模式** | 完美适配系统深色模式 |
| **iOS 风格底栏** | 优雅的毛玻璃导航栏效果 |
| **卡片动画** | 波浪式进场动画 + 弹性缩放 + 共享元素过渡 |
| **骨架屏加载** | Shimmer 效果，优雅的加载占位 |
| **Lottie 动画** | 点赞/投币/收藏 精美交互反馈 |
| **庆祝动画** | 三连成功烟花粒子特效 |
| **粒子消散** | "不感兴趣"操作触发灭霸响指式粒子消散动画 |
| **平板适配** | 侧边栏支持持久化切换，底部栏自动居中适配大屏体验 |

### 👤 个人中心

| 功能 | 描述 |
|-----|-----|
| **双登录方式** | 扫码登录 / 网页登录 |
| **个人信息** | 头像、昵称、等级、硬币数展示 |
| **观看历史** | 自动记录观看历史，支持云同步 |
| **收藏管理** | 收藏夹列表与视频管理 |
| **关注/粉丝** | 关注列表与粉丝列表浏览 |

### 🔒 隐私友好

- 🚫 **无广告** - 纯净观看体验，无任何广告植入
- 🔐 **权限最小化** - 仅申请必要权限 (无位置/通讯录/电话)
- 💾 **数据本地存储** - 登录凭证仅存本地，不上传任何隐私数据
- 🔍 **开源透明** - 完整源码公开，接受社区审查

---

## 📦 下载安装

<p align="left">
  <a href="https://github.com/jay3-yy/BiliPai/releases">
    <img src="https://img.shields.io/badge/Download-Official%20Release-fb7299?style=for-the-badge&logo=github" alt="Official Download">
  </a>
  <a href="https://github.com/AIALRA-0/BiliPai_Focus/releases">
    <img src="https://img.shields.io/badge/Download-Focus%20Release-1f8bff?style=for-the-badge&logo=github" alt="Focus Download">
  </a>
</p>

### 版本选择

| 版本 | 说明 | 下载地址 |
| --- | --- | --- |
| 官方原版 | 保持上游默认行为与发布节奏 | [jay3-yy/BiliPai Releases](https://github.com/jay3-yy/BiliPai/releases) |
| Focus 定制版 | 在上游基础上补充 Focus 开关、关注分组过滤和更克制的默认入口 | [AIALRA-0/BiliPai_Focus Releases](https://github.com/AIALRA-0/BiliPai_Focus/releases) |

### 系统要求

| 项目 | 要求 |
|-----|-----|
| **Android 版本** | Android 8.0+ (API 26) |
| **处理器架构** | 64 位 (arm64-v8a) |
| **推荐版本** | Android 12+ 获得完整 Material You 体验 |
| **安装包大小** | 因 ABI 与构建方式不同会有差异，请以 Releases 实际产物为准 |

### 安装步骤

1. 如果你想保持上游默认体验，请在 [官方 Releases](https://github.com/jay3-yy/BiliPai/releases) 下载 APK
2. 如果你想直接使用这份定制版，请在 [Focus Releases](https://github.com/AIALRA-0/BiliPai_Focus/releases) 下载 APK
3. 在设备上点击安装（可能需要允许“未知来源”应用）
4. 打开应用，扫码或网页登录 Bilibili 账号
5. 安装 Focus 版后，可前往 `设置 → 常规 → Focus` 调整专注模式与关注过滤

---

## 🛠 技术栈

### 核心框架

| 类别 | 技术 | 说明 |
|-----|-----|-----|
| **语言** | Kotlin 1.9+ | 100% Kotlin 开发 |
| **UI 框架** | Jetpack Compose | 声明式 UI，Material 3 设计语言 |
| **架构模式** | MVVM + Clean Architecture | 分层清晰，易于维护 |

### 网络与数据

| 类别 | 技术 | 说明 |
|-----|-----|-----|
| **网络请求** | Retrofit + OkHttp | RESTful API 调用 |
| **序列化** | Kotlinx Serialization | JSON 解析 |
| **本地存储** | Room + DataStore | 数据库 + 偏好设置 |
| **图片加载** | Coil Compose | 支持 GIF 解码 |

### 媒体播放

| 类别 | 技术 | 说明 |
|-----|-----|-----|
| **视频播放** | ExoPlayer (Media3) | DASH / HLS / MP4 支持 |
| **弹幕引擎** | DanmakuFlameMaster | B 站官方弹幕库 |
| **硬件解码** | MediaCodec | 高效硬件加速 |

### UI 增强

| 类别 | 技术 | 说明 |
|-----|-----|-----|
| **动画** | Lottie Compose | 高品质矢量动画 |
| **毛玻璃** | Haze | iOS 风格模糊效果 |
| **Material You** | Material 3 | 动态取色主题 |

---

## 📂 项目结构

### 仓库目录（Root）

```
├── app/                      # Android 应用主模块（Compose UI、业务实现）
├── baselineprofile/          # Macrobenchmark / Baseline Profile 生成模块
├── docs/                     # 文档与截图资源
├── scripts/                  # 构建与性能辅助脚本
├── plugins/                  # 外置插件与规则样例
├── androidMain/              # 多平台预留目录
├── commonMain/               # 多平台预留目录
├── build.gradle.kts          # 根构建脚本
└── settings.gradle.kts       # Gradle 模块声明
```

### Android 主源码结构

> 主路径：`app/src/main/java/com/android/purebilibili`

```
app/src/main/java/com/android/purebilibili
├── app/                      # Application / Activity 入口与启动流程
├── core/                     # 跨业务公共层（cache/network/store/ui/player/...）
├── data/                     # 数据层（model/repository）
├── domain/                   # 领域层（usecase）
├── feature/                  # 功能层（按场景拆分）
│   ├── audio/ bangumi/ cast/ category/ download/
│   ├── dynamic/ following/ home/ list/ live/
│   ├── login/ message/ onboarding/ partition/
│   ├── plugin/ profile/ search/ settings/
│   ├── space/ story/ video/ watchlater/ web/
│   ├── settings/             # 子分层：policy / screen / ui / update / webdav
│   └── video/                # 子分层：controller / danmaku / interaction / policy /
│                             #         player / screen / state / ui / usecase / util / viewmodel
└── navigation/               # 路由与导航编排
```

> [!TIP]
> 结构按当前 `main` 主分支整理。新增目录会在 Release 周期内同步到文档。
> 结构维护约束见：`STRUCTURE_GUIDELINES.adoc`

---

## 📚 Wiki

- AI / LLM 入口：[`llms.txt`](llms.txt)
- AI 导航指南：[`docs/wiki/AI.md`](docs/wiki/AI.md)
- Wiki 首页：[`docs/wiki/README.md`](docs/wiki/README.md)
- 功能矩阵：[`docs/wiki/FEATURE_MATRIX.md`](docs/wiki/FEATURE_MATRIX.md)
- 架构说明：[`docs/wiki/ARCHITECTURE.md`](docs/wiki/ARCHITECTURE.md)
- 发布流程：[`docs/wiki/RELEASE_WORKFLOW.md`](docs/wiki/RELEASE_WORKFLOW.md)
- QA 手册：[`docs/wiki/QA.md`](docs/wiki/QA.md)

---

## 🗺️ 路线图

> [!TIP]
> 路线图最后同步于 2026-03-04（v6.6.0）。功能以最新 Release、`CHANGELOG.md` 与主分支代码为准。

### ✅ 已完成功能

- [x] 首页推荐流 + 瀑布流布局
- [x] 视频播放 + 弹幕 + 手势控制 + 画中画 + 后台播放
- [x] 听视频模式 + 收藏夹/稍后再看播放列表 + 顺序/随机/单曲循环
- [x] 番剧/影视播放 + 选集面板
- [x] 直播播放 + 分区浏览
- [x] 动态页面 + 图片下载 + GIF 支持 + 多 UP 切换稳定性修复
- [x] 图片预览文案与过渡升级（评论场景顶部文案 + 方向感动画）
- [x] 离线下载 + 当前视频批量缓存 + 本地播放
- [x] 搜索 + 历史记录（含批量删除）
- [x] Material You + 深色模式
- [x] 高画质扫码登录 + 首播清晰度鉴权修复（非大会员首次 720P 回退问题）
- [x] 横屏控制栏增强（字幕面板 / 更多面板 / 播放顺序快捷切换）
- [x] 共享元素过渡动画 + 返回首页动效优化
- [x] 平板/折叠屏适配（侧边栏 + 底栏布局）
- [x] 应用内更新（手动检查 + 自动检查 + 启动提示 + 应用内下载/安装）
- [x] 插件系统核心架构
- [x] 内置插件 (空降助手 / 去广告 / 弹幕增强 / 夜间护眼 / 今日推荐单)
- [x] Firebase Analytics + Crashlytics（支持用户行为统计与崩溃追踪）
- [x] 评论/动态可选择复制能力（长按进入选择面板）

### 🚧 开发中

- [ ] 首页 / 动态 / 播放器高频链路性能重构（状态拆分、减少重组、降低首屏请求扇出）
- [ ] 文档站与 Wiki 持续补全（模块 API / 调试手册 / 回归清单）

### 📋 计划中

- [ ] 观看历史云同步
- [ ] 收藏夹管理
- [ ] 多账户切换
- [ ] 英文/繁体中文支持

---

## 🔄 更新日志

查看完整更新记录：[CHANGELOG.md](CHANGELOG.md)

### 最近更新 (v7.1.0 · 2026-03-22)

- 🎞️ **同步上游番剧 Overlay 动作重构**：番剧播放器现在跟上游 `v7.1.0` 一致，分享、交互按钮和控制层逻辑都收口到最新实现。
- 🧭 **Focus 命名统一完成**：设置入口、设置搜索、README 和独立变更日志不再混用旧 `AIALRA` 命名，统一改为 `Focus`。
- 🧱 **Windows 构建更稳**：`ci_verify_windows.ps1` 现在会自动探测 `Java 21` 和 Android SDK，减少本地重复配环境。
- 📘 **Focus 变更日志已独立版本化**：`FOCUS_CHANGLOG.md` 改成和官方 `CHANGELOG.md` 一样的版本分段样式，便于长期维护。

### 历史版本

- v5.2.0 / v5.1.4 / v5.1.3 / v5.1.1 / v5.1.0 / v5.0.5 / v5.0.4 变更详情请查看 [CHANGELOG.md](CHANGELOG.md)

---

## 🏗️ 构建项目

```bash
# 克隆 Focus 仓库
git clone git@github.com:AIALRA-0/BiliPai_Focus.git
cd BiliPai_Focus

# 命令行构建
./gradlew assembleDebug
```

### 构建要求

- JDK 21+
- Android Studio 2024.1+ 或更高版本
- Android SDK 36（Compile SDK）
- Gradle 8.13+
- (可选) `google-services.json`: 放置于 `app/` 目录下以启用 Firebase 功能。如无此文件，构建脚本将自动跳过相关插件，不影响编译运行。

### Windows 本地构建

```powershell
winget install --id GitHub.cli -e
winget install --id EclipseAdoptium.Temurin.21.JDK -e
winget install --id Google.AndroidStudio -e

$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot"
$env:ANDROID_SDK_ROOT="$env:LOCALAPPDATA\Android\Sdk"
$env:Path="$env:JAVA_HOME\bin;$env:ANDROID_SDK_ROOT\platform-tools;$env:Path"

.\scripts\ci_verify_windows.ps1
.\scripts\ci_verify_windows.ps1 -IncludeConnectedAndroidTests
.\scripts\ci_verify_windows.ps1 -IncludeBaselineProfile
```

- 脚本默认按稳定顺序执行 `:app:testDebugUnitTest`、`:app:lintDebug`、`:app:assembleDebug`、`:app:assembleRelease`。
- `-IncludeConnectedAndroidTests` 会额外执行 `:app:connectedDebugAndroidTest`。
- `-IncludeBaselineProfile` 会额外执行 `:baselineprofile:pixel6Api31BenchmarkAndroidTest`。
- `baselineprofile` 托管设备使用 `Pixel 6 / API 31 / AOSP image / x86_64 testedAbi`，而 `app` 的 `benchmark` 变体单独打出 `x86_64` 测试包；实际 `release` 产物仍保持 `arm64-v8a only`。

### 与上游同步

```bash
git remote add upstream https://github.com/jay3-yy/BiliPai.git
git fetch upstream
git checkout main
git merge upstream/main
```

> [!TIP]
> Focus 定制项都集中记录在 [`FOCUS_CHANGLOG.md`](FOCUS_CHANGLOG.md)。同步上游后，优先检查首页顶部栏、搜索页、视频详情页和历史记录页的冲突。

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

---

## 🙏 致谢

| 项目 | 说明 |
|-----|-----|
| [Jetpack Compose](https://developer.android.com/jetpack/compose) | 声明式 UI 框架 |
| [ExoPlayer (Media3)](https://github.com/androidx/media) | 媒体播放引擎 |
| [DanmakuFlameMaster](https://github.com/bilibili/DanmakuFlameMaster) | B 站弹幕引擎 |
| [DanmakuRenderEngine](https://github.com/bytedance/DanmakuRenderEngine) | 字节跳动高性能弹幕引擎 |
| [bilibili-API-collect](https://github.com/SocialSisterYi/bilibili-API-collect) | B 站 API 文档 |
| [biliSendCommAntifraud](https://github.com/freedom-introvert/biliSendCommAntifraud) | 评论反诈检测参考实现 |
| [Haze](https://github.com/chrisbanes/haze) | 毛玻璃效果库 |
| [Backdrop](https://github.com/Kyant0/AndroidLiquidGlass) | 液态玻璃效果 |
| [Lottie](https://github.com/airbnb/lottie-android) | Airbnb 动画库 |
| [Coil](https://github.com/coil-kt/coil) | Kotlin 图片加载库 |
| [Compose Shimmer](https://github.com/valentinilk/compose-shimmer) | 骨架屏加载效果 |
| [Compose Cupertino](https://github.com/alexzhirkevich/compose-cupertino) | iOS 风格 UI 组件 |
| [ZXing](https://github.com/zxing/zxing) | 二维码生成 |
| [Room](https://developer.android.com/training/data-storage/room) | 数据库持久化 |
| [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) | 偏好设置存储 |
| [Retrofit](https://github.com/square/retrofit) | HTTP 网络请求 |
| [Retrofit Kotlinx Serialization Converter](https://github.com/JakeWharton/retrofit2-kotlinx-serialization-converter) | Retrofit + Kotlinx 序列化转换器 |
| [OkHttp](https://github.com/square/okhttp) | HTTP 客户端 |
| [Brotli Decoder](https://github.com/google/brotli) | Brotli 内容解压支持 |
| [Cling](https://github.com/4thline/cling) | DLNA/UPnP 投屏能力 |
| [Jetty](https://github.com/jetty/jetty.project) | 内嵌 HTTP/Servlet 容器（投屏服务链路） |
| [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd) | 轻量本地代理服务（投屏回源） |
| [pinyin4j](https://sourceforge.net/projects/pinyin4j/) | 中文拼音转换（搜索/排序辅助） |
| [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) | Kotlin 序列化库 |
| [Firebase Crashlytics](https://firebase.google.com/docs/crashlytics) | 崩溃追踪分析 |
| [Orbital](https://github.com/skydoves/Orbital) | 共享元素过渡动画 |
| [AndroidX Palette](https://developer.android.com/training/material/palette-colors) | 动态取色引擎 |
| [LeakCanary](https://github.com/square/leakcanary) | 内存泄漏检测 |
| [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) | 后台任务管理 |
| [MockK](https://github.com/mockk/mockk) | Kotlin 单元测试 Mock 框架 |
| [Turbine](https://github.com/cashapp/turbine) | Kotlin Flow 测试断言工具 |

如有遗漏，欢迎通过 Issue / PR 继续补充致谢项目与说明。

---

## ⚠️ 免责声明

> [!CAUTION]
>
> 1. 本项目仅供 **学习交流**，严禁用于商业用途
> 2. 数据来源 Bilibili 官方 API，版权归上海幻电信息科技有限公司所有
> 3. 登录信息仅保存本地，不会上传任何隐私数据
> 4. 使用本应用观看内容时，请遵守相关法律法规
> 5. 如涉及版权问题，请联系删除

---

## 📄 许可证

本项目采用 [GPL-3.0 License](LICENSE) 开源协议

这意味着：

- ✅ 可以自由使用、修改和分发
- ✅ 修改后的代码必须同样开源
- ❌ 不得用于商业目的
- ❌ 不得移除原作者信息

## ⭐ Star History

如果这个项目对你有帮助，欢迎点个 Star ⭐

[![Star History Chart](https://api.star-history.com/svg?repos=AIALRA-0/BiliPai_Focus&type=Date)](https://github.com/AIALRA-0/BiliPai_Focus/stargazers)

---

<p align="center">
  Made with ❤️ by <a href="https://x.com/YangY_0x00">YangY</a>
  <br>
  <sub>( ゜- ゜)つロ 干杯~</sub>
</p>

