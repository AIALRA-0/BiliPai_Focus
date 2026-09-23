package com.android.purebilibili.core.ui.performance

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalView
import androidx.metrics.performance.PerformanceMetricsState
import androidx.metrics.performance.PerformanceMetricsState.Holder
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberMetricsStateHolder(): Holder {
    val localView = LocalView.current
    return remember(localView) {
        PerformanceMetricsState.getHolderForHierarchy(localView)
    }
}

@Composable
fun TrackJank(
    vararg keys: Any?,
    reportMetric: suspend CoroutineScope.(state: Holder) -> Unit
) {
    val metrics = rememberMetricsStateHolder()
    LaunchedEffect(metrics, *keys) {
        reportMetric(metrics)
    }
}

@Composable
fun TrackScrollJank(
    scrollableState: ScrollableState,
    stateName: String
) {
    TrackJank(scrollableState, stateName) { metricsHolder ->
        snapshotFlow { scrollableState.isScrollInProgress }
            .collect { isScrollInProgress ->
                metricsHolder.state?.apply {
                    if (isScrollInProgress) {
                        putState(stateName, "Scrolling=true")
                    } else {
                        removeState(stateName)
                    }
                }
            }
    }
}

@Composable
fun TrackJankStateValue(
    stateName: String,
    stateValue: String?
) {
    val metrics = rememberMetricsStateHolder()
    SideEffect {
        metrics.state?.apply {
            if (stateValue.isNullOrBlank()) {
                removeState(stateName)
            } else {
                putState(stateName, stateValue)
            }
        }
    }
    DisposableEffect(metrics, stateName) {
        onDispose {
            metrics.state?.removeState(stateName)
        }
    }
}

@Composable
fun TrackJankStateFlag(
    stateName: String,
    isActive: Boolean,
    activeValue: String = "true"
) {
    TrackJankStateValue(
        stateName = stateName,
        stateValue = if (isActive) activeValue else null
    )
}
