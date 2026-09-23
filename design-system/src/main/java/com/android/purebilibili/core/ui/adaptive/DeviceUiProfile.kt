package com.android.purebilibili.core.ui.adaptive

enum class AdaptiveWidthClass {
    Compact,
    Medium,
    Expanded,
    Large,
    ExtraLarge,
}

enum class AdaptiveFoldPosture {
    None,
    Flat,
    Book,
    Tabletop,
}

data class DeviceUiProfileSpec(
    val isTablet: Boolean,
    val motionTier: MotionTier,
    val foldPosture: AdaptiveFoldPosture = AdaptiveFoldPosture.None,
)

fun resolveDeviceUiProfileSpec(
    widthClass: AdaptiveWidthClass,
    foldPosture: AdaptiveFoldPosture = AdaptiveFoldPosture.None,
): DeviceUiProfileSpec {
    val baseTier = if (widthClass >= AdaptiveWidthClass.Large) {
        MotionTier.Enhanced
    } else {
        MotionTier.Normal
    }
    val motionTier = when (foldPosture) {
        AdaptiveFoldPosture.Book,
        AdaptiveFoldPosture.Tabletop -> MotionTier.Reduced
        AdaptiveFoldPosture.None,
        AdaptiveFoldPosture.Flat -> baseTier
    }

    return DeviceUiProfileSpec(
        isTablet = widthClass != AdaptiveWidthClass.Compact,
        motionTier = motionTier,
        foldPosture = foldPosture,
    )
}
