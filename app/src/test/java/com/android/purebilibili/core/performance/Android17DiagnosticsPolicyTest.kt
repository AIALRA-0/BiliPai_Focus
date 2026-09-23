package com.android.purebilibili.core.performance

import kotlin.test.Test
import kotlin.test.assertEquals

class Android17DiagnosticsPolicyTest {
    @Test
    fun `retention keeps newest three artifacts within size cap`() {
        val keep = selectProfilingArtifactPathsToKeep(
            artifacts = listOf(
                ProfilingArtifactSnapshot("old", 20, 1),
                ProfilingArtifactSnapshot("newest", 30, 4),
                ProfilingArtifactSnapshot("newer", 30, 3),
                ProfilingArtifactSnapshot("new", 30, 2)
            ),
            maxArtifacts = 3,
            maxTotalBytes = 100
        )

        assertEquals(setOf("newest", "newer", "new"), keep)
    }

    @Test
    fun `oversized artifact is skipped without blocking smaller files`() {
        val keep = selectProfilingArtifactPathsToKeep(
            artifacts = listOf(
                ProfilingArtifactSnapshot("oversized", 200, 3),
                ProfilingArtifactSnapshot("small", 40, 2),
                ProfilingArtifactSnapshot("smallest", 20, 1)
            ),
            maxArtifacts = 3,
            maxTotalBytes = 60
        )

        assertEquals(setOf("small", "smallest"), keep)
    }

    @Test
    fun `resolveProcessExitSubReasonLabel maps known subreasons`() {
        assertEquals("无", resolveProcessExitSubReasonLabel(0))
        assertEquals("系统内存压力", resolveProcessExitSubReasonLabel(6))
        assertEquals("CPU 占用过高", resolveProcessExitSubReasonLabel(7))
        assertEquals("广播未及时交付", resolveProcessExitSubReasonLabel(11))
        assertEquals("子原因(999)", resolveProcessExitSubReasonLabel(999))
    }

    @Test
    fun `AbnormalProcessExitException clears synthetic stack trace and embeds tombstone`() {
        val fakeTombstone = "signal 11 (SIGSEGV), code 1\n#00 pc 000000000021b3a4 /system/lib64/libhwui.so"
        val exception = AbnormalProcessExitException(
            message = "系统记录的上次异常退出：Native 崩溃",
            nativeTrace = fakeTombstone
        )

        assertEquals(0, exception.stackTrace.size)
        val traceString = exception.stackTraceToString()
        kotlin.test.assertTrue(traceString.contains("系统记录的上次异常退出：Native 崩溃"))
        kotlin.test.assertTrue(traceString.contains("----- 系统异常回溯 (Tombstone / Trace) -----"))
        kotlin.test.assertTrue(traceString.contains("libhwui.so"))
    }
}
