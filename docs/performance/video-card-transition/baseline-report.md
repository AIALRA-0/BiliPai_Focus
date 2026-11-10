# 首页视频卡片过渡性能测试

对象：小米 13（2211133C），已安装 BiliPai 0.6.7（versionCode 529），1080×2400，屏幕模式 120Hz。
日志确认 realtime_blur=true、speed=STANDARD、predictive_style=MIUIX、reduced_motion=false。第一轮同一张首页左上卡片完成三次进入/返回，Perfetto 录制期间没有屏幕录制。

## 结论

开启模糊时存在实际性能卡顿。进入阶段有 74–93ms 的连续呈现间隔；返回阶段多数呈现间隔约 20–35ms。无法解释为单纯的缓动曲线或模糊视觉错觉。

注意：8.3ms 是当前 120Hz 屏幕节拍，不代表应用实际能持续提交 120fps。下表以应用主窗口 SurfaceFlinger FrameTimeline 和过渡日志区间统计；并非播放器视频 Surface 的帧率。

| 方向/轮次 | 窗口帧数 | App Deadline Missed 帧数 | 呈现间隔中位数 ms | 最大呈现间隔 ms |
| --- | ---: | ---: | ---: | ---: |
| OPENING 1 | 9 | 4 | 22.6 | 89.1 |
| RETURNING 1 | 10 | 8 | 27.4 | 33.1 |
| OPENING 2 | 9 | 5 | 22.4 | 93.4 |
| RETURNING 2 | 11 | 4 | 24.6 | 34.7 |
| OPENING 3 | 11 | 5 | 23.4 | 74.2 |
| RETURNING 3 | 10 | 4 | 23.2 | 28.4 |

## 边界与未确认项

- 时间范围由 VideoCardMotion 的 OPENING→HELD、RETURNING→IDLE 定义，使用 trace 内 clock_snapshot 对齐 CLOCK_MONOTONIC 与 BOOTTIME。
- FrameTimeline 中 App Deadline Missed 表明应用帧未按时完成。长呈现间隔可证实真实不流畅，但不能仅靠它判定具体哪段源码耗时。
- 此设备 trace 未取得主线程/RenderThread 调度切片；另有 39 条 FrameTimeline parser error，不能把此样本视为完整无损追踪。gfxinfo 原始记录同样出现较长帧间隔，作为旁证；其 Flags 为 32，未将其汇总作为主要掉帧率。
- 没有完成关闭模糊的 A/B 对照，因此不能断言模糊是唯一瓶颈。未录制可靠的逐帧视觉素材，不能排除曲线、背景接管或模糊量化额外造成视觉分段感。
- 当前源码 blur 半径采用 2px 量化，接近清晰时采用 0.5px；这是潜在视觉分段来源，不是已证实根因。当前源码与安装包一致性未确认。
- 第二轮自动操作未逐步确认页面，发生退到桌面，已停止，该轮不纳入结论。已重新打开应用。

## 后续功能方向

优先减少卡片进入首段和返回过程的逐帧工作，再检查背景模糊与卡片几何变化的同步及视觉接管边界。要将瓶颈进一步归到模糊、页面挂载或渲染提交，需要有效调度切片及关闭模糊对照，不能仅凭量化步长猜测。

未修改源码，未编译，未运行 Gradle 测试。
原始资料位于同目录：blur.perfetto-trace、log-blur.txt、gfx-blur.txt、timeline.csv。


# 第二轮：直接 atrace 与同一卡片模糊开关对照

已完成关闭→恢复开启的同一卡片对照；除背景模糊外未改其他设置。通过设置控件 checked 状态及 VideoCardMotion 的 realtime_blur=false/true 双重核对。最终确认首页可见且背景模糊恢复开启。

正式对照 trace：off-open-clean.atrace / on-open-clean.atrace / off-return-clean.atrace / on-return-clean.atrace。每次仅一次点击或返回，之后等待1.2秒，再停止采集；动画区间内无自动截图、gfxinfo或UI层级查询。

## 对照结果

| 指标 | 模糊关闭 | 模糊开启 |
| --- | ---: | ---: |
| 进入主线程 doFrame 最大 | 76.361ms | 78.677ms |
| 进入 measureAndLayout 最大 | 49.368ms | 50.546ms |
| 进入 RenderThread DrawFrames 中位数（含跨阶段帧） | 25.810ms | 22.951ms |
| 返回准备阶段主线程长帧 | 96.508ms | 94.725ms |
| 返回准备阶段播放器唤醒前主线程睡眠 | 25.335ms | 18.386ms |
| 返回 doFrame 中位数 | 24.700ms | 27.317ms |
| 返回 RenderThread DrawFrames 中位数 | 31.139ms | 29.865ms |
| 返回 flush layers 中位数 | 14.010ms | 15.153ms |

这些是单对样本，不是控制温度、缓存与顺序的统计基准，不能用其计算模糊精确成本或证明开启模糊更快。可靠结论是：关闭背景模糊后仍存在同量级的页面布局、重组和渲染长帧，模糊并非卡顿的唯一或已证实主要根因。

## 已定位的共同负担

1. 进入时的 Compose 布局/重组突发。干净对照中主线程长帧实际CPU运行约62ms，布局约50ms；不是单纯系统抢占。
2. 返回准备阶段首页恢复重组与布局。关闭与开启模糊时分别约96.5ms、94.7ms主帧，明显超过120Hz帧节拍。
3. 返回时播放器Surface交接同步等待。干净对照都出现applyChanges内主线程睡眠，被ExoPlayer:Playb唤醒。辅助旧样本进一步追到ExoPlayer:Place创建/连接图形Surface、MediaCodec更换输出面的等待链。
4. 持续的RenderThread绘图/图层与命令提交CPU负担。干净返回区间RenderThread实际运行占区间约92.6%（关闭）/93.7%（开启）；多帧约30ms。不能把所有flush layers开销都归为模糊GPU计算。

源码定位候选：VideoCard.kt点击中的整卡像素捕获（captureWholeCardReturnBitmap→toImageBitmap）；VideoPlayerSection.kt的PlayerView解绑与onRelease（view.player=null）及VideoOutputRouter中的输出面路由。当前没有函数级调用栈证明具体某一行就是全部耗时来源，需把候选与已验证等待链分开。

## 采集边界修正

- 第一轮FrameTimeline统计经独立复核准确，但按帧起点归段，未包含每段之前开始并跨入该段的一个帧。定位首段要向前扩展。
- 第二轮初始direct.atrace及return.atrace有紧随操作的自动截图，相关动画幅度仅作辅助；正式结论优先使用上面的四份clean trace。返回准备阶段长帧早于截图，后续干净样本也复现。
- 直接atrace弥补了第一份Perfetto缺少ftrace调度的限制；无需更改系统刷新率或启动系统守护进程。
- 尚未完成逐帧视觉节奏审查，因此不能排除模糊量化、曲线与图层接管造成额外分段感；这些无法解释已测到的CPU长帧。

## 建议优先级

先减少详情进入和首页恢复时集中发生的重组/测量，将播放器输出面交接避开过渡关键帧，并减少逐帧重复绘图/图层提交；随后再校准卡片与模糊的视觉同步。保留模糊强度与120Hz模式，不用关闭效果或降刷新率替代解决长帧。

详见 open-analysis.md 与 return-analysis.md。源码未修改，未编译、未运行Gradle测试。
