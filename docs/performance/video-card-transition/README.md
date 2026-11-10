# 视频卡片过渡性能排查记录

记录日期：2026-10-29（Asia/Shanghai）。重点为首页视频卡片进入与返回，优先检查背景模糊开启路径。

## 当前状态

- 已对修改前的已安装版本采集并分析性能数据。
- 已完成本轮源码修改与静态调用链、Git 差异检查，并补充回归用例。
- 未编译、未运行 Gradle 测试或新增用例，未采集修改后的性能数据。**不能据此宣布掉帧已解决或给出提速百分比。**
- 测量时手机最终已恢复首页，转场背景模糊恢复开启。源码修改未部署到该安装包。

## 先看哪些资料

| 资料 | 用途 |
| --- | --- |
| [修改前测量总报告](baseline-report.md) | 结论、两轮采集、证据边界 |
| [进入阶段分析](baseline-open-analysis.md) | 主线程布局、重组、点击像素捕获、渲染线程证据 |
| [返回阶段分析](baseline-return-analysis.md) | 首页恢复长帧、播放器输出面等待、模糊开关对照 |
| [性能与开合动效复查](motion-review-2026-10-29.md) | 基线一致性、自动动画与手势接管、圆角和交接窗口的优化优先级 |
| [功能更新记录](../../../CHANGELOG.md) | 本轮对用户可见的优化及修复 |

上面三个 `baseline-` 文件为当时报告副本，保留了历史 `/tmp/bilipai-card-perf` 路径。后续优先使用下面的持久归档。

原始 trace、日志、统计 JSON、SQL 查询输出及分析脚本归档在：

`/Users/yiyang/Desktop/BiliPai-performance/2026-10-29-video-card-transition/`

其中 `manifest.json` 记录每个归档文件的大小与 SHA-256。大型 trace 放在仓库外，避免提交到 Git。

| 文件 | 用途及限制 |
| --- | --- |
| `blur.perfetto-trace`、`log-blur.txt`、`timeline.csv` | 第一轮三次进入/返回；含 FrameTimeline，缺 ftrace 调度切片 |
| `off-open-clean.atrace`、`on-open-clean.atrace` | 同一卡片关闭/开启背景模糊的进入对照 |
| `off-return-clean.atrace`、`on-return-clean.atrace` | 同一卡片关闭/开启背景模糊的返回对照 |
| `log-ab-final.txt` | 确认 `realtime_blur=false/true`、过渡配置与相位 |
| `analyze_clean.py`、`compare_return.py` | 历史单次采集脚本；执行器路径、PID/TID 写死，复用前必须适配新 trace，不能直接作为通用分析器 |
| `direct.atrace`、`return.atrace` | 辅助探索样本，部分动画区间有自动截图干扰，不能代替 clean 对照 |

## 修改前确认的事实

设备为小米 13，1080×2400，屏幕模式 120Hz；安装包 BiliPai 0.6.7，versionCode 529。屏幕 8.3ms 节拍不等于应用实际提交或呈现 120fps。

第一轮开启模糊的三次进入，最长相邻呈现间隔约 89、93、74ms；系统记录了 App Deadline Missed，说明存在真实性能卡顿。

干净模糊开关对照仅各一个样本，按关闭→开启顺序，未控制缓存、温度和后台负载。以下时长不能简单相减当作模糊精确成本。

| 指标 | 模糊关闭 | 模糊开启 |
| --- | ---: | ---: |
| 进入最长主线程 doFrame | 76.361ms | 78.677ms |
| 进入最长 measureAndLayout | 49.368ms | 50.546ms |
| 返回准备阶段最长主帧 | 96.508ms | 94.725ms |
| 返回 RenderThread 帧中位数 | 31.139ms | 29.865ms |
| 返回准备期播放器相关主线程睡眠 | 25.335ms | 18.386ms |

关闭背景模糊仍有同量级重组、布局和绘图负担。本次证据不支持把背景模糊认定为主要瓶颈。Surface 等待链支持播放器输出面交接为部分原因，但缺少 Java 方法调用栈，不能把所有等待归到某一个源码行。视觉分段感也可能叠加，尚未完成可靠逐帧视觉复核。

