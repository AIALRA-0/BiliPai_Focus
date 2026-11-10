# 返回过渡：开启/关闭背景模糊的正式干净对照

[SQL] 对象为同一安装包、同一卡片。主流程确认背景模糊OFF/ON，采集操作为atrace开始→单次系统返回→1.2秒后停止；期间不执行截图、UI dump或gfxinfo。两份trace均没有screencap线程。开启状态已恢复。

[SQL] phase枚举来自源码：0 Idle、3 Held、4 AutoReturn。OFF窗口1257023544853000→1257023816407000，271.554ms；ON窗口1257192869474000→1257193145345000，275.871ms。直接使用同一trace内phase计数，无跨trace时钟推算。

## 结论

[SQL] 关闭背景模糊后仍有约30ms级RenderThread帧，且准备期仍有约95ms主线程长帧；没有恢复120Hz流畅度。背景模糊不是这次返回卡顿的唯一原因，也不支持将它列为已证实的主瓶颈。

[INFERRED] 证据优先级指向来源首页恢复/布局、播放器Surface相关同步等待、持续CPU端图层渲染工作。应先减少这些开销，而不是只缩短动画或降低模糊强度。

## 正式动画阶段对照

所有操作统计按slice起点位于AutoReturn窗口筛选，sum为整段slice时长；线程Running按窗口交集裁剪。嵌套项不能相加。跨越phase起点的上一帧不计入本表单帧分布，但其CPU工作计入Running。

| 指标 | 模糊关闭OFF | 模糊开启ON |
| --- | ---: | ---: |
| 阶段时长ms | 271.554 | 275.871 |
| doFrame数量 | 8.000 | 8.000 |
| doFrame中位数ms | 24.700 | 27.317 |
| doFrame最大ms | 35.000 | 33.593 |
| RT DrawFrames数量 | 8.000 | 8.000 |
| RT DrawFrames中位数ms | 31.139 | 29.865 |
| RT DrawFrames最大ms | 35.174 | 39.239 |
| flush layers中位数ms | 14.010 | 15.152 |
| flush layers最大ms | 15.169 | 19.689 |
| flush layers累计ms | 110.319 | 122.263 |
| 主线程Running ms | 150.705 | 151.897 |
| RT Running ms | 251.479 | 258.387 |

[SQL] ON最大flush layers=19.689ms，其中Running19.623ms、Runnable0.066ms。这是CPU端真实执行，不能称为单纯等待GPU。正式窗口RT Running占OFF92.6%、ON93.7%，且Runnable很少，不支持CPU抢占是主因。

[GAP] 仅一对正式AB样本，不能将中位数差直接当模糊的精确成本；trace启用了细粒度图形事件，也会增加采集开销。结论是两条路径均存在同级真实负担，不是量化模糊shader成本。没有GPU硬件计数器，也没有可绑定到具体Composable的调用堆栈。

## 准备阶段：恢复页面的长帧

[SQL] OFF最长准备doFrame96.508ms，含Recomposer:recompose55.786ms、AndroidOwner:measureAndLayout28.329ms。ON相应94.725ms，含重组58.305ms、measureAndLayout27.844ms；ON整帧Running86.725ms，测量全程Running27.844ms。长帧在AutoReturn开始前已出现，不能只统计显式动画phase而漏掉触发后的恢复开销。

[SQL] 准备期播放器等待在OFF和ON均复现：

- OFF第二个准备doFrame63.666ms，其Compose:applyChanges26.446ms内主线程Sleeping25.335ms，被ExoPlayer:Playb（utid252）唤醒。
- ON第二个准备doFrame46.101ms，其Compose:applyChanges19.330ms内主线程Sleeping18.386ms，被ExoPlayer:Playb（utid232）唤醒。

[INFERRED] 该等待符合播放器输出Surface同步切换。不能将applyChanges整个时长视为Compose重组计算。需要源码或Java方法trace进一步确认具体clear/set surface调用。

## 旧辅助trace的等待链与干扰边界

[SQL] 旧return.atrace里主线程applyChanges35.394ms包含Sleeping33.980ms，唤醒方为ExoPlayer:Playb（utid196/tid958）。播放线程先Sleeping21.026ms后被ExoPlayer:Place（utid790）唤醒，再Sleeping11.386ms被MediaCodec_loop（utid799）唤醒。Place区间Running13.855ms，有eglGetDisplay、connect、setConsumerName、getConfigForSurface；随后MediaCodec有connect/attachBuffer/disconnect。这条依赖链支持占位Surface初始化与编解码器输出Surface切换为部分根因，但尚无Java堆栈确认具体API。

[SQL] 旧trace的screencap从1256698993377000开始，与RETURNING动画重叠；旧动画段44ms级RT开销受采集干扰，不作为正式AB依据。旧准备阶段108.527ms和63.651ms帧均早于截图，其现象在干净AB被复现。

## 资料

- off-return-clean.atrace / on-return-clean.atrace
- off-return-clean.analysis.json / on-return-clean.analysis.json
- compare_return.py（相同统计逻辑；输入trace路径自动解析phase和主/RT线程）
- log-ab-final.txt（由主流程采集）

未操作手机、未修改源码、未编译。
