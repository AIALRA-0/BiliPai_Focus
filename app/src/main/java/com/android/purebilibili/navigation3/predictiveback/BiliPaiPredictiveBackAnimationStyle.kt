package com.android.purebilibili.navigation3.predictiveback

internal enum class BiliPaiPredictiveBackAnimationStyle(val storageValue: String) {
    NONE("none"),
    AOSP("aosp"),
    MIUIX("miuix"),
    SCALE("scale"),
    CLASSIC("bilipai_classic"),
    FADE("fade"),
    RIGHT_TO_LEFT("right_to_left"),
    LEFT_TO_RIGHT("left_to_right"),
    UP_TO_DOWN("up_to_down"),
    DOWN_TO_UP("down_to_up"),
    RIGHT_TO_LEFT_FADE("right_to_left_fade"),
    LEFT_TO_RIGHT_FADE("left_to_right_fade"),
    ZOOM("zoom"),
    CIRCULAR_REVEAL("circular_reveal"),
    IOS("ios"),
    FADE_IN("fade_in"),
    SIZE("size"),
    SHARED_AXIS("shared_axis"),
    NO_TRANSITION("no_transition");

    companion object {
        fun fromStorageValue(value: String?): BiliPaiPredictiveBackAnimationStyle {
            return when (value) {
                "default" -> MIUIX
                "classic" -> CLASSIC
                else -> entries.find { it.storageValue == value } ?: MIUIX
            }
        }
    }
}
