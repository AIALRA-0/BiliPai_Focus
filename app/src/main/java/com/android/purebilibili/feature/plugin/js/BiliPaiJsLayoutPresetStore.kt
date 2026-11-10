package com.android.purebilibili.feature.plugin.js

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * `.bplayout` 布局预设：分享某个插件模块的「参数 + 布局」组合。
 * 文件是 JSON；导入后按 `pluginId/moduleId` 存入 SharedPreferences，
 * 插件内容页打开对应模块时套用（用户改过的参数仍然优先）。
 */
@Serializable
data class BiliPaiJsLayoutPreset(
    val formatVersion: Int = 1,
    val name: String = "",
    val pluginId: String,
    val moduleId: String,
    val layout: String = "list",
    val params: Map<String, String> = emptyMap()
)

object BiliPaiJsLayoutPresetStore {
    private const val PREFS_NAME = "bilipai_js_layout_presets"
    private const val KEY_PREFIX = "preset_"

    private val json = Json { ignoreUnknownKeys = true }

    fun parsePreset(text: String): Result<BiliPaiJsLayoutPreset> = runCatching {
        val preset = json.decodeFromString(BiliPaiJsLayoutPreset.serializer(), text)
        require(preset.formatVersion == 1) { "不支持的 .bplayout 版本: ${preset.formatVersion}" }
        require(preset.pluginId.isNotBlank()) { ".bplayout 缺少 pluginId" }
        require(preset.moduleId.isNotBlank()) { ".bplayout 缺少 moduleId" }
        require(preset.layout in listOf("list", "grid")) { ".bplayout 布局无效: ${preset.layout}" }
        preset
    }

    fun encodePreset(preset: BiliPaiJsLayoutPreset): String {
        return json.encodeToString(preset)
    }

    fun savePreset(context: Context, preset: BiliPaiJsLayoutPreset) {
        prefs(context).edit()
            .putString(prefsKey(preset.pluginId, preset.moduleId), encodePreset(preset))
            .apply()
    }

    fun readPreset(context: Context, pluginId: String, moduleId: String): BiliPaiJsLayoutPreset? {
        val raw = prefs(context).getString(prefsKey(pluginId, moduleId), null) ?: return null
        return runCatching { json.decodeFromString(BiliPaiJsLayoutPreset.serializer(), raw) }.getOrNull()
    }

    fun removePreset(context: Context, pluginId: String, moduleId: String) {
        prefs(context).edit().remove(prefsKey(pluginId, moduleId)).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun prefsKey(pluginId: String, moduleId: String): String {
        return KEY_PREFIX + safePart(pluginId) + "_" + safePart(moduleId)
    }

    private fun safePart(value: String): String {
        return value.replace(Regex("[^A-Za-z0-9_.-]"), "_")
    }
}
