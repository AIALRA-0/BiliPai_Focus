package com.android.purebilibili.feature.home.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomBarDynamicReminderBadgePolicyTest {

    @Test
    fun `only dynamic tab shows reminder badge`() {
        assertTrue(shouldShowBottomBarDynamicReminderBadge(BottomNavItem.DYNAMIC, unreadCount = 180))
        assertFalse(shouldShowBottomBarDynamicReminderBadge(BottomNavItem.DYNAMIC, unreadCount = 0))
        assertFalse(shouldShowBottomBarDynamicReminderBadge(null, unreadCount = 180))

        BottomNavItem.values()
            .filterNot { it == BottomNavItem.DYNAMIC }
            .forEach { item ->
                assertFalse(
                    shouldShowBottomBarDynamicReminderBadge(item, unreadCount = 180),
                    "${item.name} should not show the dynamic reminder badge"
                )
            }
    }

    @Test
    fun `dynamic reminder badge formats concrete unread count`() {
        assertEquals(null, formatBottomBarDynamicReminderBadge(0))
        assertEquals("1", formatBottomBarDynamicReminderBadge(1))
        assertEquals("180", formatBottomBarDynamicReminderBadge(180))
        assertEquals("999+", formatBottomBarDynamicReminderBadge(1_000))
    }

    @Test
    fun `dynamic reminder badge uses vivid red token`() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt"),
            File("src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        ).first { it.exists() }.readText()

        assertTrue(source.contains(".background(iOSRed, CircleShape)"))
        assertFalse(source.contains("Color.Red"))
    }

    @Test
    fun `android native item does not clip dynamic reminder badge`() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt"),
            File("src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        ).first { it.exists() }.readText()
        val itemSource = source
            .substringAfter("@Composable\nprivate fun RowScope.AndroidNativeBottomBarItem(")
            .substringBefore("private fun resolveMaterialBottomBarIcon(")

        assertFalse(itemSource.contains(".clip(resolveSharedBottomBarCapsuleShape())"))
    }

    @Test
    fun `dynamic reminder polling does not consume unread baseline`() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt"),
            File("src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")
        ).first { it.exists() }.readText()

        assertTrue(source.contains("getDynamicUpdateCount("))
        assertTrue(source.contains("advanceBaseline = false"))
    }
}
