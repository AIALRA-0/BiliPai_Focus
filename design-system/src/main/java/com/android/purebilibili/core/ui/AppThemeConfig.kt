package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.blur.BlurIntensity

/**
 * UI 组件运行时需要的应用级配置。
 *
 * 数据读取由应用外壳负责，core/ui 只消费已解析的值，避免反向依赖设置存储。
 */
data class AppThemeConfig(
    val blurIntensity: BlurIntensity = BlurIntensity.THIN,
    val headerBlurEnabled: Boolean = true,
    val bottomBarBlurEnabled: Boolean = false,
    val progressiveTopBlurEnabled: Boolean = true,
    val progressiveTopFadeEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val globalTextTapCopyEnabled: Boolean = false,
    val uiEntranceAnimationEnabled: Boolean = true,
    val runtimeVisualGuardEnabled: Boolean = true,
    val nativeMiuixPopupsEnabled: Boolean = true,
    // Matches the persisted default; each application host supplies the observed preference.
    val liquidGlassEnabled: Boolean = false,
)

fun isMiuixNonGlassEnabled(uiStyle: AppUiStyle, liquidGlassEnabled: Boolean): Boolean =
    uiStyle == AppUiStyle.MIUIX && !liquidGlassEnabled

@Composable
@ReadOnlyComposable
fun isMiuixNonGlassEnabled(): Boolean = isMiuixNonGlassEnabled(
    LocalAppUiStyle.current,
    LocalAppThemeConfig.current.liquidGlassEnabled,
)

enum class AppChromeMaterial { SOLID, BLUR, LIQUID_GLASS }

/** Callers resolve hardware/runtime capability before requesting glass or blur. */
fun resolveAppChromeMaterial(
    liquidGlassEnabled: Boolean,
    blurEnabled: Boolean,
    blurAvailable: Boolean,
): AppChromeMaterial = when {
    liquidGlassEnabled -> AppChromeMaterial.LIQUID_GLASS
    blurEnabled && blurAvailable -> AppChromeMaterial.BLUR
    else -> AppChromeMaterial.SOLID
}

val LocalAppThemeConfig = staticCompositionLocalOf { AppThemeConfig() }

@Composable
fun ProvideAppThemeConfig(
    config: AppThemeConfig,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalAppThemeConfig provides config, content = content)
}
