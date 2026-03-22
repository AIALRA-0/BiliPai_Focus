package com.android.purebilibili.core.store

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppIconAliasMappingTest {

    @Test
    fun resolveAppIconLauncherAlias_supportsCanonicalAndLegacyKeys() {
        val packageName = "com.android.purebilibili"

        assertEquals(
            "com.android.purebilibili.MainActivityAliasFlatMaterial",
            resolveAppIconLauncherAlias(packageName, "icon_flat_material")
        )
        assertEquals(
            "com.android.purebilibili.MainActivityAliasFlatMaterial",
            resolveAppIconLauncherAlias(packageName, "Flat Material")
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
    fun allManagedAppIconLauncherAliases_containsFlatMaterialAndHeadphone() {
        val aliases = allManagedAppIconLauncherAliases("com.android.purebilibili")
        assertTrue(aliases.contains("com.android.purebilibili.MainActivityAliasFlatMaterial"))
        assertTrue(aliases.contains("com.android.purebilibili.MainActivityAliasHeadphone"))
    }
}
