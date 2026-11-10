package com.android.purebilibili.core.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.WindowRegionLayout
import com.android.purebilibili.core.util.LocalAppWindowAdaptiveInfo
import com.android.purebilibili.core.util.layoutHinges
import com.android.purebilibili.core.util.resolveHingeSafeContentRegions

/** Keeps media in the first physical pane and supporting content in the last safe pane. */
@Composable
internal fun AppHingePaneLayout(
    primaryContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    secondaryContent: (@Composable () -> Unit)? = null,
    tertiaryContent: (@Composable () -> Unit)? = null,
) {
    val hinges = LocalAppWindowAdaptiveInfo.current.foldingFeature.layoutHinges()
    val clearancePx = with(LocalDensity.current) { 16.dp.roundToPx() }
    WindowRegionLayout(
        regionProvider = { size, origin ->
            resolveHingeSafeContentRegions(size.width, size.height, hinges, origin, clearancePx)
        },
        primaryContent = primaryContent,
        secondaryContent = secondaryContent,
        tertiaryContent = tertiaryContent,
        modifier = modifier,
    )
}
