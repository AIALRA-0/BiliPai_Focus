# Android Native KernelSU Bottom Bar Design

**Date:** 2026-04-04

**Goal**

将 Android Native 预设下的浮动底栏重写为接近 `KernelSU` manager 的实现：保留当前项目的导航入口、图标和文本语义，但在渲染结构上采用 `backdrop + blur/vibrancy/lens + moving capsule` 方案。iOS 与 Android Native 之后各自维护独立参数，仅共享底栏外壳与指示器的胶囊形状策略。

**Current Behavior**

- 当前底栏统一从 [`FrostedBottomBar`](/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt) 进入。
- iOS 分支已经有自绘玻璃链路，包含背景捕获、glass surface、moving indicator 与多套 liquid glass tuning。
- Android Native 分支当前分为两类：
  - `floating` 时主要走 `MiuixFloatingNavigationBar`
  - `docked` 时走 `NavigationBar`
- Android Native `floating` 分支虽然接入了 blur 和容器色映射，但并没有真正采用 `KernelSU` 那种独立 glass shell + indicator lens 的结构；液态参数也仍然和 iOS 分支有较强耦合。

**Reference Model**

这次 Android Native `floating` 底栏直接参考本地克隆的 `KernelSU` manager：

- [`BottomBarMiuix.kt`](/Users/yiyang/Desktop/BiliPai/.external/KernelSU/manager/app/src/main/java/me/weishu/kernelsu/ui/component/bottombar/BottomBarMiuix.kt)
- [`FloatingBottomBar.kt`](/Users/yiyang/Desktop/BiliPai/.external/KernelSU/manager/app/src/main/java/me/weishu/kernelsu/ui/component/FloatingBottomBar.kt)
- [`MainActivity.kt`](/Users/yiyang/Desktop/BiliPai/.external/KernelSU/manager/app/src/main/java/me/weishu/kernelsu/ui/MainActivity.kt)

`KernelSU` 的关键做法不是“系统原生材质”，而是：

1. 页面内容先作为 backdrop source 输出
2. 外层底栏是独立的 continuous capsule
3. glass shell 使用 blur / vibrancy / lens 组合
4. 选中态 capsule 独立渲染并可跟随位置移动
5. 图标文本内容只是放在这个 glass shell 里面

**User Decisions**

- Android Native 和 iOS 分别维护独立参数，不再互相回退。
- 两端唯一共用的视觉策略只有：
  - 底栏外壳 capsule 形状
  - 指示器 capsule 形状
- Android Native `floating` 底栏照抄 `KernelSU` 的结构模型。
- Android Native `docked` 底栏继续保留系统 `NavigationBar`。

**Target Behavior**

- iOS:
  - 保持现有 iOS 玻璃底栏实现与 iOS tuning
- Android Native:
  - `floating` 分支改为 `KernelSU-style` 自绘 glass shell
  - `docked` 分支继续使用 `NavigationBar`
  - 图标/标签/路由逻辑沿用当前项目
  - 参数由 Android Native 自己维护，不读取 iOS liquid glass tuning

**Target UX**

```text
Shared shape only
┌──────────────────────────────────────┐
│   outer bottom bar capsule shell     │
│  ┌────────────────────────────────┐  │
│  │ moving selected indicator      │  │
│  └────────────────────────────────┘  │
└──────────────────────────────────────┘

iOS
└─ current iOS liquid glass renderer + iOS tuning

Android Native (floating)
└─ KernelSU-style glass shell + Android tuning
```

示意：

```text
Android Native floating
┌──────────────────────────────────────┐
│   glass shell (blur/vibrancy/lens)   │
│    ╭──────── moving capsule ───────╮ │
│    │ home  dynamic  history  mine  │ │
│    ╰───────────────────────────────╯ │
└──────────────────────────────────────┘

Android Native docked
┌──────────────────────────────────────┐
│      system NavigationBar row        │
└──────────────────────────────────────┘
```

**Approach Options**

1. Keep existing Android Native Miuix wrapper and only tune colors
   - Smallest diff
   - Does not actually match `KernelSU` structure

2. Rewrite only Android Native floating renderer to match `KernelSU`
   - Recommended
   - Preserves existing iOS branch and docked Android branch
   - Gives the intended visual model without rewriting the whole bottom bar system

3. Fully unify iOS and Android renderers again
   - Highest maintenance risk
   - Conflicts with the confirmed requirement of separate parameter sets

**Chosen Approach**

选择方案 2：

- `FrostedBottomBar` 保留为统一入口
- iOS 分支继续走现有 renderer
- Android Native `floating` 分支替换为 `KernelSU-style` renderer
- Android Native `docked` 分支继续走原生 `NavigationBar`
- capsule shape 提炼为共享 policy，供两端共用
- tuning/spec 拆分为 `iOS` 与 `Android Native` 两套

