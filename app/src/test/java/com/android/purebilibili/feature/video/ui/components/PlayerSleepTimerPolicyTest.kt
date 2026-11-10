package com.android.purebilibili.feature.video.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerSleepTimerPolicyTest {
    @Test
    fun disabledTimerIncludesReferencePresets() {
        assertEquals(listOf(0, 15, 30, 45, 60), resolvePlayerSleepTimerPresets(null))
    }

    @Test
    fun customDurationIsInsertedInSortedOrderWithoutLosingPresets() {
        assertEquals(listOf(0, 15, 22, 30, 45, 60), resolvePlayerSleepTimerPresets(22))
        assertEquals(listOf(0, 15, 30, 45, 60, 1499), resolvePlayerSleepTimerPresets(1499))
    }

    @Test
    fun existingPresetAndDisabledDurationAreNotDuplicated() {
        assertEquals(listOf(0, 15, 30, 45, 60), resolvePlayerSleepTimerPresets(30))
        assertEquals(listOf(0, 15, 30, 45, 60), resolvePlayerSleepTimerPresets(0))
    }
}
