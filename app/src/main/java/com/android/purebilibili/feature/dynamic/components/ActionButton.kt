// 文件路径: feature/dynamic/components/ActionButton.kt
package com.android.purebilibili.feature.dynamic.components
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText

import com.android.purebilibili.core.ui.AppChromeSizeTokens
import com.android.purebilibili.core.ui.AppSpacingTokens

import com.android.purebilibili.core.ui.motion.AppMotionTokens
import com.android.purebilibili.feature.dynamic.DynamicActionButtonPalette
import com.android.purebilibili.feature.dynamic.DynamicStatusPalette
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import com.android.purebilibili.core.ui.rememberAppLikeFilledIcon
import com.android.purebilibili.core.ui.rememberAppLikeIcon
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.feature.dynamic.resolveDynamicActionButtonText
import androidx.compose.foundation.isSystemInDarkTheme
/**
 *  iOS 风格操作按钮 - 现代化胶囊设计
 * 
 * @param icon 图标
 * @param count 数量
 * @param label 标签（点赞/评论/转发）
 * @param isActive 是否激活状态（如已点赞）
 * @param onClick 点击回调
 */
@Composable
fun ActionButton(
    count: Int,
    label: String,
    isActive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    activeColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val isLike = label == "点赞"
    val isForward = label == "转发"
    val isComment = label == "评论"
    
    //  统一中性操作按钮颜色 - 避免主题色入侵卡片底部操作区
    val neutralContentColor = DynamicActionButtonPalette.content(isDark)
    val buttonColor = when {
        !enabled -> DynamicActionButtonPalette.disabledContent(isDark)
        isLike && isActive -> DynamicStatusPalette.liked()
        else -> neutralContentColor
    }
    val containerBgColor = DynamicActionButtonPalette.container(isDark)
    val disabledContainerBgColor = DynamicActionButtonPalette.disabledContainer(isDark)
    val isMiuixTheme = LocalAppUiStyle.current == AppUiStyle.MIUIX
    
    //  iOS 风格按压动画
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "actionButtonScale"
    )
    
    //  优雅的图标 - 根据状态切换填充/描边
    val buttonIcon = when {
        isLike && isActive -> rememberAppLikeFilledIcon()
        isLike -> rememberAppLikeIcon()
        // 动态底栏采用 PiliPlus 的“方框箭头 / 方框气泡”线性图标，而不是节点式 Share。
        isForward -> Icons.Outlined.IosShare
        isComment -> Icons.Outlined.Sms
        else -> Icons.Outlined.Sms
    }
    val countFadeAnimationSpec = AppMotionTokens.standardSpec<Float>()
    val countSlideAnimationSpec = AppMotionTokens.standardSpec<IntOffset>()
    Box(modifier = modifier) {
        val actionText = remember(label, count) {
            resolveDynamicActionButtonText(
                label = label,
                count = count
            )
        }

        AppButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().then(
                if (isMiuixTheme) {
                    Modifier
                } else {
                    Modifier
                        .heightIn(min = AppChromeSizeTokens.MinimumTouchTarget)
                        .scale(scale)
                }
            ),
            contentPadding = if (isMiuixTheme) {
                PaddingValues(
                    horizontal = AppSpacingTokens.Small,
                    vertical = AppSpacingTokens.Medium,
                )
            } else {
                PaddingValues(
                    horizontal = AppSpacingTokens.Small + AppSpacingTokens.Micro,
                    vertical = AppSpacingTokens.Small,
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isMiuixTheme) containerBgColor else Color.Transparent,
                contentColor = buttonColor,
                disabledContainerColor = if (isMiuixTheme) disabledContainerBgColor else Color.Transparent,
                disabledContentColor = if (isMiuixTheme) {
                    DynamicActionButtonPalette.disabledContent(isDark)
                } else {
                    buttonColor
                },
            ),
        ) {
            AppIcon(
                imageVector = buttonIcon,
                contentDescription = label,
                modifier = Modifier.size(AppSpacingTokens.Large + AppSpacingTokens.Micro),
                tint = buttonColor,
            )
            DynamicNativeActionText(
                actionText = actionText,
                countFadeAnimationSpec = countFadeAnimationSpec,
                countSlideAnimationSpec = countSlideAnimationSpec,
                spacing = AppSpacingTokens.ExtraSmall,
            )
        }
    }
}

@Composable
private fun DynamicNativeActionText(
    actionText: String?,
    countFadeAnimationSpec: androidx.compose.animation.core.FiniteAnimationSpec<Float>,
    countSlideAnimationSpec: androidx.compose.animation.core.FiniteAnimationSpec<IntOffset>,
    spacing: androidx.compose.ui.unit.Dp
) {
    if (actionText == null) return
    Spacer(modifier = Modifier.width(spacing))
    AnimatedContent(
        targetState = actionText,
        transitionSpec = {
            (fadeIn(animationSpec = countFadeAnimationSpec) +
                slideInVertically(animationSpec = countSlideAnimationSpec) { it / 3 })
                .togetherWith(
                    fadeOut(animationSpec = countFadeAnimationSpec) +
                        slideOutVertically(animationSpec = countSlideAnimationSpec) { -it / 3 }
                )
        },
        label = "nativeActionButtonCount"
    ) { text ->
        AppText(
            text = text,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
