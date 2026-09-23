package com.android.purebilibili.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.util.WindowWidthSizeClass
import kotlinx.coroutines.launch
import kotlin.math.abs

/** 双指向外扩 (放大视图，减少列数) 的单档阈值 */
internal const val PINCH_ZOOM_IN_FACTOR = 1.25f

/** 双指向内捏 (缩小视图，增加列数) 的单档阈值 (1 / 1.25 = 0.80) */
internal const val PINCH_ZOOM_OUT_FACTOR = 0.80f

/** 手指轻微抖动过滤阈值，避免误触发手势 */
internal const val PINCH_ACTIVATION_THRESHOLD = 0.05f

/**
 * 根据屏幕尺寸档位与内容宽度解析双指捏合允许调节的网格列数上下限。
 */
internal fun resolveHomeFeedPinchColumnBounds(
    widthSizeClass: WindowWidthSizeClass,
    contentWidthDp: Int,
    displayMode: Int = 0,
): IntRange {
    if (displayMode == 1) {
        // 单列沉浸模式固定 1 列
        return 1..1
    }
    val minColumns = if (contentWidthDp >= 900) 2 else 1
    val maxColumns = when {
        contentWidthDp >= 1200 -> 7
        contentWidthDp >= 840 -> 6
        contentWidthDp >= 600 -> 5
        contentWidthDp >= 380 -> 4
        else -> 3
    }
    return minColumns..maxColumns
}

/**
 * 计算单步缩放后的目标列数与重置后的累积缩放比例。
 *
 * @param currentColumns 当前已生效的列数
 * @param cumulativeZoom 当前手势周期内累积的缩放比率
 * @param bounds 允许调节的最小/最大列数范围
 * @return Pair(新目标列数, 重置或钳位后的累积缩放值)
 */
internal fun calculatePinchStepColumns(
    currentColumns: Int,
    cumulativeZoom: Float,
    bounds: IntRange,
): Pair<Int, Float> {
    if (cumulativeZoom >= PINCH_ZOOM_IN_FACTOR) {
        // 向外撑开：列数减少 (卡片变大)
        val target = (currentColumns - 1).coerceIn(bounds)
        return Pair(target, 1.0f)
    }
    if (cumulativeZoom <= PINCH_ZOOM_OUT_FACTOR) {
        // 向内捏合：列数增加 (卡片变小)
        val target = (currentColumns + 1).coerceIn(bounds)
        return Pair(target, 1.0f)
    }
    // 未越过换档阈值，保持当前列数，累积比率保留 (防止极端累积)
    return Pair(currentColumns, cumulativeZoom.coerceIn(0.5f, 2.0f))
}

/**
 * 计算手势过程中的实时直接操纵（Direct Manipulation）视觉缩放比例。
 * 当处于列数临界极值（最小/最大列）时施加物理阻尼，提供真实的边界张力感。
 */
internal fun calculatePinchVisualScale(
    cumulativeZoom: Float,
    currentColumns: Int,
    bounds: IntRange,
): Float {
    return if (cumulativeZoom >= 1.0f) {
        if (currentColumns <= bounds.first) {
            // 已达最小列数（单列/最大卡片），向外撑开施加橡皮筋阻尼
            (1.0f + (cumulativeZoom - 1.0f) * 0.25f).coerceIn(1.0f, 1.15f)
        } else {
            cumulativeZoom.coerceIn(0.70f, 1.35f)
        }
    } else {
        if (currentColumns >= bounds.last) {
            // 已达最大列数（最紧凑网格），向内捏合施加橡皮筋阻尼
            (1.0f - (1.0f - cumulativeZoom) * 0.25f).coerceIn(0.85f, 1.0f)
        } else {
            cumulativeZoom.coerceIn(0.70f, 1.35f)
        }
    }
}

/**
 * 首页/网格双指缩放调整列数修饰符（流体过渡动画版）。
 *
 * 特性：
 * 1. 单指操作 (上下滚动、水平划切 Tab、点击卡片) 100% 放行，零干扰；
 * 2. 连续物理微缩放：手势操作中整个网格跟随双指距离实时缩放（Direct Manipulation），在 GPU 硬件图层中呈现 120Hz 视口流动感；
 * 3. 跨档平滑换接：越过阈值跨越列数瞬间，触发刻度震动，缩放比例通过 Spring 弹簧曲线快速归位，与卡片重排布局无缝交接；
 * 4. 抬手弹性回正：手指离屏瞬间，视口由弹簧物理惯性弹性吸附回正（1.0f），无任何突兀跳变。
 */
