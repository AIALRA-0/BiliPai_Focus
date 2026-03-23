package com.android.purebilibili.feature.settings

enum class DarkThemeStyle(val value: Int, val label: String) {
    DEFAULT(0, "普通黑"),
    AMOLED(1, "AMOLED纯黑");

    companion object {
        fun fromValue(value: Int): DarkThemeStyle = entries.find { it.value == value } ?: DEFAULT
    }
}
