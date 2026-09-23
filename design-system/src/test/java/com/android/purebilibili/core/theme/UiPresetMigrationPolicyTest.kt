package com.android.purebilibili.core.theme

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 覆盖 FRONTEND_ARCHITECTURE_THEME_SIMPLIFICATION_PLAN.md §5.2 的单向迁移表。
 * 目标：历史 iOS、非法值解析为 MIUIX；全量缺失使用 Material 3；MD3 组合保留有效选择。
 */
class UiPresetMigrationPolicyTest {

    private fun resolveLegacy(
        rawUiPreset: Int?,
        rawAndroidNativeVariant: Int?,
    ): AppUiStyle = AppUiStyle.fromLegacyValues(rawUiPreset, rawAndroidNativeVariant)

    // --- 迁移表：旧 iOS 值 → MIUIX ---

    @Test
    fun legacyIos_withMaterial3Variant_migratesToMiuix() {
        assertEquals(
            AppUiStyle.MIUIX,
            resolveLegacy(UiPreset.IOS.value, AndroidNativeVariant.MATERIAL3.value)
        )
    }

    @Test
    fun legacyIos_withMiuixVariant_migratesToMiuix() {
        assertEquals(
            AppUiStyle.MIUIX,
            resolveLegacy(UiPreset.IOS.value, AndroidNativeVariant.MIUIX.value)
        )
    }

    @Test
    fun legacyIos_withMissingVariant_migratesToMiuix() {
        assertEquals(AppUiStyle.MIUIX, resolveLegacy(UiPreset.IOS.value, null))
    }

    // --- 迁移表：MD3 组合保留有效选择 ---

    @Test
    fun legacyMd3_withMiuixVariant_keepsMiuix() {
        assertEquals(
            AppUiStyle.MIUIX,
            resolveLegacy(UiPreset.MD3.value, AndroidNativeVariant.MIUIX.value)
        )
    }

    @Test
    fun legacyMd3_withMaterial3Variant_keepsMaterial3() {
        assertEquals(
            AppUiStyle.MATERIAL3,
            resolveLegacy(UiPreset.MD3.value, AndroidNativeVariant.MATERIAL3.value)
        )
    }

    // --- 迁移表：全量缺失 → Material 3；部分缺失 / 非法值 → MIUIX ---

    @Test
    fun missingBothKeys_migratesToMaterial3() {
        assertEquals(AppUiStyle.MATERIAL3, resolveLegacy(null, null))
    }

    @Test
    fun missingPreset_withMaterial3Variant_migratesToMiuix() {
        assertEquals(
            AppUiStyle.MIUIX,
            resolveLegacy(null, AndroidNativeVariant.MATERIAL3.value)
        )
    }

    @Test
    fun invalidPresetValue_migratesToMiuix() {
        assertEquals(AppUiStyle.MIUIX, resolveLegacy(99, AndroidNativeVariant.MATERIAL3.value))
    }

    @Test
    fun invalidVariantValue_migratesToMiuix() {
        assertEquals(AppUiStyle.MIUIX, resolveLegacy(UiPreset.MD3.value, 99))
    }

    @Test
    fun removedLegacyVariantValue2_keepsMaterial3Mapping() {
        // 历史已移除的变体值 2 保留原有映射为 MATERIAL3。
        assertEquals(
            AppUiStyle.MATERIAL3,
            resolveLegacy(UiPreset.MD3.value, 2)
        )
    }

    // --- 运行时选择结果不得产生 iOS ---

    @Test
    fun noLegacyCombinationProducesIos() {
        val combos = listOf(
            Triple(UiPreset.IOS, AndroidNativeVariant.MATERIAL3, AppUiStyle.MIUIX),
            Triple(UiPreset.IOS, AndroidNativeVariant.MIUIX, AppUiStyle.MIUIX),
            Triple(UiPreset.MD3, AndroidNativeVariant.MATERIAL3, AppUiStyle.MATERIAL3),
            Triple(UiPreset.MD3, AndroidNativeVariant.MIUIX, AppUiStyle.MIUIX),
        )
        combos.forEach { (preset, variant, expected) ->
            assertEquals(expected, resolveUiStyle(preset, variant))
        }
        // 运行时枚举只保留两值，任何组合都不可能产生 iOS 选择。
        assertEquals(
            setOf(AppUiStyle.MATERIAL3, AppUiStyle.MIUIX),
            AppUiStyle.entries.toSet()
        )
    }

    // --- 默认值：枚举非法值仍兜底为 MIUIX 组合 ---

    @Test
    fun enumFallbacks_resolveToMiuixDefault() {
        assertEquals(UiPreset.MD3, UiPreset.fromValue(99))
        assertEquals(AndroidNativeVariant.MIUIX, AndroidNativeVariant.fromValue(99))
        assertEquals(
            AppUiStyle.MIUIX,
            resolveUiStyle(UiPreset.fromValue(99), AndroidNativeVariant.fromValue(99))
        )
    }
}
