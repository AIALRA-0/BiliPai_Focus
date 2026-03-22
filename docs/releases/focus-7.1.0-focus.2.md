# BliPai Focus v7.1.0-focus.2 发布说明

BliPai Focus `v7.1.0-focus.2` 已发布，基于上游 `BiliPai v7.1.0`，继续围绕 Focus 使用场景收口默认入口、关注分组和动态页关注加载体验。

## ✨ 重点更新

- 关注分组管理继续收口为单层可展开卡片：分组可见性、重命名、删除和成员归属都放到同一处完成。
- 优化分组卡片标题区布局，状态文案不再和编辑、删除、展开按钮互相挤压，窄屏观感更稳定。
- 动态页关注对象加载改成“本地缓存即时回填 + 页面启动并行预热”，不再强制等主动态加载完成后再额外延迟补水。
- 首次使用声明、README、CHANGELOG 与 Focus 文档入口已同步指向 Focus 仓库与 Focus 发布线。

## 🧩 Focus 默认特性

- 首页 `推荐 / 热门 / 直播 / 游戏 / 分区` 默认隐藏
- 搜索页热门搜索默认关闭，但保留搜索建议、搜索发现、搜索结果与搜索历史
- 观看历史支持“一键清空全部历史记录”
- 视频详情页相关推荐默认隐藏，可在 Focus 设置中恢复
- 动态页与首页“关注”支持统一的本地关注分组过滤，并提供总开关

## 📦 版本信息

- Focus Version: `7.1.0-focus.2`
- Upstream Base: `7.1.0`
- VersionCode: `125`
- Tag: `v7.1.0-focus.2`

## 📱 产物命名

- Debug APK: `BliPai-Focus-debug-7.1.0-focus.2-debug.apk`
- Release APK: `BliPai-Focus-release-7.1.0-focus.2.apk`

## ✅ 验证

- `:app:testDebugUnitTest`
- `:app:lintDebug`
- `:app:assembleDebug`
- `:app:assembleRelease`

欢迎继续反馈问题与想法，我们会在保持上游可同步性的前提下，继续把 Focus 这条分支打磨得更克制、更稳定。
