package com.android.purebilibili.feature.profile

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileLoadPolicyTest {

    @Test
    fun firstAutomaticLoad_startsWhenNothingHasLoadedYet() {
        assertTrue(
            shouldStartProfileLoad(
                hasLoadedOnce = false,
                isLoadInFlight = false,
                force = false
            )
        )
    }

    @Test
    fun duplicateAutomaticLoad_isSkippedAfterBootstrapStarted() {
        assertFalse(
            shouldStartProfileLoad(
                hasLoadedOnce = true,
                isLoadInFlight = false,
                force = false
            )
        )
    }

    @Test
    fun manualRefresh_canBypassBootstrapLoadGate() {
        assertTrue(
            shouldStartProfileLoad(
                hasLoadedOnce = true,
                isLoadInFlight = false,
                force = true
            )
        )
    }

    @Test
    fun inFlightLoad_rejectsDuplicateForceOrAutomaticRequests() {
        assertFalse(
            shouldStartProfileLoad(
                hasLoadedOnce = false,
                isLoadInFlight = true,
                force = false
            )
        )
        assertFalse(
            shouldStartProfileLoad(
                hasLoadedOnce = true,
                isLoadInFlight = true,
                force = true
            )
        )
    }
}
