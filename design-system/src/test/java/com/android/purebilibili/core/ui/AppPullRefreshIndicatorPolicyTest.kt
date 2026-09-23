package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AppUiStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class AppPullRefreshIndicatorPolicyTest {

    @Test
    fun refreshIndicatorRenderer_branchesByUiStyle() {
        assertEquals(
            AppPullRefreshIndicatorRenderer.MIUIX,
            resolveAppPullRefreshIndicatorRenderer(AppUiStyle.MIUIX),
        )
        assertEquals(
            AppPullRefreshIndicatorRenderer.MATERIAL3,
            resolveAppPullRefreshIndicatorRenderer(AppUiStyle.MATERIAL3),
        )
    }
}
