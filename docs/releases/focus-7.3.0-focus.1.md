# BliPai Focus v7.3.0-focus.1

发布日期：2026-03-31

## 更新摘要
- 这一版切换到上游 `7.3.0` 基线，重点同步播放器恢复链路、seek 会话、SponsorBlock 稳定化与进度条标记，同时保留 Focus 的共存包名、Focus-only 更新与首页 FOLLOW 定制。

## 主要改动
- 并入上游 `7.2.3/7.3.0` 的播放器生命周期协调、用户主动操作跟踪、恢复播放兼容 seek、scrubbing 会话和更完整的调试信息。
- 并入上游 SponsorBlock 片段预过滤/排序/缓存、seek 后稳定重同步、手动跳过按钮和进度条提示模式。
- 插件中心与播放设置页的 SponsorBlock 开关现在统一写回同一份设置源，不再保留双写源。
- 删除两份未被引用的历史 `SponsorBlockUseCase` 包装层，减少同步后继续遗留的重复旧逻辑。
- `baselineprofile` / Macrobenchmark 的目标包名与视频详情启动组件已切到 Focus 共存包，性能门槛可继续直接验证 Focus 发布线。
- Focus 版继续保持共存包名、Focus-only 更新源、首用声明入口，以及首页 FOLLOW 的分组/排序/自动同步链路。

## 验证
- 已通过 `:app:testDebugUnitTest`。
- 已通过 `:app:assembleRelease`。
- 已通过 `:baselineprofile:pixel6Api31BenchmarkAndroidTest`（Home Feed benchmark 仍按仓库现状保持 `@Ignore`，启动与视频详情基准通过）。
