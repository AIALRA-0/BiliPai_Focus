package com.android.purebilibili.feature.plugin

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.purebilibili.core.plugin.PLUGIN_EFFECT_HINT_EYE_COOLDOWN_MS
import com.android.purebilibili.core.plugin.Plugin
import com.android.purebilibili.core.plugin.PluginEffectHintBus
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.plugin.PluginStore
import com.android.purebilibili.core.plugin.resolveEyeProtectionEffectHint
import com.android.purebilibili.core.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar

private const val TAG = "EyeProtectionPlugin"

@Serializable
enum class EyeCarePreset {
    GENTLE,
    BALANCED,
    FOCUS,
    CUSTOM
}

@Serializable
data class EyeCareProfile(
    val brightnessLevel: Float,
    val warmFilterStrength: Float,
    val reminderIntervalMinutes: Int,
    val snoozeMinutes: Int
)

@Serializable
data class EyeCareReminder(
    val usageMinutes: Int,
    val title: String,
    val message: String,
    val suggestion: String
)

@Serializable
data class EyeProtectionConfig(
    val nightModeEnabled: Boolean = true,
    val nightModeStartHour: Int = 22,
    val nightModeEndHour: Int = 7,
    val usageReminderEnabled: Boolean = true,
    val usageDurationMinutes: Int = 30,
    val reminderSnoozeMinutes: Int = 10,
    val remindOnlyDuringNight: Boolean = true,
    val brightnessLevel: Float = 0.78f,
    val warmFilterStrength: Float = 0.22f,
    val carePreset: EyeCarePreset = EyeCarePreset.BALANCED,
    val profileGentle: EyeCareProfile = EyeCareProfile(
        brightnessLevel = 0.88f,
        warmFilterStrength = 0.12f,
        reminderIntervalMinutes = 45,
        snoozeMinutes = 10
    ),
    val profileBalanced: EyeCareProfile = EyeCareProfile(
        brightnessLevel = 0.78f,
        warmFilterStrength = 0.22f,
        reminderIntervalMinutes = 30,
        snoozeMinutes = 10
    ),
    val profileFocus: EyeCareProfile = EyeCareProfile(
        brightnessLevel = 0.65f,
        warmFilterStrength = 0.32f,
        reminderIntervalMinutes = 25,
        snoozeMinutes = 5
    ),
    val forceEnabled: Boolean = false,
    val scheduleRampMinutes: Int = 20,
    val weakenDuringPlayback: Boolean = true,
    val showEffectHint: Boolean = true
)

class EyeProtectionPlugin : Plugin {

    override val id = EYE_PROTECTION_PLUGIN_ID
    override val name = "夜间护眼"
    override val description = "柔和降蓝光、定时护眼与休息提醒"
    override val version = "2.2.0"
    override val author = "BiliPai项目组"
    override val icon: ImageVector = Icons.Outlined.DarkMode

    private var config: EyeProtectionConfig = EyeProtectionConfig()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var usageTrackingJob: Job? = null

    private var usageMinutes = 0
    private var snoozeUntilMinute: Int? = null
    private var lastReminderMinute: Int? = null

    private val _isNightModeActive = MutableStateFlow(false)
    val isNightModeActive: StateFlow<Boolean> = _isNightModeActive.asStateFlow()

    private val _brightnessLevel = MutableStateFlow(1.0f)
    val brightnessLevel: StateFlow<Float> = _brightnessLevel.asStateFlow()

    private val _warmFilterStrength = MutableStateFlow(0f)
    val warmFilterStrength: StateFlow<Float> = _warmFilterStrength.asStateFlow()

    private val _settingsPreviewEnabled = MutableStateFlow(false)
    val settingsPreviewEnabled: StateFlow<Boolean> = _settingsPreviewEnabled.asStateFlow()

    private val _careReminder = MutableStateFlow<EyeCareReminder?>(null)
    val careReminder: StateFlow<EyeCareReminder?> = _careReminder.asStateFlow()

    private val _usageMinutes = MutableStateFlow(0)
    val usageMinutesFlow: StateFlow<Int> = _usageMinutes.asStateFlow()

    private val _weakenDuringPlayback = MutableStateFlow(true)
    val weakenDuringPlayback: StateFlow<Boolean> = _weakenDuringPlayback.asStateFlow()

    private var allowActivationHint = false
    private var usageElapsedMs = 0L

    override suspend fun onEnable() {
        allowActivationHint = false
        loadConfigSuspend()
        applyVisualState()
        startUsageTracking()
        allowActivationHint = true
        Logger.d(TAG, "夜间护眼插件已启用")
    }

