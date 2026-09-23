package com.android.purebilibili.core.util

import android.app.ApplicationExitInfo
import com.android.purebilibili.core.performance.isAbnormalProcessExitReason
import kotlin.test.Test
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
}
