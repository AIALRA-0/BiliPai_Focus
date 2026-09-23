package com.android.purebilibili.feature.plugin

import com.android.purebilibili.core.plugin.formatClockHour
import kotlin.math.min

internal const val EYE_PROTECTION_PLUGIN_ID = "eye_protection"
internal const val EYE_WARM_FILTER_COLOR = 0xFFFFC07A
private const val MINUTES_PER_DAY = 24 * 60
private const val PLAYBACK_WEAKEN_FACTOR = 0.42f

internal data class EyeCareTuning(
    val brightnessLevel: Float,
    val warmFilterStrength: Float,
    val reminderIntervalMinutes: Int
)

internal data class EyeVisualState(
    val isActive: Boolean,
    val brightnessLevel: Float,
    val warmFilterStrength: Float,
    val scheduleProgress: Float = if (isActive) 1f else 0f
)

internal data class EyeOverlayPaint(
    val dimAlpha: Float,
    val warmAlpha: Float,
    val warmColor: Long = EYE_WARM_FILTER_COLOR
)

internal data class EyeProtectionStatusCopy(
    val title: String,
    val subtitle: String,
    val isActive: Boolean
)

internal data class EyeReminderDialogLayoutPolicy(
    val useCompactSecondaryActions: Boolean,
    val maxHeightFraction: Float
)

internal fun resolveEyeReminderDialogLayoutPolicy(
    screenHeightDp: Int
): EyeReminderDialogLayoutPolicy {
    return if (screenHeightDp <= 700) {
        EyeReminderDialogLayoutPolicy(
            useCompactSecondaryActions = true,
            maxHeightFraction = 0.86f
        )
    } else {
        EyeReminderDialogLayoutPolicy(
            useCompactSecondaryActions = false,
            maxHeightFraction = 0.92f
        )
    }
}

internal fun minuteOfDay(hour: Int, minute: Int = 0): Int {
    val safeHour = hour.coerceIn(0, 23)
    val safeMinute = minute.coerceIn(0, 59)
    return safeHour * 60 + safeMinute
}

internal fun isWithinProtectionWindow(
    currentHour: Int,
    startHour: Int,
    endHour: Int
): Boolean {
    return isWithinProtectionWindowMinutes(
        currentMinuteOfDay = minuteOfDay(currentHour),
        startMinute = minuteOfDay(startHour),
        endMinute = minuteOfDay(endHour)
    )
}

internal fun isWithinProtectionWindowMinutes(
    currentMinuteOfDay: Int,
    startMinute: Int,
    endMinute: Int
): Boolean {
    val current = wrapMinuteOfDay(currentMinuteOfDay)
    val start = wrapMinuteOfDay(startMinute)
    val end = wrapMinuteOfDay(endMinute)
    return if (start > end) {
        current >= start || current < end
    } else if (start < end) {
        current >= start && current < end
    } else {
        true
    }
}

internal fun resolveScheduleProgress(
    currentMinuteOfDay: Int,
    startHour: Int,
    endHour: Int,
    rampMinutes: Int
): Float {
    val startMinute = minuteOfDay(startHour)
    val endMinute = minuteOfDay(endHour)
    if (!isWithinProtectionWindowMinutes(currentMinuteOfDay, startMinute, endMinute)) {
        return 0f
    }
    val elapsed = minutesSinceStart(currentMinuteOfDay, startMinute)
    val length = windowLengthMinutes(startMinute, endMinute)
    val remaining = (length - elapsed).coerceAtLeast(0)
    val ramp = rampMinutes.coerceAtLeast(0)
    if (ramp == 0) return 1f
    val fadeIn = (elapsed.toFloat() / ramp).coerceIn(0f, 1f)
    val fadeOut = (remaining.toFloat() / ramp).coerceIn(0f, 1f)
    val raw = min(fadeIn, fadeOut)
    // Keep a perceptible floor so the window start is "on" instead of waiting a full minute.
    return if (raw <= 0f) 0.08f else raw
}

internal fun shouldTriggerCareReminder(
    usageMinutes: Int,
    intervalMinutes: Int,
    snoozeUntilMinute: Int?,
    lastReminderMinute: Int?
): Boolean {
    if (intervalMinutes <= 0 || usageMinutes <= 0) return false
    if (usageMinutes % intervalMinutes != 0) return false
    if (snoozeUntilMinute != null && usageMinutes < snoozeUntilMinute) return false
    if (lastReminderMinute != null && usageMinutes - lastReminderMinute < intervalMinutes) return false
    return true
}

