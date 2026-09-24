# Alpha.2 后续验证记录

本记录承接 [`2026-09-23-focus-upstream-sync.md`](2026-09-23-focus-upstream-sync.md)。

- 上游 `main` 在 2026-09-23 21:40 UTC 为 `6d5496b44abb832309d762adb991e9d6b2270ec5`，即 `0.2.3-alpha.2`。
- 合并工作树的 Focus 版本为 `9.1.1-focus.5`、`versionCode=388`；主源码执行 `:app:compileDebugKotlin` 通过，日志 `.local/compile-alpha2a.log`。
- `:app:lintDebug` 首轮在固定 Miuix 源码、JDK 21、4 GiB Gradle 堆、单 worker、无 daemon 与无 configuration cache 下报告 13 error、590 warning、2 hint。修复 13 个错误后同配置复测通过，耗时 24 分 1 秒，最终为 0 error、590 warning、2 hint；报告备份为 `.local/lint-results-debug-alpha2c.{html,sarif,txt}`，任务日志 `.local/lint-debug-alpha2c.log`。警告总数未增加。

## 上游主线阶段核验

按 `b15e464e`（9.1.1）至 `6d5496b44`（主线 0.2.3-alpha.2）的实际可达 Git 历史，跨度为主线 2,739 提交、含合并侧枝共 3,086 提交；这里的「主线」是 `--first-parent` 计数。此前 Focus 的 `550e07425` 合并以 `25f0f4b` 为上游父提交，吸收了截至 alpha.1 的主线；之后 alpha.2 增量为主线 4 提交、含合并侧枝 5 提交。alpha.1/alpha.2 是主线源码和日志中的开发版本，不能当作已存在的 Git Tag 或正式 Release。

| 可达上游阶段 | 截止提交 | 主线／含侧枝 | 主要代码演化及本次处置 |
| --- | --- | ---: | --- |
| 9.1.1 → 9.5.0 | `81db1c121` | 179／180 | 小窗与画中画、首页卡片骨架、竖屏进度和音量、横屏速度等；继承新版播放器，保留 Focus 首页入口。 |
| 9.5.0 → 9.9.1 | `e27f11b9f` | 187／196 | Miuix 迁移、预测返回、竖屏播放和评论分页；纳入上游新 UI，修复 Focus 详情转场。 |
| 9.9.1 → 26.0805.1 | `2c1bd2ddf` | 569／742 | 动态顶栏、取消返回时保留播放、Android 12 模糊恢复等；保留 Focus 动态分组过滤，并迁移至新页结构。 |
| 26.0805.1 → 0.2.2 | `746b1a357` | 82／191 | 恢复语义版本、App/Web 合并流与搜索、96 提交的 UI 分支合并；Focus 独立包名和更新版本序列继续沿用。 |
| 0.2.2 → beta.21 | `57f6b89da` | 953／975 | 诊断导出、转场及折叠屏修复；吸收新版设置/诊断。 |
| beta.21 → beta.34 | `1438c981d` | 323／324 | 顶栏材质、渐进模糊、底栏/搜索联动；保留 Focus 首页显隐和触控行为。 |
| beta.34 → beta.46 | `ed1121505` | 414／438 | 通知路由、跨尺寸播放器、课程播放、评论与聊天更新；重点复测详情、播放与返回。 |
| beta.46 → 主线 alpha.2 | `6d5496b44` | 32／40 | 订阅/动态版式、合并首页 Cookie 身份修复、空闲刷新率释放；已合入工作树并验证构建/运行中。 |

