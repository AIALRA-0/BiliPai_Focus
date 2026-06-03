package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.plugin.ExternalPluginInstallDecision
import com.android.purebilibili.core.plugin.PluginCapability
import com.android.purebilibili.core.plugin.PluginCapabilityManifest
import com.android.purebilibili.core.plugin.kotlinpkg.ExternalKotlinPluginPayloadEntry
import com.android.purebilibili.core.plugin.kotlinpkg.ExternalKotlinPluginPayloadType
import com.android.purebilibili.core.plugin.kotlinpkg.InstalledExternalPluginPackage
import com.android.purebilibili.core.plugin.skin.InstalledUiSkinPackage
import com.android.purebilibili.core.plugin.skin.UiSkinAssets
import com.android.purebilibili.core.plugin.skin.UiSkinManifest
import com.android.purebilibili.core.plugin.skin.UiSkinPackagePreview
import com.android.purebilibili.core.plugin.skin.UiSkinSurface
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PluginsScreenPolicyTest {

    @Test
    fun sponsorBlockToggle_routesThroughSettingsPath() = runTest {
        var sponsorBlockToggleCount = 0
        var genericToggleCount = 0

        dispatchBuiltInPluginToggle(
            pluginId = "sponsor_block",
            enabled = true,
            onSponsorBlockToggle = { sponsorBlockToggleCount += 1 },
            onGenericPluginToggle = { _, _ -> genericToggleCount += 1 }
        )

        assertEquals(1, sponsorBlockToggleCount)
        assertEquals(0, genericToggleCount)
    }

    @Test
    fun nonSponsorBlockToggle_routesThroughGenericPluginPath() = runTest {
        var sponsorBlockToggleCount = 0
        var genericToggleCount = 0

        dispatchBuiltInPluginToggle(
            pluginId = "danmaku_enhance",
            enabled = false,
            onSponsorBlockToggle = { sponsorBlockToggleCount += 1 },
            onGenericPluginToggle = { _, _ -> genericToggleCount += 1 }
        )

        assertEquals(0, sponsorBlockToggleCount)
        assertEquals(1, genericToggleCount)
    }

    @Test
    fun capabilityModels_markNetworkAndStorageAsSensitiveAuthorizationItems() {
        val models = resolvePluginCapabilityUiModels(
            setOf(
                PluginCapability.RECOMMENDATION_CANDIDATES,
                PluginCapability.NETWORK,
                PluginCapability.PLUGIN_STORAGE
            )
        )

        assertEquals(listOf("推荐候选", "网络访问", "插件存储"), models.map { it.label })
        assertTrue(models.first { it.capability == PluginCapability.NETWORK }.requiresExplicitApproval)
        assertTrue(models.first { it.capability == PluginCapability.PLUGIN_STORAGE }.requiresExplicitApproval)
    }

    @Test
    fun externalInstallPreview_exposesHashSignerAndSensitiveCapabilities() {
        val manifest = PluginCapabilityManifest(
            pluginId = "dev.example.cloud",
            displayName = "云推荐",
            version = "1.0.0",
            apiVersion = 1,
            entryClassName = "dev.example.CloudPlugin",
            capabilities = setOf(PluginCapability.NETWORK, PluginCapability.LOCAL_HISTORY_READ)
        )
        val preview = buildExternalPluginInstallPreview(
            ExternalPluginInstallDecision.RequiresUserApproval(
                manifest = manifest,
                packageSha256 = "abcdef123456",
                signerTrusted = false,
                sensitiveCapabilities = setOf(PluginCapability.NETWORK, PluginCapability.LOCAL_HISTORY_READ)
            )
        )

        assertEquals("云推荐", preview.title)
        assertEquals("SHA-256: abcdef123456", preview.packageHashText)
        assertEquals("签名未信任", preview.signerText)
        assertEquals(listOf("观看历史", "网络访问"), preview.sensitiveCapabilityLabels)
    }

    @Test
    fun jsonRulePluginType_mapsToHostCapabilities() {
        assertEquals(
            setOf(PluginCapability.RECOMMENDATION_CANDIDATES),
            resolveJsonRulePluginCapabilities("feed")
        )
        assertEquals(
            setOf(PluginCapability.DANMAKU_STREAM, PluginCapability.DANMAKU_MUTATION),
            resolveJsonRulePluginCapabilities("danmaku")
        )
    }

    @Test
    fun externalPluginPayloadSummary_prefersConcretePayloadTypesOverDexOnlyText() {
        val summary = buildExternalPluginPayloadSummary(
            listOf(
                ExternalKotlinPluginPayloadEntry(
                    path = "classes.jar",
                    type = ExternalKotlinPluginPayloadType.CLASSES_JAR,
                    sizeBytes = 12
                ),
                ExternalKotlinPluginPayloadEntry(
                    path = "assets/compass.json",
                    type = ExternalKotlinPluginPayloadType.OTHER,
                    sizeBytes = 24
                )
            )
        )

        assertEquals("载荷：classes.jar、其他文件 1 个，当前不执行", summary)
    }

    @Test
    fun installedExternalPackageUi_exposesDisabledNonExecutingStateAndAuthorizationHash() {
        val installed = InstalledExternalPluginPackage(
            manifest = PluginCapabilityManifest(
                pluginId = "dev.example.cloud",
                displayName = "云推荐",
                version = "1.0.0",
                apiVersion = 1,
                entryClassName = "dev.example.CloudPlugin",
                capabilities = setOf(
                    PluginCapability.RECOMMENDATION_CANDIDATES,
                    PluginCapability.NETWORK
                )
            ),
            packageSha256 = "abcdef123456",
            signerSha256 = "trusted",
            grantedCapabilities = setOf(PluginCapability.RECOMMENDATION_CANDIDATES),
            packagePath = "/tmp/cloud.bpplugin",
            installedAtMillis = 1234L,
            enabled = false
        )

        val uiModel = buildInstalledExternalPluginUiModels(listOf(installed)).single()

        assertEquals("云推荐", uiModel.title)
        assertEquals("dev.example.cloud · v1.0.0", uiModel.subtitle)
        assertEquals("已保存，暂不运行", uiModel.stateText)
        assertEquals("SHA-256: abcdef123456", uiModel.packageHashText)
        assertEquals(listOf("推荐候选"), uiModel.grantedCapabilityLabels)
    }

    @Test
    fun uiSkinInstallPreview_exposesSourceLicenseAndShareability() {
        val preview = UiSkinPackagePreview(
            manifest = UiSkinManifest(
                formatVersion = 1,
                skinId = "community.pastel",
                displayName = "收藏馆浅彩风",
                version = "1.0.0",
                apiVersion = 1,
                author = "BiliPai",
                surfaces = setOf(UiSkinSurface.HOME_BOTTOM_BAR),
                assets = UiSkinAssets(bottomBarTrim = "assets/bottom_trim.png"),
                styleSourceName = "KimmyXYC/bilibili-skin",
                styleSourceUrl = "https://github.com/KimmyXYC/bilibili-skin",
                licenseNote = "原创重绘资源，可作为社区包分享",
                communityShareable = true,
                containsOfficialAssets = false
            ),
            packageSha256 = "abcdef",
            assetEntries = emptyList()
        )

        val uiModel = buildUiSkinPackagePreview(preview)

        assertEquals("收藏馆浅彩风", uiModel.title)
        assertEquals("community.pastel · v1.0.0", uiModel.subtitle)
        assertEquals("来源：KimmyXYC/bilibili-skin", uiModel.sourceText)
        assertEquals("授权：原创重绘资源，可作为社区包分享", uiModel.licenseText)
        assertEquals("社区分享：允许", uiModel.shareText)
        assertEquals("官方素材：未声明包含", uiModel.officialAssetText)
    }

    @Test
    fun uiSkinInstallPreview_warnsWhenPackageDeclaresOfficialAssets() {
        val preview = UiSkinPackagePreview(
            manifest = UiSkinManifest(
                formatVersion = 1,
                skinId = "local.bilibili_skin.xiaoyi",
                displayName = "萧逸",
                version = "1644150184",
                apiVersion = 1,
                surfaces = setOf(UiSkinSurface.HOME_BOTTOM_BAR),
                assets = UiSkinAssets(bottomBarTrim = "assets/tail_bg.png"),
                styleSourceName = "KimmyXYC/bilibili-skin",
                licenseNote = "由用户本地主题目录转换，输出包包含原存档/官方装扮素材",
                communityShareable = false,
                containsOfficialAssets = true
            ),
            packageSha256 = "abcdef",
            assetEntries = emptyList()
        )

        val uiModel = buildUiSkinPackagePreview(preview)

        assertEquals("社区分享：未声明允许", uiModel.shareText)
        assertEquals("官方素材：声明包含，请勿作为社区包分发", uiModel.officialAssetText)
    }

    @Test
    fun installedUiSkinSubtitle_usesShortChineseSurfaceLabels() {
        val manifest = UiSkinManifest(
            formatVersion = 1,
            skinId = "local.bilibili_skin.2233_summer_ice",
            displayName = "2233夏日冰品",
            version = "1628501914",
            apiVersion = 1,
            surfaces = setOf(UiSkinSurface.HOME_BOTTOM_BAR, UiSkinSurface.HOME_TOP_CHROME)
        )

        assertEquals(
            "1628501914 · 首页底栏、首页顶部 · 资源装饰",
            buildInstalledUiSkinSubtitle(manifest)
        )
    }

    @Test
    fun installedUiSkinPreview_exposesStatusHashAndDeleteCapability() {
        val installed = InstalledUiSkinPackage(
            manifest = UiSkinManifest(
                formatVersion = 1,
                skinId = "local.bilibili_skin.preview",
                displayName = "预览皮肤",
                version = "1.0.0",
                apiVersion = 1,
                surfaces = setOf(UiSkinSurface.HOME_BOTTOM_BAR, UiSkinSurface.HOME_TOP_CHROME),
                assets = UiSkinAssets(
                    bottomBarTrim = "assets/tail_bg.png",
                    topAtmosphere = "assets/head_bg.jpg"
                ),
                styleSourceName = "KimmyXYC/bilibili-skin",
                licenseNote = "仅本地使用",
                communityShareable = false,
                containsOfficialAssets = true
            ),
            packageSha256 = "abcdef123456",
            packagePath = "/tmp/preview.bpskin",
            installedAtMillis = 1234L,
            assetFiles = mapOf(
                "assets/tail_bg.png" to "/tmp/tail_bg.png",
                "assets/head_bg.jpg" to "/tmp/head_bg.jpg"
            )
        )

        val uiModel = buildInstalledUiSkinPreview(
            installed = installed,
            isActive = true
        )

        assertEquals("预览皮肤", uiModel.title)
        assertEquals("1.0.0 · 首页底栏、首页顶部 · 当前启用", uiModel.subtitle)
        assertEquals("SHA-256: abcdef123456", uiModel.packageHashText)
        assertEquals("资源：2 个，已保存到本地", uiModel.assetSummaryText)
        assertEquals("来源：KimmyXYC/bilibili-skin", uiModel.sourceText)
        assertEquals("授权：仅本地使用", uiModel.licenseText)
        assertEquals("官方素材：声明包含，请勿作为社区包分发", uiModel.officialAssetText)
        assertTrue(uiModel.canDelete)
    }

    @Test
    fun uiSkinImagePreviewModels_useDeclaredAssetLabelsAndLocalPaths() {
        val models = buildUiSkinImagePreviewItems(
            assetFiles = mapOf(
                "assets/tail_bg.png" to "/tmp/tail_bg.png",
                "assets/head_bg.jpg" to "/tmp/head_bg.jpg",
                "assets/unknown.png" to "/tmp/unknown.png"
            )
        )

        assertEquals(
            listOf("底栏饰面", "顶部氛围", "资源图片"),
            models.map { it.label }
        )
        assertEquals(
            listOf("/tmp/tail_bg.png", "/tmp/head_bg.jpg", "/tmp/unknown.png"),
            models.map { it.localPath }
        )
    }

    @Test
    fun uiSkinImportError_hidesRawByteLimitFromUserMessage() {
        assertEquals(
            "装扮存档资源较大，已放宽导入限制；请重新选择该装扮包导入",
            resolveUiSkinImportErrorMessage("装扮存档解压后内容超过 33554432 字节")
        )
    }
}
