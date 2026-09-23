package com.android.purebilibili.feature.video.ui.components

import androidx.compose.ui.unit.dp
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoSettingsPanelActionPolicyTest {

    @Test
    fun nonGlassMiuixPanelUsesSemanticSpacingWithoutChangingLegacySpecs() {
        val miuixNonGlass = resolveVideoSettingsPanelVisualSpec(
            usesTonalContainerTreatment = false,
            useMiuixNonGlassPresentation = true,
        )
        assertEquals(16.dp, miuixNonGlass.rowHorizontalPadding)
        assertEquals(12.dp, miuixNonGlass.rowVerticalPadding)
        assertEquals(20.dp, miuixNonGlass.iconSize)
        assertEquals(12.dp, miuixNonGlass.iconGap)
        assertEquals(36.dp, miuixNonGlass.chipHeight)
        assertEquals(10.dp, miuixNonGlass.chipCornerRadius)
        assertEquals(12.dp, miuixNonGlass.chipHorizontalPadding)
        assertEquals(9.dp, miuixNonGlass.chipSpacing)

        val legacyTonal = resolveVideoSettingsPanelVisualSpec(
            usesTonalContainerTreatment = true,
            useMiuixNonGlassPresentation = false,
        )
        assertEquals(13.dp, legacyTonal.chipHorizontalPadding)
        assertEquals(7.dp, legacyTonal.chipSpacing)

        val legacyMaterial = resolveVideoSettingsPanelVisualSpec(
            usesTonalContainerTreatment = false,
            useMiuixNonGlassPresentation = false,
        )
        assertEquals(14.dp, legacyMaterial.rowVerticalPadding)
        assertEquals(24.dp, legacyMaterial.iconSize)
        assertEquals(16.dp, legacyMaterial.iconGap)
        assertEquals(8.dp, legacyMaterial.chipSpacing)
    }

    @Test
    fun customPanelTextKeepsLegacyTypographyOutsideMiuixNonGlassMode() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/ui/components/VideoSettingsPanel.kt")
            .readText()
        val textFacade = source
            .substringAfter("private enum class VideoSettingsPanelTextRole")
            .substringBefore("internal data class VideoSettingsPanelVisualSpec")

        assertTrue(textFacade.contains("if (isMiuixNonGlassEnabled())"))
        assertTrue(textFacade.contains("MaterialTheme.typography.titleSmall"))
        assertTrue(textFacade.contains("MaterialTheme.typography.bodySmall"))
        assertTrue(textFacade.contains("MaterialTheme.typography.labelMedium"))
        assertTrue(textFacade.contains("MaterialTheme.typography.labelSmall"))
        assertTrue(textFacade.contains("fontSize = legacyFontSize"))
        assertEquals(1, source.windowed("fontSize =".length).count { it == "fontSize =" })
        assertTrue(source.contains("val useMiuixNonGlassPresentation = isMiuixNonGlassEnabled()"))
    }

    @Test
    fun compactPhone_usesDenseScrollablePills() {
        val policy = resolveVideoSettingsPanelActionPolicy(widthDp = 393)

        assertEquals(10, policy.rowItemSpacingDp)
        assertEquals(46, policy.pillHeightDp)
        assertEquals(116, policy.pillMinWidthDp)
        assertEquals(18, policy.pillIconSizeDp)
        assertEquals(14, policy.pillHorizontalPaddingDp)
    }

    @Test
    fun mediumTablet_expandsPillTargets() {
        val policy = resolveVideoSettingsPanelActionPolicy(widthDp = 720)

        assertEquals(12, policy.rowItemSpacingDp)
        assertEquals(48, policy.pillHeightDp)
        assertEquals(126, policy.pillMinWidthDp)
        assertEquals(18, policy.pillIconSizeDp)
        assertEquals(16, policy.pillHorizontalPaddingDp)
    }

    @Test
    fun expandedTablet_usesLargestPillTier() {
        val policy = resolveVideoSettingsPanelActionPolicy(widthDp = 1024)

        assertEquals(12, policy.rowItemSpacingDp)
        assertEquals(50, policy.pillHeightDp)
        assertEquals(136, policy.pillMinWidthDp)
        assertEquals(19, policy.pillIconSizeDp)
        assertEquals(16, policy.pillHorizontalPaddingDp)
    }

    @Test
    fun videoSettingsPanel_exposesLongPressSpeedLockSwitch() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/ui/components/VideoSettingsPanel.kt")
            .readText()

        assertTrue(source.contains("长按倍速锁定"))
        assertTrue(source.contains("长按后拖至上下区域保持倍速"))
        assertTrue(source.contains("setLongPressSpeedLockEnabled"))
    }

    @Test
    fun videoSettingsPanel_usesStyleNeutralPreferenceEntrypoints() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/ui/components/VideoSettingsPanel.kt")
            .readText()

        assertTrue(source.contains("VideoSettingsSwitchRow("))
        assertTrue(source.contains("AppSwitchPreference("))
        assertTrue(source.contains("AppPreference("))
        assertTrue(source.contains("rememberAppPlayerChromeProfile()"))
        assertTrue(source.contains("title = \"弹幕设置\""))
        assertTrue(source.contains("onDanmakuSettingsClick"))
    }

}
