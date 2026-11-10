package com.android.purebilibili.core.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.HingeSafeOverlayRegions
import com.android.purebilibili.core.util.AppHingeSafeContentPurpose
import com.android.purebilibili.core.util.AppWindowAdaptiveInfo
import com.android.purebilibili.core.util.LocalAppWindowAdaptiveInfo
import com.android.purebilibili.core.util.layoutHinges
import com.android.purebilibili.core.util.resolveHingeSafeContentRegions
import com.android.purebilibili.core.util.resolveHingeSafeRegion
import com.android.purebilibili.core.util.resolvePreferredHingeSafePane

/** The host supplies its actual window origin and size, including system bar and IME insets. */
@Composable
fun rememberHingeSafeOverlayRegions(
    adaptiveInfo: AppWindowAdaptiveInfo = LocalAppWindowAdaptiveInfo.current,
): HingeSafeOverlayRegions {
    val clearancePx = with(LocalDensity.current) { 8.dp.roundToPx() }
    return remember(adaptiveInfo.foldingFeature, adaptiveInfo.shouldAvoidHinge, clearancePx) {
        if (!adaptiveInfo.shouldAvoidHinge) return@remember HingeSafeOverlayRegions()
        val hinges = adaptiveInfo.foldingFeature.layoutHinges()
        fun provider(purpose: AppHingeSafeContentPurpose): (IntSize, IntOffset) -> List<IntRect> =
            { size, origin ->
                val regions = resolveHingeSafeContentRegions(
                    size.width, size.height, hinges, origin, clearancePx,
                )
                val preferred = resolveHingeSafeRegion(
                    regions,
                    resolvePreferredHingeSafePane(adaptiveInfo.posture, purpose),
                )
                // If the IME or a parent inset removed a pane, select from the remaining regions.
                if (preferred == null) emptyList() else listOf(preferred) + regions.filter { it != preferred }
            }
        HingeSafeOverlayRegions(
            dialog = provider(AppHingeSafeContentPurpose.Dialog),
            sheet = provider(AppHingeSafeContentPurpose.Sheet),
        )
    }
}
