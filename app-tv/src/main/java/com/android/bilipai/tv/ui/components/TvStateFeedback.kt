package com.android.bilipai.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.android.bilipai.tv.ui.LocalTvInteractive
import com.android.bilipai.tv.ui.LocalTvReduceMotion
import com.android.bilipai.tv.ui.LocalTvSimpleEffects
import com.android.purebilibili.core.ui.BlueSnowMaidAnimation
import com.android.purebilibili.core.ui.MaidAnimation
import com.android.purebilibili.core.ui.AppSpacingTokens

/** State feedback is passive; the recovery action is the only focus target. */
@Composable
internal fun TvStateFeedback(
    message: String,
    animation: MaidAnimation?,
    actionLabel: String,
    onAction: () -> Unit,
    requester: FocusRequester,
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = true,
) {
    val interactive = LocalTvInteractive.current
    LaunchedEffect(requester, interactive) { if (interactive && requestInitialFocus) requester.requestFocus() }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val size = if (maxHeight < 360.dp) 112.dp else 180.dp
        Column(Modifier.align(Alignment.Center).padding(AppSpacingTokens.Large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Large)) {
            if (animation != null) BlueSnowMaidAnimation(animation,
                Modifier.size(size), isVisible = interactive, reducedMotion = LocalTvReduceMotion.current || LocalTvSimpleEffects.current)
            Text(message, style = MaterialTheme.typography.bodyLarge)
            TvAppButton(onAction, Modifier.focusRequester(requester)) { Text(actionLabel) }
        }
    }
}

@Composable
internal fun TvBrandFeedback(
    animation: MaidAnimation?,
    eventId: Int,
    onFinished: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (animation == null) return
    val hold = when (animation) {
        MaidAnimation.FAVORITE_SAVED, MaidAnimation.FOLLOW_SUCCESS -> 800L
        else -> 500L
    }
    Box(modifier.padding(AppSpacingTokens.Large)) {
        BlueSnowMaidAnimation(animation, Modifier.size(128.dp), replayKey = eventId,
            reducedMotion = LocalTvReduceMotion.current || LocalTvSimpleEffects.current,
            completionHoldDurationMs = hold, onFinished = { onFinished(eventId) })
    }
}
