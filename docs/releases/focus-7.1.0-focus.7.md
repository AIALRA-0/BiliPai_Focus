# BliPai Focus v7.1.0-focus.7 发布说明

BliPai Focus `v7.1.0-focus.7` 基于上游 `BiliPai v7.1.0`，这一版主要继续微调关注分组顶部操作区，把 `刷新 / 添加` 按钮的视觉高度压回到更贴近搜索框的状态。

## ✨ 重点更新

- 关注分组顶部的 `刷新 / 添加` 按钮高度下调，不再像上一版那样显得过厚。
- 保留与输入框一致的圆角样式和整体布局，不改动已完成的 Focus 过滤、补页和空态修复逻辑。
- README / README_EN / CHANGELOG / Focus Changelog 已同步切到 `focus.7` 版本线与发布入口。

## 🧩 Focus 默认特性

- 首页 `推荐 / 热门 / 直播 / 游戏 / 分区` 默认隐藏
- 搜索页热门搜索默认关闭，但保留搜索建议、搜索发现、搜索结果与搜索历史
- 观看历史支持“一键清空全部历史记录”
- 视频详情页相关推荐默认隐藏，可在 Focus 设置中恢复
- 动态页与首页“关注”支持统一的本地关注分组过滤，并提供总开关

## 📦 版本信息

- Focus Version: `7.1.0-focus.7`
- Upstream Base: `7.1.0`
- VersionCode: `130`
- Tag: `v7.1.0-focus.7`

## 📱 产物命名

- Debug APK: `BliPai-Focus-debug-7.1.0-focus.7-debug.apk`
- Release APK: `BliPai-Focus-release-7.1.0-focus.7.apk`

## 🧪 测试说明

- 当前仅在 `真我 Neo 7` 与 `联想 Y700 2023` 上进行过验证
- 如在其他设备上遇到兼容性或行为问题，请提交 Issue
- Focus 目前尽量与主项目保持同主版本号，暂定只跟进上游大版本，再在同一基线上维护 Focus 子版本
