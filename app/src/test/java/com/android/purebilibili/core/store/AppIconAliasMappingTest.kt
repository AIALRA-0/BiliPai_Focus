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
            "com.android.purebilibili.MainActivityAliasBiliPaiPink",
            resolveAppIconLauncherAlias(packageName, "icon_bilipai_pink")
        )
        assertEquals(
            "com.android.purebilibili.MainActivityAliasBiliPaiWhite",
            resolveAppIconLauncherAlias(packageName, "BiliPai White")
        )
        assertEquals(
            "com.android.purebilibili.MainActivityAliasBiliPaiMonet",
            resolveAppIconLauncherAlias(packageName, "BiliPai Monet")
        )
        assertEquals(
            "com.android.purebilibili.MainActivityAliasHeadphone",
            resolveAppIconLauncherAlias(packageName, "icon_headphone")
        )
        assertEquals(
            "com.android.purebilibili.MainActivityAlias3DLauncher",
            resolveAppIconLauncherAlias(packageName, "unknown")
        )
        assertEquals(
            "com.android.purebilibili.MainActivityAliasBiliPaiNoIcon",
            resolveAppIconLauncherAlias(packageName, "icon_bilipai", splashIconVisible = false)
        )
        assertEquals(
            "com.android.purebilibili.MainActivityAlias3DNoIcon",
            resolveAppIconLauncherAlias(packageName, "unknown", splashIconVisible = false)
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
        assertTrue(aliases.contains("com.android.purebilibili.MainActivityAliasBiliPaiPink"))
        assertTrue(aliases.contains("com.android.purebilibili.MainActivityAliasBiliPaiWhite"))
        assertTrue(aliases.contains("com.android.purebilibili.MainActivityAliasBiliPaiMonet"))
        assertTrue(aliases.contains("com.android.purebilibili.MainActivityAliasHeadphone"))
        assertTrue(aliases.contains("com.android.purebilibili.MainActivityAlias3D"))
        assertTrue(aliases.contains("com.android.purebilibili.MainActivityAliasBiliPaiNoIcon"))
        assertTrue(aliases.contains("com.android.purebilibili.MainActivityAlias3DNoIcon"))
        kotlin.test.assertFalse(aliases.contains("com.android.purebilibili.MainActivityAliasBlue"))
        kotlin.test.assertFalse(aliases.contains("com.android.purebilibili.MainActivityAliasNeon"))
        kotlin.test.assertFalse(aliases.contains("com.android.purebilibili.MainActivityAliasTelegramBlueCoin"))
        kotlin.test.assertFalse(aliases.contains("com.android.purebilibili.MainActivityAliasPink"))
        kotlin.test.assertFalse(aliases.contains("com.android.purebilibili.MainActivityAliasPurple"))
        kotlin.test.assertFalse(aliases.contains("com.android.purebilibili.MainActivityAliasGreen"))
        kotlin.test.assertFalse(aliases.contains("com.android.purebilibili.MainActivityAliasFlatMaterial"))
        kotlin.test.assertFalse(aliases.contains("com.android.purebilibili.MainActivityAliasRetro"))
    }
}
