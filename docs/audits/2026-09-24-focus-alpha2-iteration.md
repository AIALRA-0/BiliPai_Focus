# Focus 0.2.3-alpha.2.focus.1 迭代验证记录

本记录承接 [上游同步报告](2026-09-23-focus-upstream-sync.md) 与 [上一轮验证报告](2026-09-23-alpha2-verification.md)。

## 源码与版本

- 上游新增的 5 个提交已通过合并提交 `bbc545841` 纳入，目标上游提交为 `f4d78bf6d`；本轮上游源码版本仍为 `0.2.3-alpha.2`。
- Focus 候选版本为 `0.2.3-alpha.2.focus.1`，版本码 389，正式包名 `com.android.purebilibili.focus`。Focus Release 计划发布到稳定渠道，发布说明须明确其上游基础是 alpha 版本。
- 上一版公开 Focus Release 是 `v9.1.1-focus.5`，版本码 388。版本名跨世代时，客户端更新比较依据版本码而非字典序。

## 设置融合与升级

- 首页标签只使用上游导航设置的显隐与排序；Focus 页面删除了重复的 9 个开关。迁移逻辑一次性读取旧两套配置，计算升级前实际可见的标签，再写入唯一配置；相关映射单元测试通过。
- API 31 模拟器从已登录的 `.5` 正式包覆盖安装本轮已签名的 389 候选包成功。升级前后登录头像、原有「只显示关注」的首页布局均保留。
- 设置首页的「Focus 专属」入口在首屏可见，设置搜索输入 `Focus` 可定位入口；Focus 页面保留独有控制项。上游导航页打开「推荐」后，返回首页显示「推荐」和「关注」。实机截图保存在本机 `.local/current-v389-*.png`，因含账号信息不发布。

## 动态与播放器实测

- API 31 已登录模拟器中，先在关注分组页隐藏默认分组和测试分组；重新进入动态页后，出现「当前分组暂无更多匹配动态」空状态。恢复默认分组可见并从分组面板返回，动态内容卡片和左侧关注对象随即重新出现。测试分组保持原有隐藏状态，默认分组已恢复可见。本机截图：`.local/dynamic-hidden.png`、`.local/dynamic-default-unhidden-sheet.png`、`.local/dynamic-default-unhidden-live.png`。
- 在实际动态视频中，播放器已经正常播放至 00:17 / 01:54，拖动进度条后显示 00:38 / 01:54 且播放按钮仍为暂停图标（代表正在播放）；之后视频画面持续变化，没有停留在缓冲画面。本机截图：`.local/seek-controls-before.png`、`.local/seek-after-2s.png`、`.local/seek-after-10s.png`。这一观察证明本次设备与视频的 Seek 后恢复；不据此推断所有网络/媒体格式均正常。

## 构建与发布门禁

- 本轮 15 个定向单测类通过，随后针对更新器跨世代版本码和稳定渠道的 2 个测试类再次通过；`:app:compileDebugAndroidTestKotlin` 通过。完整设备套件的上一轮原始结果仍为 63 项中 16 失败、3 跳过，不能宣称全绿。用户已将其余测试修复暂缓，首页语义节点和折叠屏场景先以实际界面判断。
- 本地 `:app:assembleRelease` 成功，包含 R8 与 `lintVitalRelease`；APK 为 `app/build/outputs/bilipai/release/BiliPai-Focus-0.2.3-alpha.2.focus.1.apk`，SHA-256 为 `570772de675f8364d74efdb43975934ec9cc1bb774ca6dac2442e0ab3839a013`。APK 的签名证书 SHA-256 为 `a2f021866ee4e5f8f63df109e3369be3ceefa98fee3485e4d583e4a24f07f6bc`，与旧版 `.5` 一致；这是本地候选产物，最终公开资产须另行核对。
- 标签 CI 对 Release Gradle 调用关闭配置缓存，以避开前一版 `verifyFocusReleaseSigning` 的配置缓存序列化失败。新流程要求签名证书连续、生成构建元数据与校验和、生成非空构建证明，校验上传资产后才把 Release 从草稿发布。是否实际通过仍需以新标签的 GitHub Actions 与公开 Release 读回为准。

## 待完成的端到端步骤

- 匿名 Dev 包的 4 张 API 31 模拟器界面截图已加入中英文 README；已目视检查画面，并确认 PNG 不含 EXIF 元数据。
- 提交并推送源码，创建受保护标签，等待标签 CI，读回校验最终 APK、元数据、签名、证明和 Release 渠道。
- 从旧版 Focus 客户端实际执行检查更新、下载、安装、启动及数据保留测试，并记录最终结果。
- 最低 API 26 ARM64 真机/模拟器、折叠屏铰链设备和此前设备套件剩余失败项未在本轮覆盖；不能作为已通过项目报告。
