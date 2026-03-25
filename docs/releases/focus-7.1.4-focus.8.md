# BliPai Focus v7.1.4-focus.8

发布日期：2026-03-24

## 更新摘要
- 这一版继续修首页 FOLLOW 的下拉刷新闪烁问题，新的列表现在会等到回弹完全稳定后再切入。

## 主要改动
- FOLLOW 手动下拉刷新现在在“刷新完成、指示器归零、内容位移归零”之后，再额外等待一个稳定窗口，避免回弹边缘提前替换列表。
- 新列表和刷新后的回顶动作一起后移，防止条目先顶上去、内容瞬时跑到下拉刷新边框上方造成 UI 错乱。
- 这次延迟只作用于 FOLLOW 的待提交刷新结果，不影响其他分类，也不改变 `loadMore`、首次首批展示和已有排序逻辑。

## 验证
- 已通过 `:app:testDebugUnitTest --tests "*HomePullRefreshUiPolicyTest" --tests "*HomeFollowFocusPolicyTest" --tests "*HomeFollowFeedMappingPolicyTest" --tests "*FocusFollowGroupStorePolicyTest"`。
- 已通过 `:app:assembleRelease`。
