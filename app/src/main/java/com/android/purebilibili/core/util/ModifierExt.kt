package com.android.purebilibili.core.util

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.android.purebilibili.core.ui.skeleton.skeletonBackground
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle

/**
 * 骨架屏闪光特效 Modifier（深浅色主题自适应）。
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val color = com.android.purebilibili.core.ui.skeleton.rememberContentSkeletonBlockColor(
        com.android.purebilibili.core.ui.skeleton.rememberContentSkeletonPulseState(),
    )
    this.skeletonBackground(color)
}

// =============================================================================
//  [问题2修复] 防抖点击 - 防止快速点击导致双重导航
// =============================================================================

/**
 *  防抖点击 Modifier
 * 
 * 防止快速连续点击导致多次导航，用于解决以下问题：
 * - 快速点击动态会出现两个二级页面
 * - 网络延迟时重复点击导致多次请求
 * 
 * @param debounceTime 防抖时间间隔（毫秒），默认 500ms
 * @param onClick 点击回调
 */
fun Modifier.debounceClickable(
    debounceTime: Long = 500L,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    
    this.clickable(enabled = enabled) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > debounceTime) {
            lastClickTime = currentTime
            onClick()
        }
    }
}

/**
 *  防抖点击函数（用于非 Modifier 场景）
 * 
 * @param debounceTime 防抖时间间隔（毫秒），默认 500ms
 * @param action 要执行的操作
 * @return 包装后的防抖函数
 */
@Composable
fun rememberDebounceClick(
    debounceTime: Long = 500L,
    action: () -> Unit
): () -> Unit {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    
    return {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > debounceTime) {
            lastClickTime = currentTime
            action()
        }
    }
}

/**
 *  防抖回调函数（用于带参数的回调）
 * 
 * @param debounceTime 防抖时间间隔（毫秒），默认 500ms
 * @param action 要执行的操作
 * @return 包装后的防抖函数
 */
@Composable
fun <T> rememberDebounceCallback(
    debounceTime: Long = 500L,
    action: (T) -> Unit
): (T) -> Unit {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    
    return { param: T ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > debounceTime) {
            lastClickTime = currentTime
            action(param)
        }
    }
}

/**
 * 视频网格骨架（历史列表等）。
 * 使用与首页一致的绘制阶段扫光，保留封面尺寸。
 */
@Composable
fun VideoGridItemSkeleton(coverAspectRatio: Float = 4f / 3f) {
    val pulse = com.android.purebilibili.core.ui.skeleton.rememberContentSkeletonPulseState()
    val blockColor = com.android.purebilibili.core.ui.skeleton.rememberContentSkeletonBlockColor(pulse)
    com.android.purebilibili.core.ui.skeleton.ContentVideoGridItemSkeleton(
        coverAspectRatio = coverAspectRatio,
        blockColorProvider = blockColor,
    )
}

// =============================================================================
//  Android 特有功能：触觉反馈 + 弹性点击
// =============================================================================

/**
 *  触觉反馈类型枚举
 */
enum class HapticType {
    LIGHT,      // 轻触 (选择/切换)
    MEDIUM,     // 中等 (确认)
    HEAVY,      // 重击 (警告/删除)
    GESTURE_STEP, // 亮度/音量等离散调节刻度
    SELECTION   // 选择变化
}

/**
 *  触发触觉反馈
 * 
 * - Android 12+: 使用新的 GESTURE_START/END 等常量
 * - 旧版本: 使用 LONG_PRESS/KEYBOARD_TAP 等
 */
@Composable
fun rememberHapticFeedback(
    scene: com.android.purebilibili.core.ui.AppHapticScene = com.android.purebilibili.core.ui.AppHapticScene.CONTROLS,
): (HapticType) -> Unit {
    val view = LocalView.current
    val config by rememberUpdatedState(com.android.purebilibili.core.ui.LocalAppThemeConfig.current)
    return remember(view, scene) {
        { type: HapticType ->
            val feedbackConstant = when (type) {
                HapticType.LIGHT -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        HapticFeedbackConstants.CONFIRM
                    } else {
                        HapticFeedbackConstants.KEYBOARD_TAP
                    }
                }
                HapticType.MEDIUM -> HapticFeedbackConstants.LONG_PRESS
                HapticType.HEAVY -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        HapticFeedbackConstants.REJECT
                    } else {
                        HapticFeedbackConstants.LONG_PRESS
                    }
                }
                HapticType.GESTURE_STEP -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        HapticFeedbackConstants.SEGMENT_TICK
                    } else {
                        HapticFeedbackConstants.CLOCK_TICK
                    }
                }
                HapticType.SELECTION -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
                    } else {
                        HapticFeedbackConstants.CLOCK_TICK
                    }
                }
            }
            // [新增] 全局触感反馈开关检查
            if (config.hapticFeedbackEnabled && config.hapticSettings.isEnabled(scene)) {
                com.android.purebilibili.core.ui.AppActionHaptics.dispatch {
                    view.performHapticFeedback(feedbackConstant)
                }
            } else {
                com.android.purebilibili.core.ui.AppActionHaptics.suppressCurrentActionFeedback()
            }
        }
    }
}

