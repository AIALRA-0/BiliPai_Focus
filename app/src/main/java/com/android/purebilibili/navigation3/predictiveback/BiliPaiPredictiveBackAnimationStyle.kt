package com.android.purebilibili.navigation3.predictiveback

internal enum class BiliPaiPredictiveBackAnimationStyle(val storageValue: String) {
    NONE("none"),
    AOSP("aosp"),
    MIUIX("miuix"),
    SCALE("scale"),
    CLASSIC("bilipai_classic");

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