    override suspend fun onDisable() {
        usageTrackingJob?.cancel()
        usageMinutes = 0
        usageElapsedMs = 0L
        snoozeUntilMinute = null
        lastReminderMinute = null
        allowActivationHint = false
        _usageMinutes.value = 0
        _careReminder.value = null
        _settingsPreviewEnabled.value = false
        _isNightModeActive.value = false
        _brightnessLevel.value = 1.0f
        _warmFilterStrength.value = 0f
        Logger.d(TAG, "夜间护眼插件已禁用")
    }

    fun dismissReminder() {
        _careReminder.value = null
    }

    fun snoozeReminder() {
        snoozeUntilMinute = usageMinutes + config.reminderSnoozeMinutes
        _careReminder.value = null
        Logger.d(TAG, "提醒已暂缓 ${config.reminderSnoozeMinutes} 分钟")
    }

    fun confirmRest() {
        usageMinutes = 0
        _usageMinutes.value = 0
        snoozeUntilMinute = null
        lastReminderMinute = null
        _careReminder.value = null
        Logger.d(TAG, "用户已确认休息，计时重置")
    }

    fun getSnoozeMinutes(): Int = config.reminderSnoozeMinutes

    fun snoozeUntilMinute(): Int? = snoozeUntilMinute

    fun snapshotConfig(): EyeProtectionConfig = config

    fun setSettingsPreviewEnabled(enabled: Boolean) {
        _settingsPreviewEnabled.value = enabled
        applyVisualState()
    }

    fun previewBrightness(value: Float) {
        _brightnessLevel.value = value.coerceIn(0.3f, 1.0f)
        _isNightModeActive.value = true
    }

    fun previewWarmFilter(value: Float) {
        _warmFilterStrength.value = value.coerceIn(0f, 0.5f)
        _isNightModeActive.value = true
    }

    fun commitConfig(newConfig: EyeProtectionConfig, refreshVisual: Boolean = true) {
        config = newConfig
        _weakenDuringPlayback.value = newConfig.weakenDuringPlayback
        saveConfig()
        if (refreshVisual) applyVisualState()
    }

    fun applyPreset(source: EyeProtectionConfig, preset: EyeCarePreset): EyeProtectionConfig {
        return applyPresetConfig(source, preset)
    }

    fun persistSliderToPreset(source: EyeProtectionConfig): EyeProtectionConfig {
        return persistCurrentValuesToSelectedPreset(source)
    }

    private fun startUsageTracking() {
        usageTrackingJob?.cancel()
        usageElapsedMs = 0L
        usageTrackingJob = workerScope.launch {
            while (isActive) {
                delay(15_000)
                applyVisualState()

                if (!shouldCountUsageMinute()) continue

                usageElapsedMs += 15_000
                if (usageElapsedMs < 60_000) continue
                usageElapsedMs = 0L
                usageMinutes++
                _usageMinutes.value = usageMinutes
                if (shouldTriggerCareReminder(
                        usageMinutes = usageMinutes,
                        intervalMinutes = config.usageDurationMinutes,
                        snoozeUntilMinute = snoozeUntilMinute,
                        lastReminderMinute = lastReminderMinute
                    )
                ) {
                    lastReminderMinute = usageMinutes
                    _careReminder.value = EyeCareReminder(
                        usageMinutes = usageMinutes,
                        title = "给眼睛一个小休息",
                        message = buildCareReminderMessage(usageMinutes),
                        suggestion = "试试 20-20-20：看向 6 米外，保持 20 秒"
                    )
                    Logger.d(TAG, "触发护眼提醒：$usageMinutes 分钟")
                }
            }
        }
    }

    private fun shouldCountUsageMinute(): Boolean {
        if (_settingsPreviewEnabled.value) return false
        if (!config.usageReminderEnabled) return false

        val appForeground = androidx.lifecycle.ProcessLifecycleOwner.get()
            .lifecycle.currentState
            .isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
        if (!appForeground) return false

        if (!config.remindOnlyDuringNight) return true
        return _isNightModeActive.value || config.forceEnabled
    }

    private fun applyVisualState() {
        val calendar = Calendar.getInstance()
        val currentMinuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 +
            calendar.get(Calendar.MINUTE)
        val wasActive = _isNightModeActive.value
        val visualState = resolveEyeVisualState(
            settingsPreviewEnabled = _settingsPreviewEnabled.value,
            forceEnabled = config.forceEnabled,
            nightModeEnabled = config.nightModeEnabled,
            currentMinuteOfDay = currentMinuteOfDay,
            startHour = config.nightModeStartHour,
            endHour = config.nightModeEndHour,
            brightnessLevel = config.brightnessLevel,
            warmFilterStrength = config.warmFilterStrength,
            rampMinutes = config.scheduleRampMinutes
        )
        _isNightModeActive.value = visualState.isActive
        _brightnessLevel.value = visualState.brightnessLevel
        _warmFilterStrength.value = visualState.warmFilterStrength
        _weakenDuringPlayback.value = config.weakenDuringPlayback
        if (
            visualState.isActive &&
            !wasActive &&
            allowActivationHint &&
            !_settingsPreviewEnabled.value &&
            config.showEffectHint
        ) {
            PluginEffectHintBus.tryEmit(
                resolveEyeProtectionEffectHint(
                    forceEnabled = config.forceEnabled,
                    endHour = config.nightModeEndHour
                ),
                cooldownMs = PLUGIN_EFFECT_HINT_EYE_COOLDOWN_MS
            )
        }
    }

