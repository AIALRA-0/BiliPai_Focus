package com.android.purebilibili.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.core.ui.LocalComponentMotionEnabled
import com.android.purebilibili.core.ui.LocalSystemReduceMotion
import com.android.purebilibili.core.ui.motion.adaptiveMiuixPressFeedback

/** 自绘小入口的轻按压：玻璃形变独立启用，原生路径保留各自的点击指示。 */
@Composable
fun Modifier.appGentleActionClickable(
    shape: Shape,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val motionEnabled = LocalComponentMotionEnabled.current && !LocalSystemReduceMotion.current
    val glass = LocalAppThemeConfig.current.liquidGlassEnabled
    val material = LocalAppUiStyle.current == AppUiStyle.MATERIAL3
    val pressed = interactionSource.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (enabled && motionEnabled && !glass && material && pressed.value) 0.98f else 1f,
        animationSpec = if (motionEnabled) spring(dampingRatio = 0.8f, stiffness = 700f) else snap(),
        label = "gentleActionPress",
    )
    // 仅玻璃路径使用指尖高光与拖动形变，不将玻璃参数传给原生控件。
    val elasticState = rememberAppElasticPressState(feedbackStrength = 0.45f)
    return Modifier
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .adaptiveMiuixPressFeedback(
            enabled = enabled && motionEnabled,
            interactionSource = interactionSource,
        )
        .appElasticPress(
            enabled = enabled && motionEnabled && glass,
            state = elasticState,
            highlightShape = shape,
        )
        .then(this)
        .clickable(
            interactionSource = interactionSource,
            indication = if (glass) null else LocalIndication.current,
            enabled = enabled,
            role = Role.Button,
            onClick = appActionWithFeedback(onClick),
        )
}
