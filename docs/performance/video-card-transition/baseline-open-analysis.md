# 首页同卡片开启/关闭背景模糊：clean A/B

本节优先于下方早期探索样本。样本由主代理在同一卡片上依次录制：关闭背景模糊一次、恢复开启后一次；过渡中没有 adb screenshot、UI dump 或屏幕录制。应用内部卡片像素捕获仍属于正常点击路径，并未禁用。

## 有证据支持的结论

**真实性能卡顿在背景模糊开启和关闭时都存在。此对照不支持将模糊认定为主要瓶颈。** 两个样本均重复出现约50ms布局、约63ms主线程运行时间的长帧；RenderThread绘图持续超过20ms。开启样本的RT中位数甚至低于关闭样本，因此不能声称关闭模糊可以解决卡顿。

模糊可能增加图层工作：开启样本的flush layers中位数稍高，但整体RT并未同步恶化。此结果只能作为可能的局部开销线索，不能当成精确因果增量。

## 精简对照表

计量范围由应用phase counter的OPENING→HELD给定。各类别按与区间相交的完整slice统计；跨边界帧计入。下列类别存在父子关系，不能将doFrame、layout、recompose相加当总CPU时间。

| 指标 | 背景模糊关闭 | 背景模糊开启 |
| --- | ---: | ---: |
| OPENING区间 |369.854ms|399.435ms|
| 主线程doFrame数量 |10|12|
| doFrame中位/最大 |19.972 /76.361ms|16.870 /78.677ms|
| 最长doFrame实际Running |62.297ms|62.616ms|
| 布局measureAndLayout最大 |49.368ms|50.546ms|
| 最长布局实际Running |38.885ms|38.237ms|
| RenderThread DrawFrames数量（含跨边界） |11|13|
| RT DrawFrames中位/最大 |25.810 /30.161ms|22.951 /29.118ms|
| 最长RT帧实际Running |30.161ms|28.815ms|
| flush layers中位/最大 |9.987 /13.736ms|11.128 /15.337ms|
| 前150ms内相交主帧最大 |99.905ms|107.393ms|
| 前150ms内相交Recomposer重组最大 |58.818ms|62.585ms|

前150ms统计含起点早于该窗口、但与窗口重叠的完整slice，不能将其完整时长解释为窗口内占用。窗口内主线程doFrame实际重叠总时长分别124.524和126.761ms。

## 具体帧和时间戳

所有以下ts为trace中的BOOTTIME纳秒，直接使用同trace phase counter，无需套用旧trace时钟偏移。

- OFF：OPENING1256978325879000；HELD1256978695733000；UI UTID20、RenderThread UTID705，PID19152。
- OFF最长主帧：Choreographer#doFrame222594777，slice36474，ts1256978573206000，76.361ms；Running62.297ms、Sleeping12.499ms、Runnable1.565ms。
- OFF最长布局：slice37213，ts1256978585789000，49.368ms；Running38.885ms、Sleeping9.336ms、Runnable1.147ms。
- OFF最长RT帧：DrawFrames222594777，slice37676，ts1256978646269000，30.161ms，全部Running。
- ON：OPENING1257173058571000；HELD1257173458006000；UI UTID56、RenderThread UTID170，PID19152。
- ON最长主帧：Choreographer#doFrame222677425，slice21917，ts1257173332232000，78.677ms；Running62.616ms、Sleeping13.318ms、Runnable2.743ms。
- ON最长布局：slice22703，ts1257173344636000，50.546ms；Running38.237ms、Sleeping10.074ms、Runnable2.235ms。
- ON最长RT帧：DrawFrames222677299，slice5954，ts1257173107323000，29.118ms；Running28.815ms、Runnable0.303ms。

## 重复出现的布局工作

最长布局的直接子slice模式完全一致：两者均有11次onRemembered、20次Compose recompose、20次applyChanges、29次简单文本measure、18次富文本measure、15次TextLayout init。对应耗时：onRemembered12.558→14.993ms；局部recompose9.108→9.219ms；简单文本measure5.820→5.427ms；applyChanges2.874→2.832ms。

这些计数表明共同的布局/子组合工作仍然存在，不能解释成单纯模糊半径量化造成的视觉错觉。trace没有Composable函数名，不能从这些标记直接断言具体哪段页面组件负责。

## 点击前置快照

