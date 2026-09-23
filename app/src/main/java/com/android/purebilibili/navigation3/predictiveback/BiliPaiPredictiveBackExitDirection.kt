package com.android.purebilibili.navigation3.predictiveback

internal enum class BiliPaiPredictiveBackExitDirection(val storageValue: String) {
    FOLLOW_GESTURE("follow_gesture"),
    ALWAYS_RIGHT("always_right"),
    ALWAYS_LEFT("always_left");

    companion object {
        fun fromStorageValue(value: String?): BiliPaiPredictiveBackExitDirection =
            entries.firstOrNull { it.storageValue == value } ?: ALWAYS_RIGHT
    }
}
