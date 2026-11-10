package com.android.purebilibili.feature.settings

import com.android.purebilibili.navigation3.BiliPaiNavKey
import com.android.purebilibili.navigation3.legacyRouteToBiliPaiNavKey
import com.android.purebilibili.navigation3.toLegacyRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoginActivityNavigationPolicyTest {
    @Test
    fun settingsSearchAndRouteBridgeOpenTheSamePage() {
        val key = BiliPaiNavKey.LoginActivitySettings
        assertEquals(key, legacyRouteToBiliPaiNavKey(key.toLegacyRoute()))
        assertEquals(SettingsRootCategory.PRIVACY_PERMISSION, resolveSettingsRootCategoryForNavKey(key))
        assertEquals(2, resolveSettingsNavDepth(key.routeBase))
        assertEquals(key, resolveSettingsContainerTarget(SettingsSearchTarget.LOGIN_ACTIVITY))
        assertEquals(key, resolveSettingsSearchNavigation(SettingsSearchResult(
            target = SettingsSearchTarget.LOGIN_ACTIVITY,
            title = "登录设备与记录", subtitle = "", section = "隐私与安全",
        )))
        assertTrue(isSettingsSubtreeRoute(key.routeBase))
    }
}
