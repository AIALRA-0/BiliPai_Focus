<div align="center">
<h1>BiliPai Focus</h1>
<p>面向哔哩哔哩日常使用的 Android 客户端，沿用 BiliPai 上游并提供 Focus 专属内容控制</p>
<p><a href="README.en.md">English</a> · <a href="https://github.com/AIALRA-0/BiliPai_Focus/releases/latest">下载最新 Focus Release</a> · <a href="CHANGELOG.md">更新记录</a> · <a href="https://github.com/jay3-yy/BiliPai">上游 BiliPai</a></p>
<p>本轮目标版本：<code>0.2.3-alpha.2.focus.1</code> / versionCode <code>389</code>（目标发布渠道为稳定版，待端到端与发布验证）· 上游基线：<code>0.2.3-alpha.2</code></p>
</div>

## 1 项目定位

BiliPai Focus 是基于 [BiliPai 上游项目](https://github.com/jay3-yy/BiliPai) 独立维护的第三方 Android 客户端

本项目不代表哔哩哔哩官方立场

上游项目提供日常内容浏览、播放和扩展能力；Focus 另外维护本分支独有的内容控制

版本核对需要分别查看源码和发布记录：

- 源码构建版本见 [`app/build.gradle.kts`](app/build.gradle.kts)
- 已发布版本见 [Focus Releases](https://github.com/AIALRA-0/BiliPai_Focus/releases)

Focus 发布以对应的上游版本为基线，再增加独立递增的小版本号

## 2 应用界面

以下页面来自 Android 31 模拟器中的 Focus Dev 构建，展示设置入口和主要界面；推荐页显示截图时加载的公开内容

<div align="center">
![Android 31 模拟器 Dev 构建的设置首页，显示搜索框和设置分类](docs/images/focus-0.2.3-alpha.2/settings-home.png)

<p><em>图 2.1　设置首页当前滚动位置；顶部可见设置搜索框和分类入口</em></p>
</div>

## 3 下载与安装

Focus 安装包的系统与设备要求如下：

- Android 8.0 或更新版本
- 64 位 ARM 处理器
- Android 12 或更新版本可提供更完整的动态主题效果
- 普通视频可提供 4K、1080P60 或 HDR 等画质档位，最终可用项取决于账号权限、视频资源和设备能力

### 3.1 安装步骤

第一步，打开 [Focus Releases](https://github.com/AIALRA-0/BiliPai_Focus/releases/latest) 并阅读发布说明

第二步，下载最新的 Focus 安装包

第三步，在 Android 设备上打开安装包；系统可能要求允许当前来源安装应用

第四步，打开应用后可以先浏览公开内容；需要账号权限的功能须先登录

当前登录页面展示本版本可用的登录方式，可能包括：

- TV 扫码
- 手机号密码
- 短信验证码
- Cookie 导入

[用户问答](docs/wiki/FAQ.md)介绍账号和网络相关问题

请只从 Focus Releases 获取本项目安装包，并遵守哔哩哔哩平台规则和内容权利方要求

## 4 Focus 专属设置

设置首页提供 **Focus 专属设置**入口，设置搜索也可以按“Focus”找到该页面

Focus 页面只放本分支独有的行为控制

- 首页顶部标签的显示状态与顺序统一在上游的导航设置中管理；Focus 不再提供重复开关
- 旧版本升级时，Focus 会将原有两套标签设置折算为升级前实际显示的标签集合，并保留用户当前顺序，再迁移到上游的唯一设置中
- 新安装默认显示「关注」；订阅插件启用时显示「订阅」，其他上游支持的标签可在导航设置中打开并排序
- Focus 页面继续保留关注分组过滤、视频相关推荐、搜索历史和历史记录清空入口的设置

<div align="center">
![Android 31 模拟器中的 Focus 专属设置页，显示关注过滤、相关推荐和历史记录选项](docs/images/focus-0.2.3-alpha.2/focus-settings.png)

<p><em>图 4.1　Focus 专属设置页；可在此调整关注过滤、相关推荐、搜索历史和历史记录清空行为</em></p>
</div>

顶部标签设置只列出上游支持配置的项目

标签显示还取决于导航设置和当前可用内容，插件也可能提供额外入口

<div align="center">
![Android 31 模拟器中的上游导航设置页，显示顶部标签显隐和排序选项](docs/images/focus-0.2.3-alpha.2/top-tabs.png)

<p><em>图 4.2　上游导航设置页；可查看顶部标签的显示开关和排序控件，当前开关状态仅代表此模拟器配置</em></p>
</div>

排查方式见[用户问答](docs/wiki/FAQ.md)

## 5 上游核心能力

Focus 延续上游面向日常使用的主流程

账号权限、视频资源和设备能力可能限制个别功能

- **视频播放**覆盖画质、弹幕与播放器控制
- **番剧与直播**提供选集、进度、分区和直播间播放
- **内容与社区**包含推荐、搜索、动态和消息
- **离线与投屏**提供下载、本地播放和设备投屏
- **备份与恢复**支持 WebDAV
- **大屏与外观**适配平板、折叠屏和多种主题
- **笔记与推荐**包含视频笔记、AI 总结草稿和 Today Watch

<div align="center">
![Android 31 模拟器 Dev 构建中的推荐页，显示推荐和关注标签以及公开内容卡片](docs/images/focus-0.2.3-alpha.2/recommend-feed.png)

<p><em>图 5.1　推荐页示例；画面显示推荐与关注标签及截图时加载的公开内容，推荐内容会随时间变化</em></p>
</div>

完整能力、实现状态和当前限制见[功能矩阵](docs/wiki/FEATURE_MATRIX.md)，其中 AI 总结草稿的使用条件取决于已配置的服务

功能方向与尚未完成项目见[路线图](docs/wiki/ROADMAP.md)

## 6 插件与扩展

内置插件共 10 个，名称和状态见[功能矩阵](docs/wiki/FEATURE_MATRIX.md)

- JSON 规则插件支持通过 URL 导入，可过滤推荐内容或弹幕；格式见[插件开发指南](docs/PLUGIN_DEVELOPMENT.md)
- 外部 `.bpskin` 皮肤包可预览受限的界面资源，见[皮肤样例](plugins/samples/winter-cloud-skin/README.md)
- 外部 `.bpplugin` Kotlin 包只解析、校验并显示能力申请，宿主不会运行包内代码；详见 [Plugin SDK 指南](plugins/sdk/README.md)
- 源码级插件需要修改仓库代码并重新构建；接口见[原生插件开发指南](docs/NATIVE_PLUGIN_DEVELOPMENT.md)

导入第三方 JSON 规则前请先核对来源和规则内容；外部包的能力声明见 [Plugin SDK 指南](plugins/sdk/README.md)

初见推荐改编自 wangdaodao 的 [TabulaBili](https://github.com/wangdaodao/TabulaBili) 与 tjsky 的 [TabulaBili-Plus](https://github.com/tjsky/TabulaBili)

这项致谢不代表 Focus 与原作者存在隶属关系

## 7 构建与开发

项目使用仓库自带的 Gradle Wrapper 和 JDK 21

Android Studio 与 Android SDK 需要支持仓库声明的 Gradle 插件和 SDK 版本

当前依赖版本见 [`gradle/libs.versions.toml`](gradle/libs.versions.toml) 与构建脚本

```bash
# Clone this repository and compile the app source
git clone https://github.com/AIALRA-0/BiliPai_Focus.git
cd BiliPai_Focus
./gradlew :app:compileDebugKotlin
```

以上命令编译应用 Kotlin 代码，不会生成安装包

需要本地安装包时，可运行 `./gradlew :app:assembleDev`；该开发产物与正式 Focus Release 使用不同的签名和安装流程

正式 Release 需要维护者持有的 Focus 签名配置，不能使用上游签名替代；发布规则见[发布流程](docs/wiki/RELEASE_WORKFLOW.md)

如果构建目录包含 `app/google-services.json`，构建会启用 Firebase 崩溃报告和使用情况统计

应用设置提供各自的控制项，源码默认开启；使用前请查看[隐私问答](docs/wiki/FAQ.md)，也不要把私有配置文件提交到仓库

主要模块包括：

- `app/`：Android 应用主体
- `design-system/`：共用界面系统
- `settings-core/`：设置策略
- `network-core/`：网络策略
- `plugin-sdk/`：插件接口
- `plugins/`：插件样例与扩展
- `baselineprofile/`：性能基准
- `docs/`：项目文档与验证记录
- `scripts/`：维护脚本

## 8 验证状态

此前的完整设备测试为 63 项中 16 项失败、3 项跳过；逐项结果见[2026-09-23 验证报告](docs/audits/2026-09-23-alpha2-verification.md)

本轮 15 个定向单测类通过，API 31 模拟器上的旧版覆盖更新、首页标签设置、动态内容恢复和 Seek 后继续播放也已完成针对性验证；设备、步骤和限制见[2026-09-24 迭代验证记录](docs/audits/2026-09-24-focus-alpha2-iteration.md)

这些定向结果不代表完整设备测试已经全绿，也不代表版本已经发布

仍待完成或补齐证据的项目包括：

- 标签 CI 的签名连续性、构建证明、资产上传和发布读回
- Focus 客户端检查更新、下载、安装、启动与数据保留的完整链路
- 最低 API 26 ARM64 设备覆盖，以及完整设备套件复跑
- 首页语义节点和折叠屏界面的实际验证

未执行或缺少设备证据的项目不会在本页写成已通过

## 9 文档、反馈与许可

- [Wiki 首页](docs/wiki/README.md)：项目文档导航
- [架构说明](docs/wiki/ARCHITECTURE.md)：模块与应用架构
- [功能矩阵](docs/wiki/FEATURE_MATRIX.md)：功能状态与限制
- [QA 手册](docs/wiki/QA.md)：验证流程
- [插件开发指南](docs/PLUGIN_DEVELOPMENT.md) 与[原生插件指南](docs/NATIVE_PLUGIN_DEVELOPMENT.md)：插件开发文档
- [版本规范](docs/wiki/VERSIONING.md)、[发布流程](docs/wiki/RELEASE_WORKFLOW.md) 与[更新日志](CHANGELOG.md)：版本与发布文档
- [代码结构规范](STRUCTURE_GUIDELINES.adoc)：协作和仓库结构约定
- [AI / LLM 文档入口](llms.txt) 与[导航指南](docs/wiki/AI.md)：自动化工具阅读项目文档的入口
- [Focus 问题反馈](https://github.com/AIALRA-0/BiliPai_Focus/issues)：提交 Focus 特有问题
- [BiliPai Issues](https://github.com/jay3-yy/BiliPai/issues)：提交上游通用问题
- [上游 Telegram 频道](https://t.me/bilipai666)：由 BiliPai 上游项目维护，发布上游公告
- [上游 Telegram 群组](https://t.me/bilipai888/1)：由 BiliPai 上游社区维护，用于项目交流
- [上游 X 账号](https://x.com/YangY_0x00)：上游维护者的公开更新入口
- [Focus Pull Requests](https://github.com/AIALRA-0/BiliPai_Focus/pulls)
  - Fork 仓库并创建 `feature/*` 或 `fix/*` 分支
  - 保持改动聚焦并补充必要测试或说明
  - Pull Request 写明目标、影响范围和验证结果

本仓库的代码许可证以 [`LICENSE`](LICENSE) 文件中的 GNU General Public License v3.0 文本为准

上游 README 另有非商业用途声明，与本仓库 LICENSE 的 GPL-3.0 文本存在不一致；本页引用仓库文件，不替代维护者对许可适用范围的确认

第三方素材和服务仍受各自许可与平台规则约束

崩溃报告和使用统计只有在构建包含 Firebase 配置时才可用

常规播放、搜索、登录、更新和插件导入需要联网

反馈问题时请勿公开 Cookie、令牌、密码、账号二维码或未脱敏日志

## 10 致谢

Focus 继承并持续同步 [BiliPai](https://github.com/jay3-yy/BiliPai) 的代码与项目说明

以下为主要依赖与参考资料：

- [Jetpack Compose](https://developer.android.com/jetpack/compose)：声明式界面工具包
- [BiliPai-miuix](https://github.com/Piracola/BiliPai-miuix)：上游界面组件 facade 与 design-system 重构贡献（@piracola）
- [Miuix](https://github.com/compose-miuix-ui/miuix)：Android 界面组件
- [AndroidX Media](https://github.com/androidx/media)：视频与音频播放组件
- [DanmakuRenderEngine](https://github.com/bytedance/DanmakuRenderEngine)：弹幕绘制
- [BilibiliSponsorBlock](https://github.com/hanydd/BilibiliSponsorBlock)：广告片段数据
- [Retrofit](https://github.com/square/retrofit)：网络请求
- [OkHttp](https://github.com/square/okhttp)：HTTP 客户端
- [Room](https://developer.android.com/training/data-storage/room)：本地数据库
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)：本地偏好存储
- [Coil](https://github.com/coil-kt/coil)：图片加载
- [Lottie](https://github.com/airbnb/lottie-android)：动画
- [Haze](https://github.com/chrisbanes/haze)：界面模糊效果
- [Compose Cupertino](https://github.com/alexzhirkevich/compose-cupertino)：iOS 风格界面组件
- [bilibili-API-collect](https://github.com/SocialSisterYi/bilibili-API-collect)：公开接口资料
- [PiliPlus](https://github.com/bggRGjQaUbCoE/PiliPlus)：移动端实现参考
- [Bili Pilot](https://github.com/siwei-yuan/bili-pilot)：移动端实现参考

Bili Pilot 的签名 CDN、分片选线与预缓存设计仅作参考；Focus 使用 Kotlin 独立实现，未复制其 JavaScript 代码

完整依赖版本见 Gradle 版本目录，各依赖的许可证以对应项目说明为准
