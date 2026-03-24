# BliPai Focus v7.1.4-focus.4

发布日期：2026-03-24

## 更新摘要
- 这一版继续收口首页 FOLLOW 的刷新时序、首批显示窗口和分页门闩，并新增首页关注排序模式。

## 主要改动
- 手动下拉刷新时，新列表会完整暂存，等下拉指示器和内容回弹都完全归零后才一次性切换，避免刷新结束前内容上窜闪烁。
- 首次进入 FOLLOW、重新打开后的重算、以及分组配置变更后的重新展示，都严格只提交首批 `16` 条。
- FOLLOW 后续分页继续按 `16` 条一组展开，并且只会在观察到真实内容下滚后才允许触发下一批。
- 关注分组设置新增“首页关注排序”，支持 `随机排序`、`UP聚类倒序`、`UP聚类正序`、`时间倒序` 和 `时间正序`，设置仅影响首页 FOLLOW。

## 验证
- 已通过 `:app:compileDebugKotlin`。
- 已通过 `FocusFollowGroupStorePolicyTest`、`HomePullRefreshUiPolicyTest`、`HomeCategoryPagePolicyTest`、`HomeFollowFocusPolicyTest` 与 `HomeFollowFastFeedCoordinatorTest`。
- 已通过 `:app:assembleRelease`。
