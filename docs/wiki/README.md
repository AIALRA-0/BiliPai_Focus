# BiliPai Wiki

最后更新：2026-08-04（按当前源码与构建配置校对）

## 目录

- [功能矩阵](FEATURE_MATRIX.md)
- [当前路线图](ROADMAP.md)
- [架构说明](ARCHITECTURE.md)
- [AI 导航指南](AI.md)
- [发布流程](RELEASE_WORKFLOW.md)
- [版本规范](VERSIONING.md)
- [更新日志撰写规范](CHANGELOG_GUIDE.md)
- [QA 测试手册](QA.md)
- [用户常见问题](FAQ.md)
- [插件开发指南（JSON）](../PLUGIN_DEVELOPMENT.md)
- [插件开发指南（原生）](../NATIVE_PLUGIN_DEVELOPMENT.md)

## 维护约定

每次 Release 至少同步以下内容：

1. `CHANGELOG.md` 新版本段落
2. `README.md` / `README_EN.md` 的 Latest 与 Roadmap 摘要
3. `docs/wiki/ROADMAP.md` 的当前优先级、完成条件与版本基线
4. 本 Wiki 的功能矩阵、架构、QA 与发布流程
5. 若 `app/build.gradle.kts` 的 `versionName` 已领先 `CHANGELOG.md`，需先补齐发布文档或明确说明仍是主线未同步状态
6. 若调整了 AI 入口或文档优先级，需同步 `AI.txt`、`llms.txt` 与 `docs/wiki/AI.md`

## 快速入口

- 设计与 Compose 组件规则：[UI 设计规范](ui-design/README.md)

- Android 主代码：`app/src/main/java/com/android/purebilibili`
- 测试代码：`app/src/test/java/com/android/purebilibili`
- 版本配置：`app/build.gradle.kts`
- 发布日志：`CHANGELOG.md`
- 当前路线图：`docs/wiki/ROADMAP.md`
- AI 入口：`llms.txt`
- 兼容别名：`AI.txt`
