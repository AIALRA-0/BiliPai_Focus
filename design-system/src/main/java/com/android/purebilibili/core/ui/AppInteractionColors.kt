package com.android.purebilibili.core.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 根主题记录的材质边界，不受页面为使用原生组件而临时关闭玻璃的局部覆盖影响。 */
internal val LocalAppNativeInteractionColorsEnabled = staticCompositionLocalOf<Boolean?> { null }

@Composable
@ReadOnlyComposable
internal fun nativeInteractionColorsEnabled(): Boolean =
    LocalAppNativeInteractionColorsEnabled.current ?: isMiuixNonGlassEnabled()

enum class AppNavigationColorRole { TABS, BAR }

@Immutable
data class AppNavigationStateColors(
    val selectedContainer: Color,
    val selectedContent: Color,
    val unselectedContent: Color,
)

@Immutable
data class AppActionStateColors(val container: Color, val content: Color)

/** 仅统一非玻璃 Miuix 的语义。返回 null 时，调用方保留原有玻璃、MD3 和显式皮肤分支。 */
object AppInteractionColors {
    @Composable
    fun nativeNavigation(role: AppNavigationColorRole = AppNavigationColorRole.TABS): AppNavigationStateColors? {
        if (!nativeInteractionColorsEnabled()) return null
        val scheme = MiuixTheme.colorScheme
        return when (role) {
            AppNavigationColorRole.TABS -> AppNavigationStateColors(
                selectedContainer = scheme.surfaceContainer,
                selectedContent = scheme.onBackground,
                unselectedContent = scheme.onSurfaceVariantSummary,
            )
            AppNavigationColorRole.BAR -> AppNavigationStateColors(
                selectedContainer = Color.Transparent,
                selectedContent = scheme.onSurfaceContainer,
                unselectedContent = scheme.onSurfaceContainer.copy(alpha = 0.4f),
            )
        }
    }

    /** 关注、已关注及个人资料编辑均为中性关系操作；状态由文案表达，禁用态沿用原生配色。 */
    @Suppress("UNUSED_PARAMETER")
    @Composable
    fun nativeFollow(isFollowing: Boolean, isOwner: Boolean = false, enabled: Boolean = true): AppActionStateColors? {
        if (!nativeInteractionColorsEnabled()) return null
        val colors = ButtonDefaults.buttonColors()
        return if (enabled) AppActionStateColors(colors.color, colors.contentColor)
            else AppActionStateColors(colors.disabledColor, colors.disabledContentColor)
    }

    /** 对比度计算使用当前原生表面与前景；应用画布仍沿用已有的桥接背景。 */
    @Composable
    @ReadOnlyComposable
    internal fun nativeContentScheme(forceMiuix: Boolean = false): ColorScheme? {
        if (!forceMiuix && !nativeInteractionColorsEnabled()) return null
        val native = MiuixTheme.colorScheme
        return MaterialTheme.colorScheme.copy(
            primary = native.primary,
            onPrimary = native.onPrimary,
            primaryContainer = native.primaryContainer,
            onPrimaryContainer = native.onPrimaryContainer,
            secondaryContainer = native.secondaryContainer,
            onSecondaryContainer = native.onSecondaryContainer,
            surface = native.surface,
            onSurface = native.onSurface,
            surfaceVariant = native.surfaceContainerHigh,
            onSurfaceVariant = native.onSurfaceVariantSummary,
            surfaceContainerLowest = native.surface,
            surfaceContainerLow = native.surface,
            surfaceContainer = native.surfaceContainer,
            surfaceContainerHigh = native.surfaceContainerHigh,
            surfaceContainerHighest = native.surfaceContainerHighest,
            surfaceBright = native.surfaceContainerHigh,
            surfaceDim = native.surface,
        )
    }
}
