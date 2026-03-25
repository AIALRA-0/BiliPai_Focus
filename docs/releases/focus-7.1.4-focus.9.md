# BliPai Focus v7.1.4-focus.9

发布日期：2026-03-24

## 更新摘要
- 这一版继续修 FOLLOW 随机排序下的刷新闪烁，并把随机模式改回真正的整池纯随机。

## 主要改动
- FOLLOW 待提交刷新阶段的回弹现在改成无过冲回弹，并把稳定提交窗口继续拉长，避免随机排序下列表在回弹完成前提前替换。
- `随机排序` 不再做 `UP` 交错，也不再把新抓到的内容强行前置；现在是对整个候选池按随机键做纯随机排序。
- 这次改动不影响默认 `时间倒序`、首批 `16` 条限制和 FOLLOW 的 `loadMore` 追加节奏。

## 验证
- 已通过 `:app:testDebugUnitTest --tests "*HomePullRefreshUiPolicyTest" --tests "*HomeFollowFocusPolicyTest"`。
- 已通过 `:app:assembleRelease`。
