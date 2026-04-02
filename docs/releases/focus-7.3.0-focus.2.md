# BliPai Focus v7.3.0-focus.2

发布日期：2026-04-02

## 更新摘要
- 这一版不切换上游基线，重点修复两个高优先级回归：部分 `UP` 空间页误显示“暂无视频”，以及竖屏视频外跳返回后详情页被压缩。

## 主要改动
- 搜索结果进入 `UP` 空间时，`mid / fans / videos / level / is_senior_member` 等字段改为柔性数值解析，兼容接口把数字以字符串返回的场景。
- 空间投稿视频列表的分页与卡片核心字段也改为柔性解析，避免某些空间页因为字段类型飘忽导致视频结果被误吃成空列表。
- 空间页首次加载投稿视频时，如果命中“默认视频页 + 用户信息正常 + 首屏异常空结果”的可疑场景，会自动刷新一次 `WBI key` 并重试拉取，不再直接把异常当成真正的“暂无视频”。
- 真正没有投稿视频的账号仍显示“暂无视频”；如果只是接口异常，则改为“投稿视频加载失败”，并提供重试入口。
- 从竖屏视频跳到 `UP` 主页、搜索页或其他页面前，会先清掉 inline 竖屏的折叠偏移和视口变换状态；返回视频详情时先复位布局，再恢复竖屏同步，避免页面被压缩或出现黑边异常。
- Focus 版继续保持共存包名、Focus-only 更新源、首页 FOLLOW 分组/排序/自动同步与首用声明入口。

## 验证
- 已通过 `:app:testDebugUnitTest`。
- 已通过 `:app:testDebugUnitTest --tests "*SpaceLoadPolicyTest" --tests "*SpaceSearchSerializationPolicyTest" --tests "*VideoDetailPlayerCollapsePolicyTest" --tests "*PortraitMainPlayerSyncPolicyTest" --tests "*PortraitVideoPagerPolicyTest"`。
- 已通过 `:app:assembleRelease`。
