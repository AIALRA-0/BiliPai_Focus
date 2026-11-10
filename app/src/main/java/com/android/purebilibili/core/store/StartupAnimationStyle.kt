package com.android.purebilibili.core.store

/** Stable stored values; unknown and older installations retain the original flyout. */
enum class StartupAnimationStyle(val value: String, val label: String) {
    ICON_FLYOUT("icon_flyout", "原图标飞出"),
    BLUE_SNOW_MAID("blue_snow_maid", "蓝雪女仆");

    companion object {
        fun fromValue(value: String?): StartupAnimationStyle =
            entries.firstOrNull { it.value == value } ?: ICON_FLYOUT
    }
}

internal fun shouldShowMaidStartup(
    coldStart: Boolean,
    agreementAccepted: Boolean,
    iconAnimationEnabled: Boolean,
    style: StartupAnimationStyle
): Boolean = coldStart && agreementAccepted && iconAnimationEnabled &&
    style == StartupAnimationStyle.BLUE_SNOW_MAID
