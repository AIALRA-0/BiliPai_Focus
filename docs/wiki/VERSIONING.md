# BiliPai 版本规范

最后更新：2026-11-11

> Focus 使用 `<上游版本>.focus.<本上游版本的 Focus 发布序号>` 作为 `versionName`，使用 `com.android.purebilibili.focus` 包名和 `BiliPai-Focus-` 交付文件名前缀。上游版本变化时，Focus 序号从 `1` 重新开始。当前源码版本是 `0.7.9-beta.9.focus.3`，`versionCode` 为 `393`，上游基线是 `0.7.9-beta.9` 的 `199164c0b` 提交；Release 页面显示该版本是否已有发布资产。

## 当前采用什么版本号

上游 BiliPai 采用语义化版本号表示自身的主版本、功能更新、修复和预发布状态：

```text
MAJOR.MINOR.PATCH
```

| 段 | 含义 | 何时递增 |
| --- | --- | --- |
| `MAJOR` | 大升级 / 不兼容或纪元级变更 | 第一位 +1，后两位归零 |
| `MINOR` | 新功能 | 第二位 +1，`PATCH` 归零 |
| `PATCH` | 修 bug / 小改进 | 第三位 +1 |

Focus 在上游版本后追加 `.focus.N`，用于区分同一上游版本基础上的 Focus 更新。当前源码版本为 `0.7.9-beta.9.focus.3` / `versionCode 393`，基于上游 `0.7.9-beta.9` 的 `199164c0b` 提交。Focus 稳定更新渠道描述的是 Focus 的分发方式；上游基础仍为 beta，不代表上游稳定版。

安装包是否已发布，以 [Focus 0.7.9-beta.9.focus.3 Release 页面](https://github.com/AIALRA-0/BiliPai_Focus/releases/tag/v0.7.9-beta.9.focus.3)列出的资产为准。只有页面出现对应 APK 后，读者才能从该页面下载。

- **不要**用日期充当 `versionName`（例如 `26.0805.1`）。
- 应用 ID、签名和用户配置格式不变。
- Android `versionCode` **独立于** `versionName`，每次发布必须单调 +1，永不回退或复用。
- 应用内更新优先比较发布元数据中的 `versionCode`；不要用 `versionName` 字符串单独判断新旧。
- 构建 / 发布日期可写在关于页、Telegram 说明、`CHANGELOG` 或 `build-metadata`，**不要**塞进主版本号。

### 展示与追溯

| 位置 | 内容 |
| --- | --- |
| `versionName` / APK 名 | `0.7.9-beta.9.focus.3` |
| 关于页建议 | `v0.7.9-beta.9.focus.3 · 393`，并可附短 commit / 构建日期 |
| Git 短 SHA / 完整 sha256 | 关于页、Telegram 说明、日志；**不**写入主 `versionName` |

历史 `9.x` / 日历号 `YY.MMDD.N` / 先前 `0.1.0` 安装升级仍以 `versionCode` 为准。此前发布的 Focus 版本 [`0.7.9-beta.7.focus.1`](https://github.com/AIALRA-0/BiliPai_Focus/releases/tag/v0.7.9-beta.7.focus.1) 使用 `versionCode 390`。

## 递增规则

1. **同一上游版本基础上的 Focus 更新**：Focus 序号 `N + 1`，同时 `versionCode + 1`。例如 `0.7.9-beta.9.focus.1` → `0.7.9-beta.9.focus.2`。
2. **上游版本变化**：采用新的上游版本，并将 Focus 序号重置为 `1`；`versionCode` 仍比上一次 Focus 发布加 `1`。例如 `0.7.9-beta.7.focus.1` / code `390` → `0.7.9-beta.9.focus.1` / code `391`。
3. 上游的 `MAJOR`、`MINOR`、`PATCH` 和预发布标识按上游版本规则变化；Focus 不会用自身序号改写上游版本。
4. 仅工程变体后缀（不改变正式 `versionName` 主体）：  
   - debug：`versionNameSuffix = "-debug"`  
   - dev：`versionNameSuffix = "-dev"`  
   - smooth：`versionNameSuffix = "-smooth"`
5. Focus 稳定更新渠道的 Git 标签：`v<versionName>`，例如 `v0.7.9-beta.9.focus.3`；GitHub Release 的 `prerelease` 应为 `false`，发布说明须披露上游 beta 基础。

## 示例

```text
0.7.9-beta.9.focus.1    # 上游 beta.9 基础上的第一个 Focus 版本
0.7.9-beta.9.focus.2    # 同一上游基础上的下一次 Focus 更新
0.7.9-beta.10.focus.1   # 上游版本变化后，Focus 序号重置为 1
```

## 与其它方案的关系

| 方向 | 示例 | BiliPai |
| --- | --- | --- |
| 上游语义化版本 `X.Y.Z` | `0.7.9-beta.9` | Focus 版本名采用的上游基础 |
| 两位年日历构建 | `26.0805.1` | 已结束（曾短暂使用） |
| 四位年 | `2026.0805.1` | 不采用 |
| 完整 sha256 进 versionName | — | 不采用 |

## 发布一致性

一次正式发布中下列值必须一致：

- `app/build.gradle.kts` 的 `versionName`；
- Git 标签去掉前缀 `v` 后的版本；
- `CHANGELOG.md` 顶部版本；
- `build-metadata.json` 中的 `versionName`；
- 交付 APK 文件名中的版本。

Focus Release APK：`BiliPai-Focus-<versionName>.apk`，例如 `BiliPai-Focus-0.7.9-beta.9.focus.3.apk`。
Focus Dev 验证包：`BiliPai-Focus-<versionName>-dev.apk`。

### 交付路径（不要拿 AGP 默认名）

| 命令 | 用户交付文件 |
| --- | --- |
| `./gradlew :app:assembleRelease` | `app/build/outputs/bilipai/release/BiliPai-Focus-0.7.9-beta.9.focus.3.apk` |
| `./gradlew :app:assembleDev` | `app/build/outputs/bilipai/dev/BiliPai-Focus-0.7.9-beta.9.focus.3-dev.apk` |

- `app/build/outputs/apk/**/app-*.apk` 或带 `-release` 后缀的中间产物**不是**对外交付名。
- `assembleRelease` / `assembleDev` 会 `finalizedBy` 导出任务，强制写成 `BiliPai-Focus-` 前缀；命名校验会拒绝 `app-release` 一类默认名。
- AGP 基名已设为 `BiliPai-Focus-<versionName>`，进一步避免默认 `app` 工程名。