本表以可达标签和主线源码为界，按阶段审计；不能把区间内的普通提交误称为独立发布版本。当前最新稳定 Release 仍为 v0.2.2，开发主线为 0.2.3-alpha.2。
- 旧版正式 APK `v9.1.1-focus.4` 已从现有 GitHub Release 下载到 `.local/old-release/`，SHA-256 为 `b0c97a5cef45fb3131617b0a93462bff86033b645c7eb3d7a82177c23e53aff4`，签名证书 SHA-256 为 `a2f021866ee4e5f8f63df109e3369be3ceefa98fee3485e4d583e4a24f07f6bc`。它将用于新版签名连续性与覆盖升级验证。
- API 31 模拟器再次确认安装 `9.1.1-focus.4`／码 226。强制结束旧包后备份数据库及 WAL/SHM，SQLite 只读查询得到 `user_version=4`、`search_history=1`、`blocked_ups=1`；升级前模拟器快照名为 `focus_preupgrade_v4`。真实新版升级后须与这些值逐项比较。
- 从该快照启动旧版，首页实际停在 Focus 的 Following 标签；Focus 设置页确认「显示推荐／直播／游戏／分区按钮」关闭、「显示关注／热门／启用关注过滤」开启。旧版设置页的「源码一致性」匹配 `.4` Release，点击「检查更新」后显示「已是最新版本」。UI 树和截图留在 `.local/focus-old-*.xml`、`.local/focus-old-focus-settings.png`，作为发布后更新发现与设置保留的对照。
- 同一 API 31 模拟器对旧版 `.4` 执行 `am force-stop` 后 `am start -W`，得到 `LaunchState: COLD`、`TotalTime: 4205 ms`、`WaitTime: 4230 ms`。新版本须在同设备同条件下复测；单次启动时长只作对照，不据此断言统计性能改善。
- alpha.2 首轮完整 `:app:testDebugUnitTest` 在补齐迁移代码的 `TextOverflow` 导入后完成 8320 项，其中 16 项失败（日志 `.local/unit-tests-alpha2b.log`）：主题边界、上游组件迁移后的源结构断言与动态按钮可访问性。逐项修复后全量复测为 1158 个测试类、8320 项、0 failure、0 error，日志 `.local/unit-tests-alpha2f.log`。
- Haze 源码注册点在 alpha.2 合并双方父提交中均为 31 处，合并本身没有新增；冻结的全局 28 处上限已经与父提交不符。番剧页无消费者时的录制条件与逐文件精确清单已调整，完整单测通过。
- `:app:clean :app:assembleDebug` 通过（8 分 33 秒，`.local/clean-debug-alpha2a.log`）；Debug APK 为 `BiliPai-Focus-9.1.1-focus.5-debug.apk`，包名 `com.android.purebilibili.focus.debug`，码 388，API 26–37，ARM64。API 31 模拟器安装后冷启动 `3390 ms`，匿名热门页加载了实时视频卡片，视频详情和第二段视频已实际运行，留有 `.local/focus-popular.png`、`.local/focus-debug-alpha2-second-video.png` 等截图。
- 初版 `:app:assembleRelease` 通过 R8、资源压缩、`lintVitalRelease`、签名校验和 APK 导出（30 分 52 秒，`.local/release-alpha2a.log`）；产物 `app/build/outputs/bilipai/release/BiliPai-Focus-9.1.1-focus.5.apk`，SHA-256 `0c9849876d1a1901560ec358d0242d13950856cfdbb108fb75161821a167e664`，包名 `com.android.purebilibili.focus`，码 388，证书 SHA-256 与旧版同为 `a2f021866ee4e5f8f63df109e3369be3ceefa98fee3485e4d583e4a24f07f6bc`。此包是性能修复前的预备包，不用于发布。
- 同一 API 31 模拟器上，预备 Release 对旧版 `.4` 执行 `adb install -r` 成功；`firstInstallTime` 保持 `2026-06-06 09:55:25`，`versionCode` 从 226 升到 388，冷启动 `1937 ms`，无 Crash/ANR/Room migration 错误。旧数据库 `user_version=4`、`search_history=1`、`blocked_ups=1`；升级后 `user_version=6` 且两张表仍分别有 1 条。新上游增加了面向新老用户的一次性「使用须知」确认，按三项确认后进入首页；Focus 设置页仍能看到推荐／直播／游戏／分区开关关闭、关注／热门开关开启，截图 `.local/focus-release-focus-settings.png`。最终包须再做一轮覆盖升级。
- StrictMode 在 Debug 的启动、详情、评论路径记录主线程同步配置读取，启动阶段 Choreographer 曾报告 129、52、95 帧跳过；正在将评论行偏好读、首页节流配置读与动态页偏好恢复改成异步且共享的状态读取。首次热门视频详情另出现 HTTP 412 验证失败，正在定位具体接口与 UI 错误反馈；此前播放视频的黑屏截图拍在播放结束后，尚不能作为播放故障证据。
- 当前正式包仅含 `arm64-v8a`。本机 API 31 模拟器报告 `x86_64,arm64-v8a`，能够运行该包；API 26 x86_64 镜像仅报告 x86 ABI，而 API 26 ARM64 AVD 在 Windows 主机启动时由 Emulator 36.4.10 明确报 `QEMU2 emulator does not support arm64 CPU architecture`。因此 API 26 正式 ARM64 APK 的设备级验证需实体设备或另一种受支持的运行环境，不能以 API 26 x86 镜像冒充通过。

