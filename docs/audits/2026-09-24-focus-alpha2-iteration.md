# Focus 0.2.3-alpha.2.focus.1 迭代验证记录

本记录承接 [上游同步报告](2026-09-23-focus-upstream-sync.md) 与 [上一轮验证报告](2026-09-23-alpha2-verification.md)。

## 源码与版本

- 上游新增的 5 个提交已通过合并提交 `bbc545841` 纳入；发布前上游又增加 1 个个人页离屏骨架动画修复 `f259fd3fa`，通过合并提交 `70ea9d1fe` 纳入。最终上游基线为 `f259fd3fa`，源码版本仍为 `0.2.3-alpha.2`。
- Focus 候选版本为 `0.2.3-alpha.2.focus.1`，版本码 389，正式包名 `com.android.purebilibili.focus`。Focus Release 计划发布到稳定渠道，发布说明须明确其上游基础是 alpha 版本。
- 上一版公开 Focus Release 是 `v9.1.1-focus.5`，版本码 388。版本名跨世代时，客户端更新比较依据版本码而非字典序。

## 设置融合与升级

- 首页标签只使用上游导航设置的显隐与排序；Focus 页面删除了重复的 9 个开关。迁移逻辑一次性读取旧两套配置，计算升级前实际可见的标签，再写入唯一配置；相关映射单元测试通过。
- API 31 模拟器从已登录的 `.5` 正式包覆盖安装本轮已签名的 389 候选包成功。升级前后登录头像、原有「只显示关注」的首页布局均保留。
- 设置首页的「Focus 专属」入口在首屏可见，设置搜索输入 `Focus` 可定位入口；Focus 页面保留独有控制项。上游导航页打开「推荐」后，返回首页显示「推荐」和「关注」。实机截图保存在本机 `.local/current-v389-*.png`，因含账号信息不发布。
- 清空独立且未登录的 Dev 包数据后完成首次使用须知和诊断选择，初次首页顶部实际只显示「关注」；未安装订阅插件时「订阅」不显示，符合 README 明示的新用户默认布局。该匿名画面保存在本机 `.local/fresh-dev-focus-default-home.png`；正式 `.5` 包和其数据未被清空。
- 为测试旧客户端桥接，在 API 31 模拟器先保存已登录 389 状态快照，再安装公开 `.4` 包（码 226）。`.4` 启动时通过现有更新弹窗发现 `.5`；点击「立即更新」，下载完成后授予系统安装来源权限，经 Android 安装器确认，安装成功并打开 `.5`（码 388）。本机截图保存于 `.local/update-e2e-old4-*.png`。这证明旧版本名比较器仍可沿现有链路到达 `.5`；下一跳 `.5 → 389` 必须等待新版公开 Release 才能测试。
- `.5` 桥接包中打开 Focus 旧设置的「显示热门」，并在上游导航设置中把热门排到关注之前；首页实际顺序为「热门 → 关注」。该旧画面已记录于 `.local/update-e2e-v5-layout-sorted.png`，将用于发布后检查两套设置合并迁移的可见结果和顺序。旧包还开启了相关推荐与搜索历史，并保存搜索词 `FocusUpgradeProbe0924` 作为升级后数据保留标记。

## 动态与播放器实测

- API 31 已登录模拟器中，先在关注分组页隐藏默认分组和测试分组；重新进入动态页后，出现「当前分组暂无更多匹配动态」空状态。恢复默认分组可见并从分组面板返回，动态内容卡片和左侧关注对象随即重新出现。测试分组保持原有隐藏状态，默认分组已恢复可见。本机截图：`.local/dynamic-hidden.png`、`.local/dynamic-default-unhidden-sheet.png`、`.local/dynamic-default-unhidden-live.png`。
- 在实际动态视频中，播放器已经正常播放至 00:17 / 01:54，拖动进度条后显示 00:38 / 01:54 且播放按钮仍为暂停图标（代表正在播放）；之后视频画面持续变化，没有停留在缓冲画面。本机截图：`.local/seek-controls-before.png`、`.local/seek-after-2s.png`、`.local/seek-after-10s.png`。这一观察证明本次设备与视频的 Seek 后恢复；不据此推断所有网络/媒体格式均正常。

## 构建与发布门禁

