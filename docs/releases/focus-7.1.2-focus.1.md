# BliPai Focus v7.1.2-focus.1 发布说明

BliPai Focus `v7.1.2-focus.1` 基于上游 `BiliPai v7.1.2`。这一版先同步上游 `7.1.2` 的播放回归和设置本地化收口，再把 Focus 首页 `title` 开关扩展到完整的 8 项，并补上“全部关闭后”的正确首页空态页。

## ✨ 重点更新

- 合并上游 `v7.1.2` 的播放回归修复、设置页本地化收口、楼中楼回复预览回归测试，以及搜索 / 历史 / 导航相关策略补强。
- Focus 首页 `title` 开关现已覆盖 `推荐 / 关注 / 热门 / 直播 / 追番 / 游戏 / 知识 / 科技` 共 8 项。
- 当首页所有 `title` 都被关闭时，当前会进入专用空态页，并提示前往 `设置 -> 常规 -> Focus` 重新开启任一首页栏目，不再强制回退到 `关注`。
- 首页顶部最终显示顺序仍先遵循原有 `HomeTopTabSettings`，再叠加 Focus 可见性过滤，尽量维持最小修改。
- 本轮未继续顺带优化其他性能分叉，只完成审计列单，供后续单独决策。

## 🧩 Focus 默认特性

- 首页 `推荐 / 热门 / 直播 / 游戏 / 分区` 默认隐藏
- 首页 `关注 / 追番 / 知识 / 科技` 默认显示，但都可在 Focus 设置中单独关闭
- 搜索页热门搜索默认关闭，但保留搜索建议、搜索发现、搜索结果与搜索历史
- 观看历史支持“一键清空全部历史记录”
- 视频详情页相关推荐默认隐藏，可在 Focus 设置中恢复
- 动态页与首页“关注”支持统一的本地关注分组过滤，并提供总开关

## 📦 版本信息

- Focus Version: `7.1.2-focus.1`
- Upstream Base: `7.1.2`
- VersionCode: `138`
- Tag: `v7.1.2-focus.1`

## 📱 产物命名

- Debug APK: `BliPai-Focus-debug-7.1.2-focus.1-debug.apk`
- Release APK: `BliPai-Focus-release-7.1.2-focus.1.apk`

## 🧪 测试说明

- 本轮已通过 `:app:testDebugUnitTest`、`:app:lintDebug`、`:app:assembleDebug`、`:app:assembleRelease`
- 当前仅在 `真我 Neo 7` 与 `联想 Y700 2023` 上进行过 Focus 相关实机验证
- 如在其他设备上遇到兼容性或行为问题，请提交 Issue
- Focus 目前尽量与主项目保持同主版本号，暂定只跟进上游大版本，再在同一基线上维护 Focus 子版本
