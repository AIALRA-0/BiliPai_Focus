package com.android.purebilibili.feature.home.components

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.plugin.skin.InstalledUiSkinPackage
import com.android.purebilibili.core.plugin.skin.UiSkinAssets
import com.android.purebilibili.core.plugin.skin.UiSkinColorTokens
import com.android.purebilibili.core.plugin.skin.UiSkinManifest
import com.android.purebilibili.core.plugin.skin.UiSkinState
import com.android.purebilibili.core.plugin.skin.UiSkinSurface
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BottomBarUiSkinDecorationTest {

    @Test
    fun bottomSkinDecorativeTrimSupportsOptionalShapeClipForFloatingShell() {
        val source = File("src/main/java/com/android/purebilibili/feature/home/components/BottomBarUiSkin.kt")
            .readText()
        val trimSource = source
            .substringAfter("internal fun BottomBarSkinDecorativeTrim(")
            .substringBefore("private fun parseUiSkinColor(")

        assertTrue(trimSource.contains("clipShape: androidx.compose.ui.graphics.Shape? = null"))
        assertTrue(trimSource.contains("clipShape?.let { Modifier.clip(it) } ?: Modifier"))
        assertTrue(trimSource.contains(".clearAndSetSemantics {}"))
        assertTrue(trimSource.contains(".drawBehind {"))
        assertTrue(trimSource.contains("ContentScale.FillBounds"))
    }

    @Test
    fun bottomSkinIconSizesMatchScreenshotLevelCharacterAssets() {
        assertEquals(40.dp, resolveBottomBarSkinDockIconSize())
        assertEquals(38.dp, resolveBottomBarMiuixSkinDockIconSize())
        assertEquals(40.dp, resolveBottomBarCompactSkinHomeIconSize())
    }

    @Test
    fun bottomSkinDockLayoutKeepsLargeIconsAndLabelVisible() {
        val padding = resolveBottomBarSkinDockContentPadding()

        assertEquals(88.dp, resolveBottomBarSkinDockHeight())
        assertEquals(64.dp, resolveMiuixDockedBottomBarItemHeight(hasUiSkinDecoration = false))
        assertEquals(
            resolveBottomBarSkinDockHeight(),
            resolveMiuixDockedBottomBarItemHeight(hasUiSkinDecoration = true)
        )
        assertEquals(40.dp, resolveBottomBarSkinDockIconSize())
        assertEquals(0.dp, padding.calculateTopPadding())
        assertEquals(0.dp, padding.calculateBottomPadding())
        assertEquals(2.dp, resolveBottomBarSkinIconLabelGap())
        assertEquals(10.dp, resolveBottomBarSkinDockIconTopPadding())
        assertEquals(18.dp, resolveBottomBarSkinDockLabelBottomPadding())
        assertEquals(12.sp, resolveBottomBarSkinDockLabelFontSize())
        assertEquals(18.sp, resolveBottomBarSkinDockLabelLineHeight())
    }

    @Test
    fun activeExternalSkinUsesExtractedBottomTrimImagePath() {
        val installed = InstalledUiSkinPackage(
            manifest = UiSkinManifest(
                formatVersion = 1,
                skinId = "dev.example.cloud",
                displayName = "云朵底栏",
                version = "1.0.0",
                apiVersion = 1,
                surfaces = setOf(UiSkinSurface.HOME_BOTTOM_BAR),
                assets = UiSkinAssets(bottomBarTrim = "assets/bottom_trim.png")
            ),
            packageSha256 = "sha",
            packagePath = "/tmp/cloud.bpskin",
            installedAtMillis = 42L,
            assetFiles = mapOf("assets/bottom_trim.png" to "/tmp/bottom_trim.png")
        )

        val decoration = resolveBottomBarUiSkinDecoration(
            UiSkinState(enabled = true, activeSkin = installed)
        )

        assertEquals("dev.example.cloud", decoration?.skinId)
        assertEquals("/tmp/bottom_trim.png", decoration?.bottomTrimImagePath)
    }

    @Test
    fun activeExternalSkinMapsBilibiliTailIconsToBottomNavItems() {
        val installed = InstalledUiSkinPackage(
            manifest = UiSkinManifest(
                formatVersion = 1,
                skinId = "dev.example.tail-icons",
                displayName = "底栏图标",
                version = "1.0.0",
                apiVersion = 1,
                surfaces = setOf(UiSkinSurface.HOME_BOTTOM_BAR),
                assets = UiSkinAssets(
                    bottomBarIcons = mapOf(
                        "home" to "assets/tail_icon_main.png",
                        "home_selected" to "assets/tail_icon_selected_main.png",
                        "following" to "assets/tail_icon_dynamic.png",
                        "following_selected" to "assets/tail_icon_selected_dynamic.png",
                        "member" to "assets/tail_icon_shop.png",
                        "member_selected" to "assets/tail_icon_selected_shop.png",
                        "profile" to "assets/tail_icon_myself.png"
                    ),
                    homeChannelIcon = "assets/tail_icon_channel.png",
                    homeChannelSelectedIcon = "assets/tail_icon_selected_channel.png"
                )
            ),
            packageSha256 = "sha",
            packagePath = "/tmp/tail-icons.bpskin",
            installedAtMillis = 42L,
            assetFiles = mapOf(
                "assets/tail_icon_main.png" to "/tmp/tail_icon_main.png",
                "assets/tail_icon_selected_main.png" to "/tmp/tail_icon_selected_main.png",
                "assets/tail_icon_dynamic.png" to "/tmp/tail_icon_dynamic.png",
                "assets/tail_icon_selected_dynamic.png" to "/tmp/tail_icon_selected_dynamic.png",
                "assets/tail_icon_shop.png" to "/tmp/tail_icon_shop.png",
                "assets/tail_icon_selected_shop.png" to "/tmp/tail_icon_selected_shop.png",
                "assets/tail_icon_myself.png" to "/tmp/tail_icon_myself.png",
                "assets/tail_icon_channel.png" to "/tmp/tail_icon_channel.png",
                "assets/tail_icon_selected_channel.png" to "/tmp/tail_icon_selected_channel.png"
            )
        )

        val decoration = resolveBottomBarUiSkinDecoration(
            UiSkinState(enabled = true, activeSkin = installed)
        )

        assertEquals("/tmp/tail_icon_main.png", decoration?.iconPathFor(BottomNavItem.HOME))
        assertEquals("/tmp/tail_icon_selected_main.png", decoration?.iconPathFor(BottomNavItem.HOME, selected = true))
        assertEquals("/tmp/tail_icon_dynamic.png", decoration?.iconPathFor(BottomNavItem.DYNAMIC))
        assertEquals(
            "/tmp/tail_icon_selected_dynamic.png",
            decoration?.iconPathFor(BottomNavItem.DYNAMIC, selected = true)
        )
        assertEquals("/tmp/tail_icon_shop.png", decoration?.iconPathFor(BottomNavItem.HISTORY))
        assertEquals(
            "/tmp/tail_icon_selected_shop.png",
            decoration?.iconPathFor(BottomNavItem.HISTORY, selected = true)
        )
        assertEquals("/tmp/tail_icon_myself.png", decoration?.iconPathFor(BottomNavItem.PROFILE))
        assertEquals("/tmp/tail_icon_channel.png", decoration?.iconPathFor(BottomNavItem.SETTINGS))
        assertEquals(
            "/tmp/tail_icon_selected_channel.png",
            decoration?.iconPathFor(BottomNavItem.SETTINGS, selected = true)
        )
        assertNull(decoration?.iconPathFor(BottomNavItem.STORY))
        assertNull(decoration?.iconPathFor(BottomNavItem.LIVE))
    }

    @Test
    fun bottomSettingsSkinIconFallsBackWhenChannelAssetMissing() {
        val installed = InstalledUiSkinPackage(
            manifest = UiSkinManifest(
                formatVersion = 1,
                skinId = "dev.example.tail-icons",
                displayName = "底栏图标",
                version = "1.0.0",
                apiVersion = 1,
                surfaces = setOf(UiSkinSurface.HOME_BOTTOM_BAR),
                assets = UiSkinAssets(
                    bottomBarIcons = mapOf(
                        "home" to "assets/tail_icon_main.png",
                        "following" to "assets/tail_icon_dynamic.png",
                        "member" to "assets/tail_icon_shop.png",
                        "profile" to "assets/tail_icon_myself.png",
                        "profile_selected" to "assets/tail_icon_selected_myself.png"
                    )
                )
            ),
            packageSha256 = "sha",
            packagePath = "/tmp/tail-icons.bpskin",
            installedAtMillis = 42L,
            assetFiles = mapOf(
                "assets/tail_icon_main.png" to "/tmp/tail_icon_main.png",
                "assets/tail_icon_dynamic.png" to "/tmp/tail_icon_dynamic.png",
                "assets/tail_icon_shop.png" to "/tmp/tail_icon_shop.png",
                "assets/tail_icon_myself.png" to "/tmp/tail_icon_myself.png",
                "assets/tail_icon_selected_myself.png" to "/tmp/tail_icon_selected_myself.png"
            )
        )

        val decoration = resolveBottomBarUiSkinDecoration(
            UiSkinState(enabled = true, activeSkin = installed)
        )

        assertEquals("/tmp/tail_icon_myself.png", decoration?.iconPathFor(BottomNavItem.SETTINGS))
        assertEquals(
            "/tmp/tail_icon_selected_myself.png",
            decoration?.iconPathFor(BottomNavItem.SETTINGS, selected = true)
        )
    }

    @Test
    fun selectedBottomSkinIconFallsBackToUnselectedAssetWhenSelectedAssetMissing() {
        val installed = InstalledUiSkinPackage(
            manifest = UiSkinManifest(
                formatVersion = 1,
                skinId = "dev.example.tail-icons",
                displayName = "底栏图标",
                version = "1.0.0",
                apiVersion = 1,
                surfaces = setOf(UiSkinSurface.HOME_BOTTOM_BAR),
                assets = UiSkinAssets(
                    bottomBarIcons = mapOf("home" to "assets/tail_icon_main.png")
                )
            ),
            packageSha256 = "sha",
            packagePath = "/tmp/tail-icons.bpskin",
            installedAtMillis = 42L,
            assetFiles = mapOf("assets/tail_icon_main.png" to "/tmp/tail_icon_main.png")
        )

        val decoration = resolveBottomBarUiSkinDecoration(
            UiSkinState(enabled = true, activeSkin = installed)
        )

        assertEquals("/tmp/tail_icon_main.png", decoration?.iconPathFor(BottomNavItem.HOME, selected = true))
    }

    @Test
    fun disabledSkinDoesNotProduceBottomBarDecoration() {
        val installed = InstalledUiSkinPackage(
            manifest = UiSkinManifest(
                formatVersion = 1,
                skinId = "dev.example.cloud",
                displayName = "云朵底栏",
                version = "1.0.0",
                apiVersion = 1,
                surfaces = setOf(UiSkinSurface.HOME_BOTTOM_BAR),
                assets = UiSkinAssets(bottomBarTrim = "assets/bottom_trim.png")
            ),
            packageSha256 = "sha",
            packagePath = "/tmp/cloud.bpskin",
            installedAtMillis = 42L,
            assetFiles = mapOf("assets/bottom_trim.png" to "/tmp/bottom_trim.png")
        )

        assertNull(
            resolveBottomBarUiSkinDecoration(
                UiSkinState(enabled = false, activeSkin = installed)
            )
        )
    }

    @Test
    fun activeExternalSkinUsesExtractedHomeAtmosphereImagePath() {
        val installed = InstalledUiSkinPackage(
            manifest = UiSkinManifest(
                formatVersion = 1,
                skinId = "dev.example.atmosphere",
                displayName = "顶部氛围",
                version = "1.0.0",
                apiVersion = 1,
                surfaces = setOf(UiSkinSurface.HOME_TOP_CHROME),
                assets = UiSkinAssets(
                    topAtmosphere = "assets/head_bg.jpg",
                    homeTopTabBackground = "assets/head_tab_bg.jpg",
                    homeSideBackground = "assets/side_bg.jpg",
                    homeProfileBackground = "assets/head_myself_bg.jpg",
                    homeProfileSquaredBackground = "assets/head_myself_squared_bg.jpg",
                    homeChannelIcon = "assets/tail_icon_channel.png",
                    homeChannelSelectedIcon = "assets/tail_icon_selected_channel.png",
                    bottomBarIcons = mapOf(
                        "home" to "assets/tail_icon_main.png",
                        "home_selected" to "assets/tail_icon_selected_main.png",
                        "following" to "assets/tail_icon_dynamic.png",
                        "following_selected" to "assets/tail_icon_selected_dynamic.png",
                        "member" to "assets/tail_icon_shop.png",
                        "member_selected" to "assets/tail_icon_selected_shop.png",
                        "profile" to "assets/tail_icon_myself.png"
                    )
                ),
                colors = UiSkinColorTokens(
                    topAtmosphereTint = "#DFF5FF",
                    searchCapsuleTint = "#FFFFFF"
                )
            ),
            packageSha256 = "sha",
            packagePath = "/tmp/atmosphere.bpskin",
            installedAtMillis = 42L,
            assetFiles = mapOf(
                "assets/head_bg.jpg" to "/tmp/head_bg.jpg",
                "assets/head_tab_bg.jpg" to "/tmp/head_tab_bg.jpg",
                "assets/side_bg.jpg" to "/tmp/side_bg.jpg",
                "assets/head_myself_bg.jpg" to "/tmp/head_myself_bg.jpg",
                "assets/head_myself_squared_bg.jpg" to "/tmp/head_myself_squared_bg.jpg",
                "assets/tail_icon_channel.png" to "/tmp/tail_icon_channel.png",
                "assets/tail_icon_selected_channel.png" to "/tmp/tail_icon_selected_channel.png",
                "assets/tail_icon_main.png" to "/tmp/tail_icon_main.png",
                "assets/tail_icon_selected_main.png" to "/tmp/tail_icon_selected_main.png",
                "assets/tail_icon_dynamic.png" to "/tmp/tail_icon_dynamic.png",
                "assets/tail_icon_selected_dynamic.png" to "/tmp/tail_icon_selected_dynamic.png",
                "assets/tail_icon_shop.png" to "/tmp/tail_icon_shop.png",
                "assets/tail_icon_selected_shop.png" to "/tmp/tail_icon_selected_shop.png",
                "assets/tail_icon_myself.png" to "/tmp/tail_icon_myself.png"
            )
        )

        val decoration = resolveHomeUiSkinDecoration(
            UiSkinState(enabled = true, activeSkin = installed)
        )

        assertEquals("dev.example.atmosphere", decoration?.skinId)
        assertEquals("/tmp/head_bg.jpg", decoration?.topAtmosphereImagePath)
        assertEquals("/tmp/head_tab_bg.jpg", decoration?.topTabBackgroundImagePath)
        assertEquals("/tmp/side_bg.jpg", decoration?.sideBackgroundImagePath)
        assertEquals("/tmp/head_myself_bg.jpg", decoration?.profileBackgroundImagePath)
        assertEquals("/tmp/head_myself_squared_bg.jpg", decoration?.profileSquaredBackgroundImagePath)
        assertEquals("/tmp/tail_icon_main.png", decoration?.topTabIconPathFor("RECOMMEND"))
        assertEquals("/tmp/tail_icon_selected_main.png", decoration?.topTabIconPathFor("RECOMMEND", selected = true))
        assertEquals("/tmp/tail_icon_dynamic.png", decoration?.topTabIconPathFor("FOLLOW"))
        assertEquals("/tmp/tail_icon_channel.png", decoration?.topTabIconPathFor("POPULAR"))
        assertEquals("/tmp/tail_icon_channel.png", decoration?.topTabPartitionIconPath())
        assertEquals("/tmp/tail_icon_selected_channel.png", decoration?.topTabPartitionIconPath(selected = true))
    }

    @Test
    fun bottomOnlySkinDoesNotProduceHomeAtmosphereDecoration() {
        val installed = InstalledUiSkinPackage(
            manifest = UiSkinManifest(
                formatVersion = 1,
                skinId = "dev.example.bottom",
                displayName = "仅底栏",
                version = "1.0.0",
                apiVersion = 1,
                surfaces = setOf(UiSkinSurface.HOME_BOTTOM_BAR),
                assets = UiSkinAssets(bottomBarTrim = "assets/bottom_trim.png")
            ),
            packageSha256 = "sha",
            packagePath = "/tmp/bottom.bpskin",
            installedAtMillis = 42L,
            assetFiles = mapOf("assets/bottom_trim.png" to "/tmp/bottom_trim.png")
        )

        assertNull(
            resolveHomeUiSkinDecoration(
                UiSkinState(enabled = true, activeSkin = installed)
            )
        )
    }
}
