package com.android.purebilibili.core.plugin.json

import android.content.Context

private const val JSON_PLUGIN_STATS_NOTIFICATION_PREFS = "json_plugin_stats_notification"
private const val KEY_ENABLED = "enabled"
private const val KEY_LAST_SUMMARY_PREFIX = "last_summary_"
private const val MAX_PLUGIN_NAMES_IN_BODY = 3

data class JsonPluginStatsNotificationConfig(
    val enabled: Boolean = false
)

internal data class JsonPluginStatsSummaryNotification(
    val title: String,
    val body: String,
    val nextSummaryStats: Map<String, Int>
)

internal fun buildJsonPluginStatsSummaryNotification(
    config: JsonPluginStatsNotificationConfig,
    plugins: List<LoadedJsonPlugin>,
    currentStats: Map<String, Int>,
    lastSummaryStats: Map<String, Int>
): JsonPluginStatsSummaryNotification? {
    if (!config.enabled) return null
    val pluginNames = plugins.associate { it.plugin.id to it.plugin.name }
    val deltas = currentStats
        .mapNotNull { (pluginId, currentCount) ->
            val delta = currentCount - lastSummaryStats.getOrDefault(pluginId, 0)
            if (delta > 0) pluginId to delta else null
        }
        .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { pluginNames[it.first] ?: it.first })
    if (deltas.isEmpty()) return null

    val total = deltas.sumOf { it.second }
    val detail = deltas
        .take(MAX_PLUGIN_NAMES_IN_BODY)
        .joinToString("、") { (pluginId, delta) ->
            "${pluginNames[pluginId] ?: pluginId} $delta"
        }
    val suffix = if (deltas.size > MAX_PLUGIN_NAMES_IN_BODY) {
        " 等 ${deltas.size} 个插件"
    } else {
        ""
    }
    return JsonPluginStatsSummaryNotification(
        title = "插件统计汇总",
        body = "上次汇总后已过滤 $total 项：$detail$suffix",
        nextSummaryStats = currentStats
    )
}

internal fun buildJsonPluginStatsTestNotification(): JsonPluginStatsSummaryNotification {
    return JsonPluginStatsSummaryNotification(
        title = "插件统计测试通知",
        body = "通知权限正常后，这里会汇总 JSON 规则插件过滤数量",
        nextSummaryStats = emptyMap()
    )
}

fun readJsonPluginStatsNotificationConfig(context: Context): JsonPluginStatsNotificationConfig {
    val prefs = context.applicationContext.getSharedPreferences(
        JSON_PLUGIN_STATS_NOTIFICATION_PREFS,
        Context.MODE_PRIVATE
    )
    return JsonPluginStatsNotificationConfig(
        enabled = prefs.getBoolean(KEY_ENABLED, false)
    )
}

fun persistJsonPluginStatsNotificationConfig(
    context: Context,
    config: JsonPluginStatsNotificationConfig
) {
    context.applicationContext
        .getSharedPreferences(JSON_PLUGIN_STATS_NOTIFICATION_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_ENABLED, config.enabled)
        .apply()
}

internal fun readLastJsonPluginStatsSummary(context: Context): Map<String, Int> {
    val prefs = context.applicationContext.getSharedPreferences(
        JSON_PLUGIN_STATS_NOTIFICATION_PREFS,
        Context.MODE_PRIVATE
    )
    return prefs.all.mapNotNull { (key, value) ->
        if (key.startsWith(KEY_LAST_SUMMARY_PREFIX) && value is Int) {
            key.removePrefix(KEY_LAST_SUMMARY_PREFIX) to value
        } else {
            null
        }
    }.toMap()
}

internal fun persistLastJsonPluginStatsSummary(
    context: Context,
    stats: Map<String, Int>
) {
    val prefs = context.applicationContext.getSharedPreferences(
        JSON_PLUGIN_STATS_NOTIFICATION_PREFS,
        Context.MODE_PRIVATE
    )
    val editor = prefs.edit()
    prefs.all.keys
        .filter { it.startsWith(KEY_LAST_SUMMARY_PREFIX) }
        .forEach { editor.remove(it) }
    stats.forEach { (pluginId, count) ->
        editor.putInt("$KEY_LAST_SUMMARY_PREFIX$pluginId", count)
    }
    editor.apply()
}
