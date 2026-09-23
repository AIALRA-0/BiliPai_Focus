package com.android.purebilibili.feature.home

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.store.HomeFeedCardWidthPreset
import com.android.purebilibili.core.store.HomeFeedCardStyle
import com.android.purebilibili.core.util.AppFoldPosture
import com.android.purebilibili.core.util.AppFoldingFeatureInfo
import com.android.purebilibili.core.util.AppHingeOrientation
import com.android.purebilibili.core.util.AppWindowAdaptiveInfo
import com.android.purebilibili.core.util.WindowHeightSizeClass
import com.android.purebilibili.core.util.WindowSizeClass
import com.android.purebilibili.core.util.WindowWidthSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeFeedGridPolicyTabletTest {

    @Test
    fun gridColumnsCapAtFourOnCompact() {
        val columns = resolveHomeFeedGridColumns(
            contentWidthDp = 1200,
            displayMode = 0,
            fixedColumnCount = 0,
            cardWidthPreset = HomeFeedCardWidthPreset.AUTO,
            widthSizeClass = WindowWidthSizeClass.Compact
        )
        assertEquals(4, columns)
    }

    @Test
    fun gridColumnsCapAtSixOnMediumAndExpanded() {
        val medium = resolveHomeFeedGridColumns(
            contentWidthDp = 1200,
            displayMode = 0,
            fixedColumnCount = 0,
            cardWidthPreset = HomeFeedCardWidthPreset.AUTO,
            widthSizeClass = WindowWidthSizeClass.Medium
        )
        val expanded = resolveHomeFeedGridColumns(
            contentWidthDp = 1200,
            displayMode = 0,
            fixedColumnCount = 0,
            cardWidthPreset = HomeFeedCardWidthPreset.AUTO,
            widthSizeClass = WindowWidthSizeClass.Expanded
        )
        assertEquals(6, medium)
        assertEquals(6, expanded)
    }

    @Test
    fun gridColumnsCapAtSevenOnLarge() {
        val columns = resolveHomeFeedGridColumns(
            contentWidthDp = 1400,
            displayMode = 0,
            fixedColumnCount = 0,
            cardWidthPreset = HomeFeedCardWidthPreset.AUTO,
            widthSizeClass = WindowWidthSizeClass.Large
        )
        assertEquals(7, columns)
    }

    @Test
    fun gridColumnsCapAtEightOnExtraLarge() {
        val columns = resolveHomeFeedGridColumns(
            contentWidthDp = 1600,
            displayMode = 0,
            fixedColumnCount = 0,
            cardWidthPreset = HomeFeedCardWidthPreset.AUTO,
            widthSizeClass = WindowWidthSizeClass.ExtraLarge
        )
        assertEquals(8, columns)
    }

    @Test
    fun cardAspectRatioSwitchesToSixteenNineOnExpanded() {
        val compactRatio = resolveHomeFeedCardAspectRatio(WindowWidthSizeClass.Compact)
        val mediumRatio = resolveHomeFeedCardAspectRatio(WindowWidthSizeClass.Medium)
        val expandedRatio = resolveHomeFeedCardAspectRatio(WindowWidthSizeClass.Expanded)
        val largeRatio = resolveHomeFeedCardAspectRatio(WindowWidthSizeClass.Large)
        val extraLargeRatio = resolveHomeFeedCardAspectRatio(WindowWidthSizeClass.ExtraLarge)

        assertEquals(16f / 10f, compactRatio)
        assertEquals(16f / 10f, mediumRatio)
        assertEquals(16f / 9f, expandedRatio)
        assertEquals(16f / 9f, largeRatio)
        assertEquals(16f / 9f, extraLargeRatio)
        assertEquals(
            16f / 9f,
            resolveHomeFeedCoverAspectRatio(
                style = HomeFeedCardStyle.BILIPAI,
                gridColumns = 6,
                widthSizeClass = WindowWidthSizeClass.Expanded,
            ),
        )
    }

    @Test
    fun bookHingeCreatesCenteredGridGapOnlyWhenAvoidanceIsRequired() {
        val bookInfo = AppWindowAdaptiveInfo(
            windowSizeClass = WindowSizeClass(
                widthSizeClass = WindowWidthSizeClass.Expanded,
                heightSizeClass = WindowHeightSizeClass.Medium,
                widthDp = 900.dp,
                heightDp = 700.dp,
            ),
            foldingFeature = AppFoldingFeatureInfo(
                posture = AppFoldPosture.Book,
                hingeOrientation = AppHingeOrientation.Vertical,
                hingeBounds = IntRect(440, 0, 460, 700),
                isSeparating = true,
            ),
        )

        val bookSpec = resolveHomeFeedBookHingeGridSpec(bookInfo, density = 1f)
        val flatSpec = resolveHomeFeedBookHingeGridSpec(
            bookInfo.copy(
                foldingFeature = bookInfo.foldingFeature.copy(posture = AppFoldPosture.Flat),
            ),
            density = 1f,
        )

        assertTrue(bookSpec.enabled)
        assertEquals(36.dp, bookSpec.centerGapDp)
        assertFalse(flatSpec.enabled)
    }
}