## 本轮源码改动索引

以下是实现意图和源码位置，不是修改后实测结果。定位优先使用符号名，避免行号随后续修改失效。

| 方向 | 文件与符号 | 行为 |
| --- | --- | --- |
| 降低加载许可失效范围 | `app/src/main/java/com/android/purebilibili/core/ui/components/PageAwareAsyncImage.kt`：`LocalPageImageLoadingAllowed` | 改用动态 CompositionLocal，只使读取许可的节点失效 |
| 首页恢复门控 | `app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt`：HOME 调用的 `isTopLevelActive` | 沿用底部 Pager 的过渡空闲许可，再恢复同步、锚点和业务更新 |
| 详情首次挂载 | `app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailTransitionPolicy.kt`：`shouldShowVideoDetailContent` | 显式延后首次重正文，以现有静态骨架承接；返回预览与完成锁存保留 |
| 详情门控接线 | 同目录 `VideoDetailPhoneContent.kt`、`VideoDetailScreenStateHolder.kt`：`deferFreshEntryContent` | 玻璃与原生路径共用挂载许可，不改材质布局参数 |
| 前台 Surface 延续 | `app/src/main/java/com/android/purebilibili/feature/video/ui/section/VideoPlayerSection.kt`、`VideoPlayerSectionPolicy.kt` | 退场 entry 已降到 CREATED 时，仅延续真实窗口仍 STARTED 且已绑定的内联输出；竖屏接管不豁免 |
| 退场准备信号 | `miuix-navigation/src/main/java/top/yukonga/miuix/kmp/nav/transition/NavTransitionScope.kt`、`core/LiveNavTransitionScope.kt`：`isRemoving` | 栈移除即可观察，包含补间尚未启动的准备帧 |
| 模糊背景分层 | `app/src/main/java/com/android/purebilibili/core/ui/transition/VideoCardTransitionBackgroundPolicy.kt`：`VideoCardTransitionSnapshotHandle.recordSource`、`releaseSession` | 内层来源离屏缓存、外层动态景深；清空两层引用 |
| 旧空闲回调保护 | `app/src/main/java/com/android/purebilibili/navigation3/BiliPaiNavDisplayHost.kt`：`LaunchedEffect(effectiveVideoCardExposure)` | 清理前复查当前 exposure 与 clock，避免新会话被旧回调清空 |
| 延后整卡捕获 | `app/src/main/java/com/android/purebilibili/core/ui/transition/VideoCardDeferredPixelCapture.kt` | 等入场落位和完整绘制帧；返回、换卡、失焦、后台、来源销毁撤销任务；不复活旧任务 |
| 捕获接线 | 同目录 `VideoCardNativeSnapshot.kt`，以及 `feature/home/components/cards/VideoCard.kt` | 点击只冻结既有图层；发布前检查协程取消与当次点击身份 |
| 嵌套归属 | `app/src/main/java/com/android/purebilibili/core/util/CardPositionManager.kt`：`isActiveVideoCardPixelOwner` | 同时检查点击对象与活跃来源；恢复父元数据不重标记子像素 |
| 返回末段合成 | `app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailWholeCardReturn.kt`：`videoDetailWholeCardReturnLayer` | 快照直接绘制 alpha；实时组用图层复用绘制指令；无位图不增加图层 |

## 交接检查表

所有以下场景目前仅核对源码与新增用例，未运行确认。材质至少覆盖液态玻璃开启、关闭后的 MD3 与 Miuix；布局至少覆盖手机、大屏及窄分屏。

