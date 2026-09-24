# Focus 0.2.3-alpha.2.focus.1 迭代验证记录

本记录承接 [上游同步报告](2026-09-23-focus-upstream-sync.md) 与 [上一轮验证报告](2026-09-23-alpha2-verification.md)。

## 源码与版本

- 上游新增的 5 个提交已通过合并提交 `bbc545841` 纳入；发布前上游又增加 1 个个人页离屏骨架动画修复 `f259fd3fa`，通过合并提交 `70ea9d1fe` 纳入。最终上游基线为 `f259fd3fa`，源码版本仍为 `0.2.3-alpha.2`。
- Focus 稳定渠道版本为 `0.2.3-alpha.2.focus.1`，版本码 389，正式包名 `com.android.purebilibili.focus`。Release 说明明确其上游基础是 alpha 版本。标签 `v0.2.3-alpha.2.focus.1` 指向 `d59a6cda41194bec89dfe7d5dc73bd1108e7d274`。
- 上一版公开 Focus Release 是 `v9.1.1-focus.5`，版本码 388。版本名跨世代时，客户端更新比较依据版本码而非字典序。

## 设置融合与升级

- 首页标签只使用上游导航设置的显隐与排序；Focus 页面删除了重复的 9 个开关。迁移逻辑一次性读取旧两套配置，计算升级前实际可见的标签，再写入唯一配置；相关映射单元测试通过。
- API 31 模拟器从已登录的 `.5` 正式包覆盖安装本轮已签名的 389 候选包成功。升级前后登录头像、原有「只显示关注」的首页布局均保留。
- 设置首页的「Focus 专属」入口在首屏可见，设置搜索输入 `Focus` 可定位入口；Focus 页面保留独有控制项。上游导航页打开「推荐」后，返回首页显示「推荐」和「关注」。实机截图保存在本机 `.local/current-v389-*.png`，因含账号信息不发布。
- 清空独立且未登录的 Dev 包数据后完成首次使用须知和诊断选择，初次首页顶部实际只显示「关注」；未安装订阅插件时「订阅」不显示，符合 README 明示的新用户默认布局。该匿名画面保存在本机 `.local/fresh-dev-focus-default-home.png`；正式 `.5` 包和其数据未被清空。
- 为测试旧客户端桥接，在 API 31 模拟器先保存已登录 389 状态快照，再安装公开 `.4` 包（码 226）。`.4` 通过现有更新弹窗发现 `.5`；点击「立即更新」、下载、授予安装来源权限并确认系统安装器后，成功启动 `.5`（码 388）。
- `.5` 桥接包中打开 Focus 旧设置的「显示热门」，并在上游导航设置中把热门排到关注之前，实际首页为「热门 → 关注」。另开启相关推荐与搜索历史，并保存搜索词 `FocusUpgradeProbe0924`，作为升级后迁移与数据保留标记。
- 发布后从 `.5` 客户端真实执行「检查更新 → 发现 389 → 下载 → Android 安装器更新 → 打开」。已安装包版本为 `0.2.3-alpha.2.focus.1` / 389；拉取设备上的 `base.apk` 后计算出的 SHA-256 与 Release APK 完全一致：`bad8a4f0a8a75deae7a3f800fecb29be38bb64b9c2ae8fb7f9bcee42652b6639`。Android `firstInstallTime` 保持 `2026-09-24 11:41:09`，证明这是覆盖升级而非卸载重装。升级后首页顺序仍为「热门 → 关注」；相关推荐与搜索历史均保持开启；搜索历史仍含 `FocusUpgradeProbe0924`。冷启动后再次确认标签顺序保留。

## 动态与播放器实测

- API 31 已登录模拟器中，先在关注分组页隐藏默认分组和测试分组；重新进入动态页后，出现「当前分组暂无更多匹配动态」空状态。恢复默认分组可见并从分组面板返回，动态内容卡片和左侧关注对象随即重新出现。测试分组保持原有隐藏状态，默认分组已恢复可见。本机截图：`.local/dynamic-hidden.png`、`.local/dynamic-default-unhidden-sheet.png`、`.local/dynamic-default-unhidden-live.png`。
- 在实际动态视频中，播放器已经正常播放至 00:17 / 01:54，拖动进度条后显示 00:38 / 01:54 且播放按钮仍为暂停图标（代表正在播放）；之后视频画面持续变化，没有停留在缓冲画面。本机截图：`.local/seek-controls-before.png`、`.local/seek-after-2s.png`、`.local/seek-after-10s.png`。这一观察证明本次设备与视频的 Seek 后恢复；不据此推断所有网络/媒体格式均正常。

