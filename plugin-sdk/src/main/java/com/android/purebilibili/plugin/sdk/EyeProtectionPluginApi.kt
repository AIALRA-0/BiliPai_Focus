package com.android.purebilibili.plugin.sdk

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class EyeCareReminder(
    val usageMinutes: Int,
    val title: String,
    val message: String,
    val suggestion: String
)

interface EyeProtectionPluginApi : Plugin {
    val isNightModeActive: StateFlow<Boolean>
    val brightnessLevel: StateFlow<Float>
    val warmFilterStrength: StateFlow<Float>
    val settingsPreviewEnabled: StateFlow<Boolean>
    val careReminder: StateFlow<EyeCareReminder?>
    val weakenDuringPlayback: StateFlow<Boolean>
    fun getSnoozeMinutes(): Int
    fun dismissReminder()
    fun snoozeReminder()
    fun confirmRest()
}
