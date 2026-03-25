# BliPai Focus v7.2.0-focus.1

发布日期：2026-03-25

## 更新摘要
- 这一版把 Focus 发布线同步到上游 `BiliPai v7.2.0`，同时继续保留 Focus 的共存、更新源和首页 FOLLOW 定制能力。

## 主要改动
- 并入上游新增的专栏数据模型、专栏详情页面、专栏图片预览/共享转场，以及历史/搜索到专栏的导航策略。
- 并入上游首页卡片、动态卡片、图片预览、WebView、搜索、历史和视频详情相关更新。
- `applicationId` 继续保持 `com.android.purebilibili.focus`，应用内更新继续只跟踪 Focus 仓库的正式 `release` 资产。
- 首页 FOLLOW 的过滤、首批 16 条、随机/时间/聚类排序、随机模式纯随机逻辑和刷新稳定化修复继续保留。

## 验证
- 已通过 `:app:testDebugUnitTest`。
- 已通过 `:app:assembleRelease`。
