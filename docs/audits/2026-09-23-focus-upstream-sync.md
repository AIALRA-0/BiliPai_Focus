# BiliPai Focus 上游同步审计与验收记录

## 调查快照

- 调查日期：2026-09-23
- Focus 起点：`f4c052a1f42c687480a0311bb4b9351b443e73f3`，`9.1.1-focus.4`，`versionCode 226`
- 上游目标：`29700cdf343a3929c8d97e0d5e2644557785fd22`，主线 `0.2.3-beta.46`，`versionCode 384`
- 历史首次分叉：共同父提交 `7f0bc8cd4fdb11190c8b056f2c673c0ec18d29fa`，双方随后分别提交 3.3.2
- 当前合并基点：`b15e464e989c6f328dfa6a780a8c76cc84812fdc`，上游 9.1.1；Focus 在 `230f2b88` 最后合入此提交
- 自当前基点分叉：Focus 372 个提交，上游 3056 个提交；同名本地标签和上游标签有冲突，版本判断以远端分支与 Release 为准
- 上游最新稳定 Release 为 `v0.2.2`；`v0.2.3-beta.46` 是预发布版，目标主线还领先其标签 10 个提交

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
| 合并与语义冲突处理 | 未开始 |
| 新版 Debug、Release、单测与 Lint | 未开始 |
| 旧版升级、完整客户端更新链路 | 未开始 |
| 正式 Release 发布 | 未开始 |