@Composable
internal fun Modifier.homeFeedPinchZoom(
    enabled: Boolean = true,
    currentColumns: Int,
    bounds: IntRange,
    onColumnsChange: (Int) -> Unit,
    onGestureEnd: (Int) -> Unit,
): Modifier {
    if (!enabled) return this

    val currentColumnsState = rememberUpdatedState(currentColumns)
    val boundsState = rememberUpdatedState(bounds)
    val onColumnsChangeState = rememberUpdatedState(onColumnsChange)
    val onGestureEndState = rememberUpdatedState(onGestureEnd)

    val liveScale = remember { Animatable(1.0f) }
    val coroutineScope = rememberCoroutineScope()

    return this
        .graphicsLayer {
            scaleX = liveScale.value
            scaleY = liveScale.value
            transformOrigin = TransformOrigin.Center
        }
        .pointerInput(enabled) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                var cumulativeZoom = 1.0f
                var isPinchActive = false
                var committedColumns = currentColumnsState.value

                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val pressedPointers = event.changes.filter { it.pressed }

                    if (pressedPointers.size >= 2) {
                        val zoomChange = event.calculateZoom()
                        cumulativeZoom *= zoomChange

                        // 检查是否越过轻微抖动门槛
                        if (!isPinchActive && abs(cumulativeZoom - 1.0f) > PINCH_ACTIVATION_THRESHOLD) {
                            isPinchActive = true
                            committedColumns = currentColumnsState.value
                        }

                        if (isPinchActive) {
                            // 消费所有多指事件，阻止子级 LazyGrid 或外层 Pager 接收到滑动/点击
                            event.changes.forEach { it.consume() }

                            // 实时跟随双指距离直接变换缩放比例
                            val visualScale = calculatePinchVisualScale(
                                cumulativeZoom = cumulativeZoom,
                                currentColumns = committedColumns,
                                bounds = boundsState.value,
                            )
                            coroutineScope.launch {
                                liveScale.snapTo(visualScale)
                            }

                            val (nextColumns, resetZoom) = calculatePinchStepColumns(
                                currentColumns = committedColumns,
                                cumulativeZoom = cumulativeZoom,
                                bounds = boundsState.value,
                            )
                            cumulativeZoom = resetZoom

                            if (nextColumns != committedColumns) {
                                committedColumns = nextColumns
                                onColumnsChangeState.value(nextColumns)
                                // 换档瞬间：弹簧阻尼快速平滑归正至 1.0f，与重排的新列数布局无缝交接
                                coroutineScope.launch {
                                    liveScale.animateTo(
                                        targetValue = 1.0f,
                                        animationSpec = spring(
                                            dampingRatio = 0.8f,
                                            stiffness = Spring.StiffnessMediumLow,
                                        ),
                                    )
                                }
                            }
                        }
                    } else if (isPinchActive) {
                        // 当处于双指缩放中途某一指先抬起时，继续消费剩余手指事件，
                        // 直至全部手指完全离开屏幕，防止单指残留导致列表剧烈滑动
                        event.changes.forEach { it.consume() }
                    }
                } while (event.changes.any { it.pressed })

                if (isPinchActive) {
                    onGestureEndState.value(committedColumns)
                    // 手指完全离屏：弹性回弹归位至 1.0f
                    coroutineScope.launch {
                        liveScale.animateTo(
                            targetValue = 1.0f,
                            animationSpec = spring(
                                dampingRatio = 0.75f,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        )
                    }
                }
            }
        }
}

/**
 * 双指缩放网格列数提示 HUD 胶囊组件。
 * 遵循应用设计规范：在 Material 3 主题下使用原生 MD3 组件（Surface + CircleShape）；在 MIUIX 主题下使用 MIUIX Squircle 容器。
 */
@Composable
internal fun GridPinchColumnHudPill(
    visible: Boolean,
    columns: Int,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(150)) + scaleIn(
            initialScale = 0.85f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ),
        exit = fadeOut(animationSpec = tween(200)) + scaleOut(
            targetScale = 0.85f,
            animationSpec = tween(200)
        ),
        modifier = modifier.zIndex(92f)
    ) {
        when (LocalAppUiStyle.current) {
            AppUiStyle.MATERIAL3 -> {
                // Material 3 原生 HUD 药丸：MD3 Surface + CircleShape + MD3 语义配色与排版
                androidx.compose.material3.Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.90f),
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                ) {
                    androidx.compose.material3.Text(
                        text = "${columns} 列网格",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            AppUiStyle.MIUIX -> {
                // MIUIX 风格 HUD 胶囊：Miuix 连续曲率 Squircle 药丸容器
                AppSurface(
                    shape = AppShapes.container(ContainerLevel.Pill),
                    color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f),
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                ) {
                    AppText(
                        text = "${columns} 列网格",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