## API 36 与登录态实测增量

- API 36 模拟器上，预备签名 Release 完成全新安装及使用须知；深色模式首页、`music` 搜索结果与 BVID 精确搜索结果均实际显示。进入两个视频详情时遇到 B 站 HTTP 412 风控，界面显示验证失败状态；这只能证明失败反馈，不能计入播放成功。模拟器在点击扫码登录入口时四次退出，Windows WER 报告宿主 `qemu-system-x86_64.exe` 异常 `0xc0000005`，可见与无窗口模式、两种图形参数均复现；无证据表明是应用进程崩溃。原始诊断留在 `.local/emulator36-*`，API 36 登录态运行验证不能据此通过。
- 新建独立 API 31 Google APIs x86_64 模拟器 `focusLoginApi31`，ARM64 转译可安装同一预备签名 Release；扫码登录页成功呈现，用户于可见模拟器手动扫码。登录后推荐流和动态流加载，强停冷启动后无登录提示、内容仍加载。此模拟器保留登录态，用于最终包覆盖安装后的账号与 Focus 配置回归。
- 在该登录态下，动态页「关注分组过滤设置」实际同步 300 位关注对象。创建 `FocusQA0923`，将一位已关注 UP 从默认组分配到测试组，测试组由 0 变 1；关闭可见性后界面明确显示「1 位关注对象 · 动态与首页隐藏」。强停冷启动后名称、成员数、隐藏状态均保留；取证 `.local/focus-group-preupgrade.png` 与 UI 树。
- 此次实测暴露真正的 Focus 过滤回归：隐藏测试组后，动态侧栏布局仍展示该 UP 的动态正文。源码核对发现 `DynamicScreen.kt` 的 SIDEBAR/DRAWER 分页构造仅执行通用用户筛选，漏调 Focus 分组过滤；横向布局已调用。已补齐侧栏页的配置/开关键与过滤，增加回归断言，最终包尚待设备复测。复现截图 `.local/dynamic-hidden-sidebar-before-fix.png`。
- 另一轮主代码与 AndroidTest 源码均编译通过，全量单测 8,325 项仅 1 项旧平板结构断言失败，已按新增评论偏好 Flow 修正。随后缓存延迟初始化与播放器速度读取修复的首轮全量单测为 8,327 项、3 项失败：两项旧 MiniPlayer 生命周期测试未设置 Main 测试调度器，导致异步初始化在纯 JVM 测试环境抛错；第三项是前两项遗留异常污染的 WatchLater 测试。测试调度器/关闭清理修复后，定向回归通过，最新全量复测为 1,160 个测试类、8,328 项、0 failure、0 error、0 skipped；AndroidTest 源码编译也通过。日志 `.local/verification-alpha2m.log`。

截至后续收口，最终 Debug/Release 构建与 API 31 旧版覆盖升级已有验证，见下节。登录态最终包验证已覆盖登录态、分组状态持久化和隐藏动态过滤；动态侧栏正向展示及 Seek 后播放恢复仅部分验证。API 36 最终包设备回归与旧客户端发现、下载、安装更新仍未验证。

## 2026-09-23 发布收口增量

- 最新完整 `:app:testDebugUnitTest` 执行 1,161 个测试类、8,334 项测试，失败与跳过均为 0；同轮 `:app:lintDebug` 报 0 error、591 warning、2 hint（另有 427 项被既有 baseline 过滤），任务成功，日志 `.local/full-unit-lint-alpha2-final.log`。README 中两处误写的版本码已改为 388。
- 现有源码执行 `:app:clean :app:assembleDebug` 通过，用时 16 分 37 秒；日志 `.local/clean-debug-alpha2-ultimate.log`。后续 Watch Later 顶栏滚动假设经设备证伪后已完整撤回，最终 Debug 与 AndroidTest 包再次构建通过，日志 `.local/debug-androidtest-scroll-timing.log`。
- `:app:connectedDebugAndroidTest` 在 API 31 模拟器运行 63 项，原始结果为 16 failure、3 skip、0 error，日志 `.local/connected-debug-alpha2-ultimate.log`，不能称为全绿。三项跳过因用例要求 API 33。失败含不适用的基准计时断言、Compose 夹具选择器和重复 `setContent` 等；没有从该轮找到应用 Crash/ANR。设备套件的失败需与产品验证分别记录。
- 三类可能影响体验的用例已单独重跑定位：MIUIX 上下滚动原先从顶栏 inset 起手，改从卡片区起手后单列、双列、主题切换 3/3 通过（`.local/isolated-scroll-interior-final.txt`）；懒加载切列期间第四张卡合法离屏，改在动画完成后核对四张卡，3/3 通过（`.local/isolated-grid-final.txt`）；弹幕弹窗在异步请求焦点完成后核对，1/1 通过（`.local/isolated-danmaku-composer-final.txt`）。三项调整都保留原有用户行为断言。
- 三列瀑布流手动锚点测试在原套件及独立复测仍失败；现行生产路径按上游 `a5fb57137` 仅对单列列表启用手动锚点，以避免多列重排跳转，而该旧测试对多列强行启用手动锚点。没有据此修改产品逻辑，也不将失败写为通过；独立日志 `.local/isolated-waterfall-alpha2.txt`。
- GitHub Focus 仓库此前没有 Tag 规则，CI 新版 Release 工作流要求受保护的 `v*` Tag。已创建并远端读回只匹配 `refs/tags/v*` 的 active 规则集 ID `23918149`，限制创建、更新、删除，管理员可执行发布；同步分支已推送，Tag 与 APK 尚未发布。

