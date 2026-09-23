package com.android.purebilibili.core.plugin.skin


import com.android.purebilibili.core.store.HomeSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UiSkinActivationPolicyTest {

    @Test
    fun defaultSelection_keepsSkinDisabled() {
        val state = resolveUiSkinState(
            selection = UiSkinSelection(),
            installedSkins = emptyList()
        )

        assertFalse(state.enabled)
        assertNull(state.activeSkin)
    }

    @Test
    fun enabledSelectionActivatesOnlyMatchingInstalledSkin() {
        val installed = installedSkin(
            skinId = "local.bilibili_skin.winter_cloud",
            packageSha256 = "1111111111111111111111111111111111111111111111111111111111111111",
            version = "1.0.0"
        )

        val state = resolveUiSkinState(
            selection = UiSkinSelection(
                enabled = true,
                selectedSkinId = installed.skinId
            ),
            installedSkins = listOf(installed)
        )

        assertTrue(state.enabled)
        assertEquals(installed.skinId, state.activeSkin?.skinId)
    }

    @Test
    fun enabledSelectionPrefersInstallIdWhenSameSkinIdHasMultiplePackages() {
        val first = installedSkin(
            skinId = "local.bilibili_skin.local_package",
            packageSha256 = "1111111111111111111111111111111111111111111111111111111111111111",
            version = "1.0.0"
        )
        val second = installedSkin(
            skinId = "local.bilibili_skin.local_package",
            packageSha256 = "2222222222222222222222222222222222222222222222222222222222222222",
            version = "1.0.1"
        )

        val state = resolveUiSkinState(
            selection = UiSkinSelection(
                enabled = true,
                selectedSkinId = first.skinId,
                selectedInstallId = second.installId
            ),
            installedSkins = listOf(first, second)
        )

        assertTrue(state.enabled)
        assertEquals(second.installId, state.activeSkin?.installId)
        assertEquals("1.0.1", state.activeSkin?.manifest?.version)
    }

    @Test
    fun legacySkinIdSelectionStillActivatesFirstMatchingInstalledSkin() {
        val first = installedSkin(
            skinId = "local.bilibili_skin.local_package",
            packageSha256 = "1111111111111111111111111111111111111111111111111111111111111111",
            version = "1.0.0"
        )
        val second = installedSkin(
            skinId = "local.bilibili_skin.local_package",
            packageSha256 = "2222222222222222222222222222222222222222222222222222222222222222",
            version = "1.0.1"
        )

        val state = resolveUiSkinState(
            selection = UiSkinSelection(
                enabled = true,
                selectedSkinId = first.skinId
            ),
            installedSkins = listOf(first, second)
        )

        assertTrue(state.enabled)
        assertEquals(first.installId, state.activeSkin?.installId)
    }

    @Test
    fun missingSelectedSkinFallsBackToNoActiveSkin() {
        val state = resolveUiSkinState(
            selection = UiSkinSelection(enabled = true, selectedSkinId = "missing"),
            installedSkins = emptyList()
        )

        assertFalse(state.enabled)
        assertNull(state.activeSkin)
    }

    @Test
    fun skinStateDoesNotMutateLiquidGlassSettings() {
        val installed = installedSkin(
            skinId = "local.bilibili_skin.winter_cloud",
            packageSha256 = "1111111111111111111111111111111111111111111111111111111111111111",
            version = "1.0.0"
        )
        val homeSettings = HomeSettings(
            isBottomBarLiquidGlassEnabled = true
        )
        val state = resolveUiSkinState(
            selection = UiSkinSelection(
                enabled = true,
                selectedSkinId = installed.skinId
            ),
            installedSkins = listOf(installed)
        )

        val resolved = resolveUiSkinHomeSettings(
            homeSettings = homeSettings,
            uiSkinState = state
        )

        assertEquals(homeSettings, resolved)
        assertTrue(resolved.isBottomBarLiquidGlassEnabled)
    }

    private fun installedSkin(
        skinId: String,
        packageSha256: String,
        version: String
    ): InstalledUiSkinPackage {
        return InstalledUiSkinPackage(
            manifest = UiSkinManifest(
                formatVersion = 1,
                skinId = skinId,
                displayName = "本地装扮资源包",
                version = version,
                apiVersion = 1,
                surfaces = setOf(UiSkinSurface.HOME_BOTTOM_BAR)
            ),
            packageSha256 = packageSha256,
            packagePath = "/tmp/$packageSha256.bpskin",
            installedAtMillis = 1L
        )
    }
}
