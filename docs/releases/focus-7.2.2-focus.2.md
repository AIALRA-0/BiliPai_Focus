# BliPai Focus v7.2.2-focus.2

发布日期：2026-03-28

## 更新摘要
- 这一版不切换上游基线，重点修复关注对象同步必须手动刷新、以及关注名单变化后首页 FOLLOW 不联动的问题。

## 主要改动
- 关注对象缓存统一改成共享快照，动态页、关注列表页和首页现在读写的是同一份数据源。
- 关注对象同步改为默认自动进行：动态页先展示缓存，再按过期状态静默补拉；设置页里的按钮改成“立即重拉”兜底。
- 首页 FOLLOW 已移除私有 `following_mids_*` 一小时缓存，改为直接订阅共享关注快照。
- 当关注名单新增 UP 时，首页 FOLLOW 会重算并纳入新 UP；当关注名单删除 UP 时，首页会立即剔除对应内容并补足当前批次。
- 只是昵称或头像变化时，不会再触发首页 FOLLOW 的无意义整页刷新。

## 验证
- 已通过 `:app:testDebugUnitTest --tests "*DynamicFollowingsRefreshPolicyTest" --tests "*HomeFollowingSyncPolicyTest" --tests "*FollowingBatchSelectionPolicyTest" --tests "*HomeFollowFocusPolicyTest"`。
- 已通过 `:app:assembleRelease`。
