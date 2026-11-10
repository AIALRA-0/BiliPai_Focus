package com.android.purebilibili.feature.video.ui.feedback

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.android.purebilibili.core.ui.motion.AppMotionEasing
import com.android.purebilibili.core.ui.LocalSystemReduceMotion
import com.android.purebilibili.core.ui.components.AppText

/** Arm only the count belonging to an explicit local action; hydration never arms it. */
@Stable
internal class VideoActionCountRequests {
    var like by mutableIntStateOf(0)
        private set
    var coin by mutableIntStateOf(0)
        private set
    var favorite by mutableIntStateOf(0)
        private set

    fun requestLike() { like++ }
    fun requestCoin() { coin++ }
    fun requestFavorite() { favorite++ }
    fun requestTriple() {
        requestLike()
        requestCoin()
        requestFavorite()
    }
}

internal fun shouldAnimateVideoActionCountChange(
    before: String,
    after: String,
    request: Int,
    consumedRequest: Int,
    reducedMotion: Boolean,
): Boolean = !reducedMotion && before != after && request != 0 && request != consumedRequest &&
    (before.any(Char::isDigit) || after.any(Char::isDigit))

private data class CountFrame(val text: String, val animate: Boolean = false)

@Composable
internal fun VideoActionCountText(
    text: String,
    request: Int,
    color: Color,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
    identity: Any? = Unit,
    style: TextStyle = MaterialTheme.typography.labelSmall,
    countValue: Int? = null,
) {
    val reducedMotion = LocalSystemReduceMotion.current
    var frame by remember(identity) { mutableStateOf(CountFrame(text)) }
    var consumedRequest by remember(identity) { mutableIntStateOf(request) }
    var previousCount by remember(identity) { mutableStateOf(countValue) }
    LaunchedEffect(identity, text, request, countValue) {
        if (frame.text != text || previousCount != countValue) {
            frame = CountFrame(
                text,
                shouldAnimateVideoActionCountChange(frame.text, text, request, consumedRequest, reducedMotion),
            )
            // Consume even if rounding keeps the formatted value at e.g. "1.2万".
            consumedRequest = request
            previousCount = countValue
        }
    }
    AnimatedContent(
        targetState = frame,
        contentKey = { it.text },
        modifier = modifier,
        contentAlignment = Alignment.Center,
        transitionSpec = {
            if (targetState.animate && !reducedMotion) {
                ((fadeIn(tween(160)) + slideInVertically(tween(180, easing = AppMotionEasing.M1Standard)) { it / 4 })
                    togetherWith (fadeOut(tween(100)) + slideOutVertically(tween(120)) { -it / 4 }))
                    .using(null)
            } else {
                (EnterTransition.None togetherWith ExitTransition.None).using(null)
            }
        },
        label = "videoActionCount",
    ) { target ->
        AppText(
            text = target.text,
            style = style,
            color = color,
            fontWeight = fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
        )
    }
}
