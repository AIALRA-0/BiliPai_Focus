package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.core.util.HapticType

internal fun resolveImagePreviewLongPressSaveStartFeedback(): HapticType {
    return HapticType.MEDIUM
}

internal fun resolveImagePreviewSaveFeedback(success: Boolean): HapticType {
    return if (success) HapticType.LIGHT else HapticType.HEAVY
}
