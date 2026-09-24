# BiliPai 版本规范

最后更新：2026-09-24

> Focus 使用 `<上游版本>.focus.<本上游版本的 Focus 发布序号>` 版本名、`com.android.purebilibili.focus` 包名和 `BiliPai-Focus-` 交付文件名前缀。上游版本变化时，Focus 序号从 `1` 重新开始。下文的 `0.2.3-beta.7` 和 `BiliPai-` 是上游历史示例，不代表当前 Focus 构建。

## 当前选择

BiliPai 采用语义化版本号：

```text
MAJOR.MINOR.PATCH
```

| 段 | 含义 | 何时递增 |
| --- | --- | --- |
| `MAJOR` | 大升级 / 不兼容或纪元级变更 | 第一位 +1，后两位归零 |
| `MINOR` | 新功能 | 第二位 +1，`PATCH` 归零 |
| `PATCH` | 修 bug / 小改进 | 第三位 +1 |

当前 Focus 构建：`0.2.3-alpha.2.focus.1` / `versionCode 389`，基于上游 `0.2.3-alpha.2` / `versionCode 387`。Focus Release 属于 Focus 稳定更新渠道；上游基础仍为 alpha，不代表上游稳定版。

- **不要**用日期充当 `versionName`（例如 `26.0805.1`）。
- 应用 ID、签名和用户配置格式不变。
- Android `versionCode` **独立于** `versionName`，每次发布必须单调 +1，永不回退或复用。
- 应用内更新优先比较发布元数据中的 `versionCode`；不要用 `versionName` 字符串单独判断新旧。
- 构建 / 发布日期可写在关于页、Telegram 说明、`CHANGELOG` 或 `build-metadata`，**不要**塞进主版本号。

### 展示与追溯

| 位置 | 内容 |
| --- | --- |
| `versionName` / APK 名 | `0.2.3-beta.7` |
| 关于页建议 | `v0.2.3-beta.7 · 296`，并可附短 commit / 构建日期 |
| Git 短 SHA / 完整 sha256 | 关于页、Telegram 说明、日志；**不**写入主 `versionName` |

历史 `9.x` / 日历号 `YY.MMDD.N` / 先前 `0.1.0` 安装升级仍以 `versionCode` 为准。

## 递增规则

1. **修 bug / 小改进**：`PATCH + 1`，同时 `versionCode + 1`  
   - 例：`0.2.0` → `0.2.1`（code 284 → 285）
2. **加功能**：`MINOR + 1`，`PATCH` 归 `0`，同时 `versionCode + 1`  
   - 例：`0.2.2` → `0.3.0`
3. **大升级**：`MAJOR + 1`，`MINOR`/`PATCH` 归 `0`，同时 `versionCode + 1`  
   - 例：`0.9.3` → `1.0.0`
4. 仅工程变体后缀（不改变正式 `versionName` 主体）：  
   - debug：`versionNameSuffix = "-debug"`  
   - dev：`versionNameSuffix = "-dev"`  
   - smooth：`versionNameSuffix = "-smooth"`
5. Focus 稳定更新渠道的 Git 标签：`v<versionName>`，例如 `v0.2.3-alpha.2.focus.1`；GitHub Release 的 `prerelease` 应为 `false`，发布说明须披露上游 alpha 基础。

## 示例

```text
0.1.0    # 语义化 0.x 纪元起点
0.2.0    # 功能更新（默认值、收藏夹风控等）
0.2.1    # 补丁
1.0.0    # 大版本
```

## 与其它方案的关系

| 方向 | 示例 | BiliPai |
| --- | --- | --- |
| 语义化 `X.Y.Z` | `0.2.0` | **当前采用** |
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

Focus Release APK：`BiliPai-Focus-<versionName>.apk`，例如 `BiliPai-Focus-0.2.3-alpha.2.focus.1.apk`。
Focus Dev 验证包：`BiliPai-Focus-<versionName>-dev.apk`。

### 交付路径（不要拿 AGP 默认名）

| 命令 | 用户交付文件 |
| --- | --- |
| `./gradlew :app:assembleRelease` | `app/build/outputs/bilipai/release/BiliPai-Focus-0.2.3-alpha.2.focus.1.apk` |
| `./gradlew :app:assembleDev` | `app/build/outputs/bilipai/dev/BiliPai-Focus-0.2.3-alpha.2.focus.1-dev.apk` |

- `app/build/outputs/apk/**/app-*.apk` 或带 `-release` 后缀的中间产物**不是**对外交付名。
- `assembleRelease` / `assembleDev` 会 `finalizedBy` 导出任务，强制写成 `BiliPai-Focus-` 前缀；命名校验会拒绝 `app-release` 一类默认名。
- AGP 基名已设为 `BiliPai-Focus-<versionName>`，进一步避免默认 `app` 工程名。
