package com.android.purebilibili.navigation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Guards the source handoff across the navigation callbacks, which are local to composition. */
class AppNavigationVideoSourceSnapshotTest {
    private fun navigationSource(): String = File(
        "src/main/java/com/android/purebilibili/navigation/AppNavigation.kt",
    ).readText()

    @Test
    fun clickSourceIsCapturedBeforeEitherEntryPointCanSuspend() {
        val source = navigationSource()
        val routeEntry = source.substringAfter("fun navigateToVideoRouteInNavigation3(")
            .substringBefore("fun navigateToVideoInNavigation3(")
        val videoEntry = source.substringAfter("fun navigateToVideoInNavigation3(")
            .substringBefore("fun navigateToHomeVideoInNavigation3(")
        for (entry in listOf(routeEntry, videoEntry)) {
            val capture = entry.indexOf(
                "val transitionSession = pendingTransitionSession ?: captureVideoCardNavigationSession(",
            )
            assertTrue(capture >= 0, "Each entry must preserve the click-time source")
            assertTrue(capture < entry.indexOf("coroutineScope.launch"))
            assertTrue(capture < entry.indexOf("resolvePortraitStoryNavigationSeed("))
        }
    }

    @Test
    fun directionAndOfflineDelegationsCarryTheSameClickSession() {
        val source = navigationSource()
            .substringAfter("fun navigateToVideoRouteInNavigation3(")
            .substringBefore("fun navigateToHomeVideoInNavigation3(")
        val delegatedCalls = Regex("(?<!fun )navigateToVideo(?:Route)?InNavigation3\\(")
            .findAll(source).map { start ->
                var depth = 1
                var end = start.range.last + 1
                while (depth > 0 && end < source.length) {
                    when (source[end++]) {
                        '(' -> depth++
                        ')' -> depth--
                    }
                }
                assertEquals(0, depth)
                source.substring(start.range.first, end)
            }.toList()
        // Two route-entry direction results, one offline retry, two video-entry direction
        // results, and the normal video-to-route delegation must all preserve the source.
        assertEquals(6, delegatedCalls.size)
        delegatedCalls.forEach { call ->
            assertTrue(call.contains("pendingTransitionSession = transitionSession,"))
        }
    }

    @Test
    fun committedRouteUsesTheFrozenSourceRatherThanRecapturingThePreviousHomeCard() {
        val routeEntry = navigationSource()
            .substringAfter("fun navigateToVideoRouteInNavigation3(")
            .substringBefore("fun navigateToVideoInNavigation3(")
        val commit = routeEntry.substringAfter("if (!canNavigate(false)) return")
        assertFalse(commit.contains("CardPositionManager."))
        assertTrue(commit.contains("session = transitionSession,"))
        assertTrue(commit.contains("sourceRoute = transitionSession.sourceRoute,"))
    }
}
