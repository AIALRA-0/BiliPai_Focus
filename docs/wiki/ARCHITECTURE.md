# 架构说明

最后更新：2026-08-15（按当前工作区与构建配置校对）

## 构建与运行基线

| 项目 | 当前值 |
| --- | --- |
| 应用版本 | 当前构建与最近完整发布记录 `v0.2.3-beta.7` / `versionCode 296` |
| Android | minSdk 26、targetSdk 35、compileSdk 37、arm64-v8a |
| 工具链 | AGP 9.3.1、Gradle 9.5、Kotlin 2.4、JDK 21 |
| Compose | BOM 2026.06.00、Material3 1.5.0-alpha25、Lifecycle 2.11.0 |
| 导航 | Navigation3 runtime/UI 1.2.0-alpha07、NavigationEvent 1.2.0-alpha03 |
| 媒体 | Media3 1.10.1、DASH/HLS、MediaSession、Texture surface 连续返回 |
| 视觉 | Miuix 0.9.4-4f86de92-SNAPSHOT、Haze 2.0.0-alpha03、Miuix Backdrop / Liquid Glass、Compose Cupertino |

## Gradle 模块

```text
app
├── design-system
├── settings-core
├── network-core
└── plugin-sdk

baselineprofile ──(benchmark target)──> app
```

| 模块 | 职责 | 当前边界 |
| --- | --- | --- |
| `app/` | Application、Activity、业务 UI、导航、播放器、Repository、UseCase 与应用测试 | 绝大多数产品逻辑仍在此模块，新增代码优先沿既有 feature/core 分层 |
| `design-system/` | MD3/Miuix/iOS 主题、语义 token、组件 facade、动效、模糊预算与自适应策略 | 不承载网络、业务状态或页面 ViewModel |
| `settings-core/` | 可复用播放速度等设置策略 | 只放跨页面、可独立测试的偏好域逻辑 |
| `network-core/` | 网络 fallback 与首页推荐匿名化策略 | 不依赖 feature UI |
| `plugin-sdk/` | 推荐、播放器、弹幕插件接口与能力 manifest | 作为外部插件稳定边界，不暴露 app 内部实现 |
| `baselineprofile/` | 启动、首页、底部 Pager、设置返回、视频详情与非实时 surface 基准 | 只负责 benchmark/profile，不承载产品代码 |

## App 源码分层

主路径：`app/src/main/java/com/android/purebilibili/`

| 目录 | 职责 | 典型内容 |
| --- | --- | --- |
| `app/` | 应用启动与全局装配 | `PureApplication`、初始化、遥测、进程级 owner |
| `core/` | 跨业务公共能力 | network、store、database、player、plugin、theme、ui、cache、lifecycle |
| `data/` | 数据模型与数据访问 | API/数据库 model、Repository、加载与缓存策略 |
| `domain/` | 可复用业务规则 | UseCase 与不依赖 Compose 的业务决策 |
| `feature/` | 业务场景 | home、video、bangumi、live、dynamic、message、download、settings 等 |
| `navigation/` | 兼容与顶层入口 | legacy route 映射、首页 Pager、链接解析、入口/外观/播放策略 |
| `navigation3/` | 当前页面导航内核 | 61 个 NavKey、返回栈策略、59 个显式 Entry、Scene、预测返回和整卡会话 |
| `androidx/navigationevent/compose/` | 本地 NavigationEvent Compose 兼容层 | 保留完成/取消提交时序与关闭跟手预览能力；需随 NavigationEvent 版本核对 |

## 导航与整卡过渡

1. `AppNavigation` 持有应用级 `List<BiliPaiNavKey>`，业务事件通过 policy 转换成 push、replace 或 pop。
2. `BiliPaiNavEntryProvider` 把 NavKey 解析为 Entry，并注入来源路由、ViewModel owner 和视觉状态。
3. `BiliPaiNavDisplayHost` 使用官方 Navigation3 runtime/UI `1.2.0-alpha07` 生成 `SceneState`，统一普通返回与预测返回。
4. 视频入口创建不可变 `VideoCardTransitionSession`，冻结 bvid、来源 key/route、边界、圆角、方向与封面身份。
5. 整卡几何只由一个 shell/shared bounds 所有；封面、标题、UP 和统计跟随卡片，不创建竞争的独立 bounds。
6. 转场时钟负责 Opening、SettledHidden、BackPreview、Returning、Restoring；详情稳态保留返回会话，但停止无收益的模糊、Miuix Backdrop 和来源重录。

