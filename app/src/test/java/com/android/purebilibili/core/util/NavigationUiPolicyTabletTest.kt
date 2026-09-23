package com.android.purebilibili.core.util

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigationUiPolicyTabletTest {

    @Test
    fun sidebarDisabledOnBookPosture() {
        val windowSizeClass = WindowSizeClass(
            widthSizeClass = WindowWidthSizeClass.Expanded,
            heightSizeClass = WindowHeightSizeClass.Medium,
            widthDp = 900.dp,
            heightDp = 800.dp
        )
        assertFalse(
            shouldUseSidebarNavigationForLayout(
                windowSizeClass = windowSizeClass,
                tabletUseSidebar = true,
                foldPosture = AppFoldPosture.Book
            )
        )
    }

    @Test
    fun sidebarDisabledOnTabletopPosture() {
        val windowSizeClass = WindowSizeClass(
            widthSizeClass = WindowWidthSizeClass.Expanded,
            heightSizeClass = WindowHeightSizeClass.Medium,
            widthDp = 900.dp,
            heightDp = 800.dp
        )
        assertFalse(
            shouldUseSidebarNavigationForLayout(
                windowSizeClass = windowSizeClass,
                tabletUseSidebar = true,
                foldPosture = AppFoldPosture.Tabletop
            )
        )
    }

    @Test
    fun sidebarEnabledOnFlatWhenUserPrefers() {
        val windowSizeClass = WindowSizeClass(
            widthSizeClass = WindowWidthSizeClass.Expanded,
            heightSizeClass = WindowHeightSizeClass.Medium,
            widthDp = 900.dp,
            heightDp = 800.dp
        )
        assertTrue(
            shouldUseSidebarNavigationForLayout(
                windowSizeClass = windowSizeClass,
                tabletUseSidebar = true,
                foldPosture = AppFoldPosture.Flat
            )
        )
    }

    @Test
    fun expandedRailDisabledOnFoldPosture() {
        val windowSizeClass = WindowSizeClass(
            widthSizeClass = WindowWidthSizeClass.Large,
            heightSizeClass = WindowHeightSizeClass.Medium,
            widthDp = 1300.dp,
            heightDp = 800.dp
        )
        assertFalse(
            shouldUseExpandedNavigationRailForLayout(
                windowSizeClass = windowSizeClass,
                foldPosture = AppFoldPosture.Book
            )
        )
    }

    @Test
    fun expandedRailEnabledOnLargeFlat() {
        val windowSizeClass = WindowSizeClass(
            widthSizeClass = WindowWidthSizeClass.Large,
            heightSizeClass = WindowHeightSizeClass.Medium,
            widthDp = 1300.dp,
            heightDp = 800.dp
        )
        assertTrue(
            shouldUseExpandedNavigationRailForLayout(
                windowSizeClass = windowSizeClass,
                foldPosture = AppFoldPosture.Flat
            )
        )
    }
}
