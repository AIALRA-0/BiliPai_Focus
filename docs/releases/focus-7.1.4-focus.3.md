# BliPai Focus v7.1.4-focus.3

发布日期：2026-03-24

## 更新摘要
- 这是一个紧急修复版，专门处理 `focus.2` release 包里“点击任意视频直接闪退”的严重问题。

## 根因
- adb 真机崩溃栈确认，进入 `VideoDetailScreen` 时会触发 release 专属 `VerifyError`。
- 根因不是首页点击逻辑本身，而是 R8 对大型 Compose 视频详情页做字节码优化后，生成了设备运行时不可校验的 dex。

## 修复方式
- 保留 release 包的代码裁剪与混淆。
- 关闭 R8 字节码优化，避免视频详情页和同类大型 Compose 页面再次被错误优化成不可校验的类。

## 验证
- 已通过 `:app:assembleRelease`。
- 已在连接的真机上通过 adb 安装修复后的 release 包。
- 已通过 adb 用标准 B 站视频链接直达视频详情页，确认应用不再因进入 `VideoDetailScreen` 闪退。