- 本轮 15 个定向单测类通过，随后针对更新器跨世代版本码和稳定渠道的 2 个测试类再次通过；`:app:compileDebugAndroidTestKotlin` 通过。完整设备套件的上一轮原始结果仍为 63 项中 16 失败、3 跳过，不能宣称全绿。用户已将其余测试修复暂缓，首页语义节点和折叠屏场景先以实际界面判断。
- 合入上游离屏骨架动画修复后再次运行本地 `:app:assembleRelease`，包含 R8 与 `lintVitalRelease`，成功用时 8 分 36 秒；APK 为 `app/build/outputs/bilipai/release/BiliPai-Focus-0.2.3-alpha.2.focus.1.apk`，SHA-256 为 `620a9fcb28787d824ccd16b64333eef67ab7597f371bad3997020061f10223c7`。`apksigner verify --print-certs` 返回 0，签名证书 SHA-256 为 `a2f021866ee4e5f8f63df109e3369be3ceefa98fee3485e4d583e4a24f07f6bc`，与旧版 `.5` 一致；这是本地候选产物，最终公开资产须另行核对。
- 标签 CI 对 Release Gradle 调用关闭配置缓存，以避开前一版 `verifyFocusReleaseSigning` 的配置缓存序列化失败。新流程要求签名证书连续、生成构建元数据与校验和、生成非空构建证明，校验上传资产后才把 Release 从草稿发布。是否实际通过仍需以新标签的 GitHub Actions 与公开 Release 读回为准。
- 首次标签 CI [run 35995414669](https://github.com/AIALRA-0/BiliPai_Focus/actions/runs/35995414669) 的质量门禁与 `Build APK` 均成功，但签名连续性步骤因工作流脚本少一个闭合引号而报 Bash `unexpected EOF`；证书比较、证明与发布未执行。`d63d04e35` 修复引号并加入基于原受保护标签与预期完整 SHA 的补发流程，不改写原标签。第一次补发 [run 35999119204](https://github.com/AIALRA-0/BiliPai_Focus/actions/runs/35999119204) 复跑质量门禁、标签提交校验、Release 构建、签名连续性、校验和及 attestation 均通过；发布 job 因缺少 Git 检出而在 `gh release create --verify-tag` 失败，未创建 Release。`11641d9e2` 为发布 job 补充标签检出；第二次补发 [run 36002700244](https://github.com/AIALRA-0/BiliPai_Focus/actions/runs/36002700244) 再次通过构建、签名、产物和 attestation，创建了没有资产的草稿 Release，但发布 job 使用 `releases/tags/{tag}` 查询草稿时 GitHub 返回 404。`313b6fd31` 改用已验证的数字 Release ID 进行上传与核验；第三次补发 [run 36005042916](https://github.com/AIALRA-0/BiliPai_Focus/actions/runs/36005042916) 正在执行。

## 大屏和首页界面观察

- 在 API 31 Dev 构建上把同一模拟器临时调整为 2208×1840、420 dpi，目视检查首页和设置首页，标签、搜索、空状态、底部导航及首屏「Focus 专属」入口均可见，未见重叠或错误跳转；界面树中可找到 `Recommend`、`Following`、`Settings` 等导航节点。截图仅保存在本机 `.local/foldable-expanded-dev-*.png`。设备尺寸已恢复为 1080×2400。
- 这属于大屏窗口模拟，不具备折叠屏铰链、屏幕姿态和多窗口状态；不能替代真实折叠设备验收。此前首页语义节点测试失败仍需结合新版实机交互判断，当前观察未定位到需要阻止本轮发布的产品故障。

## 待完成的端到端步骤

- 匿名 Dev 包的 4 张 API 31 模拟器界面截图已加入中英文 README；已目视检查画面，并确认 PNG 不含 EXIF 元数据。
- GitHub 实际渲染最初把位于 `<div>` 内的 Markdown 截图语法显示为文字；已改为 HTML `<img>`。用 GitHub Markdown API 对中英文 README 分别渲染，均确认 4 张截图生成 `<img>` 元素，图片路径均存在。
- 提交并推送源码，创建受保护标签，等待标签 CI，读回校验最终 APK、元数据、签名、证明和 Release 渠道。
- 从旧版 Focus 客户端实际执行检查更新、下载、安装、启动及数据保留测试，并记录最终结果。
- 最低 API 26 ARM64 真机/模拟器、折叠屏铰链设备和此前设备套件剩余失败项未在本轮覆盖；不能作为已通过项目报告。
