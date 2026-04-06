package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.core.store.DanmakuPanelWidthMode
import com.android.purebilibili.feature.video.danmaku.DanmakuCloudSyncStatus
import com.android.purebilibili.feature.video.danmaku.DanmakuCloudSyncUiState
import com.android.purebilibili.feature.video.danmaku.DanmakuBlockRuleSections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DanmakuSettingsPanelPolicyTest {

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
        assertEquals(480, policy.maxHeightDp)
    }

    @Test
    fun fullscreenPanelWidthMode_isFixedToQuarterWidth() {
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

        assertEquals(221, fullWidth.maxWidthDp)
        assertEquals(221, halfWidth.maxWidthDp)
        assertEquals(221, thirdWidth.maxWidthDp)
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
