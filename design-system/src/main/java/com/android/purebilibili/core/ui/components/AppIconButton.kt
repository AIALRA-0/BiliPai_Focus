package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.renderer.material3.AppMaterial3IconButton
import com.android.purebilibili.core.ui.renderer.miuix.AppMiuixIconButton

@Immutable
data class AppIconButtonColors(
    val containerColor: Color = Color.Unspecified,
    val contentColor: Color = Color.Unspecified,
    val disabledContainerColor: Color = Color.Unspecified,
    val disabledContentColor: Color = Color.Unspecified,
)

object AppIconButtonDefaults {
    fun colors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
    ): AppIconButtonColors = AppIconButtonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    )
}

internal enum class AppIconButtonVariant {
    Standard,
    Filled,
}

@Composable
fun AppIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: AppIconButtonColors? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) = AppIconButtonImpl(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    colors = colors,
    interactionSource = interactionSource,
    variant = AppIconButtonVariant.Standard,
    content = content,
)

@Composable
fun AppFilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: AppIconButtonColors? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) = AppIconButtonImpl(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    colors = colors,
    interactionSource = interactionSource,
    variant = AppIconButtonVariant.Filled,
    content = content,
)

@Composable
private fun AppIconButtonImpl(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    colors: AppIconButtonColors?,
    interactionSource: MutableInteractionSource?,
    variant: AppIconButtonVariant,
    content: @Composable () -> Unit,
): Unit = AppExactTouchTargets {
    val feedbackClick = appActionWithFeedback(onClick)
    val decorator = LocalAppTopBarIconDecorator.current.takeIf {
        LocalAppTopBarIconDecorationEnabled.current
    }
    if (decorator != null) {
        androidx.compose.runtime.CompositionLocalProvider(LocalAppTopBarIconDecorationEnabled provides false) {
            decorator.Render(modifier, enabled) { innerModifier ->
                AppIconButtonImpl(onClick, innerModifier, enabled, colors, interactionSource, variant, content)
            }
        }
        return@AppExactTouchTargets
    }
    // 玻璃图标的弹性由外层 dock 提供，普通 MD3 图标保留原生涟漪与固定形状。
    val elasticFeedbackEnabled = enabled &&
        ((LocalAppUiStyle.current == AppUiStyle.MIUIX &&
            com.android.purebilibili.core.ui.LocalAppThemeConfig.current.liquidGlassEnabled) ||
            LocalAppIconButtonGlassSurfaceProvided.current)
    ProvideAppElasticFeedback(enabled = elasticFeedbackEnabled) {
        when (LocalAppUiStyle.current) {
            AppUiStyle.MATERIAL3 -> AppMaterial3IconButton(
                onClick = feedbackClick,
                modifier = modifier,
                enabled = enabled,
                colors = colors,
                interactionSource = interactionSource,
                variant = variant,
                content = content,
            )
            AppUiStyle.MIUIX -> AppMiuixIconButton(
                onClick = feedbackClick,
                modifier = modifier.appElasticPress(
                    enabled = elasticFeedbackEnabled,
                    highlightShape = androidx.compose.foundation.shape.RoundedCornerShape(
                        com.android.purebilibili.core.ui.AppChromeSizeTokens.MiuixNativeCompactCornerRadiusDp.dp,
                    ),
                ),
                enabled = enabled,
                colors = colors,
                interactionSource = interactionSource,
                variant = variant,
                content = content,
            )
        }
    }
}
