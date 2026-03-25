# BliPai Focus v7.1.4-focus.6

发布日期：2026-03-24

## 更新摘要
- 这一版修正首页 FOLLOW 的排序根因，确保时间排序、聚类排序和随机排序都基于真实发布时间工作。

## 主要改动
- FOLLOW 动态映射成 `VideoItem` 时现在会保留作者模块里的 `pub_ts`，不再丢失发布时间。
- `时间倒序/时间正序` 与 `UP聚类倒序/UP聚类正序` 现在会真正区分“从晚到早”和“从早到晚”。
- `随机排序` 继续保留随机性，但整体发布时间顺序现在固定为“最新在上、最旧在下”；随机只影响同一时间层内的顺序。
- `loadMore` 统一沿用当前排序结果追加，避免把未排序原始顺序直接拼到列表尾部。

## 验证
- 已通过 `:app:testDebugUnitTest --tests "*HomeFollowFocusPolicyTest" --tests "*HomeFollowFeedMappingPolicyTest"`。
- 已通过 `:app:assembleRelease`。