    internal suspend fun loadConfigSuspend() {
        try {
            val context = PluginManager.getContext()
            val jsonStr = PluginStore.getConfigJson(context, id)
            if (!jsonStr.isNullOrBlank()) {
                config = Json.decodeFromString<EyeProtectionConfig>(jsonStr)
            }
            config = applyPresetConfig(config, config.carePreset)
            _weakenDuringPlayback.value = config.weakenDuringPlayback
        } catch (e: Exception) {
            Logger.e(TAG, "加载配置失败", e)
        }
    }

    private fun saveConfig() {
        ioScope.launch {
            try {
                val context = PluginManager.getContext()
                PluginStore.setConfigJson(context, id, Json.encodeToString(config))
            } catch (e: Exception) {
                Logger.e(TAG, "保存配置失败", e)
            }
        }
    }

    private fun normalizePreset(preset: EyeCarePreset): EyeCarePreset {
        return if (preset == EyeCarePreset.CUSTOM) EyeCarePreset.BALANCED else preset
    }

    private fun profileForPreset(
        source: EyeProtectionConfig,
        preset: EyeCarePreset
    ): EyeCareProfile {
        return when (normalizePreset(preset)) {
            EyeCarePreset.GENTLE -> source.profileGentle
            EyeCarePreset.BALANCED -> source.profileBalanced
            EyeCarePreset.FOCUS -> source.profileFocus
            EyeCarePreset.CUSTOM -> source.profileBalanced
        }
    }

    private fun withPresetProfile(
        source: EyeProtectionConfig,
        preset: EyeCarePreset,
        profile: EyeCareProfile
    ): EyeProtectionConfig {
        return when (normalizePreset(preset)) {
            EyeCarePreset.GENTLE -> source.copy(profileGentle = profile)
            EyeCarePreset.BALANCED -> source.copy(profileBalanced = profile)
            EyeCarePreset.FOCUS -> source.copy(profileFocus = profile)
            EyeCarePreset.CUSTOM -> source.copy(profileBalanced = profile)
        }
    }

    private fun applyPresetConfig(
        source: EyeProtectionConfig,
        preset: EyeCarePreset
    ): EyeProtectionConfig {
        val normalizedPreset = normalizePreset(preset)
        val profile = profileForPreset(source, normalizedPreset)
        return source.copy(
            carePreset = normalizedPreset,
            brightnessLevel = profile.brightnessLevel.coerceIn(0.3f, 1.0f),
            warmFilterStrength = profile.warmFilterStrength.coerceIn(0f, 0.5f),
            usageDurationMinutes = profile.reminderIntervalMinutes.coerceAtLeast(1),
            reminderSnoozeMinutes = profile.snoozeMinutes.coerceAtLeast(1)
        )
    }

    private fun persistCurrentValuesToSelectedPreset(source: EyeProtectionConfig): EyeProtectionConfig {
        val normalizedPreset = normalizePreset(source.carePreset)
        val profile = EyeCareProfile(
            brightnessLevel = source.brightnessLevel.coerceIn(0.3f, 1.0f),
            warmFilterStrength = source.warmFilterStrength.coerceIn(0f, 0.5f),
            reminderIntervalMinutes = source.usageDurationMinutes.coerceAtLeast(1),
            snoozeMinutes = source.reminderSnoozeMinutes.coerceAtLeast(1)
        )
        return withPresetProfile(source.copy(carePreset = normalizedPreset), normalizedPreset, profile)
    }

    @Composable
    override fun SettingsContent() {
        SettingsContent(Modifier.fillMaxWidth())
    }

    @Composable
    override fun SettingsContent(modifier: Modifier) {
        EyeProtectionSettings(modifier = modifier, plugin = this)
    }

    companion object {
        fun getInstance(): EyeProtectionPlugin? {
            return PluginManager.plugins.find {
                it.plugin.id == EYE_PROTECTION_PLUGIN_ID
            }?.plugin as? EyeProtectionPlugin
        }
    }
}
