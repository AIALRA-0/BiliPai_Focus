package com.android.purebilibili.feature.video.ui.section

import android.os.Build
import android.view.WindowManager
import android.view.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.android.purebilibili.core.util.LocalAppWindowAdaptiveInfo
import com.android.purebilibili.feature.video.danmaku.DanmakuViewport
import com.android.purebilibili.feature.video.danmaku.resolveDanmakuViewport

/** One measured surface for ordinary, authored and interactive danmaku. */
@Composable
internal fun DanmakuViewportHost(
    modifier: Modifier,
    content: @Composable BoxScope.(DanmakuViewport) -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val display = LocalAppWindowAdaptiveInfo.current.displayContext
    val reference = remember(context, configuration, density.density, display) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = context.getSystemService(WindowManager::class.java).maximumWindowMetrics
            // Fullscreen hides system bars; only the display cutout constrains this reference.
            val safe = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.displayCutout())
            minOf(metrics.bounds.width() - safe.left - safe.right,
                metrics.bounds.height() - safe.top - safe.bottom).toFloat()
        } else {
            val width = display.maximumWindowWidthDp
            val height = display.maximumWindowHeightDp
            if (width != null && height != null) minOf(width, height) * density.density else 0f
        }
    }
    BoxWithConstraints(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        val viewport = remember(constraints.maxWidth, constraints.maxHeight, density.density, reference) {
            resolveDanmakuViewport(constraints.maxWidth, constraints.maxHeight, density.density, reference)
        }
        if (viewport != null) {
            Box(Modifier.size(with(density) { viewport.widthPx.toDp() },
                with(density) { viewport.heightPx.toDp() }), content = { content(viewport) })
        }
    }
}
