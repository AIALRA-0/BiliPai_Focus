# BliPai Focus v7.1.4-focus.7

发布日期：2026-03-24

## 更新摘要
- 这一版重新调整首页 FOLLOW 的随机排序语义，并把应用默认排序从 `随机排序` 改成 `时间倒序`。

## 主要改动
- `随机排序` 恢复成整池随机推送，刷新时会对整个可见池做随机交错，不再近似等于 `时间倒序`。
- 新安装和未设置用户的首页 FOLLOW 默认排序已改为 `时间倒序`，默认体验变成“最新内容优先显示”。
- `时间倒序/正序` 与 `UP聚类倒序/正序` 继续沿用 `focus.6` 修复后的真实发布时间排序基础。

## 验证
- 已通过 `:app:testDebugUnitTest --tests "*HomeFollowFocusPolicyTest" --tests "*HomeFollowFeedMappingPolicyTest" --tests "*FocusFollowGroupStorePolicyTest"`。
- 已通过 `:app:assembleRelease`。
