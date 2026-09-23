package com.android.purebilibili.feature.video.ui.overlay

import com.android.purebilibili.core.ui.AppEffectCapability
import com.android.purebilibili.core.ui.AppPlayerChromeProfile
import com.android.purebilibili.core.ui.AppSemanticIconFamily
import com.android.purebilibili.core.ui.AppTopTabPresentation
import com.android.purebilibili.core.ui.CompactCapsuleChromeSpec
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MiniPlayerOverlayShellPolicyTest {

    @Test
    fun materialShellKeepsLayoutCornersAndLegacyAccent() {
        val layout = resolveMiniPlayerOverlayLayoutPolicy(widthDp = 393)
        val visual = resolveMiniPlayerOverlayShellVisual(
            layout = layout,
            chromeProfile = testChromeProfile(usesTonalContainerTreatment = false),
        )

        assertEquals(layout.cardCornerRadiusDp, visual.cardCornerRadiusDp)
        assertEquals(layout.cardElevationDp, visual.cardElevationDp)
        assertEquals(layout.cardShadowDp, visual.cardShadowDp)
        assertEquals(layout.seekHintCornerRadiusDp, visual.seekHintCornerRadiusDp)
        assertFalse(visual.useThemePrimaryAccent)
    }

    @Test
    fun miuixShellUsesFlatterElevationAndThemePrimaryAccent() {
        val layout = resolveMiniPlayerOverlayLayoutPolicy(widthDp = 393)
        val visual = resolveMiniPlayerOverlayShellVisual(
            layout = layout,
            chromeProfile = testChromeProfile(usesTonalContainerTreatment = true),
        )

        assertTrue(visual.cardCornerRadiusDp >= layout.cardCornerRadiusDp)
        assertEquals(0, visual.cardElevationDp)
        assertTrue(visual.cardShadowDp < layout.cardShadowDp)
        assertTrue(visual.seekHintCornerRadiusDp >= layout.seekHintCornerRadiusDp)
        assertTrue(visual.useThemePrimaryAccent)
    }

    @Test
    fun miniPlayerOverlayWiresShellVisualAndSurfaceTokens() {
        val source = load(
            "app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/MiniPlayerOverlay.kt"
        )
        assertTrue(source.contains("resolveMiniPlayerOverlayShellVisual("))
        assertTrue(source.contains("AppSurfaceTokens.primary()"))
        assertTrue(source.contains("shellVisual.cardCornerRadiusDp"))
        assertTrue(source.contains("shellVisual.useThemePrimaryAccent"))
    }

    private fun load(path: String): String {
        val normalized = path.removePrefix("app/")
        return listOf(File(path), File(normalized))
            .first { it.exists() }
            .readText()
    }

    private fun testChromeProfile(usesTonalContainerTreatment: Boolean) = AppPlayerChromeProfile(
        tabPresentation = if (usesTonalContainerTreatment) {
            AppTopTabPresentation.TONAL_CAPSULE
        } else {
            AppTopTabPresentation.MATERIAL_UNDERLINE
        },
        iconFamily = AppSemanticIconFamily.MATERIAL,
        compactChromeSpec = CompactCapsuleChromeSpec(
            primaryHeightDp = 48,
            secondaryButtonSizeDp = 48,
            chipHeightDp = 32,
            compactChipHeightDp = 28,
            primaryCornerRadiusDp = 24,
            secondaryButtonCornerRadiusDp = 16,
            chipCornerRadiusDp = 16,
            compactChipCornerRadiusDp = 14,
            iconSizeDp = 20,
            smallIconSizeDp = 16,
            inputHorizontalPaddingDp = 14,
            chipHorizontalPaddingDp = 12,
            compactChipHorizontalPaddingDp = 10,
            standardGapDp = 12,
        ),
        effects = AppEffectCapability(
            supportsIndependentLiquidGlass = false,
            usesTonalContainerTreatment = usesTonalContainerTreatment,
        ),
    )
}
