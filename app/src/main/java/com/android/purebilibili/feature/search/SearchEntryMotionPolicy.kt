package com.android.purebilibili.feature.search

enum class SearchEntryMotionSource {
    NONE,
    BOTTOM_BAR
}

data class SearchEntryMotionSpec(
    val durationMillis: Int,
    val initialAlpha: Float,
    val initialScale: Float,
    val initialTranslationYDp: Float,
    val transformOriginPivotX: Float,
    val transformOriginPivotY: Float
)

internal fun resolveSearchEntryMotionSpec(
    source: SearchEntryMotionSource,
    reducedMotionBudget: Boolean
): SearchEntryMotionSpec? {
    if (source != SearchEntryMotionSource.BOTTOM_BAR) return null
    return if (reducedMotionBudget) {
        SearchEntryMotionSpec(
            durationMillis = 0,
            initialAlpha = 1f,
            initialScale = 1f,
            initialTranslationYDp = 0f,
            transformOriginPivotX = 0.88f,
            transformOriginPivotY = 1f
        )
    } else {
        SearchEntryMotionSpec(
            durationMillis = 320,
            initialAlpha = 0.58f,
            initialScale = 0.88f,
            initialTranslationYDp = 360f,
            transformOriginPivotX = 0.5f,
            transformOriginPivotY = 1f
        )
    }
}

internal fun resolveSearchExitMotionSpec(
    entrySpec: SearchEntryMotionSpec?,
    screenHeightDp: Int,
): SearchEntryMotionSpec? {
    return entrySpec?.copy(
        durationMillis = 300,
        initialAlpha = 0.16f,
        initialScale = 0.74f,
        initialTranslationYDp = (screenHeightDp - 136)
            .coerceAtLeast(360)
            .toFloat(),
        transformOriginPivotX = 0.88f,
        transformOriginPivotY = 1f,
    )
}