## 最终签名 Release 与覆盖升级核验

- 最终源码提交为 `09eb220aeb93efcc4e980b410870dc6834e4ada8`，合入上游 `6d5496b44abb832309d762adb991e9d6b2270ec5`。最终签名 Release 构建通过，日志 `.local/release-alpha2-final-signed.log`；产物 `app/build/outputs/bilipai/release/BiliPai-Focus-9.1.1-focus.5.apk`，SHA-256：`0E9528C62CD92411B4DDF6B0A8BB31278FA30A8635890C812EB5D85A4D276291`。包名为 `com.android.purebilibili.focus`，版本名 `9.1.1-focus.5`，版本码 388，`targetSdk=37`，ABI 为 `arm64-v8a`；APK 签名验证通过，证书 SHA-256 `a2f021866ee4e5f8f63df109e3369be3ceefa98fee3485e4d583e4a24f07f6bc` 与旧版 `.4` 一致。
- 使用 API 31 模拟器的旧版快照进行实际 `adb install -r` 覆盖升级：旧版 `.4`／码 226 升至 `.5`／码 388，安装成功，`firstInstallTime` 保持 `2026-06-06 09:55:25`。升级后 Room `user_version=6`；`search_history` 与 `blocked_ups` 各仍有 1 行，旧探针 `FocusUpgradeProbe0923` 和 UID `998877665544` 均存在。Focus 设置仍为 Recommend OFF、Following/Popular ON、Live/Game/Partition OFF。新版冷启动 `am start -W` 为 3637 ms。此证据证明本机签名 APK 的安装器覆盖升级和数据迁移，不代表 GitHub 更新器链路已验证。
- 登录态模拟器 `emulator-5556` 已覆盖安装签名 `.5`。B 站登录态保持有效；`FocusQA0923` 分组、成员分配和隐藏设置在强停冷启动后仍保留。隐藏该分组时，动态侧栏中该 UP 的内容未出现，验证了隐藏过滤；切为可见后分组内能看到该 UP，但侧栏中正向显示其动态内容未能确认，记为部分验证。测试结束已恢复原隐藏状态。
- 同一登录态环境中视频详情可打开并暂停；播放位置从约 3 秒 Seek 到约 20 秒后持续缓冲，未见 ExoPlayer 或 AndroidRuntime 错误。Seek 后恢复播放未验证通过，记为部分验证，不能据此声称播放/Seek 全链路正常。
- 本节收口时，分支 CI run `35959218778` 的 `quality-guards` 成功、`build` 仍在运行。GitHub Tag/Release 尚未发布，因此旧客户端通过 GitHub 检查更新、下载、安装和启动的端到端链路仍为 **待验证**。不能把本机 `adb install -r` 结果写成更新器验收通过。
- 最终测试状态：`:app:testDebugUnitTest` 为 8,334 项通过；`:app:lintDebug` 为 0 error、591 warning、2 hint。最终干净 Debug 与 AndroidTest 构建通过。API 31 `:app:connectedDebugAndroidTest` 共 63 项，16 fail、3 skip；滚动 3/3、网格切换 3/3、弹幕输入 1/1 的独立复测通过。三列瀑布流手动锚点用例仍失败，原因和生产路径边界见前节；其余 instrumentation 失败没有被归并为通过。
