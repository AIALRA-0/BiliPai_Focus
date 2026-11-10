package com.android.purebilibili.core.store.home

/** 存储值固定，新增方案不改变已有选择。未知值回退默认方案。 */
enum class ProgressiveBlurRenderer(val value: Int) {
    MIUIX(0),
    ANDROID(1);

    companion object {
        fun fromValue(value: Int): ProgressiveBlurRenderer =
            entries.firstOrNull { it.value == value } ?: MIUIX
    }
}
