package com.android.purebilibili.feature.video.ui.components

import android.os.Build
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.components.AppText
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val CircularPercentEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

@Composable
internal fun CircularGesturePercentText(
    percent: Int,
    color: Color,
    textStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    val target = percent.coerceIn(0, 100)
    val displayedPercent = remember { Animatable(target.toFloat()) }
    val blur = remember { Animatable(0f) }
    val motionTracker = remember { CircularGesturePercentMotionTracker() }
    val numeralStyle = textStyle.copy(fontFeatureSettings = "tnum")

    LaunchedEffect(target) {
        val motion = motionTracker.update(target, SystemClock.uptimeMillis())
            ?: return@LaunchedEffect
        // Retarget from the current display value; cancellation never queues old numbers.
        launch {
            blur.snapTo(motion.blurRadiusDp)
            blur.animateTo(0f, tween(motion.durationMillis, easing = CircularPercentEasing))
        }
        displayedPercent.animateTo(
            target.toFloat(),
            tween(motion.durationMillis, easing = CircularPercentEasing)
        )
    }

    Box(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = "$target%" }
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        // Reserve the maximum label width, including font scaling, to keep the
        // circle's center stable when crossing 9/10 or 99/100. No extra text layer animates.
        AppText(
            text = "100%",
            color = Color.Transparent,
            style = numeralStyle,
            maxLines = 1,
            tapToCopyEnabled = false
        )
        AppText(
            text = "${displayedPercent.value.roundToInt().coerceIn(0, 100)}%",
            color = color,
            style = numeralStyle,
            maxLines = 1,
            tapToCopyEnabled = false,
            modifier = Modifier.graphicsLayer {
                val radius = blur.value.dp.toPx()
                renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && radius > 0.01f) {
                    BlurEffect(radius, radius, TileMode.Decal)
                } else {
                    null
                }
            }
        )
    }
}
