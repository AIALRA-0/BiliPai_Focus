# BiliPai Focus 上游同步审计与验收记录

## 调查快照

- 调查日期：2026-09-23
- Focus 起点：`f4c052a1f42c687480a0311bb4b9351b443e73f3`，`9.1.1-focus.4`，`versionCode 226`
- 初次合并的上游目标：`29700cdf343a3929c8d97e0d5e2644557785fd22`，主线 `0.2.3-beta.46`，`versionCode 384`。审计期间主线新增 `0ed40f8a62b81f68a2a510a4c854ea0aacdca790`，已在 `7d5a3b208` 合入并推送；发布前仍须完成运行验证。
- 2026-09-23 17:03 UTC 再次 `git fetch upstream main`：主线前进至 `25f0f4b54548a5a7b915a28725c37518e8ee4798`，`0.2.3-alpha.1`，`versionCode 386`，较前次合入基点新增 24 提交、54 个改动文件。此批已在 `550e07425` 语义合并并推送；Focus 版本码为 387，高于上游 386。
- 2026-09-23 18:10 UTC 主线再前进至 `6d5496b44abb832309d762adb991e9d6b2270ec5`，`0.2.3-alpha.2`，`versionCode 387`；相对 alpha.1 新增 5 提交、改动 11 个文件。最终同步提交为 `09eb220aeb93efcc4e980b410870dc6834e4ada8`，父提交分别为 Focus `550e0742503a0dfd2066897b13442fc22b0fb1fb` 与上游 `6d5496b44abb832309d762adb991e9d6b2270ec5`；Focus 版本为 `9.1.1-focus.5`、码 388。
- 历史首次分叉：共同父提交 `7f0bc8cd4fdb11190c8b056f2c673c0ec18d29fa`，双方随后分别提交 3.3.2
- 当前合并基点：`b15e464e989c6f328dfa6a780a8c76cc84812fdc`，上游 9.1.1；Focus 在 `230f2b88` 最后合入此提交
- 自当前基点分叉：Focus 372 个提交，上游 3056 个提交；同名本地标签和上游标签有冲突，版本判断以远端分支与 Release 为准
- 上游最新稳定 Release 为 `v0.2.2`；审计开始时最新预发布为 `v0.2.3-beta.46`，2026-09-23 16:05 UTC 又发布 `v0.2.3-Alpha.1`（`gh release list` 已核对）。最终目标须重新按主线而非旧 beta 标签确定。

## 上游版本演进与融合范围

| 上游阶段 | 主要变化 | Focus 融合方向 |
| --- | --- | --- |
| 9.1.2–9.2.0 | 平板设置与播放器、播放进度恢复、关注缓存刷新、评论入口、画中画与小窗、收藏加载修复 | 接收上游实现，保留 Focus 关注入口与数据键 |
| 9.2.1–9.3.6 | 首页滚动和卡片、直播重连、动态分页、历史筛选、搜索转场、Android 16 详情崩溃修复、竖屏播放控制 | 接收上游实现；动态分页与 Focus 分组过滤按结构化结果融合 |
| 9.4–9.9 | Story 竖屏流及预加载、视频画质和音频回退、播放器过渡、插件 API、缓存和生命周期优化 | 迁移到新版详情与插件架构；Focus 相关推荐显隐作用于新版详情状态 |
| 0.1.0–0.2.2 | 上游版本名重置而版本码继续递增；App/Web 混合首页、搜索新布局、收藏/稍后看/历史整合 | 保留 Focus 独立包名、签名与高于旧版的版本名；融合新版搜索 UI |
| 0.2.3 beta.1–beta.20 | 更新渠道、订阅及过滤插件、Miuix 设置、折叠屏窗口类别、首页瀑布流、详情组件拆分、启动诊断 | 保留上游订阅标签和自适应设置结构；迁移 Focus 设置入口与关注逻辑 |
| 0.2.3 beta.30–beta.46 + 10 提交 | 通知后台服务及设置迁移、低重组滚动、平板双栏详情、课程播放、私信卡片、评论能力、订阅顶栏与共享过渡 | 接收上游主线；针对 Focus 的首页、动态、详情、更新路径重点回归 |
| 0.2.3 alpha.1 新增 24 提交 | 视频标签尺寸、动态详情图片布局、订阅阅读和瀑布流、交互刷新率、首页过滤首次初始化修复、视频共享转场回退 | 已在 `550e07425` 合并；Debug 编译及 API 31/36 模拟器部分流程通过，完整单测仍有失败 |
| 0.2.3 alpha.2 新增 5 提交 | 合并首页 App 半边 Cookie 剥离与身份头、播放器空闲刷新率释放、版本和更新日志 | 已进入合并工作树；Focus 版本冲突按独立包升级语义解决，Cookie 与 buvid 冷启动路径重点回归 |

