package com.android.purebilibili.feature.video.ui.components

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.luminance
import com.android.purebilibili.core.store.DanmakuPanelWidthMode
import com.android.purebilibili.feature.video.danmaku.DanmakuCloudSyncStatus
import com.android.purebilibili.feature.video.danmaku.DanmakuCloudSyncUiState
import com.android.purebilibili.feature.video.danmaku.resolveDanmakuCloudSyncToggleSubtitle
import com.android.purebilibili.feature.video.danmaku.DanmakuBlockRuleSections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DanmakuSettingsPanelPolicyTest {
    @Test
    fun blockManagerTabs_keepDynamicLabelsReadable() {
        val source = java.io.File(
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/DanmakuSettingsPanel.kt"
        ).readText()
        val blockManagerTabs = source
            .substringAfter("options = listOf(\"关键词\", \"正则\", \"UID(hash)\")")
            .substringBefore("AppOutlinedTextField(")

        assertTrue(blockManagerTabs.contains("scrollable = true"))
        assertTrue(blockManagerTabs.contains("minTabWidth = 88.dp"))
    }

    @Test
    fun portraitPanelAnchorsToBottomAndUsesWiderSheetWidth() {
        val policy = resolveDanmakuSettingsPanelLayoutPolicy(
            isFullscreen = false,
            screenWidthDp = 411,
            screenHeightDp = 915
        )

        assertEquals(
            DanmakuSettingsPanelPresentation.BottomSheet,
            policy.presentation
        )
        assertEquals(16, policy.horizontalPaddingDp)
        assertEquals(20, policy.bottomPaddingDp)
        assertTrue(policy.maxWidthDp >= 520)
    }

    @Test
    fun fullscreenPanelKeepsCenteredDialogPresentation() {
        val policy = resolveDanmakuSettingsPanelLayoutPolicy(
            isFullscreen = true,
            screenWidthDp = 915,
            screenHeightDp = 411
        )

        assertEquals(
            DanmakuSettingsPanelPresentation.CenteredDialog,
            policy.presentation
        )
        assertEquals(0, policy.bottomPaddingDp)
        assertTrue(policy.maxHeightDp in 280..420)
        assertEquals(
            DanmakuSettingsPanelAnchor.End,
            policy.anchor
        )
    }

    @Test
    fun fullscreenPanelWidth_isNarrowSideRail() {
        val fullWidth = resolveDanmakuSettingsPanelLayoutPolicy(
            isFullscreen = true,
            screenWidthDp = 915,
            screenHeightDp = 411,
            fullscreenWidthMode = DanmakuPanelWidthMode.FULL
        )
        val halfWidth = resolveDanmakuSettingsPanelLayoutPolicy(
            isFullscreen = true,
            screenWidthDp = 915,
            screenHeightDp = 411,
            fullscreenWidthMode = DanmakuPanelWidthMode.HALF
        )
        val thirdWidth = resolveDanmakuSettingsPanelLayoutPolicy(
            isFullscreen = true,
            screenWidthDp = 915,
            screenHeightDp = 411,
            fullscreenWidthMode = DanmakuPanelWidthMode.THIRD
        )

        // 分区 Tab 侧栏：放宽到约 45% 屏宽（380-440dp），避免控件拥挤
        assertEquals(fullWidth.maxWidthDp, halfWidth.maxWidthDp)
        assertEquals(fullWidth.maxWidthDp, thirdWidth.maxWidthDp)
        assertTrue(fullWidth.maxWidthDp in 380..440)
        assertEquals(fullWidth.minWidthDp, fullWidth.maxWidthDp)
        assertEquals(DanmakuSettingsPanelAnchor.End, fullWidth.anchor)
    }

    @Test
    fun wideTabletInlinePanel_usesCenteredDialogInsteadOfFullWidthSheet() {
        val policy = resolveDanmakuSettingsPanelLayoutPolicy(
            isFullscreen = false,
            screenWidthDp = 1280,
            screenHeightDp = 800
        )

        assertEquals(
            DanmakuSettingsPanelPresentation.CenteredDialog,
            policy.presentation
        )
        assertEquals(640, policy.maxWidthDp)
        assertEquals(0, policy.bottomPaddingDp)
        assertEquals(DanmakuSettingsPanelAnchor.Center, policy.anchor)
    }

    @Test
    fun portraitPanelKeepsBottomAnchor() {
        val policy = resolveDanmakuSettingsPanelLayoutPolicy(
            isFullscreen = false,
            screenWidthDp = 411,
            screenHeightDp = 915
        )

        assertEquals(DanmakuSettingsPanelAnchor.Bottom, policy.anchor)
    }

    @Test
    fun fullscreenPanelSurfaceColors_followDarkThemeTokens() {
        val darkScheme = darkColorScheme()
        val colors = resolveDanmakuSettingsPanelSurfaceColors(
            colorScheme = darkScheme
        )

        assertTrue(colors.panelColor.luminance() < 0.5f)
        assertTrue(colors.titleColor.luminance() > colors.panelColor.luminance())
        assertTrue(colors.titleColor.alpha > colors.supportingColor.alpha)
        assertEquals(darkScheme.primary, colors.sliderActiveTrackColor)
    }

    @Test
    fun fullscreenPanelSurfaceColors_followLightThemeTokens() {
        val lightScheme = lightColorScheme()
        val colors = resolveDanmakuSettingsPanelSurfaceColors(
            colorScheme = lightScheme
        )

        assertTrue(colors.panelColor.luminance() > 0.5f)
        assertTrue(colors.titleColor.luminance() < colors.panelColor.luminance())
        assertTrue(colors.titleColor.alpha > colors.supportingColor.alpha)
        assertEquals(lightScheme.primary, colors.sliderThumbColor)
    }

    @Test
    fun backdropTapDismissesPanelWhenPointerStaysWithinTouchSlop() {
        assertTrue(
            shouldDismissDanmakuSettingsPanelFromBackdropGesture(
                maxDragDistancePx = 4f,
                touchSlopPx = 8f
            )
        )
    }

    @Test
    fun backdropDragDoesNotDismissPanel() {
        assertFalse(
            shouldDismissDanmakuSettingsPanelFromBackdropGesture(
                maxDragDistancePx = 18f,
                touchSlopPx = 8f
            )
        )
    }

    @Test
    fun syncStatusBadge_usesExpectedLabels() {
        assertEquals(
            "待同步",
            resolveDanmakuSyncStatusBadgeText(
                DanmakuCloudSyncUiState(status = DanmakuCloudSyncStatus.PENDING)
            )
        )
        assertEquals(
            "同步中",
            resolveDanmakuSyncStatusBadgeText(
                DanmakuCloudSyncUiState(status = DanmakuCloudSyncStatus.SYNCING)
            )
        )
        assertEquals(
            "已同步",
            resolveDanmakuSyncStatusBadgeText(
                DanmakuCloudSyncUiState(status = DanmakuCloudSyncStatus.SUCCESS)
            )
        )
        assertEquals(
            "同步失败",
            resolveDanmakuSyncStatusBadgeText(
                DanmakuCloudSyncUiState(status = DanmakuCloudSyncStatus.FAILURE)
            )
        )
    }

    @Test
    fun cloudSyncToggleSubtitle_mentionsWebImpactWhenEnabled() {
        assertTrue(resolveDanmakuCloudSyncToggleSubtitle(enabled = true).contains("网页版"))
        assertTrue(resolveDanmakuCloudSyncToggleSubtitle(enabled = false).contains("本机"))
    }

    @Test
    fun syncRetryVisibility_onlyAppearsForFailure() {
        assertFalse(shouldShowDanmakuSyncRetry(DanmakuCloudSyncStatus.IDLE))
        assertFalse(shouldShowDanmakuSyncRetry(DanmakuCloudSyncStatus.PENDING))
        assertFalse(shouldShowDanmakuSyncRetry(DanmakuCloudSyncStatus.SYNCING))
        assertFalse(shouldShowDanmakuSyncRetry(DanmakuCloudSyncStatus.SUCCESS))
        assertTrue(shouldShowDanmakuSyncRetry(DanmakuCloudSyncStatus.FAILURE))
    }

    @Test
    fun blockManagerSections_parseRawRulesIntoThreeBuckets() {
        val sections = resolveDanmakuBlockManagerSections(
            "剧透\nregex:第\\d+集\nuid:abc123\n哈哈"
        )

        assertEquals(
            DanmakuBlockRuleSections(
                keywordRules = listOf("剧透", "哈哈"),
                regexRules = listOf("regex:第\\d+集"),
                userHashRules = listOf("uid:abc123")
            ),
            sections
        )
    }

    @Test
    fun blockManagerSections_saveSectionsAsNormalizedRawText() {
        val raw = persistDanmakuBlockManagerSections(
            DanmakuBlockRuleSections(
                keywordRules = listOf("剧透", "哈哈"),
                regexRules = listOf("regex:第\\d+集"),
                userHashRules = listOf("abc123", "uid:xyz")
            )
        )

        assertEquals(
            "剧透\n哈哈\nregex:第\\d+集\nuid:abc123\nuid:xyz",
            raw
        )
    }

    @Test
    fun blockRuleCountBadge_sumsAllRuleGroups() {
        val count = resolveDanmakuBlockRuleCount(
            DanmakuBlockRuleSections(
                keywordRules = listOf("剧透", "哈哈"),
                regexRules = listOf("regex:第\\d+集"),
                userHashRules = listOf("uid:abc123", "uid:xyz")
            )
        )

        assertEquals(5, count)
        assertEquals("5", resolveDanmakuBlockRuleBadgeText(count))
    }

    @Test
    fun blockRuleBadgeText_capsAtNinetyNinePlus() {
        assertEquals("99+", resolveDanmakuBlockRuleBadgeText(120))
    }

    @Test
    fun blockManagerTabLabel_includesCountOnlyWhenPresent() {
        assertEquals("关键词 2", resolveDanmakuBlockManagerTabLabel("关键词", 2))
        assertEquals("正则", resolveDanmakuBlockManagerTabLabel("正则", 0))
    }
}