**Architecture**

```text
FrostedBottomBar
├─ iOS branch
│  ├─ iOS glass renderer
│  └─ iOS tuning
└─ Android Native branch
   ├─ floating
   │  ├─ shared capsule shape policy
   │  ├─ KernelSU bottom bar shell
   │  ├─ Android Native tuning
   │  └─ Android icon/text/nav actions
   └─ docked
      └─ system NavigationBar
```

Android Native `floating` 分层：

```text
KernelSuAlignedBottomBar
├─ backdrop host input
├─ outer glass shell
│  ├─ blur
│  ├─ vibrancy
│  ├─ lens
│  └─ shell surface tint
├─ content row
│  └─ Android nav items
└─ moving selected capsule
   ├─ combined backdrop
   ├─ lens effect
   └─ selection highlight
```

**Parameter Ownership**

- Shared:
  - bottom bar shell shape
  - selected indicator shape

- iOS-owned:
  - existing `LiquidGlassTuning`
  - iOS-specific blur/refraction/alpha/readability rules

- Android Native-owned:
  - shell blur radius
  - vibrancy enablement and intensity
  - shell surface alpha
  - lens radius / lens amount
  - selected capsule tint / shadow / inner shadow
  - tab scale / press response / drag deformation

Android Native 参数来源于 `KernelSU` 参考实现，而不是从 iOS tuning 反推。

**Implementation Notes**

- 在 [`BottomBar.kt`](/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt) 内新增 Android Native 专用 renderer，避免把 iOS tuning 条件判断继续塞进现有 `MaterialBottomBar`。
- `MaterialBottomBar` 将退化为：
  - 一个 `docked` 原生底栏渲染入口
  - 一个 Android Native `floating` 分支的调度入口
- 将 shared capsule shape 提取成显式 policy/helper，避免两边分别写圆角值。
- Android Native 参考 `KernelSU` 的 `FloatingBottomBar` 结构，但要保留本项目现有：
  - `BottomNavItem`
  - `onHomeDoubleTap` / `onDynamicDoubleTap`
  - 平板 sidebar toggle
  - 可见 tab 列表与自定义排序

**Why This Design**

- 满足“安卓照抄 `KernelSU` 实现”的核心要求，但不破坏本项目已有导航逻辑。
- 避免再次把 iOS / Android Native 参数揉成一套，后续调参边界更清晰。
- 保留共享 shape policy，能维持跨预设的底栏骨架一致性。
- 控制改动范围：真正重写的是 Android Native `floating` renderer，而不是整套底栏系统。

**Explicit Non-Goals**

- 本次不重写 iOS 底栏 renderer
- 本次不让 Android Native 继续回退到 iOS tuning
- 本次不重做底栏 tab 数据模型、排序设置、导航路由
- 本次不把 `KernelSU` 整个 Miuix 主题体系搬进本项目

**Files**

- Modify: [`/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt`](/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt)
- Modify: [`/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt`](/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt)
- Modify: [`/Users/yiyang/Desktop/BiliPai/app/src/test/java/com/android/purebilibili/feature/home/components/BottomBarMiuixPolicyTest.kt`](/Users/yiyang/Desktop/BiliPai/app/src/test/java/com/android/purebilibili/feature/home/components/BottomBarMiuixPolicyTest.kt)
- Modify: [`/Users/yiyang/Desktop/BiliPai/app/src/test/java/com/android/purebilibili/feature/home/components/BottomBarMiuixStructureTest.kt`](/Users/yiyang/Desktop/BiliPai/app/src/test/java/com/android/purebilibili/feature/home/components/BottomBarMiuixStructureTest.kt)
- Add if needed: Android Native tuning/spec helper near bottom bar implementation

**Risks**

1. `KernelSU` 的 renderer 使用的 backdrop / lens / highlight 组合与当前项目现有玻璃链路不同。
   - Mitigation: 将 Android Native `floating` 渲染器局部化，不强行替换 iOS 和共享 renderer。

2. 共享 shape 与分离 tuning 容易在代码层重新耦合。
   - Mitigation: 明确拆出 shared shape helper 与 platform-owned tuning helper。

3. Android Native 分支如果继续复用太多现有 iOS helper，会导致“看起来改了、实际上还在共用参数”。
   - Mitigation: 为 Android Native 增加独立命名的 tuning/spec 入口，并在测试里锁住这一点。

4. 当前工作树已经存在其他用户改动。
   - Mitigation: 仅在底栏相关文件内做最小闭环修改，不碰无关改动。

**Verification**

- 策略测试验证 Android Native 与 iOS tuning 已解耦
- 结构测试验证 Android Native `floating` 分支走 `KernelSU-style` renderer
- 手动检查 Android Native `floating` 底栏是否具备外层 shell + moving capsule
- 手动检查 iOS 视觉不被这次改动带偏