/**
 *  弹性点击 Modifier (带缩放动画 + 触觉反馈)
 * 
 * Android 特有的交互体验：
 * - 按压时缩放到 0.95
 * - 弹性回弹动画
 * - 自动触觉反馈
 */
fun Modifier.bouncyClickable(
    hapticType: HapticType = HapticType.LIGHT,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = rememberHapticFeedback()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounce_scale"
    )
    
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled
        ) {
            com.android.purebilibili.core.ui.AppActionHaptics.runAction {
                onClick()
                haptic(hapticType)
            }
        }
}

/**
 *  带涟漪效果的触觉点击 (Material 3 风格)
 */
fun Modifier.hapticClickable(
    hapticType: HapticType = HapticType.LIGHT,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val haptic = rememberHapticFeedback()
    
    this.clickable(enabled = enabled) {
        com.android.purebilibili.core.ui.AppActionHaptics.runAction {
            onClick()
            haptic(hapticType)
        }
    }
}

/**
 *  iOS 风格点击效果 Modifier
 * 
 * 特性：
 * - 按压时缩放到 0.96f (iOS 默认值)
 * - 弹性回弹动画 (damping=0.6f)
 * - 自动触发轻量触觉反馈
 * 
 * @param scale 按压时的缩放比例，默认 0.96f
 * @param hapticEnabled 是否启用触觉反馈
 * @param onClick 点击回调
 */
fun Modifier.iOSTapEffect(
    scale: Float = 0.96f,
    hapticEnabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val uiStyle = LocalAppUiStyle.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = rememberHapticFeedback()
    val targetScale = if (isPressed) {
        if (uiStyle == AppUiStyle.MATERIAL3) 0.985f else scale
    } else {
        1f
    }
    
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = if (uiStyle == AppUiStyle.MATERIAL3) 0.9f else 0.6f,
            stiffness = if (uiStyle == AppUiStyle.MATERIAL3) 650f else 400f
        ),
        label = "ios_tap_scale"
    )
    
    this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null
        ) {
            com.android.purebilibili.core.ui.AppActionHaptics.runAction {
                onClick()
                if (hapticEnabled) {
                    haptic(HapticType.LIGHT)
                }
            }
        }
}

/**
 *  iOS 风格点击效果 (仅动画，不处理点击事件)
 * 
 * 用于需要自定义点击处理的场景
 */
fun Modifier.iOSTapScale(
    scale: Float = 0.96f
): Modifier = composed {
    val uiStyle = LocalAppUiStyle.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) {
            if (uiStyle == AppUiStyle.MATERIAL3) 0.985f else scale
        } else {
            1f
        },
        animationSpec = spring(
            dampingRatio = if (uiStyle == AppUiStyle.MATERIAL3) 0.9f else 0.6f,
            stiffness = if (uiStyle == AppUiStyle.MATERIAL3) 650f else 400f
        ),
        label = "ios_tap_scale_only"
    )
    
    this.graphicsLayer {
        scaleX = animatedScale
        scaleY = animatedScale
    }
}

/**
 *  iOS 风格卡片点击效果 Modifier（增强版）
 * 
 * 特性：
 * - 按压时：缩放 + 轻微下沉 + 透明度微调
 * - 释放时：弹性回弹 + 过冲效果
 * - 符合物理规律的动画曲线
 * 
 * @param pressScale 按压时的缩放比例，默认 0.96f
 * @param pressTranslationY 按压时的下沉距离，默认 4dp
 * @param hapticEnabled 是否启用触觉反馈
 * @param onClick 点击回调
 */
fun Modifier.iOSCardTapEffect(
    pressScale: Float = 0.96f,
    pressTranslationY: Float = 8f,
    hapticEnabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val uiStyle = LocalAppUiStyle.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = rememberHapticFeedback()
    val targetScale = if (isPressed) {
        if (uiStyle == AppUiStyle.MATERIAL3) 0.985f else pressScale
    } else {
        1f
    }
    val targetTranslation = if (isPressed) {
        if (uiStyle == AppUiStyle.MATERIAL3) 2f else pressTranslationY
    } else {
        0f
    }
    
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = if (uiStyle == AppUiStyle.MATERIAL3) 0.92f else if (isPressed) 0.75f else 0.55f,
            stiffness = if (uiStyle == AppUiStyle.MATERIAL3) 700f else if (isPressed) 600f else 300f
        ),
        label = "card_tap_scale"
    )
    
    val animatedTranslationY by animateFloatAsState(
        targetValue = targetTranslation,
        animationSpec = spring(
            dampingRatio = if (uiStyle == AppUiStyle.MATERIAL3) 0.95f else if (isPressed) 0.85f else 0.5f,
            stiffness = if (uiStyle == AppUiStyle.MATERIAL3) 850f else if (isPressed) 800f else 250f
        ),
        label = "card_tap_translationY"
    )
    
    this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
            translationY = animatedTranslationY
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null
        ) {
            com.android.purebilibili.core.ui.AppActionHaptics.runAction {
                onClick()
                if (hapticEnabled) {
                    haptic(HapticType.LIGHT)
                }
            }
        }
}
