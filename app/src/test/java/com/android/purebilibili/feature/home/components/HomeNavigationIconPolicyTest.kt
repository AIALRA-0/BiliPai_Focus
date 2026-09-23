package com.android.purebilibili.feature.home.components

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeNavigationIconPolicyTest {

    @Test
    fun `uses native Miuix icons for every home navigation role`() {
        listOf(
            "HOME",
            "DYNAMIC",
            "STORY",
            "HISTORY",
            "LISTEN_VIDEO",
            "PROFILE",
            "FAVORITE",
            "LIVE",
            "WATCHLATER",
            "SETTINGS",
            "PLUGINS",
            "FOLLOW",
            "POPULAR",
            "ANIME",
            "GAME",
            "PARTITION",
            "KNOWLEDGE",
            "TECH",
        ).forEach { tabId ->
            assertEquals(
                HomeNavigationIconSource.MIUIX,
                resolveMiuixPreferredHomeNavigationIconSource(tabId),
            )
        }
    }

}
