package com.android.purebilibili.feature.home.components.cards

import com.android.purebilibili.core.store.HomeCardBadgeEffectMode

internal enum class HomeVideoBadgeStyle {
    GLASS,
    PLAIN
}

internal data class HomeVideoGlassBadgeStylePolicy(
    val coverStyle: HomeVideoBadgeStyle,
    val infoStyle: HomeVideoBadgeStyle
)

/**
 * Runtime look for card badges.
 *
 * Light blur uses the same Haze path as the bottom bar ([unifiedBlur] + BOTTOM_BAR budget).
 * Scroll does **not** fall back to soft glass — matching bottom bar (blur stays on, budget only).
 */
internal data class HomeCardBadgeEffectVisual(
    val coverStyle: HomeVideoBadgeStyle,
    val infoStyle: HomeVideoBadgeStyle,
    val glassEnabled: Boolean,
    val blurEnabled: Boolean,
    /** Real Haze sampling (same stack as bottom bar). */
    val useRealtimeHaze: Boolean,
    val effectiveMode: HomeCardBadgeEffectMode
)

/**
 * @param hasHazeState whether [LocalMainHazeState] is available (bottom-bar source).
 * @param scrollLiteModeEnabled unused for mode demotion; kept for call-site stability.
 *   Realtime path follows bottom bar: keep haze while scrolling.
 */
internal fun resolveHomeCardBadgeEffectVisual(
    mode: HomeCardBadgeEffectMode,
    scrollLiteModeEnabled: Boolean,
    hasHazeState: Boolean = false
): HomeCardBadgeEffectVisual {
    @Suppress("UNUSED_PARAMETER")
    val unusedScroll = scrollLiteModeEnabled
    return when (mode) {
        HomeCardBadgeEffectMode.OFF -> HomeCardBadgeEffectVisual(
            coverStyle = HomeVideoBadgeStyle.PLAIN,
            infoStyle = HomeVideoBadgeStyle.PLAIN,
            glassEnabled = false,
            blurEnabled = false,
            useRealtimeHaze = false,
            effectiveMode = HomeCardBadgeEffectMode.OFF
        )
        HomeCardBadgeEffectMode.SOFT_GLASS -> HomeCardBadgeEffectVisual(
            coverStyle = HomeVideoBadgeStyle.GLASS,
            infoStyle = HomeVideoBadgeStyle.GLASS,
            glassEnabled = true,
            blurEnabled = false,
            useRealtimeHaze = false,
            effectiveMode = HomeCardBadgeEffectMode.SOFT_GLASS
        )
        HomeCardBadgeEffectMode.LIGHT_BLUR -> {
            // Prefer real bottom-bar Haze; without source keep frosted soft glass (same glass shell).
            val realtime = hasHazeState
            HomeCardBadgeEffectVisual(
                coverStyle = HomeVideoBadgeStyle.GLASS,
                infoStyle = HomeVideoBadgeStyle.GLASS,
                glassEnabled = true,
                blurEnabled = true,
                useRealtimeHaze = realtime,
                effectiveMode = HomeCardBadgeEffectMode.LIGHT_BLUR
            )
        }
    }
}

/**
 * Back-compat entry used by older call sites that only pass boolean glass flags.
 */
internal fun resolveHomeVideoGlassBadgeStylePolicy(
    showCoverGlassBadges: Boolean,
    showInfoGlassBadges: Boolean
): HomeVideoGlassBadgeStylePolicy {
    val mode = if (showCoverGlassBadges || showInfoGlassBadges) {
        HomeCardBadgeEffectMode.SOFT_GLASS
    } else {
        HomeCardBadgeEffectMode.OFF
    }
    val visual = resolveHomeCardBadgeEffectVisual(
        mode = mode,
        scrollLiteModeEnabled = false,
        hasHazeState = false
    )
    return HomeVideoGlassBadgeStylePolicy(
        coverStyle = if (showCoverGlassBadges) visual.coverStyle else HomeVideoBadgeStyle.PLAIN,
        infoStyle = if (showInfoGlassBadges) visual.infoStyle else HomeVideoBadgeStyle.PLAIN
    )
}
