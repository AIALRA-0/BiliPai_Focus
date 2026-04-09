# BliPai Focus v7.5.1-focus.1

发布日期：2026-04-08

## 更新摘要
- 这一版跟进官方 `7.5.1` 版本线。经核对，官方 `7.5.1` tag 与 `7.5.0` 指向同一源码提交，所以这次 Focus 不重复 merge upstream 代码，重点是同步版本线、文档与发布渠道，并带上本地播放器 Home 返回主页修复。

## 主要改动
- 同步 Focus 版本线到 `7.5.1-focus.1 / 161`，README、Focus changelog、release notes 和应用内更新展示版本全部对齐。
- 修复播放器左上角房子按钮：普通视频详情页、竖屏全屏和平板布局现在都会统一直接回首页，不再回到上一条视频或偶发失效。
- `back` 箭头继续保持原有“返回上一层”语义不变；这次只修正 `home` 的导航落点。
- Focus 版继续保持共存包名、Focus-only 更新源、首页 FOLLOW 分组/排序/自动同步、release-only 更新资产选择和首用声明里的 Focus GitHub / Telegram 入口。
- 已再次审计 `bli` 仓库，未发现跨项目残留或意外混入的外部文件。

## 验证
- 已通过 `:app:testDebugUnitTest`。
- 已通过播放器 Home 导航链相关定向单测。
- 已通过 `:app:assembleRelease`。
