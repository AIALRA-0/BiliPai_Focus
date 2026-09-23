# BiliPai Focus 上游同步审计与验收记录

## 调查快照

- 调查日期：2026-09-23
- Focus 起点：`f4c052a1f42c687480a0311bb4b9351b443e73f3`，`9.1.1-focus.4`，`versionCode 226`
- 上游目标：`29700cdf343a3929c8d97e0d5e2644557785fd22`，主线 `0.2.3-beta.46`，`versionCode 384`
- 历史首次分叉：共同父提交 `7f0bc8cd4fdb11190c8b056f2c673c0ec18d29fa`，双方随后分别提交 3.3.2
- 当前合并基点：`b15e464e989c6f328dfa6a780a8c76cc84812fdc`，上游 9.1.1；Focus 在 `230f2b88` 最后合入此提交
- 自当前基点分叉：Focus 372 个提交，上游 3056 个提交；同名本地标签和上游标签有冲突，版本判断以远端分支与 Release 为准
- 上游最新稳定 Release 为 `v0.2.2`；`v0.2.3-beta.46` 是预发布版，目标主线还领先其标签 10 个提交

## 上游版本演进与融合范围

| 上游阶段 | 主要变化 | Focus 融合方向 |
| --- | --- | --- |
| 9.1.2–9.2.0 | 平板设置与播放器、播放进度恢复、关注缓存刷新、评论入口、画中画与小窗、收藏加载修复 | 接收上游实现，保留 Focus 关注入口与数据键 |
| 9.2.1–9.3.6 | 首页滚动和卡片、直播重连、动态分页、历史筛选、搜索转场、Android 16 详情崩溃修复、竖屏播放控制 | 接收上游实现；动态分页与 Focus 分组过滤按结构化结果融合 |
| 9.4–9.9 | Story 竖屏流及预加载、视频画质和音频回退、播放器过渡、插件 API、缓存和生命周期优化 | 迁移到新版详情与插件架构；Focus 相关推荐显隐作用于新版详情状态 |
| 0.1.0–0.2.2 | 上游版本名重置而版本码继续递增；App/Web 混合首页、搜索新布局、收藏/稍后看/历史整合 | 保留 Focus 独立包名、签名与高于旧版的版本名；融合新版搜索 UI |
| 0.2.3 beta.1–beta.20 | 更新渠道、订阅及过滤插件、Miuix 设置、折叠屏窗口类别、首页瀑布流、详情组件拆分、启动诊断 | 保留上游订阅标签和自适应设置结构；迁移 Focus 设置入口与关注逻辑 |
| 0.2.3 beta.30–beta.46 + 10 提交 | 通知后台服务及设置迁移、低重组滚动、平板双栏详情、课程播放、私信卡片、评论能力、订阅顶栏与共享过渡 | 接收上游主线；针对 Focus 的首页、动态、详情、更新路径重点回归 |

上游从 9.1.1 基点到主线跨 3056 个提交，包含大量非独立发布提交与重构；此表按主要版本阶段归并，具体提交仍以 `git log b15e464..29700cdf` 为准

## 语义冲突处理记录

- 首页与动态：保留 Focus 本地关注分组的单归属、隐藏及五档排序；吸收上游 `DynamicFeedFetchResult`、更新基线和新分页结构，并继续补齐过滤后可见批次
- 设置与导航：采用上游分类式自适应界面和 Navigation3；在新版分类与路由中接入 Focus 设置，保留既有 DataStore 键及默认值
- 搜索与历史：采用上游加载、错误、筛选和管理 UI；热搜、发现、历史三个区段继续独立受 Focus 设置控制，清空历史菜单仍受 Focus 开关控制
- 视频详情：采用上游拆分后的手机与平板状态承载结构，在共享详情状态中应用相关推荐显隐，需运行时检查详情和播放器叠层
- 数据库：上游将 Room v4 升至 v6 并引入评论反诈记录；显式增加 4→6 迁移，取消破坏性回退，避免旧 Focus 搜索历史和屏蔽 UP 数据被删除
- 更新与发布：保留 Focus GitHub Release 更新源及旧版版本名比较；吸收上游 APK 摘要、候选版本筛选和构建元数据，正式构建增加签名连续性门禁

## 升级测试预置数据

- Android 12 / API 31 模拟器已安装旧版正式包 `9.1.1-focus.4`、版本码 226，包名 `com.android.purebilibili.focus`
- 旧版中开启「显示热门」和「显示搜索历史」，创建搜索历史 `FocusUpgradeProbe0923`
- 强制结束并重启旧版后，热门标签和该搜索历史仍可见；新版覆盖安装后须再次确认
- 从模拟器旧版应用数据库只读备份核对：Room `user_version=4`，`search_history` 有 1 行，`blocked_ups` 有 0 行；新版升级后须确认 v6 且搜索历史仍有 1 行
- 旧版设置页实际点击「检查更新」显示「已是最新版本」及 `v9.1.1-focus.4`；发布后从同一入口验证新版本发现、下载与安装
- 模拟器未登录，关注分组、登录态及账号内容的完整运行回归仍需测试账号

## 必须保护的 Focus 功能基线

