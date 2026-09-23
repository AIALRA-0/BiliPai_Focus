package com.android.purebilibili.core.store

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.AppUiStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 阶段 2 DataStore 单向迁移测试：覆盖
 * FRONTEND_ARCHITECTURE_THEME_SIMPLIFICATION_PLAN.md §5.2 迁移表、
 * 新键优先、幂等与旧键删除。
 */
class ThemeSelectionMigrationPolicyTest {

    private val keyPreset = intPreferencesKey("ui_preset")
    private val keyVariant = intPreferencesKey("android_native_variant_v1")
    private val keySelection = stringPreferencesKey("theme_selection_v1")

    private fun migrate(prefs: androidx.datastore.preferences.core.Preferences) =
        resolveThemeSelectionMigration(prefs, keyPreset, keyVariant)

    // --- 新键优先 ---

    @Test
    fun newKeyWins_overLegacyIosValues() {
        val result = migrate(
            mutablePreferencesOf(
                keySelection to AppUiStyle.MIUIX.name,
                keyPreset to UiPreset.IOS.value,
                keyVariant to AndroidNativeVariant.MATERIAL3.value
            )
        )
        assertEquals(AppUiStyle.MIUIX, result.selection)
        assertTrue(result.needsWrite)
    }

    @Test
    fun newKeyMaterial3_isNotOverwrittenByLegacyValues() {
        val result = migrate(
            mutablePreferencesOf(
                keySelection to AppUiStyle.MATERIAL3.name,
                keyPreset to UiPreset.MD3.value,
                keyVariant to AndroidNativeVariant.MIUIX.value
            )
        )
        assertEquals(AppUiStyle.MATERIAL3, result.selection)
    }

    // --- 迁移表 ---

    @Test
    fun legacyIos_withMiuixVariant_migratesToMiuix() {
        val result = migrate(
            mutablePreferencesOf(
                keyPreset to UiPreset.IOS.value,
                keyVariant to AndroidNativeVariant.MIUIX.value
            )
        )
        assertEquals(AppUiStyle.MIUIX, result.selection)
    }

    @Test
    fun legacyIos_withMaterial3Variant_migratesToMiuix() {
        val result = migrate(
            mutablePreferencesOf(
                keyPreset to UiPreset.IOS.value,
                keyVariant to AndroidNativeVariant.MATERIAL3.value
            )
        )
        assertEquals(AppUiStyle.MIUIX, result.selection)
    }

    @Test
    fun legacyMd3_withMiuixVariant_keepsMiuix() {
        val result = migrate(
            mutablePreferencesOf(
                keyPreset to UiPreset.MD3.value,
                keyVariant to AndroidNativeVariant.MIUIX.value
            )
        )
        assertEquals(AppUiStyle.MIUIX, result.selection)
    }

    @Test
    fun legacyMd3_withMaterial3Variant_keepsMaterial3() {
        val result = migrate(
            mutablePreferencesOf(
                keyPreset to UiPreset.MD3.value,
                keyVariant to AndroidNativeVariant.MATERIAL3.value
            )
        )
        assertEquals(AppUiStyle.MATERIAL3, result.selection)
    }

    @Test
    fun missingKeys_migrateToMaterial3() {
        val result = migrate(mutablePreferencesOf())
        assertEquals(AppUiStyle.MATERIAL3, result.selection)
        assertTrue(result.needsWrite)
    }

    @Test
    fun invalidKeys_migrateToMiuix() {
        val result = migrate(
            mutablePreferencesOf(
                keyPreset to 99,
                keyVariant to 99
            )
        )
        assertEquals(AppUiStyle.MIUIX, result.selection)
    }

    @Test
    fun invalidNewKey_isTreatedAsMissing_andUsesMaterial3Default() {
        val result = migrate(
            mutablePreferencesOf(
                keySelection to "IOS"
            )
        )
        assertEquals(AppUiStyle.MATERIAL3, result.selection)
        assertTrue(result.needsWrite)
    }

    // --- 单事务写入新键并删除旧键 ---

    @Test
    fun migrationWritesNewKeyAndRemovesLegacyKeys_inOneEdit() {
        val prefs = mutablePreferencesOf(
            keyPreset to UiPreset.IOS.value,
            keyVariant to AndroidNativeVariant.MATERIAL3.value
        )
        val result = migrate(prefs)
        result.migrationEdits(prefs)

        assertEquals(AppUiStyle.MIUIX.name, prefs[keySelection])
        assertNull(prefs[keyPreset])
        assertNull(prefs[keyVariant])
    }

    @Test
    fun migrationWithExistingNewKey_onlyRemovesStaleLegacyKeys() {
        val prefs = mutablePreferencesOf(
            keySelection to AppUiStyle.MATERIAL3.name,
            keyPreset to UiPreset.MD3.value,
            keyVariant to AndroidNativeVariant.MIUIX.value
        )
        val result = migrate(prefs)
        result.migrationEdits(prefs)

        assertEquals(AppUiStyle.MATERIAL3.name, prefs[keySelection])
        assertNull(prefs[keyPreset])
        assertNull(prefs[keyVariant])
    }

    // --- 幂等 ---

    @Test
    fun rerunAfterMigration_isIdempotent() {
        val prefs = mutablePreferencesOf(
            keyPreset to UiPreset.IOS.value,
            keyVariant to AndroidNativeVariant.MATERIAL3.value
        )
        migrate(prefs).migrationEdits(prefs)

        val second = migrate(prefs)
        assertEquals(AppUiStyle.MIUIX, second.selection)
        assertFalse(second.needsWrite)
    }

    @Test
    fun freshInstallWithNoThemeData_isAlreadySettledWithMaterial3AfterFirstRun() {
        val prefs = mutablePreferencesOf()
        migrate(prefs).migrationEdits(prefs)

        val second = migrate(prefs)
        assertEquals(AppUiStyle.MATERIAL3, second.selection)
        assertFalse(second.needsWrite)
    }
}
