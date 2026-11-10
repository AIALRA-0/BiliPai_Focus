package com.Android.purebilibili.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.ui.WindowRegionLayout
import com.android.purebilibili.core.ui.adaptive.AppHingePaneLayout
import com.android.purebilibili.core.util.AppFoldPosture
import com.android.purebilibili.core.util.AppFoldingFeatureInfo
import com.android.purebilibili.core.util.AppHingeFeature
import com.android.purebilibili.core.util.AppHingeOrientation
import com.android.purebilibili.core.util.AppWindowAdaptiveInfo
import com.android.purebilibili.core.util.LocalAppWindowAdaptiveInfo
import com.android.purebilibili.core.util.WindowHeightSizeClass
import com.android.purebilibili.core.util.WindowSizeClass
import com.android.purebilibili.core.util.WindowWidthSizeClass
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class HingePaneUiRegressionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun zeroWidthFold_withParentInset_keepsBothPanesOnTheirPhysicalSide() = verify(1f, LayoutDirection.Ltr)

    @Test
    fun zeroWidthFold_withCustomDensity_doesNotScaleItsWindowCoordinatesTwice() = verify(1.25f, LayoutDirection.Ltr)

    @Test
    fun zeroWidthFold_inRtl_keepsTheSamePhysicalSplit() = verify(1f, LayoutDirection.Rtl)

    @Test
    fun overlayWindowPositionsItsSurfaceBeforeApplyingTheWidthLimit() {
        var measuredBodyWidth = 0
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                WindowRegionLayout(
                    modifier = Modifier.requiredSize(800.dp, 300.dp),
                    regionProvider = { _, _ -> listOf(IntRect(416, 0, 800, 300)) },
                    primaryContent = {
                        Box(Modifier.widthIn(max = 420.dp).fillMaxSize()) {
                            Box(Modifier.fillMaxSize().onSizeChanged { measuredBodyWidth = it.width })
                        }
                    },
                )
            }
        }
        composeRule.runOnIdle { assertEquals(384, measuredBodyWidth) }
    }

    private fun verify(densityScale: Float, direction: LayoutDirection) {
        val hingeX = (140 * densityScale).roundToInt()
        val clearance = (16 * densityScale).roundToInt()
        val hinge = AppHingeFeature(AppHingeOrientation.Vertical, IntRect(hingeX, 0, hingeX, 10000), true, false, false)
        val info = AppWindowAdaptiveInfo(
            windowSizeClass = WindowSizeClass(WindowWidthSizeClass.Compact, WindowHeightSizeClass.Compact, 280.dp, 200.dp),
            foldingFeature = AppFoldingFeatureInfo(AppFoldPosture.Book, AppHingeOrientation.Vertical,
                hinge.bounds, true, false, listOf(hinge)),
        )
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(densityScale),
                LocalLayoutDirection provides direction,
                LocalAppWindowAdaptiveInfo provides info,
            ) {
                Box(Modifier.size(280.dp, 200.dp).padding(start = 20.dp)) {
                    AppHingePaneLayout(
                        modifier = Modifier.fillMaxSize(),
                        primaryContent = { Box(Modifier.fillMaxSize().testTag("primary")) },
                        secondaryContent = { Box(Modifier.fillMaxSize().testTag("secondary")) },
                    )
                }
            }
        }
        composeRule.waitForIdle()
        val primary = composeRule.onNodeWithTag("primary").fetchSemanticsNode().boundsInRoot
        val secondary = composeRule.onNodeWithTag("secondary").fetchSemanticsNode().boundsInRoot
        assertEquals((hingeX - clearance).toFloat(), primary.right, 1f)
        assertEquals((hingeX + clearance).toFloat(), secondary.left, 1f)
        assertEquals((clearance * 2).toFloat(), secondary.left - primary.right, 1f)
    }
}