| 用户能力与入口 | 实现和数据契约 | 上游对应与迁移要求 | 运行验收 |
| --- | --- | --- | --- |
| 独立安装与更新：首次声明、设置检查更新 | `app/build.gradle.kts` 的 `com.android.purebilibili.focus` 包名；`feature/settings/update/AppUpdateChecker.kt` 的 Focus Releases 地址和回退地址 | 上游包名和更新地址指向官方版；保留 Focus 包名、旧签名和更新源，吸收有效的新版资产校验 | 旧版发现更新、下载、覆盖安装、启动并保留数据 |
| 首页聚焦：设置 → Focus 设置 → 首页标签和分区显隐 | `core/store/SettingsManager.kt` 的 Focus 默认值；`feature/home/HomeTopCategoryPolicy.kt` 和 `HomeScreen.kt` | 上游保留通用标签配置，并增加 `Subscriptions` 标签；迁移时保留 Focus 默认显隐及已有用户顺序，定义新标签显隐 | 修改设置后首页即时变化，重启及升级后保持 |
| 本地关注分组：动态页分组入口，创建、重命名、删除、隐藏、分配 UP | `core/store/FocusFollowGroupStore.kt` 的 `focus_follow_group_config_json_v1`；`feature/dynamic/components/FocusFollowGroupSheet.kt`；`DynamicFocusFollowGroupPolicy.kt` | 上游提供服务器端关注标签，但没有本地单归属、跨首页及动态过滤；两个功能应共存 | 各操作生效，跨页过滤一致，重启及升级后分组和分配保持 |
| 首页关注排序与补页 | `feature/home/HomeFollowFocusPolicy.kt`、`HomeFollowingSyncPolicy.kt`、`HomeViewModel.kt` | 保留上游新的关注流和分页基础，重新接入 Focus 的五种排序、隐藏分组过滤及可见批次补页 | 五档排序、刷新、深分页、过滤后补页、无可见数据均正确 |
| 动态过滤、关注快照与补页 | `feature/dynamic/DynamicViewModel.kt`、`DynamicScreen.kt`、`DynamicFocusFollowGroupPolicy.kt` | 与上游新版动态数据和分页游标融合，不能回退到旧数据结构 | 隐藏分组、单 UP、选中对象被隐藏、分页结束和重进页面正确 |
| 搜索降噪：热搜、发现、搜索历史区块 | `search_hot_section_enabled`、`search_discover_section_enabled`、`search_history_section_enabled`；`SearchLandingUi.kt` | 前两项与上游同键但 Focus 默认关闭；历史显隐为 Focus 独有，保留键和值 | 三个开关独立生效并持久化 |
| 详情相关推荐显隐 | `focus_video_related_videos_section_visible`；`VideoDetailScreen.kt` | 上游推荐功能仍在，需在新版手机和平板详情布局统一应用 Focus 显隐 | 开关生效，详情重进、重启及升级后保持 |
| 历史清空入口显隐 | `focus_history_clear_all_action_enabled`；`CommonListScreen.kt` | 仅控制菜单项，不删除历史数据或底层清空能力 | 隐藏入口后历史仍在，开启后入口恢复 |
| 网络布尔字段兼容 | `FlexibleBooleanSerializer` 及动态、空间、收藏等模型字段 | 按新版响应结构逐字段保留仍有效的容错，避免用旧模型覆盖上游新字段 | 数字、字符串及正常布尔响应都能解析 |

## 已取得的验证证据

- 旧版 `9.1.1-focus.4` 的本地 APK 在 Android 12 / API 31 模拟器成功安装并启动
- 首次声明和引导已完成，首页显示关注、首页、动态、历史和个人入口；未登录关注页显示登录提示
- 起点执行 `:app:compileDebugKotlin --no-daemon --no-configuration-cache` 通过，59 个任务中 57 个执行、2 个命中缓存；首次构建耗时 10 分 52 秒
- `git merge-tree --write-tree HEAD upstream/main` 预演报告 44 条冲突；这是文件级冲突数量，不能代替功能级审计

## 发布链路约束

- 旧客户端读取 `AIALRA-0/BiliPai_Focus` 的 GitHub Releases，原版项目的分发策略不适用于 Focus
- 旧版本使用版本名称比较；上游回到 `0.2.x` 后，新 Focus 版本名称必须确保旧客户端认为它高于 `9.1.1-focus.4`
- 正式包名必须保持 `com.android.purebilibili.focus`，版本码必须高于已发布的 226，正式签名证书必须与旧版本一致
- 当前 `Build.yml` 缺少发布签名密钥注入，且构建元数据把包名写成了官方包名；发布前必须修复并实测
- 下载到安装之间当前缺少强制摘要校验，发布前应补充并验证损坏文件不会进入安装

## 后续验收状态

| 项目 | 状态 |
| --- | --- |
| 历史与版本调查 | 已核实 |
| 特色功能代码基线 | 已登记，待逐项运行验证 |
| 上游版本差异审计 | 进行中 |
| 合并与语义冲突处理 | 44 个文件冲突已解除并暂存，编译及运行验证中 |
| 新版 Debug、Release、单测与 Lint | 编译因 MIUIX GitHub Packages 401 转为固定源码构建，正在验证 |
| 旧版升级、完整客户端更新链路 | 未开始 |
| 正式 Release 发布 | 未开始 |