上游从 9.1.1 基点到首次合并目标跨 3056 个提交，包含大量非独立发布提交与重构；此表按主要版本阶段归并，后续增量以 `git log 29700cdf..upstream/main` 核对。

## 语义冲突处理记录

- 首页与动态：保留 Focus 本地关注分组的单归属、隐藏及五档排序；吸收上游 `DynamicFeedFetchResult`、更新基线和新分页结构，并继续补齐过滤后可见批次
- 设置与导航：采用上游分类式自适应界面和 Navigation3；在新版分类与路由中接入 Focus 设置，保留既有 DataStore 键及默认值
- 搜索与历史：采用上游加载、错误、筛选和管理 UI；热搜、发现、历史三个区段继续独立受 Focus 设置控制，清空历史菜单仍受 Focus 开关控制
- 视频详情：采用上游拆分后的手机与平板状态承载结构，在共享详情状态中应用相关推荐显隐，需运行时检查详情和播放器叠层
- 数据库：上游将 Room v4 升至 v6 并引入评论反诈记录；显式增加 4→6 迁移，取消破坏性回退，避免旧 Focus 搜索历史和屏蔽 UP 数据被删除
- 更新与发布：保留 Focus GitHub Release 更新源及旧版版本名比较；吸收上游 APK 摘要、候选版本筛选和构建元数据，正式构建增加签名连续性门禁
- Alpha.1 订阅页：保留 Focus 间距、动效及图片尺寸规格，融合上游居中阅读列、重试、选择文本、代码滚动、链接和图片预览。首轮编译发现冲突解决遗漏 `dp` 导入，已补齐待复验。
- Alpha.1 视频转场：接受上游移除 Official 静态共享边界控制器的回退，保留 Focus 预测返回进度预热、返回准备、取消时恢复及首页封面预取；需设备验证视觉转场和返回状态。
- Alpha.1 动态与设置：保留 Focus 动态卡片点击策略并融合上游图片布局；新增图片布局键沿用上游默认值并纳入 Focus 设置导出；视频标签尺寸采用上游新设置。
- Alpha.2 版本冲突：上游版本名改为 `0.2.3-alpha.2`、版本码 387；Focus 保留已发布客户端可识别的 `9.1.1-focus.5` 名称、独立包名和签名，并将版本码提升至 388。版本策略测试同步更新。
- Alpha.2 网络与播放器：接收上游仅对合并首页 App 半边剥离 Cookie 的策略与 HD 身份头、播放器空闲刷新率释放；冷启动 SPI 无 buvid 时的身份兜底仍在修复和测试。

## 升级测试预置数据

- Android 12 / API 31 模拟器已安装旧版正式包 `9.1.1-focus.4`、版本码 226，包名 `com.android.purebilibili.focus`
- 旧版中开启「显示热门」和「显示搜索历史」，创建搜索历史 `FocusUpgradeProbe0923`
- 强制结束并重启旧版后，热门标签和该搜索历史仍可见；新版覆盖安装后须再次确认
- 从模拟器旧版应用数据库只读备份核对：Room `user_version=4`，`search_history` 有 1 行。随后在旧版「黑名单管理 → 粘贴导入」录入测试 UID `998877665544`，界面显示导入成功；强制结束旧版并连同 WAL/SHM 只读备份后，`blocked_ups` 有 1 行。新版升级后须确认 v6 且两张表各仍有 1 行。
- 旧版设置页实际点击「检查更新」显示「已是最新版本」及 `v9.1.1-focus.4`；发布后从同一入口验证新版本发现、下载与安装
- 模拟器未登录，关注分组、登录态及账号内容的完整运行回归仍需测试账号

## 必须保护的 Focus 功能基线

