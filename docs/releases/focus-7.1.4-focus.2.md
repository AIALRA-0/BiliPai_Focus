# BliPai Focus v7.1.4-focus.2

发布日期：2026-03-24

## 更新摘要
- 这个补丁版继续基于上游 `BiliPai v7.1.4`，只针对首页 FOLLOW 的两个时序 bug 做收口：手动下拉刷新过早换列表，以及刷新后底部分页偶发失效。

## 手动刷新不再提前换新列表
- FOLLOW 手动下拉刷新时，新的结果现在会先暂存，直到刷新指示器完全回弹、刷新态真正结束后，才一次性替换当前列表。
- 同时补上了刷新期间的预览保护，避免后台配置刷新或用户信息回补链路在刷新动画还没结束时抢先改写页面内容。

## 刷新后底部分页立即恢复
- FOLLOW 在刷新完成并回顶之后，会重新正确解锁底部分页，不再出现拉到最底下却不继续加载、必须切去别的页再回来的问题。
- 这次把分页判定里的旧状态缓存一起修掉，确保刷新后的最新门闩条件会立刻参与到底部触发判断里。

## 验证
- 已通过 `:app:compileDebugKotlin`。
- 已通过 `HomePullRefreshUiPolicyTest`、`HomeCategoryPagePolicyTest`、`HomeFollowFocusPolicyTest` 与 `HomeFollowFastFeedCoordinatorTest`。
- 已通过 `:app:assembleRelease`。
