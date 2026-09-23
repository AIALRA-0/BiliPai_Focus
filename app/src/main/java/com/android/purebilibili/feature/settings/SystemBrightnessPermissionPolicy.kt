package com.android.purebilibili.feature.settings

internal enum class SystemBrightnessToggleAction {
    ENABLE,
    DISABLE,
    REQUEST_PERMISSION
}

internal fun resolveSystemBrightnessToggleAction(
    requestedEnabled: Boolean,
    canWriteSystemSettings: Boolean
): SystemBrightnessToggleAction {
    return when {
        !requestedEnabled -> SystemBrightnessToggleAction.DISABLE
        canWriteSystemSettings -> SystemBrightnessToggleAction.ENABLE
        else -> SystemBrightnessToggleAction.REQUEST_PERMISSION
    }
}

internal fun normalizeSystemBrightnessSetting(
    storedEnabled: Boolean,
    canWriteSystemSettings: Boolean
): Boolean = storedEnabled && canWriteSystemSettings
