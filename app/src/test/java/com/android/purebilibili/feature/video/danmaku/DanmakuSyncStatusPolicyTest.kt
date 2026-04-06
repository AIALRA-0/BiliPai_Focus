package com.android.purebilibili.feature.video.danmaku

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DanmakuSyncStatusPolicyTest {

    @Test
    fun queuedSync_transitionsToPendingAndClearsTransientMessage() {
        val state = resolveDanmakuCloudSyncStateAfterQueued(
            DanmakuCloudSyncUiState(
                status = DanmakuCloudSyncStatus.FAILURE,
                message = "网络错误",
                lastSuccessAtMillis = 1200L
            )
        )

        assertEquals(DanmakuCloudSyncStatus.PENDING, state.status)
        assertEquals(null, state.message)
        assertEquals(1200L, state.lastSuccessAtMillis)
    }

    @Test
    fun startedSync_transitionsToSyncingWithoutDroppingLastSuccessTimestamp() {
        val state = resolveDanmakuCloudSyncStateAfterStarted(
            DanmakuCloudSyncUiState(
                status = DanmakuCloudSyncStatus.PENDING,
                lastSuccessAtMillis = 2200L
            )
        )

        assertEquals(DanmakuCloudSyncStatus.SYNCING, state.status)
        assertEquals(null, state.message)
        assertEquals(2200L, state.lastSuccessAtMillis)
    }

    @Test
    fun successfulSync_updatesStatusAndTimestamp() {
        val state = resolveDanmakuCloudSyncStateAfterResult(
            previous = DanmakuCloudSyncUiState(
                status = DanmakuCloudSyncStatus.SYNCING,
                lastSuccessAtMillis = 1200L
            ),
            result = Result.success(Unit),
            completedAtMillis = 5600L
        )

        assertEquals(DanmakuCloudSyncStatus.SUCCESS, state.status)
        assertEquals("已同步", state.message)
        assertEquals(5600L, state.lastSuccessAtMillis)
    }

    @Test
    fun failedSync_preservesLastSuccessAndExposesErrorMessage() {
        val state = resolveDanmakuCloudSyncStateAfterResult(
            previous = DanmakuCloudSyncUiState(
                status = DanmakuCloudSyncStatus.SYNCING,
                lastSuccessAtMillis = 1200L
            ),
            result = Result.failure(IllegalStateException("鉴权失败")),
            completedAtMillis = 5600L
        )

        assertEquals(DanmakuCloudSyncStatus.FAILURE, state.status)
        assertEquals("鉴权失败", state.message)
        assertEquals(1200L, state.lastSuccessAtMillis)
    }

    @Test
    fun newerManualRequest_bypassesDebounceGate() {
        assertTrue(
            shouldRunDanmakuManualCloudSync(
                manualRequestVersion = 3L,
                lastHandledManualRequestVersion = 2L
            )
        )
        assertFalse(
            shouldRunDanmakuManualCloudSync(
                manualRequestVersion = 3L,
                lastHandledManualRequestVersion = 3L
            )
        )
        assertFalse(
            shouldRunDanmakuManualCloudSync(
                manualRequestVersion = null,
                lastHandledManualRequestVersion = 3L
            )
        )
    }
}
