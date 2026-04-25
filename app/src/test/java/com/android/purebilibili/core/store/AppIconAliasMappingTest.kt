package com.android.purebilibili.core.store

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppIconAliasMappingTest {

    @Test
    fun resolveAppIconLauncherAlias_supportsCanonicalAndLegacyKeys() {
        val packageName = "com.android.purebilibili"

        assertEquals(
            "com.android.purebilibili.MainActivityAliasBiliPai",
            resolveAppIconLauncherAlias(packageName, "icon_bilipai")
        )
        assertEquals(
            "com.android.purebilibili.MainActivityAliasBiliPai",
            resolveAppIconLauncherAlias(packageName, "BiliPai")
        )
        assertEquals(
            "com.android.purebilibili.MainActivityAliasHeadphone",
            resolveAppIconLauncherAlias(packageName, "icon_headphone")
        )
        assertEquals(
            "com.android.purebilibili.MainActivityAlias3DLauncher",
            resolveAppIconLauncherAlias(packageName, "unknown")
        )
    }

    @Test
    fun resolveAppIconLauncherAlias_keepsStableComponentNamespaceForDebugBuilds() {
        assertEquals(
            "com.android.purebilibili.MainActivityAlias3DLauncher",
            resolveAppIconLauncherAlias("com.android.purebilibili.debug", "icon_3d")
        )
    }

    @Test
    fun allManagedAppIconLauncherAliases_containsBiliPaiAndHeadphone_withoutRemovedAliases() {
        val aliases = allManagedAppIconLauncherAliases("com.android.purebilibili")
        assertTrue(aliases.contains("com.android.purebilibili.MainActivityAliasBiliPai"))
        assertTrue(aliases.contains("com.android.purebilibili.MainActivityAliasHeadphone"))
        assertTrue(aliases.contains("com.android.purebilibili.MainActivityAlias3D"))
        kotlin.test.assertFalse(aliases.contains("com.android.purebilibili.MainActivityAliasFlatMaterial"))
        kotlin.test.assertFalse(aliases.contains("com.android.purebilibili.MainActivityAliasRetro"))
    }
}