- OFF：onTouch29.316ms，开始于OPENING前222.044ms；RT内部515×509 ImageReader的DrawFrames -1为3.728ms。
- ON：onTouch41.838ms，开始于OPENING前243.042ms；同尺寸ImageReader的DrawFrames -1为10.973ms。
- 该快照不是adb屏幕截图，属于应用自身点击流程。两样本都存在；差异受缓存、分配、运行顺序等影响，不能据此把截图耗时变化归到模糊开关。
- 源码路径及关联边界见下方早期报告：VideoCard.kt:1200–1208→captureWholeCardReturnBitmap→layer.toImageBitmap。trace没有函数级marker，该映射为源码与离屏事件的高度匹配，非直接callstack证明。

## 结论边界与下一步

本次每种状态只有一个样本，按关闭→开启顺序执行，非随机交叉重复。图片/字体/图层缓存、CPU频率、温度、异步详情数据和后台负载未控制，开场phase区间长度和帧数也不同，因此**不能把两列相减当作模糊的精确增量**。

直接atrace没有FrameTimeline，不提供此两次的实际呈现掉帧率或视频Surface帧率；它确证的是线程工作时长和CPU/等待状态。主线程Sleeping内部缺少waker/符号，未终结这些等待链；但主要Running耗时已足以证实存在CPU工作瓶颈。此处不将未知等待归因给GPU。

优化优先级应为：识别过渡期间重复的布局/子组合挂载与remember工作；降低持续的图层绘制/命令准备开销；再单独研究点击像素捕获的等待。视觉接管与模糊步进是否额外导致跳动，仍需逐帧视觉材料，不能由本次CPU数据单独判定。

原始机器统计：off-open-clean.summary.json、on-open-clean.summary.json、ab-cpu-states.json、ab-layout-children.json；统计脚本analyze_clean.py。未操作手机、未修改源码、未编译。

---

# 早期探索样本（保留证据，非正式A/B）

# Direct atrace verified evidence

Target process UPID=8, PID=19152, UI UTID=11, RenderThread UTID=730 (TID=20614).

- bili.video_card.phase counter: OPENING=1256645717525000 ns, HELD=1256646023663000 ns. Interval 306.138ms.
- Choreographer#doFrame 222458657: slice 39802, ts=1256645912354000, dur=72.271ms. Thread states: Running=54.937ms; S=12.791ms; D=3.610ms; R+=0.493ms; R=0.440ms.
- AndroidOwner:measureAndLayout: slice 40785, ts=1256645923341000, dur=48.631ms. Running=34.532ms, S=9.628ms, D=3.610ms, R+=0.493ms, R=0.368ms.
- Direct children of slice 40785: Compose:onRemembered 11 calls/12.467ms; Compose:recompose 20 calls/8.667ms; TextAnnotatedStringNode:measure 18 calls/6.065ms; TextStringSimpleNode::measure 29 calls/4.997ms; Compose:applyChanges 20 calls/2.863ms.
- UI D state ts=1256645931754000, dur=3.610ms, blocked_function=lock_page_maybe_drop_mmap+0x138/0x20c; it overlaps TextAnnotatedStringNode:measure slice 40853 (ts=1256645931450000, dur=4.255ms).
- UI sleeping intervals inside onRemembered have no waker_utid or more detailed trace slice identifying their blockers.
- RenderThread DrawFrames 222458540: slice 26784, ts=1256645713636000, dur=39.132ms; Running=32.073ms, D=5.829ms, R=1.166ms, R+=0.064ms.
- Its flush layers slice 26884: 24.000ms. Direct children: OpsTask::onExecute 209 calls/7.241ms; OpsTask::onPrepare 209 calls/2.305ms; allocateImageMemory 17 calls/2.478ms; QueueSubmit 3 calls/8.226ms. D states include gmu_core_timed_poll_check, gen7_hfi_send_cmd_wait_inline, adreno_hwsched_wait_ack_completion.
- Sustained RenderThread DrawFrames slices: 28831 23.586ms (Running23.523); 30494 20.722ms (Running20.581); 32061 18.368ms (Running18.368); 33703 18.963ms (Running18.963); 35541 19.789ms (Running19.590); 37481 25.788ms (Running25.788); 39136 18.537ms (Running18.537); 41552 23.428ms (Running23.378); 43407 24.660ms (Running24.638).
- No blur/filter/shader/pipeline-named slices found on RenderThread overlapping OPENING.
- Before OPENING, AndroidOwner:onTouch slice 23345 at1256645497636000 lasts36.552ms (Running11.158, Sleeping24.334,Runnable1.060). Nested UI acquisition and RenderThread 515x509 ImageReader frame are recorded; no source-level function symbol identifies screenshot owner.
- Before OPENING, Recomposer:recompose slice23482 lasts41.947ms (Running41.718ms).

