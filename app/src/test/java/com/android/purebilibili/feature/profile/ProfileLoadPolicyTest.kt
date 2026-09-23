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
    fun inFlightLoad_rejectsAutomaticButAllowsForceToInvalidateOldLoad() {
        assertFalse(
            shouldStartProfileLoad(
                hasLoadedOnce = false,
                isLoadInFlight = true,
                force = false
            )
        )
        assertTrue(
            shouldStartProfileLoad(
                hasLoadedOnce = true,
                isLoadInFlight = true,
                force = true
            )
        )
    }

    @Test
    fun accountSessionRefresh_forcesExactlyOneProfileReload() {
        assertTrue(
            shouldForceProfileLoadForAccountSessionRefresh(
                isCurrentPage = true,
                accountSessionRefreshGeneration = 1,
                handledAccountSessionRefreshGeneration = 0
            )
        )
        assertFalse(
            shouldForceProfileLoadForAccountSessionRefresh(
                isCurrentPage = true,
                accountSessionRefreshGeneration = 1,
                handledAccountSessionRefreshGeneration = 1
            )
        )
        assertFalse(
            shouldForceProfileLoadForAccountSessionRefresh(
                isCurrentPage = false,
                accountSessionRefreshGeneration = 2,
                handledAccountSessionRefreshGeneration = 1
            )
        )
    }
}
