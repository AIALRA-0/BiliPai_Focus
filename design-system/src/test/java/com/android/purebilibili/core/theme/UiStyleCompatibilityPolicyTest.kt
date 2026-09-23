package com.android.purebilibili.core.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class UiStyleCompatibilityPolicyTest {

    @Test
    fun allLegacyCombinations_deriveExpectedAppUiStyle() {
        // 单向迁移：历史 iOS 值在运行时解析为默认主题 MIUIX，不再产生 iOS 选择。
        assertEquals(AppUiStyle.MIUIX, resolveUiStyle(UiPreset.IOS, AndroidNativeVariant.MATERIAL3))
        assertEquals(AppUiStyle.MIUIX, resolveUiStyle(UiPreset.IOS, AndroidNativeVariant.MIUIX))
        assertEquals(AppUiStyle.MATERIAL3, resolveUiStyle(UiPreset.MD3, AndroidNativeVariant.MATERIAL3))
        assertEquals(AppUiStyle.MIUIX, resolveUiStyle(UiPreset.MD3, AndroidNativeVariant.MIUIX))
    }

    @Test
    fun runtimeEnum_keepsOnlyTwoValues() {
        // 两值模型：运行时枚举不再包含 iOS，历史 iOS 只存在于迁移边界。
        assertEquals(
            setOf(AppUiStyle.MATERIAL3, AppUiStyle.MIUIX),
            AppUiStyle.entries.toSet()
        )
    }
}
