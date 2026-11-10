package com.android.purebilibili.core.util

import android.app.ApplicationExitInfo
import com.android.purebilibili.core.performance.isAbnormalProcessExitReason
import com.android.purebilibili.core.performance.ProcessExitCandidate
import com.android.purebilibili.core.performance.selectLatestAbnormalMainProcessExitIndex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrashSnapshotPolicyTest {
    @Test
    fun `snapshot policy covers process exits unavailable to uncaught handler`() {
        assertTrue(isAbnormalProcessExitReason(ApplicationExitInfo.REASON_CRASH_NATIVE))
        assertTrue(isAbnormalProcessExitReason(ApplicationExitInfo.REASON_ANR))
        assertTrue(isAbnormalProcessExitReason(ApplicationExitInfo.REASON_LOW_MEMORY))
        assertTrue(isAbnormalProcessExitReason(ApplicationExitInfo.REASON_SIGNALED))
        assertFalse(isAbnormalProcessExitReason(ApplicationExitInfo.REASON_USER_REQUESTED))
        assertFalse(isAbnormalProcessExitReason(ApplicationExitInfo.REASON_USER_STOPPED))
    }

    @Test
    fun `latest normal exit does not resurrect an older crash`() {
        assertNull(selectLatestAbnormalMainProcessExitIndex(listOf(
            ProcessExitCandidate("app", 100L, ApplicationExitInfo.REASON_CRASH),
            ProcessExitCandidate("app", 200L, ApplicationExitInfo.REASON_USER_REQUESTED),
        ), "app"))
    }

    @Test
    fun `latest main process exit is selected regardless of ordering or child crashes`() {
        assertEquals(2, selectLatestAbnormalMainProcessExitIndex(listOf(
            ProcessExitCandidate("app", 100L, ApplicationExitInfo.REASON_CRASH),
            ProcessExitCandidate("app:worker", 300L, ApplicationExitInfo.REASON_CRASH_NATIVE),
            ProcessExitCandidate("app", 200L, ApplicationExitInfo.REASON_SIGNALED),
        ), "app"))
        assertNull(selectLatestAbnormalMainProcessExitIndex(listOf(
            ProcessExitCandidate("app:worker", 300L, ApplicationExitInfo.REASON_CRASH_NATIVE),
        ), "app"))
        assertNull(selectLatestAbnormalMainProcessExitIndex(emptyList(), "app"))
    }
}
