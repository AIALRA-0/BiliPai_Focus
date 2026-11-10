package com.android.bilipai.tv.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography
import androidx.tv.material3.Shapes
import com.android.purebilibili.core.theme.AppTextStyles
import com.android.purebilibili.core.ui.AppShapeTokens
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.ContainerLevel

internal class TvFocusReturnTarget { var requester by mutableStateOf<FocusRequester?>(null) }
internal val LocalTvReturnTarget = staticCompositionLocalOf<TvFocusReturnTarget?> { null }

internal val LocalTvReduceMotion = staticCompositionLocalOf { false }

internal val LocalTvBackdropUrl = staticCompositionLocalOf<String?> { null }

internal val LocalTvSimpleEffects = staticCompositionLocalOf { false }
internal val LocalTvInteractive = staticCompositionLocalOf { true }

internal object TvUiTokens {
    val pagePadding = AppSpacingTokens.DoubleExtraLarge
    val gridPadding = AppSpacingTokens.Large
    // 密度评审调整(2026-10-03):960dp 窗口按 150dp 最小宽出 4 列(约 160dp/卡),
    // 对齐主流 TV 应用的小卡密度;原 220dp 在同窗口只能出 2 列,过于稀疏。
    val cardGap = AppSpacingTokens.Large
    val cardPadding = AppSpacingTokens.Medium
    val minimumCardWidth = 150.dp
    val minimumButtonHeight = 48.dp
    val focusBorderWidth = 2.dp
    val progressTrackHeight = AppSpacingTokens.ExtraSmall
    val focusedProgressTrackHeight = AppSpacingTokens.Small
    val progressThumbSize = AppSpacingTokens.Large
    const val focusedCardScale = 1.04f

    fun shape(level: ContainerLevel) = RoundedCornerShape(
        (AppShapeTokens.baseCornerDp(level) * AppShapeTokens.MaterialCornerRadiusScale).dp
    )

    // Same full-round silhouette as the phone Material button; TV owns its dimensions.
    val buttonShape = RoundedCornerShape(percent = 50)

    val shapes = Shapes(
        extraSmall = RoundedCornerShape(AppShapeTokens.MaterialExtraSmall),
        small = RoundedCornerShape(AppShapeTokens.MaterialSmall),
        medium = RoundedCornerShape(AppShapeTokens.MaterialMedium),
        large = RoundedCornerShape(AppShapeTokens.MaterialLarge),
        extraLarge = RoundedCornerShape(AppShapeTokens.MaterialExtraLarge),
    )

    val typography = Typography(
        displayLarge = AppTextStyles.displayLarge,
        displayMedium = AppTextStyles.displayMedium,
        displaySmall = AppTextStyles.displaySmall,
        headlineLarge = AppTextStyles.headlineLarge,
        headlineMedium = AppTextStyles.headlineMedium,
        headlineSmall = AppTextStyles.headlineSmall,
        titleLarge = AppTextStyles.titleLarge,
        titleMedium = AppTextStyles.titleMedium.copy(fontSize = 18.sp, lineHeight = 26.sp),
        // 卡片级字阶:小卡密度下的 10 英尺可读下限
        titleSmall = AppTextStyles.titleSmall.copy(fontSize = 16.sp, lineHeight = 24.sp),
        bodyLarge = AppTextStyles.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
        bodyMedium = AppTextStyles.bodyMedium.copy(fontSize = 16.sp, lineHeight = 24.sp),
        bodySmall = AppTextStyles.bodySmall.copy(fontSize = 16.sp, lineHeight = 24.sp),
        labelLarge = AppTextStyles.labelLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
        labelMedium = AppTextStyles.labelMedium.copy(fontSize = 16.sp, lineHeight = 24.sp),
        labelSmall = AppTextStyles.labelSmall.copy(fontSize = 14.sp, lineHeight = 20.sp),
    )
}
