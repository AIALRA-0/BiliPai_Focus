# BliPai Focus v7.5.1-focus.3

发布日期：2026-04-08

## 更新摘要
- 这一版继续停留在官方 `7.5.1` 版本线。经核对，官方 `7.5.1` tag 与 `7.5.0` 仍指向同一源码提交，所以这次 `.3` 不引入新的 upstream 代码，主要用于验证 Focus 应用内更新检测与正式发版链路。

## 主要改动
- 同步 Focus 版本线到 `7.5.1-focus.3 / 163`，README、Focus changelog、release notes 和应用内更新展示版本全部对齐。
- 业务代码维持 `v7.5.1-focus.2` 的播放器 Home 返回主页修复不变。
- 继续保持只发布 `release` APK，不附带 `debug` 资产。

## 验证
- 已通过 `:app:assembleRelease`。
