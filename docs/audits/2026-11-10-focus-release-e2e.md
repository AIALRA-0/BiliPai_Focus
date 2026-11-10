# 2026 年 11 月 10 日 Focus 正式版升级端到端审计

## 发布从哪个代码构建，产物是什么

本次同步的共同祖先是 `7f0bc8cd4fdb11190c8b056f2c673c0ec18d29fa`。发布前核对时，`upstream/main` 仍指向 `05c4e2ac5c1621b7a5f6008572661075096af007`。合并 `v0.7.9-beta.7` 的提交是 `6d88f5fcd`，随后合并上游 main 布局与动效改动的提交是 `4bf8bfaf7`。

规范发布 [`v0.7.9-beta.7.focus.1`](https://github.com/AIALRA-0/BiliPai_Focus/releases/tag/v0.7.9-beta.7.focus.1) 指向提交 [`521ec722a0e9707053e16ed104ffe4410d7b8084`](https://github.com/AIALRA-0/BiliPai_Focus/commit/521ec722a0e9707053e16ed104ffe4410d7b8084)，版本码为 `390`。对应的 [Build 运行 42828014967](https://github.com/AIALRA-0/BiliPai_Focus/actions/runs/42828014967) 成功，Release 提供五个文件：APK、`build-metadata.json`、`build-provenance.intoto.jsonl`、`checksums.txt` 和 `verification-metadata.json`。APK 的 [发布文件](https://github.com/AIALRA-0/BiliPai_Focus/releases/download/v0.7.9-beta.7.focus.1/BiliPai-Focus-0.7.9-beta.7.focus.1.apk) SHA-256 为 `b473541318db7b4fa2246454145d05c6343029ed6bf4b64f2caa06ba5ecbad01`；签名证书 SHA-256 为 `a2f021866ee4e5f8f63df109e3369be3ceefa98fee3485e4d583e4a24f07f6bc`。构建证明记录 ID 为 `65858955`，Release 同时提供 [provenance 文件](https://github.com/AIALRA-0/BiliPai_Focus/releases/download/v0.7.9-beta.7.focus.1/build-provenance.intoto.jsonl)。

旧更新器兼容别名 [`v9.1.1-focus.6`](https://github.com/AIALRA-0/BiliPai_Focus/releases/tag/v9.1.1-focus.6) 指向同一提交，包含五个同名且摘要相同的文件，供旧版 `v9.1.1` 更新器发现使用。GitHub Release 列表仍将规范 tag 标为 Latest。

## 旧版客户端能否安装并打开新版本

API 31 上运行的已签名旧版 `0.2.3-alpha.2.focus.1`（版本码 `389`）发现规范更新，下载后核对 APK SHA-256，通过 Android 安装器覆盖安装，并成功打开版本码 `390`。升级把 Room 数据库从 v6 迁到 v7；设置文件保留，本地登录会话文件仍为 781 字节，「我的」页没有重新提示登录，收藏入口仍可见，首页 `Recommend`→`Following` 标签顺序保持不变。首次进入需要重新接受用户协议。

API 36 上的旧版 `9.1.1-focus.4`（版本码 `226`）通过别名 `v9.1.1-focus.6` 找到更新，下载后经过系统的未知来源安装确认和 Play Protect 扫描，安装版本码 `390` 并进入首页。这个流程验证了旧更新器发现兼容别名、再由系统安装新 APK 的完整升级路径。

## 升级后数据和功能有哪些实际表现

API 31 上，386 人的默认动态分组隐藏后，侧栏只显示「全部」和「我的」；重新显示后，「直播成员」和动态卡片都回来，冷启动后恢复显示的侧栏和内容仍保留。发布版视频能显示真实画面；应用切到后台再返回后能继续播放，使用横向 Seek 手势后也继续播放。保存的 `1080P` 偏好触发了「不可用」提示，但实际 `720P` 画质正常播放，因此本次只确认了可用画质下的播放与恢复。

设备截图属于本机 `.local` 目录，未随仓库提交：API 31 的 `.local/api31-update-found.png`、`.local/api31-update-downloaded.png`、`.local/api31-post-upgrade-main.png`、`.local/api31-dynamic-hidden-settled.png`、`.local/api31-dynamic-reshown.png`、`.local/api31-dynamic-cold.png`、`.local/api31-player-after-resume.png`、`.local/api31-player-after-seek2.png`；API 36 的 `.local/api36-old4-update-found.png`、`.local/api36-old4-download.png`、`.local/api36-old4-scan-result.png`、`.local/api36-old4-installed.png`、`.local/api36-old4-new-main2.png`。

## 哪些检查仍未通过或没有覆盖

发布 Build 成功不代表整套质量门禁全绿。完整 `lintDev` 检查报告了 1,753 个错误、996 个警告和 25 个提示；共运行 10,656 个完整单元测试，其中 481 个失败，分布在 226 个测试类。此前 63 项设备测试有 16 项失败、3 项跳过；本轮针对性设备检查 6/6 通过。API 26 ARM64 模拟器在当前 Windows 环境不可用，因此没有该环境下的升级复测；也没有完整的量化性能基线，不能据此声称性能指标已经达标。

## 发布后上游又更新了什么

本次规范发布的上游截止提交是 `05c4e2ac5c1621b7a5f6008572661075096af007`。发布后，`upstream/main` 在 2026-11-10 07:30–07:31 PST 新增 4 个提交，推进到 `199164c0`，版本升至 `0.7.9-beta.9`。这些提交涉及顶栏模糊、缓存和动效，播放器全屏弹幕、控制栏与过渡，详情统计与标签修复，以及版本元数据；没有安全、网络或数据库迁移改动。本次发布和端到端验证没有包含这 4 个提交，留待下一轮语义合并与回归。
