package com.android.purebilibili.core.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdaptiveSideNavigationRailIntegrationTest {

    @Test
    fun adaptiveNavigationUsesSharedRailPolicyWithoutLegacyAppImplementation() {
        val legacySource = File(
            "src/main/java/com/android/purebilibili/core/ui/AdaptiveNavigation.kt"
        ).takeIf { it.exists() } ?: File(
            "app/src/main/java/com/android/purebilibili/core/ui/AdaptiveNavigation.kt"
        )
        val sharedSource = listOf(
            File(
                "design-system/src/main/java/com/android/purebilibili/core/ui/" +
                    "AdaptiveSideNavigationRailPolicy.kt"
            ),
            File(
                "../design-system/src/main/java/com/android/purebilibili/core/ui/" +
                    "AdaptiveSideNavigationRailPolicy.kt"
            ),
        ).first { it.exists() }.readText()

        assertFalse(legacySource.exists())
        assertTrue(sharedSource.contains("resolveAdaptiveSideNavigationRailRenderer("))
        assertTrue(sharedSource.contains("AdaptiveSideNavigationRailRenderer.MIUIX"))
        assertTrue(sharedSource.contains("AdaptiveSideNavigationRailRenderer.MATERIAL3"))
    }
}