Prior trace /tmp/bilipai-card-perf/blur.perfetto-trace has no thread_state records. It has frame_timeline_event_parser_errors=39 (info), traced_chunks_discarded=15 (info). Two clock snapshots differ in BOOTTIME-MONOTONIC offset by104ns. Recomputed original six rows match report.md. Each original phase interval has one overlapping frame that started before its phase log and was excluded by frame-start-based grouping.

# Interpretation and source correlation

[VERIFIED TRACE] Actual long work is present in UI and RenderThread. The 72.271ms UI frame is dominated by layout/recomposition/remember hooks (not runnable scheduling starvation). Nine RT frames take18.37–25.79ms and are nearly entirely Running; CPU-side layer drawing / GPU-command preparation is a sustained bottleneck. These values exceed both8.33ms and16.67ms cadence budgets.

[VERIFIED TRACE] Touch input path contains offscreen515×509 ImageReader rendering, with RT DrawFrames -1 lasting14.424ms, Drawing11.942ms, Vulkan finish frame10.853ms. UI touch callback36.552ms includes24.334ms Sleeping. It creates/configures a surface, starts RT work, acquires a buffer and releases/disconnects it. This demonstrates offscreen pixel capture work at click time.

[VERIFIED SOURCE] Current VideoCard.kt lines1200–1208 freezes source layers then launches captureWholeCardReturnBitmap with CoroutineStart.UNDISPATCHED, before onClick(video.bvid, video.cid) at1212. VideoCardNativeSnapshot.kt lines257–263 invokes layer.toImageBitmap() then records the bitmap. CoroutineStart.UNDISPATCHED begins executing on the caller until the first suspension; it does not prove entire bitmap work blocks synchronously.

[INFERRED] Trace offscreen515×509 work likely corresponds to this whole-card bitmap capture path. Its temporal placement inside AndroidOwner:onTouch and card-like dimensions match the current source. However no named trace marker/callstack directly associates the ImageReader with captureWholeCardReturnBitmap, and installed-package/source equality has not been checked. Therefore the 36.552ms callback must not be attributed100% to that function.

[GAP] onRemembered sleeps have no waking-thread data or named lock/binder operation. Text measure D-state is a page/mmap kernel wait; owning file/page origin is unrecorded. RT initial D-state is graphics-driver GMU/HFI/ack work, but no GPU hardware timeline is available. No blur-labeled slices means the contribution of blur cannot be separated from other graph/layer work using this trace alone. A/B is required to show incremental blur cost.

[GAP] Direct atrace does not include FrameTimeline classification. It verifies CPU/work timings rather than exact presentation/jank counts. Its BOOTTIME timestamps are anchored directly by application phase counters; use these rather than an assumed monotonic offset from the earlier trace.

# Queries used (validated against returned schemas)

```sql
SELECT c.ts,c.value,ct.name
FROM counter c JOIN counter_track ct ON c.track_id=ct.id
WHERE ct.name GLOB '*video_card.phase*';

SELECT s.id,s.name,st.state,
 round(sum(min(st.ts+st.dur,s.ts+s.dur)-max(st.ts,s.ts))/1e6,3) ms
FROM slice s
JOIN thread_track tt ON s.track_id=tt.id
JOIN thread_state st ON st.utid=tt.utid
 AND st.ts<s.ts+s.dur AND st.ts+st.dur>s.ts
WHERE s.id IN (40785,39802,26784,26884,23482,23345)
GROUP BY s.id,st.state ORDER BY s.id,ms DESC;

WITH RECURSIVE d AS (
 SELECT id,ts,dur,name,parent_id,depth FROM slice WHERE id=40785
 UNION ALL
 SELECT s.id,s.ts,s.dur,s.name,s.parent_id,s.depth
 FROM slice s JOIN d ON s.parent_id=d.id
)
SELECT d.id,d.ts,d.dur/1e6 ms,d.name,d.parent_id,d.depth
FROM d WHERE d.dur>500000 ORDER BY d.ts;
```

All inspected slices/thread-state intervals had positive complete durations. No phone UI operations or code mutations were performed during this analysis.
