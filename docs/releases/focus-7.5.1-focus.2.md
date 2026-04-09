# BliPai Focus v7.5.1-focus.2

发布日期：2026-04-08

## 更新摘要
- 这一版继续跟进官方 `7.5.1` 版本线。经核对，官方 `7.5.1` tag 与 `7.5.0` 仍指向同一源码提交，所以这次 Focus 仍然不重复 merge upstream 代码，重点是把播放器 Home 真正回首页的问题修到底，并完成补丁发布。

## 主要改动
- 同步 Focus 版本线到 `7.5.1-focus.2 / 162`，README、Focus changelog、release notes 和应用内更新展示版本全部对齐。
- 修复播放器左上角房子按钮：现在会强制回首页根页面，不再只是退回当前页、上一条视频，或因为当前返回栈状态而看起来没有生效。
- 竖屏全屏场景也一并修正，不会再先退出到当前详情页再尝试跳首页。
- `back` 箭头继续保持原有“返回上一层”语义不变；这次只修正 `home` 的导航落点。
- Focus 版继续保持共存包名、Focus-only 更新源、首页 FOLLOW 分组/排序/自动同步、release-only 更新资产选择和首用声明里的 Focus GitHub / Telegram 入口。

## 验证
- 已通过 `:app:testDebugUnitTest --tests "*AppTopLevelNavigationPolicyTest" --tests "*VideoDetailScreenPolicyTest" --tests "*PortraitVideoPagerPolicyTest"`。
- 已通过 `:app:assembleRelease`。
