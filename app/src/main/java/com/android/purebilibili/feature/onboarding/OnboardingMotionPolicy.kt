package com.android.purebilibili.feature.onboarding

internal data class OnboardingPagerMotionSpec(
    val minScale: Float,
    val minAlpha: Float
)

internal data class OnboardingFloatingMotionSpec(
    val translationYPx: Float,
    val durationMillis: Int
)

internal data class OnboardingHaloMotionSpec(
    val minScale: Float,
    val maxScale: Float,
    val durationMillis: Int
)

internal data class OnboardingCardMotionSpec(
    val selectedScale: Float,
    val unselectedScale: Float,
    val durationMillis: Int
)

internal data class OnboardingMotionSpec(
    val pager: OnboardingPagerMotionSpec,
    val floating: OnboardingFloatingMotionSpec,
    val halo: OnboardingHaloMotionSpec,
    val card: OnboardingCardMotionSpec
)

internal fun resolveOnboardingMotionSpec(
    reduceMotion: Boolean
): OnboardingMotionSpec {
    return if (reduceMotion) {
        OnboardingMotionSpec(
            pager = OnboardingPagerMotionSpec(minScale = 1f, minAlpha = 1f),
            floating = OnboardingFloatingMotionSpec(translationYPx = 0f, durationMillis = 0),
            halo = OnboardingHaloMotionSpec(minScale = 1f, maxScale = 1f, durationMillis = 0),
            card = OnboardingCardMotionSpec(
                selectedScale = 1f,
                unselectedScale = 1f,
                durationMillis = 0
            )
        )
    } else {
        OnboardingMotionSpec(
            pager = OnboardingPagerMotionSpec(minScale = 0.92f, minAlpha = 0.36f),
            floating = OnboardingFloatingMotionSpec(translationYPx = 8f, durationMillis = 2800),
            halo = OnboardingHaloMotionSpec(minScale = 0.9f, maxScale = 1.08f, durationMillis = 1800),
            card = OnboardingCardMotionSpec(
                selectedScale = 1.02f,
                unselectedScale = 1f,
                durationMillis = 180
            )
        )
    }
}

internal fun resolveOnboardingPageCount(): Int = 5

internal fun resolveOnboardingLastPageIndex(pageCount: Int = resolveOnboardingPageCount()): Int {
    return (pageCount - 1).coerceAtLeast(0)
}
