package com.android.purebilibili.core.store

enum class LiquidGlassReadabilityMode(val value: Int, val label: String) {
    STABLE(0, "稳定内容色"),
    ADAPTIVE(1, "自动适配");

    companion object {
        fun fromValue(value: Int): LiquidGlassReadabilityMode =
            entries.find { it.value == value } ?: STABLE
    }
}