| 场景 | 必须保持的行为 | 重点定位 |
| --- | --- | --- |
| 普通进入→停留→返回 | 模糊仍按原进度更新，落点不变，正文由骨架承接，返回结束释放缓存 | 首次正文门控、背景双层缓存、Surface 延续 |
| 入场未结束立即返回 | 不启动补充位图读回，封面与文字 fallback 仍能落位，无空槽 | `captureVideoCardPixelsAfterEntry`、现有来源封面/文字交接 |
| 同卡重复点击、连续换卡 | 旧任务不能接收新点击身份，清理不能清掉新会话 | `VideoCardPixelOwner`、宿主 Idle 清理保护 |
| 预测返回后取消 | 取消已撤销的补抓不复活，实时正文恢复最新内容，冻结背景不变黑 | 捕获撤销监听、实时组图层、背景缓存 |
| 首页→A→B→A→首页 | 子页迟到像素不污染父页，父页已保留位图不被重新标记 | 活跃来源校验、`rememberVideoDetailWholeCardReturn` |
| 等待或读回时切后台/失焦→恢复 | 旧任务结束且不重启，播放器按原后台规则处理 | 窗口许可、readback 后 `ensureActive` |
| 返回中进入竖屏/PiP或切后台 | 不抢占竖屏输出，不因退场延续规则永久保活输出 | `shouldBindInlinePlayerViewToPlayer`、真实窗口生命周期 |
| 内容更新、旋转/改尺寸 | 缓存不得跨错误身份或错误视口复用，最新内容能恢复 | 来源几何失效、缓存刷新和会话释放 |
| 减少动态效果、模糊关闭、旧系统 | 不启动无用模糊；非宿主回空闲也释放新增内层 | `retainSnapshot`、`releaseSession` |

## 回归用例位置

- `app/src/test/java/com/android/purebilibili/core/ui/transition/VideoCardDeferredPixelCaptureTest.kt`：落位等待、快速返回、取消、读回中返回、恢复父来源、后台/失焦及销毁。
- `app/src/test/java/com/android/purebilibili/feature/video/screen/VideoDetailReturnCoverPolicyTest.kt`：首次正文门控及已加载返回预览。
- `app/src/test/java/com/android/purebilibili/feature/video/ui/section/VideoPlayerSectionPolicyTest.kt`：前台退场保留输出、后台和竖屏边界。
- `miuix-navigation/src/test/java/top/yukonga/miuix/kmp/nav/core/LiveNavTransitionScopeTest.kt`：补间前可读取 `isRemoving`。
- `app/src/androidTest/java/com/Android/purebilibili/feature/home/HomeCardTransitionDrawCacheUiTest.kt`：背景复用、实时交接、取消及下一会话。
- `app/src/androidTest/java/com/Android/purebilibili/feature/video/VideoDetailWholeCardReturnUiRegressionTest.kt`：透明边缘混合对照、绘制次数、最新内容恢复、嵌套像素身份。

## 仍需关注的边界

1. 新增背景离屏层会增加显存和一次首次栅格化成本；缓存是否降低总体 RenderThread 工作尚未实测，不可只凭源码断言更快。1080×2400 的一个 RGBA 面约 9.9MiB，实际分配、格式、驱动缓存与并行会话可能增加成本。
2. 延后捕获依赖来源 scope 及图层仍存活；来源卸载就取消，快速返回可能没有整卡位图，必须检查既有封面/文字 fallback 的视觉连续性。
3. 正文延后降低入场竞争，但可能把重工作移到落位后；需要观察落位后的首个内容帧，而不只统计 OPENING。
4. Surface 延续仅改变退场时机；输出解绑最终仍有成本，后续排查需把动画结束与释放区间一起纳入，不能把耗时移出统计窗口后称为消失。
5. 所有新增绘制缓存都没有新增无限动画或周期帧循环；延后捕获仅等待两个帧边界，并随任务结束或取消清理监听。

## 后续采集沿用的口径

先确认当前页面、安装包版本和模糊开关；每次只执行一次进入或返回，过渡期间不截图、不 dump UI、不调用 gfxinfo。操作结束后再检查页面，避免连续盲发返回键退到桌面。

优先保留 FrameTimeline、主线程与 RenderThread 调度切片、应用相位计数及 `VideoCardMotion` 日志。以同 trace 相位计数定位，纳入点击到 OPENING 前的准备区间、跨边界长帧和落位后的释放/内容挂载区间。分别统计呈现间隔、线程 Running/Runnable/阻塞、重组布局和图层提交，不能混同屏幕刷新率、应用刷新率请求与实际呈现帧。

若进行对照，记录同一卡片、相同材质、窗口尺寸和设置，交叉重复并关注缓存/温度影响。旧安装包的数据只能作为基线，不能当作本轮源码优化后的表现。
