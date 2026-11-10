package com.android.bilipai.tv.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.android.purebilibili.core.ui.motion.AppMotionEasing

internal object TvMotion {
    const val focusMs = 120
    const val pageMs = 180
    const val enterMs = 180
    const val exitMs = 120
    const val backdropMs = 240
}

/** Outgoing content may remain drawn, but must stop participating in navigation immediately. */
@Composable
internal fun TvVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reduce = LocalTvReduceMotion.current
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(if (reduce) 0 else TvMotion.enterMs, easing = AppMotionEasing.Continuity)),
        exit = fadeOut(tween(if (reduce) 0 else TvMotion.exitMs, easing = AppMotionEasing.Continuity)),
    ) {
        CompositionLocalProvider(LocalTvInteractive provides visible) {
        Box(if (visible) Modifier else Modifier
            .focusProperties { canFocus = false }
            .onPreviewKeyEvent { true }
            .clearAndSetSemantics { }) { content() }
        }
    }
}