internal fun resolveMinutesUntilReminder(
    usageMinutes: Int,
    intervalMinutes: Int,
    snoozeUntilMinute: Int?,
    reminderEnabled: Boolean
): Int? {
    if (!reminderEnabled || intervalMinutes <= 0) return null
    if (snoozeUntilMinute != null && snoozeUntilMinute > usageMinutes) {
        return snoozeUntilMinute - usageMinutes
    }
    val elapsedInCycle = usageMinutes % intervalMinutes
    return if (elapsedInCycle == 0 && usageMinutes > 0) intervalMinutes else intervalMinutes - elapsedInCycle
}

internal fun isVisualEffectActive(
    forceEnabled: Boolean,
    nightModeEnabled: Boolean,
    currentHour: Int,
    startHour: Int,
    endHour: Int
): Boolean {
    return isVisualEffectActive(
        forceEnabled = forceEnabled,
        nightModeEnabled = nightModeEnabled,
        currentMinuteOfDay = minuteOfDay(currentHour),
        startHour = startHour,
        endHour = endHour,
        rampMinutes = 0
    )
}

internal fun isVisualEffectActive(
    forceEnabled: Boolean,
    nightModeEnabled: Boolean,
    currentMinuteOfDay: Int,
    startHour: Int,
    endHour: Int,
    rampMinutes: Int
): Boolean {
    if (forceEnabled) return true
    if (!nightModeEnabled) return false
    return resolveScheduleProgress(
        currentMinuteOfDay = currentMinuteOfDay,
        startHour = startHour,
        endHour = endHour,
        rampMinutes = rampMinutes
    ) > 0.01f
}

internal fun resolveEyeVisualState(
    settingsPreviewEnabled: Boolean,
    forceEnabled: Boolean,
    nightModeEnabled: Boolean,
    currentHour: Int,
    startHour: Int,
    endHour: Int,
    brightnessLevel: Float,
    warmFilterStrength: Float
): EyeVisualState {
    return resolveEyeVisualState(
        settingsPreviewEnabled = settingsPreviewEnabled,
        forceEnabled = forceEnabled,
        nightModeEnabled = nightModeEnabled,
        currentMinuteOfDay = minuteOfDay(currentHour),
        startHour = startHour,
        endHour = endHour,
        brightnessLevel = brightnessLevel,
        warmFilterStrength = warmFilterStrength,
        rampMinutes = 0
    )
}

internal fun resolveEyeVisualState(
    settingsPreviewEnabled: Boolean,
    forceEnabled: Boolean,
    nightModeEnabled: Boolean,
    currentMinuteOfDay: Int,
    startHour: Int,
    endHour: Int,
    brightnessLevel: Float,
    warmFilterStrength: Float,
    rampMinutes: Int
): EyeVisualState {
    val clampedBrightness = brightnessLevel.coerceIn(0.3f, 1.0f)
    val clampedWarmFilter = warmFilterStrength.coerceIn(0f, 0.5f)
    val progress = when {
        settingsPreviewEnabled || forceEnabled -> 1f
        !nightModeEnabled -> 0f
        else -> resolveScheduleProgress(
            currentMinuteOfDay = currentMinuteOfDay,
            startHour = startHour,
            endHour = endHour,
            rampMinutes = rampMinutes
        )
    }
    val active = progress > 0.01f
    return if (active) {
        EyeVisualState(
            isActive = true,
            brightnessLevel = lerp(1f, clampedBrightness, progress),
            warmFilterStrength = clampedWarmFilter * progress,
            scheduleProgress = progress
        )
    } else {
        EyeVisualState(
            isActive = false,
            brightnessLevel = 1.0f,
            warmFilterStrength = 0f,
            scheduleProgress = 0f
        )
    }
}

internal fun resolveEyeOverlayPaint(
    brightnessLevel: Float,
    warmFilterStrength: Float,
    playbackWeaken: Boolean
): EyeOverlayPaint {
    val weaken = if (playbackWeaken) PLAYBACK_WEAKEN_FACTOR else 1f
    val dim = ((1f - brightnessLevel.coerceIn(0.3f, 1.0f)) * 0.82f * weaken).coerceIn(0f, 0.50f)
    val warm = (warmFilterStrength.coerceIn(0f, 0.5f) * 0.78f * weaken).coerceIn(0f, 0.34f)
    return EyeOverlayPaint(
        dimAlpha = dim,
        warmAlpha = warm
    )
}

