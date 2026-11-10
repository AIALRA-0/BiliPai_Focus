package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsEditStateTest {
    @Test
    fun untouchedDraftReceivesStoredConfiguration() {
        val state = SettingsEditState(listOf("HOME", "PROFILE"))
        val value by state
        state.acceptStoredValue(listOf("DYNAMIC", "HOME"))
        assertEquals(listOf("DYNAMIC", "HOME"), value)
    }

    @Test
    fun lateLoadAndOlderSaveCannotOverrideDragInProgress() {
        val state = SettingsEditState(listOf("HOME", "PROFILE", "DYNAMIC"))
        var value by state
        value = listOf("PROFILE", "HOME", "DYNAMIC")
        state.acceptStoredValue(listOf("HOME", "PROFILE", "DYNAMIC"))
        assertEquals(listOf("PROFILE", "HOME", "DYNAMIC"), value)
        value = listOf("PROFILE", "DYNAMIC", "HOME")
        state.acceptStoredValue(listOf("PROFILE", "HOME", "DYNAMIC"))
        assertEquals(listOf("PROFILE", "DYNAMIC", "HOME"), value)
    }

    @Test
    fun repeatedValueDoesNotMistakeOldReceiptForLatestConfirmation() {
        val state = SettingsEditState("A")
        var value by state
        value = "B"
        value = "A"
        state.acceptStoredValue("A")
        state.acceptStoredValue("B")
        assertEquals("A", value)
    }

    @Test
    fun explicitResetRemainsAuthoritativeUntilPageIsRecreated() {
        val state = SettingsEditState(setOf("HOME", "PROFILE"))
        var value by state
        value = emptySet()
        state.acceptStoredValue(setOf("HOME", "PROFILE"))
        assertEquals(emptySet(), value)

        val reopened = SettingsEditState(setOf("DYNAMIC"))
        val reopenedValue by reopened
        assertEquals(setOf("DYNAMIC"), reopenedValue)
    }
}
