# 冬日云朵皮肤包示例

这是一个数据型 `.bpskin` 示例包，用来验证 BiliPai 首页底栏和顶部氛围的资源皮肤链路。皮肤只提供资源和颜色 token，不包含 Kotlin、Dex、Compose 代码，也不能替换底栏组件本体。

当前宿主会在用户主动导入并启用后读取：

- `bottomBarTrim`：底栏云朵饰边资源。
- `topAtmosphere`：顶部柔和氛围资源，预留给首页顶部装饰。
- `searchCapsuleBackground`：搜索框背景资源，预留给搜索胶囊装饰。
- `bottomBarIcons`：底栏图标贴纸候选，预留给后续受控图标资源消费。
- `colors`：底栏和顶部氛围的轻量颜色 token。

## 构建

本示例不需要 Android SDK，也没有独立 wrapper。请从示例目录使用仓库根 wrapper：

```bash
cd plugins/samples/winter-cloud-skin
../../../gradlew -p . packageBpSkin --no-daemon
```

输出文件：

```text
build/distributions/winter-cloud.bpskin
```

确认包结构：

```bash
unzip -l build/distributions/winter-cloud.bpskin
```

根目录应直接包含：

- `skin-manifest.json`
- `assets/`

导入宿主后，皮肤默认不执行任何代码；宿主只保存授权记录、资源文件和用户选择状态。
