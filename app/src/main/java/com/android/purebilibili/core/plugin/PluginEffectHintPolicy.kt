package com.android.purebilibili.core.plugin

internal const val PLUGIN_EFFECT_HINT_VISIBLE_MS = 2_400L
internal const val PLUGIN_EFFECT_HINT_FEED_COOLDOWN_MS = 10 * 60 * 1_000L
internal const val PLUGIN_EFFECT_HINT_DANMAKU_COOLDOWN_MS = 10 * 60 * 1_000L
internal const val PLUGIN_EFFECT_HINT_ENABLED_COOLDOWN_MS = 1_500L
internal const val PLUGIN_EFFECT_HINT_EYE_COOLDOWN_MS = 30_000L

internal const val PLUGIN_EFFECT_HINT_FEED_GROUP_ID = "feed_filter"
internal const val PLUGIN_EFFECT_HINT_DANMAKU_GROUP_ID = "danmaku_filter"

enum class PluginEffectHintKind {
    GENERIC,
    EYE_CARE,
    FEED_FILTER,
    DANMAKU
}

internal fun shouldShowPluginEffectHint(
    kind: PluginEffectHintKind,
    effectMatchHintsEnabled: Boolean
): Boolean {
    if (effectMatchHintsEnabled) return true
    return when (kind) {
        PluginEffectHintKind.FEED_FILTER,
        PluginEffectHintKind.DANMAKU -> false
        PluginEffectHintKind.GENERIC,
        PluginEffectHintKind.EYE_CARE -> true
    }
}

data class PluginEffectHint(
    val pluginId: String,
    val title: String,
    val subtitle: String? = null,
    val kind: PluginEffectHintKind = PluginEffectHintKind.GENERIC,
    val issuedAtMs: Long = 0L,
    val visibleDurationMs: Long = PLUGIN_EFFECT_HINT_VISIBLE_MS
)

internal fun shouldAcceptPluginEffectHint(
    lastAcceptedAtMs: Long?,
    nowMs: Long,
    cooldownMs: Long
): Boolean {
    if (cooldownMs <= 0L || lastAcceptedAtMs == null) return true
    return nowMs - lastAcceptedAtMs >= cooldownMs
}

internal fun resolvePluginEnabledEffectHint(
    pluginId: String,
    pluginName: String
): PluginEffectHint {
    return PluginEffectHint(
        pluginId = pluginId,
        title = "${pluginName}已启用",
        subtitle = "相关效果会在对应场景自动生效",
        kind = PluginEffectHintKind.GENERIC
    )
}

internal fun resolveFeedFilterEffectHint(
    removedCount: Int,
    pluginNames: List<String>
): PluginEffectHint? {
    if (removedCount <= 0 || pluginNames.isEmpty()) return null
    val uniqueNames = pluginNames.distinct()
    val title = if (uniqueNames.size == 1) {
        "${uniqueNames.first()}已生效"
    } else {
        "插件已过滤内容"
    }
    return PluginEffectHint(
        pluginId = PLUGIN_EFFECT_HINT_FEED_GROUP_ID,
        title = title,
        subtitle = "已隐藏 ${removedCount} 条内容",
        kind = PluginEffectHintKind.FEED_FILTER
    )
}

internal fun resolveDanmakuFilterEffectHint(
    pluginId: String,
    pluginName: String
): PluginEffectHint {
    return PluginEffectHint(
        pluginId = PLUGIN_EFFECT_HINT_DANMAKU_GROUP_ID,
        title = "${pluginName}已生效",
        subtitle = "已屏蔽干扰弹幕",
        kind = PluginEffectHintKind.DANMAKU
    )
}

internal fun resolveEyeProtectionEffectHint(
    forceEnabled: Boolean,
    endHour: Int
): PluginEffectHint {
    val subtitle = if (forceEnabled) {
        "暖色滤镜已开启，不影响触控"
    } else {
        "将持续至 ${formatClockHour(endHour)}，色温会缓慢变暖"
    }
    return PluginEffectHint(
        pluginId = "eye_protection",
        title = "夜间护眼已开启",
        subtitle = subtitle,
        kind = PluginEffectHintKind.EYE_CARE
    )
}

internal fun resolvePluginListActivityLabel(
    enabled: Boolean,
    unavailable: Boolean,
    effectActive: Boolean
): String? {
    if (!enabled || unavailable || !effectActive) return null
    return "生效中"
}

internal fun formatClockHour(hour: Int): String {
    val safeHour = hour.coerceIn(0, 23)
    val padded = safeHour.toString().padStart(2, '0')
    return "$padded:00"
}