| 用户能力与入口 | 实现和数据契约 | 上游对应与迁移要求 | 运行验收 |
| --- | --- | --- | --- |
| 独立安装与更新：首次声明、设置检查更新 | `app/build.gradle.kts` 的 `com.android.purebilibili.focus` 包名；`feature/settings/update/AppUpdateChecker.kt` 的 Focus Releases 地址和回退地址 | 上游包名和更新地址指向官方版；保留 Focus 包名、旧签名和更新源，吸收有效的新版资产校验 | 旧版发现更新、下载、覆盖安装、启动并保留数据 |
| 旧版本地数据：搜索历史与屏蔽 UP | `core/database/AppDatabase.kt` 的 Room 4→6 迁移；`search_history`、`blocked_ups` 表 | 上游 Room 已到 v6；旧 Focus v4 数据必须原位迁移，不能破坏性重建 | 旧包各造非空记录，签名相同的新版覆盖安装后从 UI 和数据库确认仍在 |
| 首页聚焦：设置 → Focus 设置 → 首页标签和分区显隐 | `core/store/SettingsManager.kt` 的 Focus 默认值；`feature/home/HomeTopCategoryPolicy.kt` 和 `HomeScreen.kt` | 上游保留通用标签配置，并增加 `Subscriptions` 标签；迁移时保留 Focus 默认显隐及已有用户顺序，定义新标签显隐 | 修改设置后首页即时变化，重启及升级后保持 |
| 本地关注分组：动态页分组入口，创建、重命名、删除、隐藏、分配 UP | `core/store/FocusFollowGroupStore.kt` 的 `focus_follow_group_config_json_v1`；`feature/dynamic/components/FocusFollowGroupSheet.kt`；`DynamicFocusFollowGroupPolicy.kt` | 上游提供服务器端关注标签，但没有本地单归属、跨首页及动态过滤；两个功能应共存 | 各操作生效，跨页过滤一致，重启及升级后分组和分配保持 |
| 首页关注排序与补页 | `feature/home/HomeFollowFocusPolicy.kt`、`HomeFollowingSyncPolicy.kt`、`HomeViewModel.kt` | 保留上游新的关注流和分页基础，重新接入 Focus 的五种排序、隐藏分组过滤及可见批次补页 | 五档排序、刷新、深分页、过滤后补页、无可见数据均正确 |
| 动态过滤、关注快照与补页 | `feature/dynamic/DynamicViewModel.kt`、`DynamicScreen.kt`、`DynamicFocusFollowGroupPolicy.kt` | 与上游新版动态数据和分页游标融合，不能回退到旧数据结构 | 隐藏分组、单 UP、选中对象被隐藏、分页结束和重进页面正确 |
| 搜索降噪：热搜、发现、搜索历史区块 | `search_hot_section_enabled`、`search_discover_section_enabled`、`search_history_section_enabled`；`SearchLandingUi.kt` | 前两项与上游同键但 Focus 默认关闭；历史显隐为 Focus 独有，保留键和值 | 三个开关独立生效并持久化 |
| 详情相关推荐显隐 | `focus_video_related_videos_section_visible`；`VideoDetailScreenStateHolder.kt` 从设置流映射详情状态，手机 `VideoContentSection.kt` 与平板布局消费该状态 | 上游推荐功能仍在，需在新版手机和平板详情布局统一应用 Focus 显隐 | 开关生效，详情重进、重启及升级后保持；关闭时标题也须消失 |
| 历史清空入口显隐 | `focus_history_clear_all_action_enabled`；`CommonListScreen.kt` | 仅控制菜单项，不删除历史数据或底层清空能力 | 隐藏入口后历史仍在，开启后入口恢复 |
| 网络布尔字段兼容 | `FlexibleBooleanSerializer` 及动态、空间、收藏等模型字段 | 按新版响应结构逐字段保留仍有效的容错，避免用旧模型覆盖上游新字段 | 数字、字符串及正常布尔响应都能解析 |

## 已取得的验证证据

