# 观感罗盘示例插件（中文）

“观感罗盘”是一个面向 BiliPai Plugin SDK 的外部 Kotlin 推荐插件样例。它把候选视频拆成“轻松起步 / 深挖正片 / 冷门宝藏”三类，并给每条推荐输出可读解释，展示插件如何使用本地历史和反馈信号做更有个性的排序。

> [!IMPORTANT]
> 当前宿主只会预览、保存和授权 `.bpplugin`，不会执行外部代码。这个样例用于开发者适配 SDK、验证包格式和体验能力授权文案。

## 能力声明

插件只申请当前算法实际使用的能力：

- `RECOMMENDATION_CANDIDATES`：读取候选视频用于排序。
- `LOCAL_HISTORY_READ`：读取本地历史样本，判断熟悉 UP 主。
- `LOCAL_FEEDBACK_READ`：读取不感兴趣视频、UP 主和关键词，避免继续推荐。

## 构建

本示例目录没有独立 Gradle wrapper。请从示例目录使用仓库根 wrapper，并显式提供 Android SDK 路径：

```bash
cd plugins/samples/watch-compass
ANDROID_HOME=/Users/yiyang/Library/Android/sdk ../../../gradlew -p . packageBpPlugin --no-daemon
```

如果复制到仓库外独立开发，也可以在示例根目录创建 `local.properties`：

```properties
sdk.dir=/path/to/android/sdk
```

输出文件：

```text
build/distributions/watch-compass.bpplugin
```

确认包结构：

```bash
unzip -l build/distributions/watch-compass.bpplugin
```

根目录应包含：

- `plugin-manifest.json`
- `classes.jar`

`classes.jar` 当前只是预览阶段载荷；宿主不会加载或执行。
