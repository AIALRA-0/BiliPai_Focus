package com.android.purebilibili.feature.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.plugin.skin.UiSkinAssets
import com.android.purebilibili.core.plugin.skin.UiSkinColorTokens
import com.android.purebilibili.core.plugin.skin.UiSkinManifest
import com.android.purebilibili.core.plugin.skin.UiSkinSurface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UiSkinCompositionPreviewPolicyTest {

    private fun manifest(
        assets: UiSkinAssets = UiSkinAssets(),
        colors: UiSkinColorTokens = UiSkinColorTokens()
    ): UiSkinManifest = UiSkinManifest(
        formatVersion = 1,
        skinId = "test-skin",
        displayName = "测试皮肤",
        version = "1.0.0",
        apiVersion = 1,
        surfaces = setOf(UiSkinSurface.HOME_BOTTOM_BAR),
        assets = assets,
        colors = colors,
        containsOfficialAssets = true
    )

    private fun data(
        manifest: UiSkinManifest,
        assetFiles: Map<String, String> = emptyMap(),
        darkMode: Boolean = false
    ): UiSkinCompositionPreviewData = UiSkinCompositionPreviewData(
        displayName = manifest.displayName,
        manifest = manifest,
        assetFiles = assetFiles,
        darkMode = darkMode
    )

    @Test
    fun dockDimensionsMatchProductionTokens() {
        assertEquals(32.dp, previewDockIconSize())
        // 48 + 16 = 64dp，与 resolveBottomBarSkinDockHeight() 对齐
        assertEquals(64.dp, previewDockHeight())
    }

    @Test
    fun resolveLayersMapsBottomBarIconsFromAssetFiles() {
        val assets = UiSkinAssets(
            bottomBarIcons = mapOf(
                "home" to "assets/tail_icon_main.png",
                "following" to "assets/tail_icon_dynamic.png",
                "member" to "assets/tail_icon_shop.png",
                "profile" to "assets/tail_icon_myself.png"
            )
        )
        val assetFiles = mapOf(
            "assets/tail_icon_main.png" to "/cache/main.png",
            "assets/tail_icon_dynamic.png" to "/cache/dynamic.png",
            "assets/tail_icon_shop.png" to "/cache/shop.png"
        )
        val layers = resolveUiSkinCompositionLayers(data(manifest(assets), assetFiles))
        assertEquals("/cache/main.png", layers.bottomBarIconPaths["home"])
        assertEquals("/cache/dynamic.png", layers.bottomBarIconPaths["following"])
        // 缺失的 profile 映射被保留为声明路径（组件层会降级为占位）
        assertTrue(layers.bottomBarIconPaths["profile"]?.isNotBlank() == true)
        assertTrue(layers.hasBottomBarIcons)
    }

    @Test
    fun resolveLayersFallsBackWhenNoAssets() {
        val layers = resolveUiSkinCompositionLayers(data(manifest()))
        assertNull(layers.bottomBarTrimImagePath)
        assertNull(layers.drawerBottomTrimImagePath)
        assertNull(layers.topAtmosphereImagePath)
        assertNull(layers.topTabBackgroundImagePath)
        assertNull(layers.profileVideoBackgroundPath)
        assertNull(layers.publishIconImagePath)
        assertTrue(layers.bottomBarIconPaths.isEmpty())
        assertFalse(layers.hasBottomBarIcons)
        assertFalse(layers.hasTopAtmosphere)
        // 颜色用默认 fallback（不崩溃）
        assertEquals(false, layers.bottomBarTrimTint == Color.Unspecified)
    }

    @Test
    fun resolveLayersUsesColorTokensWhenPresent() {
        val colors = UiSkinColorTokens(
            bottomBarTrimTint = "#d3526b",
            topAtmosphereTint = "#13203a",
            searchCapsuleTint = "#fffaed"
        )
        val layers = resolveUiSkinCompositionLayers(data(manifest(colors = colors)))
        assertEquals(Color(0xFFd3526b), layers.bottomBarTrimTint)
        assertEquals(Color(0xFF13203a), layers.topAtmosphereTint)
        assertEquals(Color(0xFFfffaed), layers.searchCapsuleTint)
    }

    @Test
    fun parsePreviewColorHandlesInvalidAndEightDigitValues() {
        assertEquals(Color.Unspecified, parsePreviewColor(null, Color.Unspecified))
        assertEquals(Color.Unspecified, parsePreviewColor("not-a-color", Color.Unspecified))
        assertEquals(Color(0xFFd3526b), parsePreviewColor("#d3526b", Color.Unspecified))
        // 8 位 ARGB（FF 前缀 + 6 位 RGB）
        assertEquals(Color(0xFFd3526b), parsePreviewColor("FFd3526b", Color.Unspecified))
        // 回退
        assertEquals(Color.Black, parsePreviewColor(null, Color.Black))
    }

    @Test
    fun resolveLayersDarkModeFallbackColorsDifferFromLight() {
        val lightLayers = resolveUiSkinCompositionLayers(data(manifest(), darkMode = false))
        val darkLayers = resolveUiSkinCompositionLayers(data(manifest(), darkMode = true))
        assertFalse(lightLayers.bottomBarTrimTint == darkLayers.bottomBarTrimTint)
        assertFalse(lightLayers.topAtmosphereTint == darkLayers.topAtmosphereTint)
    }

    @Test
    fun resolveLayersDetectsTopAtmosphereCapability() {
        val withTop = manifest(assets = UiSkinAssets(topAtmosphere = "assets/head_bg.jpg"))
        val withTab = manifest(assets = UiSkinAssets(homeTopTabBackground = "assets/head_tab_bg.jpg"))
        val without = manifest()
        assertTrue(resolveUiSkinCompositionLayers(data(withTop)).hasTopAtmosphere)
        assertTrue(resolveUiSkinCompositionLayers(data(withTab)).hasTopAtmosphere)
        assertFalse(resolveUiSkinCompositionLayers(data(without)).hasTopAtmosphere)
    }

    @Test
    fun resolveLayersMapsEveryExtendedUpstreamSurface() {
        val assets = UiSkinAssets(
            drawerBottomTrim = "assets/side_bg_bottom.png",
            homeTopTabBackground = "assets/head_tab_bg.jpg",
            homeSideBackground = "assets/side_bg.jpg",
            homeProfileBackground = "assets/head_myself_bg.jpg",
            homeProfileSquaredBackground = "assets/head_myself_squared_bg.jpg",
            homeProfileVideoBackground = "assets/head_myself_mp4_bg.mp4",
            dynamicPublishIcon = "assets/tail_icon_pub_btn_bg.png",
        )
        val assetFiles = assets.declaredPaths().associateWith { path -> "/cache/${path.substringAfterLast('/')}" }

        val layers = resolveUiSkinCompositionLayers(data(manifest(assets), assetFiles))

        assertEquals("/cache/side_bg_bottom.png", layers.drawerBottomTrimImagePath)
        assertEquals("/cache/head_tab_bg.jpg", layers.topTabBackgroundImagePath)
        assertEquals("/cache/side_bg.jpg", layers.sideBackgroundImagePath)
        assertEquals("/cache/head_myself_bg.jpg", layers.profileBackgroundImagePath)
        assertEquals("/cache/head_myself_squared_bg.jpg", layers.profileSquaredBackgroundImagePath)
        assertEquals("/cache/head_myself_mp4_bg.mp4", layers.profileVideoBackgroundPath)
        assertEquals("/cache/tail_icon_pub_btn_bg.png", layers.publishIconImagePath)
    }

    @Test
    fun assetPathHelperReturnsNullForMissingPath() {
        val d = data(manifest(), assetFiles = mapOf("assets/a.png" to "/x/a.png"))
        assertEquals("/x/a.png", d.assetPath("assets/a.png"))
        assertNull(d.assetPath("assets/missing.png"))
        assertNull(d.assetPath(null))
    }
}