- 旧版 `9.1.1-focus.4` 的本地 APK 在 Android 12 / API 31 模拟器成功安装并启动
- 首次声明和引导已完成，首页显示关注、首页、动态、历史和个人入口；未登录关注页显示登录提示
- 起点执行 `:app:compileDebugKotlin --no-daemon --no-configuration-cache` 通过，59 个任务中 57 个执行、2 个命中缓存；首次构建耗时 10 分 52 秒
- `git merge-tree --write-tree HEAD upstream/main` 预演报告 44 条冲突；这是文件级冲突数量，不能代替功能级审计
- 首次融合提交 `42a2e2d9b` 后，`:app:compileDebugKotlin` 在固定 Miuix 源码依赖下通过；`:app:testDebugUnitTest` 首轮在上游 API 变更后出现测试源码编译错误，修复测试迁移后运行 8,272 项、352 项失败。失败含结构断言过期与真实功能/性能问题，不能作为发布通过。
- 第二轮定向单测编译通过，运行 239 项、8 项失败；动态正文解析、Focus 列表补页、播放器键盘映射、启动与模糊缓存相关定向测试通过。8 项集中于 Home 布局/结构断言 6 项和性能棘轮 2 项；修复正在复验。日志保存在本地 `.local/targeted-tests-3.log`，该目录不纳入版本库。
- 首次 CI 的 Build 工作流在旧 `setup-android@v3` 请求已移除的 `tools` SDK 包时失败；工作流已改为 `setup-android@v4` 和 `platform-tools`，需推送后以新 CI 运行确认。
- 上游新增共享边界转场提交合入后，Debug 主代码与测试代码编译通过；全量单测运行 8,275 项，261 项失败，较上轮少 91 项。失败较集中于旧版结构断言、样式迁移棘轮及部分真实导航返回问题；不能据此发布。
- 对 `7d5a3b208` 的 CI 已越过 Android SDK 安装，Build 的质量门禁执行 53 项、9 项失败，其中硬编码颜色/间距/字级、样式迁移/允许列表、Haze 与同步设置读取棘轮未通过；后续 APK 构建被门禁阻挡。
- 首次 `:app:assembleDebug` 通过，生成 `BiliPai-Focus-9.1.1-focus.5-debug.apk`（70,319,625 字节），并以独立 Debug 包名在 API 31 模拟器成功安装。此构建早于下面两项运行时修复，不能视为最终包。
- 模拟器首次启动完成使用须知；首页视频加载、`DeepSeek` 搜索结果、视频详情播放、暂停、Seek、全屏和返回均实际运行。Focus 设置页的「显示热门」开关切换后保持开启，重启并返回首页可见 `Popular` 标签。未登录状态无法验证关注分组和账号内容。
- 实机流程发现 Debug `9.1.1-focus.5-debug` 被错误提示更新到旧版 `9.1.1-focus.4`：版本解析未识别本地构建后缀，已修解析并以 `AppUpdateCheckerTest` 验证三类后缀。第二版 Debug APK 覆盖安装后冷启动无旧版更新弹窗，`Popular` 设置仍在。
- 视频详情返回 411dp 竖屏后，首页曾错误显示侧边栏，冷启动才恢复底栏。仅补 `remember` 缓存键的第二版 Debug APK 仍复现；第三版改为在 adaptive 类别与当前 Activity 配置宽度不一致时采用当前宽度类别，定点测试通过。第三版覆盖安装后连续两次打开不同视频并返回，底栏均保持，`Recommend` 的界面树位置为 `[231,2143][437,2196]`，系统配置仍为 `w411dp`；截图和界面树见本地 `.local/focus-third-return*`，Logcat 采样未见崩溃或 ANR。
- 后续全量单测依次为 8,283 项／163 项失败和 8,284 项／123 项失败。新的导航、设置、Home 与详情修复已将失败数继续降低，但发布门禁仍未通过。
- 发布工作流的签名证书比对原来把 `apksigner` 输出的第二字段当摘要，实际第二字段是固定标题，可能对异签包假阳性。已改为最后字段并校验 64 位十六进制；正式发布前仍需验证新旧证书实际相同。
- Debug 包中将相关推荐、热搜、搜索发现、搜索历史和历史清空入口开启后，强制结束并重启，五项界面开关仍为开启。搜索首页三项全开时均有区块，全关时均无区块；只重新开启历史后，先前的 `DeepSeek` 本地历史再次出现。视频详情在相关推荐开启时显示带视频卡片的推荐列表。关闭后曾留下无卡片的「相关推荐」标题，已改为仅有可见推荐时显示标题；新 APK 运行复验待完成。
- Debug 包动态页的 Focus 关注分组经 UI 实操：创建本地测试分组、设置隐藏、重命名，强制结束并重启后名称及隐藏状态仍在；五档首页关注排序选项均显示，将排序从时间倒序改为随机后重启仍保持随机。随后把 `-ANVER-` 从默认组分配到临时隐藏组，计数由 300/0 变为 299/1；强制结束并重启后，隐藏状态、计数与成员姓名仍在。删除临时组后默认组恢复 300 人，并恢复时间倒序。证据为 `.local/debug-group-*.xml`、`.local/debug-assigned-*.xml` 与截图；跨页隐藏过滤与旧版升级保留仍待验证。
- 设置模型与 Focus/HomeTopTab 读写实现拆分后，`SettingsManager.kt` 从 8128 行降到 6659 行，保留 DataStore key、默认值和兼容门面。随后完整单测主代码与测试代码编译通过，运行 8287 项、89 项失败；相比上一轮 123 项失败减少 34 项，仍需逐项收束。
- 最新折叠屏方向策略定点测试（折叠屏、播放器方向/呈现、启动主题、权限和 SettingsManager 大小棘轮）共 51 项通过。方向判断增加活动窗口是否被方向请求约束的显式信号，以区分外屏实际竖握与被锁定为竖屏的横向物理外屏；Release 及真实折叠设备仍待验证。
- 安装 Android 36 Play Store x86_64 系统镜像并冷启动第二台模拟器，第三版 Debug APK 首装、使用须知、首页与搜索页可打开，当前 Logcat 未见该进程崩溃或 ANR。该模拟器未登录，首页关注页显示登录空态；`DeepSeek` 搜索展示无结果，且本机直接访问 B 站公开搜索端点得到 HTTP 412，需在新包及可用网络环境复测，不能把无结果直接归因于应用。搜索交互期间 StrictMode 精确捕获 `SearchViewModel.search()` 首次在主线程读取 `easter_egg` SharedPreferences；已改为异步 DataStore 状态流，尚待新 APK 复测。播放器点赞/投币彩蛋及视频笔记的同步设置读取也已迁移到挂起式读取，待编译和回归。
- 最近完整单测主代码与测试代码编译通过，运行 8,295 项、46 项失败；与上一轮 89 项相比再减少 43 项。修复了样式迁移期间的一处重复 import 后得到此结果；余下有真实性能棘轮和结构断言，未达到发布门禁。
- 合入 alpha.1 后首轮完整单测主代码和测试代码均编译通过，执行 8,302 项、34 项失败。相比合入前最近一轮的 8,296 项、32 项失败，新增的失败主要是上游回退 Official shared-bounds 后旧导航结构断言仍要求已删除路径，以及订阅页冲突遗漏的 `dp` 导入已在测试前修正；现有样式、性能棘轮和若干 UI 结构失败仍在修复。日志为本地 `.local/unit-tests-alpha1b.log`，未达到发布门禁。
- 导航断言改为检查当前 Miuix 转场、预测返回与封面预热的实际调用链后，第二轮完整单测执行 8,303 项、23 项失败；主代码和测试代码均编译通过。`FeedDocumentParserTest` 13 项、`DynamicCardClickPolicyTest` 40 项、`AppVersionPolicyTest` 1 项、`BiliPaiFeedFilterPluginTest` 20 项在首轮合并测试中均通过；样式、性能棘轮及七处结构断言仍需处理。本地日志 `.local/unit-tests-alpha1c.log`。
- Alpha.1 Debug APK 构建通过并在 API 31 和 API 36 模拟器覆盖安装；API 31 首页加载、两次视频详情返回到底栏正常，API 36 搜索返回、深色设置页及 1280×800 模拟平板双栏布局正常。网络对公开搜索接口返回 HTTP 412，故无结果不能算搜索成功；两台设备均无测试账号。
- Alpha.1 `:app:assembleRelease` 的 Kotlin 编译与签名校验通过，但 Android Lint 在并发源码编辑期间分析 `AudioModeMusicPlayer.kt` 报内部 `Unexpected owner function: null`，构建中止。源码稳定后使用单 worker、无 Gradle daemon 单独重跑 `:app:lintVitalAnalyzeRelease`，3 分 9 秒通过；完整 Release 包、R8 结果和正式安装尚未验证。