## 构建与发布门禁

- 本轮 15 个定向单测类通过，随后针对更新器跨世代版本码和稳定渠道的 2 个测试类再次通过；`:app:compileDebugAndroidTestKotlin` 通过。完整设备套件结果仍为 63 项中 16 失败、3 跳过，不能宣称全绿。其余失败修复已暂缓。
- 标签 CI Release run [36005042916](https://github.com/AIALRA-0/BiliPai_Focus/actions/runs/36005042916) 成功，公开 Release 为稳定渠道（`prerelease=false`），共 5 个资产：APK、校验和、构建元数据、验证元数据及 `intoto` 构建证明。Release APK SHA-256 为 `bad8a4f0a8a75deae7a3f800fecb29be38bb64b9c2ae8fb7f9bcee42652b6639`。签名证书 SHA-256 `a2f021866ee4e5f8f63df109e3369be3ceefa98fee3485e4d583e4a24f07f6bc` 与前一 Focus 正式版一致。
- 构建证明存在且其 bundle/digest 已核对；但因 GitHub Actions 的 `workflow_dispatch` 补发，证明声明的 workflow source ref 是 `main` 提交 `313b6fd31`，发布标签指向 `d59a6cda4`。两提交间仅工作流文件有差异。证明摘要与发布资产一致，但严格按 release tag 引用验证不通过；因此不把该证明描述为已通过 tag-ref 来源验证。
- 首次标签 CI [run 35995414669](https://github.com/AIALRA-0/BiliPai_Focus/actions/runs/35995414669) 质量门禁和构建成功，但 Bash 引号错误阻断后续步骤。后续补发依次修复脚本、补充发布 job 检出、再改用数字 Release ID，以解决 `gh release create --verify-tag` 和草稿 Release 查询问题。最终 [run 36005042916](https://github.com/AIALRA-0/BiliPai_Focus/actions/runs/36005042916) 全部成功并发布 5 个资产；该手动补发方式造成前述证明 source ref 与 tag 不一致，来源限制已如实记录。

## 大屏和首页界面观察

- 在 API 31 Dev 构建上把同一模拟器临时调整为 2208×1840、420 dpi，目视检查首页和设置首页，标签、搜索、空状态、底部导航及首屏「Focus 专属」入口均可见，未见重叠或错误跳转；界面树中可找到 `Recommend`、`Following`、`Settings` 等导航节点。截图仅保存在本机 `.local/foldable-expanded-dev-*.png`。设备尺寸已恢复为 1080×2400。
- 这属于大屏窗口模拟，不具备折叠屏铰链、屏幕姿态和多窗口状态；不能替代真实折叠设备验收。此前首页语义节点测试失败仍需结合新版实机交互判断，当前观察未定位到需要阻止本轮发布的产品故障。

## 尚未覆盖的验证范围

- 匿名 Dev 包的 4 张 API 31 模拟器界面截图已加入中英文 README；已目视检查画面，并确认 PNG 不含 EXIF 元数据。
- GitHub 实际渲染最初把位于 `<div>` 内的 Markdown 截图语法显示为文字；已改为 HTML `<img>`。用 GitHub Markdown API 对中英文 README 分别渲染，均确认 4 张截图生成 `<img>` 元素，图片路径均存在。
- 最低 API 26 ARM64 真机/模拟器未验证：当前 Windows 模拟器不支持该 AVD 的 ARM64 CPU 架构。
- 真实折叠屏铰链、姿态、多窗口行为未验证；此前设备套件中的首页语义节点失败也未完整重跑。展开窗口尺寸的模拟观察不能代替真机验证。
- 完整设备套件仍有 16 失败、3 跳过；此次按用户范围只复测了指定设置迁移、更新、动态内容与 Seek 场景。