Miuix `0.9.4-4f86de92-SNAPSHOT` 继续用于组件与视觉；当前项目仍不混用其 NavDisplay，
Navigation3 runtime/UI 必须保持官方同版。

## 播放主链路

```text
Feature UI
  -> screen state holder / ViewModel
  -> domain UseCase / playback policy
  -> data Repository
  -> core network + account/auth state
  -> ViewInfo / PlayUrlData / player intent
  -> Media3 player + surface + overlay
```

- 登录态由 Cookie、CSRF 与 access token 共同参与；画质由用户偏好、账号权限、编码能力和 fallback 策略决定。
- 播放器覆盖普通视频、番剧、直播、离线、竖屏 feed 与听视频模式，页面必须明确 player/surface 的 owner。
- 视频整卡返回保留 Texture 实时画面或最后一帧；从未播放时由封面承担转场，不为动画启动无意义播放器帧。
- PiP、后台播放与系统媒体控制通过 MediaSession/通知继续共享播放状态。

## 视觉与自适应

正式 UI 设计合同、三风格边界、组件入口和页面档案见 [UI 设计规范](ui-design/README.md)。本页继续说明技术架构，不重复设计规则。

- `design-system` 提供 MD3、Miuix 与 iOS facade，feature 只消费语义 token 和能力接口。
- Haze、Miuix blur 与 Miuix Backdrop 都受平台能力、运行时视觉预算和转场安全门控约束。
- 手机使用底栏/单栏为主；平板和折叠屏使用 rail、双栏或影院布局。
- 液态玻璃复用遵循 sibling/combined backdrop 拓扑，避免控件采样自身造成黑边或 RenderThread 问题。

## 插件边界

| 形态 | 当前能力 |
| --- | --- |
| 内置插件 | `PureApplication` 注册 10 个实现，可接入推荐、播放器、弹幕与投屏链路 |
| JSON / `.bp` | URL 导入、规则预览、启停与本地执行 |
| 外部 `.bpplugin` | manifest、SHA-256、签名状态、能力声明和包预览；外部 Dex 执行仍未正式开放 |
| 源码示例/皮肤 | `plugins/samples/` 提供源码插件和数据型皮肤包示例 |

外部执行正式化前必须先定义签名信任、能力授权、隔离、版本兼容与失败回滚，不能由 UI 预览状态推导为“可安全执行”。

## 测试与性能

- App policy/structure tests：`app/src/test/`。
- Design system、network、settings、plugin SDK 各模块拥有独立纯 Kotlin 测试。
- `baselineprofile/` 覆盖 Startup、FrameTiming、视频详情、首页、底部 Pager 和设置返回。
- 性能相关改动按“目标测试 → `:app:compileDebugKotlin` → Macrobenchmark/真机”逐级验证。
- 当前 AGP 9 迁移后的 app 单元测试注解解析仍是路线图 P0；恢复前不得把生产编译成功等同于测试全绿。

## 结构维护原则

- screen composable 消费不可变状态与事件 lambda，ViewModel 不向叶子组件传播。
- 可测试决策优先抽成 `Policy`/UseCase；业务 I/O 不放入 Composable。
- 跨 feature 的视觉能力先进入 `design-system`，业务公共能力进入 `core`/独立模块。
- 新模块只在边界稳定且能减少反向依赖时建立，不以模块数量代替架构质量。
- 更完整的目录归属与依赖规则见 `STRUCTURE_GUIDELINES.adoc`。

## 事实入口

- 当前优先级：`docs/wiki/ROADMAP.md`
- 版本与依赖：`app/build.gradle.kts`、根 `build.gradle.kts`、Gradle wrapper
- 发布历史：`CHANGELOG.md`
- 功能状态：`docs/wiki/FEATURE_MATRIX.md`
- 回归标准：`docs/wiki/QA.md`
