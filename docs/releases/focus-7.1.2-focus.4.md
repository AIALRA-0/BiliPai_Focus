# BliPai Focus v7.1.2-focus.4

发布日期：2026-03-24

## 更新摘要
- 本次为应用内更新通道热修复版本，主要处理 GitHub Release 同时存在 `debug / release` APK 资产时的自动选择错误。

## 应用内更新修复
- 应用内自动更新现在会明确优先选择 `release` APK，不再因为 `debug` 包体积更大而误下 `debug`。
- `debug / dev` 资产会自动降权，仅在没有正常 `release` APK 可用时才作为兜底候选。
- GitHub Release 对外资产只保留正式 `release` APK，不再上传 `debug` APK。
- 补充对应策略单测，确保后续发布继续稳定优先下载正式包。
