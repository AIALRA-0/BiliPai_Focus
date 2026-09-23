package com.android.purebilibili.core.store

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.android.purebilibili.core.theme.AppUiStyle
import kotlinx.coroutines.flow.first

/**
 * 主题选择持久化与一次性迁移（theme_selection_v1）。
 *
 * 目标架构：运行时只保留 MIUIX / MATERIAL3 两值，枚举持久化使用稳定字符串。
 * 历史旧键 `ui_preset` / `android_native_variant_v1` 在首次读取时按
 * FRONTEND_ARCHITECTURE_THEME_SIMPLIFICATION_PLAN.md §5.2 迁移表单向迁移：
 * iOS、非法值 → MIUIX；全量缺失 → 新用户默认 Material 3；同一事务写入新键并删除旧键。
 */

internal val KEY_THEME_SELECTION = stringPreferencesKey("theme_selection_v1")

/** 解析新键的稳定字符串值。非法值（含历史 iOS 字符串）视为缺失。 */
internal fun parseThemeSelectionString(rawValue: String): AppUiStyle? = when (rawValue) {
    AppUiStyle.MATERIAL3.name -> AppUiStyle.MATERIAL3
    AppUiStyle.MIUIX.name -> AppUiStyle.MIUIX
    else -> null
}

/** 只读解析：优先新键，缺失时回退到旧键的两值解析（不写库）。 */
internal fun resolveThemeSelectionFromPreferences(
    preferences: Preferences,
    legacyPresetKey: Preferences.Key<Int>,
    legacyVariantKey: Preferences.Key<Int>
): AppUiStyle {
    return preferences[KEY_THEME_SELECTION]?.let(::parseThemeSelectionString)
        ?: AppUiStyle.fromLegacyValues(preferences[legacyPresetKey], preferences[legacyVariantKey])
}

internal data class ThemeSelectionMigrationResult(
    val selection: AppUiStyle,
    val needsWrite: Boolean,
    val migrationEdits: MutablePreferences.() -> Unit
)

/**
 * 按迁移表计算迁移结果（纯函数，可测试）：
 * - 新键优先；非法新键视为缺失。
 * - 旧 iOS、非法值 → MIUIX；全量缺失 → Material 3；MD3 组合保留有效选择。
 * - 同一事务写入新键并删除旧键。
 */
internal fun resolveThemeSelectionMigration(
    preferences: Preferences,
    legacyPresetKey: Preferences.Key<Int>,
    legacyVariantKey: Preferences.Key<Int>
): ThemeSelectionMigrationResult {
    val newKeyValue = preferences[KEY_THEME_SELECTION]?.let(::parseThemeSelectionString)
    return if (newKeyValue != null) {
        ThemeSelectionMigrationResult(
            selection = newKeyValue,
            needsWrite = preferences.contains(legacyPresetKey) ||
                preferences.contains(legacyVariantKey),
            migrationEdits = {
                remove(legacyPresetKey)
                remove(legacyVariantKey)
            }
        )
    } else {
        val legacyStyle = AppUiStyle.fromLegacyValues(
            preferences[legacyPresetKey],
            preferences[legacyVariantKey]
        )
        ThemeSelectionMigrationResult(
            selection = legacyStyle,
            needsWrite = true,
            migrationEdits = {
                this[KEY_THEME_SELECTION] = legacyStyle.name
                remove(legacyPresetKey)
                remove(legacyVariantKey)
            }
        )
    }
}

/** 一次性幂等迁移：主题 Flow 对外发射前执行，避免旧值导致首帧 iOS 或主题闪切。 */
internal suspend fun ensureThemeSelectionMigrated(
    context: Context,
    legacyPresetKey: Preferences.Key<Int>,
    legacyVariantKey: Preferences.Key<Int>
) {
    val snapshot = context.settingsDataStore.data.first()
    if (resolveThemeSelectionMigration(snapshot, legacyPresetKey, legacyVariantKey).needsWrite) {
        context.settingsDataStore.edit { preferences ->
            resolveThemeSelectionMigration(
                preferences,
                legacyPresetKey,
                legacyVariantKey
            ).migrationEdits(preferences)
        }
    }
}
