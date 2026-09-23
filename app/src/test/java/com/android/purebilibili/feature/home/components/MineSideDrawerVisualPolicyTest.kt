package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.ui.AppDrawerContainerTreatment
import com.android.purebilibili.core.ui.PresetPrimitiveRenderer
import com.android.purebilibili.core.ui.resolveAppDrawerVisualPolicy
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class MineSideDrawerVisualPolicyTest {

    @Test
    fun `drawer reuses home backdrop and custom liquid tuning`() {
        val drawerSource = File(
            "src/main/java/com/android/purebilibili/feature/home/components/MineSideDrawer.kt"
        ).readText()
        val homeSource = File("src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
            .readText()

        assertTrue(drawerSource.contains("Modifier.biliPaiFloatingDockShell("))
        assertTrue(drawerSource.contains("liquidGlassTuning = liquidGlassTuning"))
        assertTrue(homeSource.contains("miuixBackdrop = readyHomeMiuixBackdrop"))
        assertTrue(homeSource.contains("homeSettings.liquidGlassProgress"))
        assertTrue(homeSource.contains("homeSettings.liquidGlassAdvancedSettings"))
        assertTrue(homeSource.contains("homeSettings.liquidGlassReadabilityMode"))
    }

    @Test
    fun `drawer uses semantic shapes for its edge and grouped surfaces`() {
        val drawerSource = File(
            "src/main/java/com/android/purebilibili/feature/home/components/MineSideDrawer.kt"
        ).readText()

        assertTrue(drawerSource.contains("AppShapes.endRounded(layoutPolicy.drawerEdgeRadiusDp.dp)"))
        assertTrue(drawerSource.contains("AppShapes.borderedContainer(ContainerLevel.Card)"))
        assertTrue(!drawerSource.contains("RoundedCornerShape("))
    }

    @Test
    fun `drawer renders the dedicated skin side background`() {
        val drawerSource = File(
            "src/main/java/com/android/purebilibili/feature/home/components/MineSideDrawer.kt"
        ).readText()
        val homeSource = File("src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
            .readText()

        assertTrue(drawerSource.contains("skinBackgroundImagePath: String? = null"))
        assertTrue(drawerSource.contains("model = File(requireNotNull(skinBackgroundImagePath))"))
        assertTrue(homeSource.contains("skinBackgroundImagePath = homeUiSkinDecoration?.sideBackgroundImagePath"))
        assertTrue(drawerSource.contains("skinBottomTrimImagePath: String? = null"))
        assertTrue(drawerSource.contains("model = File(skinBottomTrimImagePath)"))
        assertTrue(homeSource.contains("skinBottomTrimImagePath = homeUiSkinDecoration?.sideBottomTrimImagePath"))
        assertTrue(homeSource.contains("skinBackgroundTint = homeUiSkinDecoration?.sideBackgroundTint"))
    }

    @Test
    fun `blur-enabled drawer should keep translucent glass surface`() {
        val light = resolveDrawerGlassPalette(isDark = false, blurEnabled = true)
        val dark = resolveDrawerGlassPalette(isDark = true, blurEnabled = true)

        assertTrue(light.drawerBaseAlpha <= 0.34f)
        assertTrue(dark.drawerBaseAlpha <= 0.38f)
        assertTrue(light.itemSurfaceAlpha <= 0.22f)
        assertTrue(dark.itemSurfaceAlpha <= 0.20f)
    }

    @Test
    fun `blur-disabled drawer can stay opaque for readability`() {
        val light = resolveDrawerGlassPalette(isDark = false, blurEnabled = false)
        val dark = resolveDrawerGlassPalette(isDark = true, blurEnabled = false)

        assertTrue(light.drawerBaseAlpha >= 0.92f)
        assertTrue(dark.drawerBaseAlpha >= 0.92f)
    }

    @Test
    fun `drawer scrim should stay light when blur is enabled`() {
        val blurScrim = resolveHomeDrawerScrimAlpha(blurEnabled = true)
        val opaqueScrim = resolveHomeDrawerScrimAlpha(blurEnabled = false)

        assertTrue(blurScrim <= 0.16f)
        assertTrue(opaqueScrim >= 0.24f)
    }

    @Test
    fun `drawer transition should use more solid glass palette while blur budget is reduced`() {
        val stable = resolveDrawerGlassPalette(
            isDark = false,
            blurEnabled = true,
            budget = DrawerMotionBudget.FULL
        )
        val transitioning = resolveDrawerGlassPalette(
            isDark = false,
            blurEnabled = true,
            budget = DrawerMotionBudget.REDUCED
        )

        assertTrue(transitioning.drawerBaseAlpha > stable.drawerBaseAlpha)
        assertTrue(transitioning.itemSurfaceAlpha >= stable.itemSurfaceAlpha)
        assertTrue(transitioning.hazeBackgroundAlpha > stable.hazeBackgroundAlpha)
    }

    @Test
    fun `material drawer should use opaque containers and larger chevron when blur is off`() {
        val policy = resolveAppDrawerVisualPolicy(
            renderer = PresetPrimitiveRenderer.MATERIAL3,
            blurEnabled = false
        )

        assertEquals(AppDrawerContainerTreatment.OPAQUE, policy.containerTreatment)
        assertEquals(20, policy.profileChevronSizeDp)
    }

    @Test
    fun `material-family drawers keep translucent glass while blur is active`() {
        listOf(
            PresetPrimitiveRenderer.MATERIAL3,
            PresetPrimitiveRenderer.MIUIX_BRIDGED,
        ).forEach { renderer ->
            val policy = resolveAppDrawerVisualPolicy(
                renderer = renderer,
                blurEnabled = true,
            )

            assertEquals(AppDrawerContainerTreatment.TRANSLUCENT, policy.containerTreatment)
            assertEquals(20, policy.profileChevronSizeDp)
        }
    }
}
