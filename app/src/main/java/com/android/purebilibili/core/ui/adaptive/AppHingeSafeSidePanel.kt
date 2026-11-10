package com.android.purebilibili.core.ui.adaptive

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.WindowRegionLayout
import com.android.purebilibili.core.util.AppFoldPosture
import com.android.purebilibili.core.util.AppHingeSafePane
import com.android.purebilibili.core.util.LocalAppWindowAdaptiveInfo
import com.android.purebilibili.core.util.layoutHinges
import com.android.purebilibili.core.util.resolveHingeSafeContentRegions
import com.android.purebilibili.core.util.resolveHingeSafeRegion

/** Keep one content tree as folding changes, so panel state is not discarded between layouts. */
@Composable
internal fun AppHingeSafeSidePanel(
    isStart: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxWithConstraintsScope.() -> Unit,
) {
    val adaptiveInfo = LocalAppWindowAdaptiveInfo.current
    val hinges = adaptiveInfo.foldingFeature.layoutHinges()
    val layoutDirection = LocalLayoutDirection.current
    val clearancePx = with(LocalDensity.current) { 16.dp.roundToPx() }
    WindowRegionLayout(
        modifier = modifier,
        regionProvider = { size, origin ->
            val regions = if (adaptiveInfo.shouldAvoidHinge) {
                resolveHingeSafeContentRegions(size.width, size.height, hinges, origin, clearancePx)
            } else {
                listOf(IntRect(0, 0, size.width, size.height))
            }
            listOfNotNull(
                resolveSidePanelSafeRegion(regions, adaptiveInfo.posture, isStart, layoutDirection)
            )
        },
        primaryContent = { BoxWithConstraints(Modifier.fillMaxSize(), content = content) },
    )
}

internal fun resolveSidePanelSafeRegion(
    regions: List<IntRect>,
    posture: AppFoldPosture,
    isStart: Boolean,
    layoutDirection: LayoutDirection,
): IntRect? {
    // Tabletop interactions use the lower pane; Book follows the requested logical edge.
    val physicalStart = isStart == (layoutDirection == LayoutDirection.Ltr)
    val pane = when {
        posture == AppFoldPosture.Tabletop -> AppHingeSafePane.Bottom
        physicalStart -> AppHingeSafePane.Start
        else -> AppHingeSafePane.End
    }
    return resolveHingeSafeRegion(regions, pane)
}
