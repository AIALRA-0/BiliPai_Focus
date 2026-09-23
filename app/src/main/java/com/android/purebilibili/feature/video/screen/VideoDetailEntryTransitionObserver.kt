package com.android.purebilibili.feature.video.screen

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import com.android.purebilibili.core.ui.transition.MiuixVideoCardTransitionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun rememberVideoDetailEntryTransitionFinished(
    deferLoad: Boolean,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    fallbackDurationMillis: Int,
    heroDriver: MiuixVideoCardTransitionState? = null,
): Boolean {
    if (!deferLoad) return true

    var finished by remember(deferLoad) { mutableStateOf(false) }
    val fallbackTimeoutMillis = remember(fallbackDurationMillis) {
        resolveVideoDetailEntryTransitionFallbackTimeoutMillis(fallbackDurationMillis)
    }

    LaunchedEffect(deferLoad, sharedTransitionScope, animatedVisibilityScope, fallbackTimeoutMillis, heroDriver) {
        if (!deferLoad) {
            finished = true
            return@LaunchedEffect
        }

        // 相关推荐返回会再次触发 shared/nav transition；若把 finished 打回 false，
        // 父详情简介/相关列表会被 AnimatedVisibility 整块卸掉，表现为播放器下方黑屏重载。
        if (finished) {
            return@LaunchedEffect
        }
        if (heroDriver != null) {
            snapshotFlow { heroDriver.progressProvider() >= 0.999f &&
                !heroDriver.isGestureInProgressProvider() }.first { it }
            finished = true
            return@LaunchedEffect
        }

        var hasObservedActiveTransition = false

        val timeoutJob = launch {
            kotlinx.coroutines.delay(fallbackTimeoutMillis.toLong())
            finished = true
        }

        try {
            snapshotFlow {
                val sharedActive = sharedTransitionScope?.isTransitionActive ?: false
                val navRunning = animatedVisibilityScope?.transition?.isRunning ?: false
                sharedActive to navRunning
            }
                .distinctUntilChanged()
                .collect { (sharedActive, navRunning) ->
                    if (sharedActive || navRunning) {
                        hasObservedActiveTransition = true
                    }
                    if (
                        shouldMarkVideoDetailEntryTransitionFinished(
                            hasObservedActiveTransition = hasObservedActiveTransition,
                            isSharedTransitionActive = sharedActive,
                            isNavEnterTransitionRunning = navRunning,
                        )
                    ) {
                        finished = true
                        timeoutJob.cancel()
                    }
                }
        } finally {
            timeoutJob.cancel()
        }
    }

    return finished
}

/**
 * 冷启动不必等整个 shared morph 完成才请求媒体：在后段开始解码，实际画面仍由
 * [VideoPlayerSection] 的首帧揭开策略接管封面，所以慢网不会露黑帧。
 */
@Composable
internal fun rememberVideoDetailEntryPlaybackReady(
    deferLoad: Boolean,
    morphDurationMillis: Int,
    heroDriver: MiuixVideoCardTransitionState? = null,
): Boolean {
    if (!deferLoad) return true

    var ready by remember(deferLoad) { mutableStateOf(false) }
    val preloadDelayMillis = remember(morphDurationMillis) {
        resolveVideoDetailEntryPlaybackPreloadDelayMillis(morphDurationMillis)
    }
    LaunchedEffect(deferLoad, preloadDelayMillis, heroDriver) {
        if (!deferLoad) {
            ready = true
            return@LaunchedEffect
        }
        if (heroDriver != null) {
            val spec = heroDriver.motionSpec
            val duration = spec?.enterDurationMillis ?: morphDurationMillis
            val fraction = resolveVideoDetailEntryPlaybackPreloadDelayMillis(duration).toFloat() /
                duration.coerceAtLeast(1)
            val threshold = spec?.enterSpatialSpec?.transform(fraction.coerceIn(0f, 1f)) ?: fraction
            snapshotFlow { heroDriver.progressProvider() >= threshold }.first { it }
        } else {
            kotlinx.coroutines.delay(preloadDelayMillis.toLong())
        }
        ready = true
    }
    return ready
}
