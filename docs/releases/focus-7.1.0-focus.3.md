# BliPai Focus v7.1.0-focus.3 发布说明

BliPai Focus `v7.1.0-focus.3` 已发布，基于上游 `BiliPai v7.1.0`，继续围绕 Focus 使用场景收口首页顶部入口、关注分组和动态页关注加载体验。

## ✨ 重点更新

- 首页顶部分类在 Focus 过滤后改为严格居中对称排布，像只保留“关注”这一栏时也会稳定居中，不再偏向左侧。
- iOS 风格和 MD3 风格的顶部分类都统一接入“少量分类居中”规则，过滤后保留 1 到 3 个分类时，视觉重心会自动回到中轴。
- 关注分组管理、动态页关注对象缓存回填与并行预热链路继续保留上一版收口结果。
- README、CHANGELOG 与 Focus 发布入口已同步更新到 `focus.3` 版本线。

## 🧩 Focus 默认特性

- 首页 `推荐 / 热门 / 直播 / 游戏 / 分区` 默认隐藏
- 搜索页热门搜索默认关闭，但保留搜索建议、搜索发现、搜索结果与搜索历史
- 观看历史支持“一键清空全部历史记录”
- 视频详情页相关推荐默认隐藏，可在 Focus 设置中恢复
- 动态页与首页“关注”支持统一的本地关注分组过滤，并提供总开关

## 📦 版本信息

- Focus Version: `7.1.0-focus.3`
- Upstream Base: `7.1.0`
- VersionCode: `126`
- Tag: `v7.1.0-focus.3`

## 📱 产物命名

- Debug APK: `BliPai-Focus-debug-7.1.0-focus.3-debug.apk`
- Release APK: `BliPai-Focus-release-7.1.0-focus.3.apk`

## ✅ 验证

- `:app:testDebugUnitTest`
- `:app:lintDebug`
- `:app:assembleDebug`
- `:app:assembleRelease`
