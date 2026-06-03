package com.android.purebilibili.feature.video.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoSettingsPanelActionPolicyTest {

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

}
