package com.android.purebilibili.navigation

internal data class AppNavigationMotionSpec(
    val slideDurationMillis: Int,
    val fastFadeDurationMillis: Int,
    val mediumFadeDurationMillis: Int,
    val slowFadeDurationMillis: Int,
    val backdropBlurDurationMillis: Int,
    val maxBackdropBlurRadius: Float,
    val fallbackFadeDurationMillis: Int,
    val quickReturnFadeDurationMillis: Int,
    val seamlessFadeDurationMillis: Int,
    val cardTargetFallbackSlideMaxDurationMillis: Int
)

private const val FALLBACK_FADE_DURATION_MILLIS = 120
private const val QUICK_RETURN_FADE_DURATION_MILLIS = 170
private const val SEAMLESS_FADE_DURATION_MILLIS = 180
private const val CARD_DISABLED_TARGET_SLIDE_MAX_DURATION_MILLIS = 220
private const val CARD_TARGET_FALLBACK_SLIDE_MAX_DURATION_MILLIS = 180

internal fun resolveAppNavigationMotionSpec(
    isTabletLayout: Boolean,
    cardTransitionEnabled: Boolean
): AppNavigationMotionSpec {
    if (!cardTransitionEnabled) {
        return AppNavigationMotionSpec(
            slideDurationMillis = 220,
            fastFadeDurationMillis = 120,
            mediumFadeDurationMillis = 160,
            slowFadeDurationMillis = 190,
            backdropBlurDurationMillis = 160,
            maxBackdropBlurRadius = 8f,
            fallbackFadeDurationMillis = FALLBACK_FADE_DURATION_MILLIS,
            quickReturnFadeDurationMillis = QUICK_RETURN_FADE_DURATION_MILLIS,
            seamlessFadeDurationMillis = SEAMLESS_FADE_DURATION_MILLIS,
            cardTargetFallbackSlideMaxDurationMillis = CARD_DISABLED_TARGET_SLIDE_MAX_DURATION_MILLIS
        )
    }

    return if (isTabletLayout) {
        AppNavigationMotionSpec(
            slideDurationMillis = 350,
            fastFadeDurationMillis = 190,
            mediumFadeDurationMillis = 240,
            slowFadeDurationMillis = 290,
            backdropBlurDurationMillis = 240,
            maxBackdropBlurRadius = 24f,
            fallbackFadeDurationMillis = FALLBACK_FADE_DURATION_MILLIS,
            quickReturnFadeDurationMillis = QUICK_RETURN_FADE_DURATION_MILLIS,
            seamlessFadeDurationMillis = SEAMLESS_FADE_DURATION_MILLIS,
            cardTargetFallbackSlideMaxDurationMillis = CARD_TARGET_FALLBACK_SLIDE_MAX_DURATION_MILLIS
        )
    } else {
        AppNavigationMotionSpec(
            slideDurationMillis = 300,
            fastFadeDurationMillis = 160,
            mediumFadeDurationMillis = 200,
            slowFadeDurationMillis = 255,
            backdropBlurDurationMillis = 200,
            maxBackdropBlurRadius = 16f,
            fallbackFadeDurationMillis = FALLBACK_FADE_DURATION_MILLIS,
            quickReturnFadeDurationMillis = QUICK_RETURN_FADE_DURATION_MILLIS,
            seamlessFadeDurationMillis = SEAMLESS_FADE_DURATION_MILLIS,
            cardTargetFallbackSlideMaxDurationMillis = CARD_TARGET_FALLBACK_SLIDE_MAX_DURATION_MILLIS
        )
    }
}