## 发布链路约束

- 旧客户端读取 `AIALRA-0/BiliPai_Focus` 的 GitHub Releases，原版项目的分发策略不适用于 Focus
- 旧版本使用版本名称比较；上游回到 `0.2.x` 后，新 Focus 版本名称必须确保旧客户端认为它高于 `9.1.1-focus.4`
- 正式包名必须保持 `com.android.purebilibili.focus`，版本码必须高于已发布的 226，正式签名证书必须与旧版本一致
- `Build.yml` 已加入 Focus 签名密钥注入、与现有 Release APK 的证书摘要连续性比较，以及发布元数据与 SHA-256 资产；Tag CI 的配置缓存序列化失败使 CI 证书连续性门禁未运行，修复提交 `2c1566558` 已推送至同步分支，正式 APK 则由 Tag 源码本机签名构建并发布
- `.4` 客户端已实测通过既有 GitHub Release 更新链路发现、下载、安装和启动 `.5`；公开资产摘要与下载、安装文件摘要相符。旧客户端启动安装器前不强制校验哈希，此次端到端测试不代表旧客户端具备该安装门禁

## 后续验收状态

| 项目 | 状态 |
| --- | --- |
| 历史与版本调查 | 已核实 |
| 特色功能代码基线 | 已登记；API 31 登录态回归与最终包复测见 [alpha.2 后续验证记录](2026-09-23-alpha2-verification.md) |
| 上游版本差异审计 | 已按共同祖先至 alpha.2 的主要阶段整理，见 alpha.2 后续验证记录 |
| 合并与语义冲突处理 | 已合并为 `09eb220aeb93efcc4e980b410870dc6834e4ada8` 并推送同步分支；语义处理详见本报告。分支 CI run `35959218778` 的 `quality-guards` 与 `build` 均成功；`publish-release` 按分支运行设计跳过 |
| 新版 Debug、Release、单测与 Lint | 全量 JVM 测试 8,334 项通过；Lint 0 error、591 warning、2 hint；最终 Debug 与 AndroidTest 构建通过。API 31 instrumentation 63 项中 16 fail、3 skip，不能视为全绿；3 项定向复测通过，三列瀑布流用例仍失败。最终签名 Release 已构建并完成本地安装升级验证。API 36 最终签名包匿名首页、设置、外观、搜索及浅/深色冒烟通过，两次冷启动 2.87 秒与 2.65 秒；未观察到应用 Crash/ANR |
| 旧版安装升级 | API 31 模拟器中真实从 `.4`（码 226）覆盖升级到签名 `.5`（码 388）；安装时间、Room 数据和 Focus 设置已核验保留。另一台 API 31 登录态模拟器也完成签名 `.5` 覆盖安装，登录态和 Focus 分组状态跨冷启动保留 |
| GitHub Release 与旧客户端更新器端到端 | 已完成。公开 `.5` Release 的 APK 被旧 `.4` 客户端发现并下载，系统安装器完成覆盖安装，新版启动；版本码、安装时间、Room v4→v6 数据、Focus 设置均已核对保留。下载及安装后 APK SHA 与公开资产一致 |
| 正式 Release 发布 | 已完成。annotated tag `v9.1.1-focus.5` 指向 `48ea5310e`；公开 Release 含 APK、`build-metadata.json`、`checksums.txt`。APK 为从 Tag 源码生成的本机签名回退构建，sha256 `cdac08633b3fa5b3e6d382f6cfa43f3fb1eb0df00a2c458ce2864de25cf2b668`，签名与 `.4` 连续。Tag CI run `35960620969` 在 Release 构建缓存序列化时失败，未执行 CI 证书门禁或生成 CI 证明；该差异已记录，不将本机签名产物称作 CI 构建 |

API 36、签名构建偏差、旧客户端真实更新器以及用户数据保留的详细证据见 [alpha.2 后续验证记录](2026-09-23-alpha2-verification.md#最终发布ci-偏差与更新器端到端核验)。发布 Tag 固定在上游 `6d5496b44`。Tag 发布期间，上游 main 已前进到 `f4d78bf6d`（2026-09-24 14:43 +08），新增 5 个提交：移除全局交互刷新率投票（`46e1521aa`）、横屏侧栏偏好（`adc63da5b`）、聊天回复避让输入栏（`a777d5170`）、大屏首页重选刷新（`cab94c203`）、显示模式偏好（`f4d78bf6d`）；涉及 25 个文件，约 365 行新增、323 行删除。这些提交未进入 `.5`，列入下一同步周期评估，已发布 Tag 不改写。本报告的上游版本差异及发布范围以 `.5` 锁定的 `6d5496b44` 快照为准，不声称它等于当前上游 HEAD。
