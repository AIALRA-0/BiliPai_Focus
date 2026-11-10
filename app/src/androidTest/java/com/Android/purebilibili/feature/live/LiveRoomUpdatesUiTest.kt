package com.android.purebilibili.feature.live

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LiveRoomUpdatesUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun sameCountUpdatesStayInTheListAndDoNotInvalidateTheRoomShell() {
        val updates = MutableStateFlow(listOf("first"))
        var shellCommits = 0
        var countCommits = 0
        var latest = emptyList<String>()
        var count = -1
        rule.setContent {
            SideEffect { shellCommits++ }
            LiveRoomUpdates(updates) { items -> SideEffect { latest = items } }
            LiveRoomCountUpdates(updates) { value -> SideEffect { count = value; countCommits++ } }
        }
        rule.waitUntil { latest == listOf("first") && count == 1 }
        val initialShell = shellCommits
        val initialCount = countCommits
        rule.runOnIdle { updates.value = listOf("changed") }
        rule.waitUntil { latest == listOf("changed") }
        rule.runOnIdle {
            assertEquals(initialShell, shellCommits)
            assertEquals(initialCount, countCommits)
            updates.value = emptyList()
        }
        rule.waitUntil { latest.isEmpty() && count == 0 }
        rule.runOnIdle { assertEquals(initialShell, shellCommits) }
    }

    @Test
    fun rebindingUsesTheNewRoomsCurrentValueAndHiddenContentStopsCollecting() {
        val oldRoom = MutableStateFlow(listOf("old-room"))
        val newRoom = MutableStateFlow(emptyList<String>())
        var current by mutableStateOf(oldRoom)
        var visible by mutableStateOf(true)
        var latest = emptyList<String>()
        var count = -1
        rule.setContent {
            if (visible) {
                LiveRoomUpdates(current) { items -> SideEffect { latest = items } }
                LiveRoomCountUpdates(current) { value -> SideEffect { count = value } }
            }
        }
        rule.waitUntil { latest == listOf("old-room") && count == 1 }
        rule.runOnIdle { current = newRoom }
        rule.waitUntil { latest.isEmpty() && count == 0 && oldRoom.subscriptionCount.value == 0 }
        rule.runOnIdle { oldRoom.value = listOf("late", "old-room") }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(emptyList<String>(), latest); assertEquals(0, count) }
        rule.runOnIdle { visible = false }
        rule.waitUntil { newRoom.subscriptionCount.value == 0 }
        rule.runOnIdle { newRoom.value = listOf("new-room") }
        rule.runOnIdle { visible = true }
        rule.waitUntil { latest == listOf("new-room") && count == 1 }
    }
}
