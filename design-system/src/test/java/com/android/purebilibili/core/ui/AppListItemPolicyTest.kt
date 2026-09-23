package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AppUiStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class AppListItemPolicyTest {

    @Test
    fun `auto style follows runtime theme`() {
        // MATERIAL3 和 MIUIX 在 AUTO 模式下均默认原生条目
        assertEquals(
            AppListItemStyle.NATIVE,
            resolveAppListItemStyle(AppListItemStyle.AUTO, AppUiStyle.MATERIAL3),
        )
        assertEquals(
            AppListItemStyle.NATIVE,
            resolveAppListItemStyle(AppListItemStyle.AUTO, AppUiStyle.MIUIX),
        )
    }

    @Test
    fun `explicit style applies to both presets`() {
        listOf(AppUiStyle.MATERIAL3, AppUiStyle.MIUIX).forEach { uiStyle ->
            assertEquals(
                AppListItemStyle.NATIVE,
                resolveAppListItemStyle(AppListItemStyle.NATIVE, uiStyle),
            )
            assertEquals(
                AppListItemStyle.CUSTOM,
                resolveAppListItemStyle(AppListItemStyle.CUSTOM, uiStyle),
            )
        }
    }

    @Test
    fun `preference parsing falls back to auto`() {
        assertEquals(AppListItemStyle.CUSTOM, resolveAppListItemStylePreference("CUSTOM"))
        assertEquals(AppListItemStyle.NATIVE, resolveAppListItemStylePreference("NATIVE"))
        assertEquals(AppListItemStyle.AUTO, resolveAppListItemStylePreference(null))
        assertEquals(AppListItemStyle.AUTO, resolveAppListItemStylePreference("UNKNOWN_VALUE"))
    }
}
