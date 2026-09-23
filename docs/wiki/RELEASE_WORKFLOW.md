# 发布流程（维护版）

最后更新：2026-08-06（语义化 MAJOR.MINOR.PATCH；Changelog 标签行格式）

## 目标

统一版本发布动作，避免出现“代码已发版但 README/Wiki 未同步”的情况。

## 官方社群

| 用途 | 链接 |
| --- | --- |
| 频道（公告 / 发布） | https://t.me/bilipai666 |
| 交流群 | https://t.me/bilipai888/1 |

发布时建议在频道或群组消息中附上：`versionName`、`versionCode`、交付 APK 文件名、短 commit、SHA-256。

## 标准步骤

1. 更新版本号  
   - 文件：`app/build.gradle.kts`
   - 规则：`versionCode + 1`；`versionName` 为 `MAJOR.MINOR.PATCH`（见 [版本规范](VERSIONING.md)）
   - 规则：大升级升 MAJOR，加功能升 MINOR，修 bug 升 PATCH；`versionCode` 每次 +1
   - 标签：稳定版标签必须为 `v<versionName>`，例如 `v0.2.0`
   - 注意：若 `versionName` 已领先公开文档，发布前必须补齐 `CHANGELOG.md` 与 README/Wiki

2. 更新发布日志  
   - 文件：`CHANGELOG.md`
   - 格式：自 v0.2.0 起使用 **标签行**（`[添加]` / `[修复]` / `[更改]` / `[适配]` / `[移除]`），段末 `vX.Y.Z(<short_sha>)`；详见 [更新日志撰写规范](CHANGELOG_GUIDE.md)
   - 要求：以 GitHub 上一个实际发布标签为比较基准，`git log <prev>..HEAD` 归纳用户可感知结果；**正文不写竞品名**
   - 频道发布：直接复制 `更新日志(ChangeLog)` 至版本行（含），再附 versionCode / SHA-256

3. 同步 README  
   - 文件：`README.md`、`README_EN.md`
   - 要求：同步顶部版本、快速导航、Latest、Roadmap 摘要（尤其“已完成基线”与当前 P0）

4. 同步路线图
   - 文件：`docs/wiki/ROADMAP.md`
   - 要求：同步版本/依赖基线、当前优先级、完成条件与已经失效的计划项

5. 同步 Wiki
   - 文件：`docs/wiki/FEATURE_MATRIX.md`、`docs/wiki/ARCHITECTURE.md`、`docs/wiki/QA.md`、`docs/wiki/RELEASE_WORKFLOW.md`
   - 要求：更新结构说明、回归清单、发布流程与能力状态

6. 同步 AI 入口
   - 文件：`llms.txt`、`docs/wiki/AI.md`
   - 要求：若 README/Wiki 页头时间、风险提示或文档优先级发生变化，需同步更新

7. 最低验证
   - 至少执行与本次改动相关的单测或构建命令
   - 至少执行一次 QA 基础检查清单（见 `docs/wiki/QA.md`）
   - 推荐：`./gradlew :app:testDebugUnitTest`
   - 需要生成可安装测试包时使用 `./gradlew :app:assembleDev`；不要把 `debug` 或 `smooth` 产物作为测试交付包
   - Dev 交付包必须出现在 `app/build/outputs/bilipai/dev/`，Release 交付包必须出现在 `app/build/outputs/bilipai/release/`

8. 提交与推送
   - 建议拆分为：
     - `chore(release): bump version to x.y.z`
     - `docs(readme): sync release notes`
     - `docs(wiki): sync docs and routing`

## 发布检查清单

- [ ] `app/build.gradle.kts` 版本号正确
- [ ] Git 标签、Changelog、构建元数据与 APK 文件名使用同一 `versionName`
- [ ] Telegram 频道/群组发布消息包含规范文件名、源码提交和 SHA-256
- [ ] `CHANGELOG.md` 新版本段存在
- [ ] 更新范围使用上一个 GitHub Release 标签，未把未发布的中间版本误作基准
- [ ] `README.md` 已同步最新版本与已完成功能
- [ ] `README_EN.md` 已同步最新版本与 Latest
- [ ] `docs/wiki/ROADMAP.md` 已同步当前优先级和完成条件
- [ ] `docs/wiki/FEATURE_MATRIX.md` 已同步
- [ ] `docs/wiki/ARCHITECTURE.md` / `QA.md` / `RELEASE_WORKFLOW.md` 已同步
- [ ] `llms.txt` / `docs/wiki/AI.md` 已同步
- [ ] 必要测试已执行并记录结果