internal fun resolveEyeProtectionStatusCopy(
    pluginEnabled: Boolean,
    isActive: Boolean,
    forceEnabled: Boolean,
    nightModeEnabled: Boolean,
    startHour: Int,
    endHour: Int,
    brightnessPercent: Int,
    warmPercent: Int,
    usageMinutes: Int,
    reminderEnabled: Boolean,
    nextReminderInMinutes: Int?
): EyeProtectionStatusCopy {
    if (!pluginEnabled) {
        return EyeProtectionStatusCopy(
            title = "护眼未开启",
            subtitle = "打开插件后会按定时或手动规则自动生效",
            isActive = false
        )
    }
    val tone = "亮度 ${brightnessPercent}% · 暖色 ${warmPercent}%"
    if (isActive) {
        val source = when {
            forceEnabled -> "手动开启"
            nightModeEnabled -> "定时至 ${formatClockHour(endHour)}"
            else -> "当前生效"
        }
        val usage = if (usageMinutes > 0) " · 已使用 ${usageMinutes} 分钟" else ""
        val rest = if (reminderEnabled && nextReminderInMinutes != null) {
            " · ${nextReminderInMinutes} 分钟后提醒休息"
        } else {
            ""
        }
        return EyeProtectionStatusCopy(
            title = "护眼已开启",
            subtitle = "$source · $tone$usage$rest",
            isActive = true
        )
    }
    if (nightModeEnabled) {
        return EyeProtectionStatusCopy(
            title = "定时护眼待机",
            subtitle = "将于 ${formatClockHour(startHour)} 自动开启 · $tone",
            isActive = false
        )
    }
    return EyeProtectionStatusCopy(
        title = "护眼待机",
        subtitle = "打开「立即开启护眼」或设定时段后生效",
        isActive = false
    )
}

internal fun tuningForPreset(preset: EyeCarePreset): EyeCareTuning {
    return when (preset) {
        EyeCarePreset.GENTLE -> EyeCareTuning(
            brightnessLevel = 0.88f,
            warmFilterStrength = 0.12f,
            reminderIntervalMinutes = 45
        )
        EyeCarePreset.BALANCED -> EyeCareTuning(
            brightnessLevel = 0.78f,
            warmFilterStrength = 0.22f,
            reminderIntervalMinutes = 30
        )
        EyeCarePreset.FOCUS -> EyeCareTuning(
            brightnessLevel = 0.65f,
            warmFilterStrength = 0.32f,
            reminderIntervalMinutes = 25
        )
        EyeCarePreset.CUSTOM -> EyeCareTuning(
            brightnessLevel = 0.78f,
            warmFilterStrength = 0.22f,
            reminderIntervalMinutes = 30
        )
    }
}

internal fun presetDescription(preset: EyeCarePreset): String {
    return when (preset) {
        EyeCarePreset.GENTLE -> "轻度降亮与暖色，适合傍晚和短时观看"
        EyeCarePreset.BALANCED -> "日常夜间观看的默认强度，兼顾色彩和舒适"
        EyeCarePreset.FOCUS -> "更深的暖色和更勤的休息提醒，适合长时间刷剧"
        EyeCarePreset.CUSTOM -> "按当前滑杆保存"
    }
}

internal fun buildCareReminderMessage(usageMinutes: Int): String {
    val messages = listOf(
        "你已经连续观看 ${usageMinutes} 分钟了，试着看向 6 米外 20 秒。",
        "眼睛已经很努力了，建议起身活动下肩颈，放松 1 分钟。",
        "喝一口温水，眨眨眼，再继续看会更舒服。"
    )
    return messages[usageMinutes % messages.size]
}

private fun wrapMinuteOfDay(minuteOfDay: Int): Int {
    return ((minuteOfDay % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
}

private fun minutesSinceStart(currentMinuteOfDay: Int, startMinute: Int): Int {
    return wrapMinuteOfDay(currentMinuteOfDay - startMinute)
}

private fun windowLengthMinutes(startMinute: Int, endMinute: Int): Int {
    val start = wrapMinuteOfDay(startMinute)
    val end = wrapMinuteOfDay(endMinute)
    return if (start == end) MINUTES_PER_DAY else wrapMinuteOfDay(end - start)
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + ((stop - start) * fraction.coerceIn(0f, 1f))
}
