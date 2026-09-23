package com.android.purebilibili.core.util

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

class AppAdaptiveStrategySnapshotTest {

    @Test
    fun `strategy snapshot records role, size class, posture, and player mode`() {
        val adaptiveInfo = AppWindowAdaptiveInfo(
            windowSizeClass = WindowSizeClass(
                widthSizeClass = WindowWidthSizeClass.Compact,
                heightSizeClass = WindowHeightSizeClass.Medium,
                widthDp = 421.dp,
                heightDp = 616.dp,
            ),
            foldingFeature = AppFoldingFeatureInfo(
                posture = AppFoldPosture.None,
                hingeBounds = IntRect(0, 0, 0, 0),
            ),
            displayContext = resolveAppDisplayContext(
                AppDisplayContextInput(
                    currentWindowWidthDp = 421,
                    currentWindowHeightDp = 616,
                    maximumWindowWidthDp = 861,
                    maximumWindowHeightDp = 609,
                    hasHingeAngleSensor = true,
                )
            ),
        )
        val text = formatAppAdaptiveStrategySnapshot(
            adaptiveInfo.toAdaptiveStrategySnapshot(playerPresentation = "in-window")
        )
        assertTrue(text.contains("role=Cover"))
        assertTrue(text.contains("basis=HingeAngleSensor"))
        assertTrue(text.contains("sizeClass=Compact/Medium"))
        assertTrue(text.contains("posture=None"))
        assertTrue(text.contains("player=in-window"))
        assertTrue(text.contains("current=421x616dp"))
    }
}
