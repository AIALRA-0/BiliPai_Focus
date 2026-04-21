package com.android.purebilibili.core.theme

import androidx.compose.runtime.staticCompositionLocalOf

enum class UiPreset(val value: Int, val label: String) {
    IOS(0, "iOS"),
    MD3(1, "安卓原生");

    companion object {
        fun fromValue(value: Int): UiPreset = entries.find { it.value == value } ?: IOS
    }
}

enum class AndroidNativeVariant(val value: Int, val label: String) {
    MATERIAL3(0, "Material 3"),
    MIUIX(1, "Miuix");

    companion object {
        fun fromValue(value: Int): AndroidNativeVariant {
            return entries.find { it.value == value } ?: MATERIAL3
        }
    }
}

data class UiRenderingProfile(
    val useMaterialChrome: Boolean,
    val useMaterialMotion: Boolean,
    val useMaterialIcons: Boolean
)

fun resolveUiRenderingProfile(preset: UiPreset): UiRenderingProfile {
    return when (preset) {
        UiPreset.IOS -> UiRenderingProfile(
            useMaterialChrome = false,
            useMaterialMotion = false,
            useMaterialIcons = false
        )

        UiPreset.MD3 -> UiRenderingProfile(
            useMaterialChrome = true,
            useMaterialMotion = true,
            useMaterialIcons = true
        )
    }
}

val LocalUiPreset = staticCompositionLocalOf { UiPreset.IOS }
val LocalAndroidNativeVariant = staticCompositionLocalOf { AndroidNativeVariant.MATERIAL3 }
val LocalDynamicColorActive = staticCompositionLocalOf { false }
