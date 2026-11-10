package com.android.purebilibili.core.util

import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class HistoricalCrashLogsTest {
    @Test
    fun `latest launch boundary excludes an older process inside the time window`() {
        val launch = "[2026-10-23 20:39:30.000] I/StartupDiagnostics: stage=application_start, app=0.5.5"
        val recent = "[2026-10-23 20:39:45.000] I/ProcessContext: screen=HomeScreen"
        val exit = requireNotNull(SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
            .parse("2026-10-23 20:39:46.000")).time
        assertEquals(listOf(launch, recent), selectLogsBeforeProcessExit(listOf(
            "[2026-10-23 20:39:20.000] E/Player: old process failure", launch, recent,
        ), exit))
    }

    @Test
    fun `historical snapshot excludes restart and old logs but retains multiline exception`() {
        val before = "[2026-10-23 20:39:45.000] E/Player: failure\n  at preserved.frame"
        val atExit = "[2026-10-23 20:39:46.000] I/ProcessContext: event=video_play"
        val entries = listOf(
            "[2026-10-23 20:39:48.000] I/StartupDiagnostics: stage=application_start",
            before, before, atExit,
            "[2026-10-23 20:00:00.000] I/StartupDiagnostics: stage=application_start",
            "not a timestamp",
        )
        val exit = requireNotNull(SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
            .parse("2026-10-23 20:39:46.000")).time
        assertEquals(listOf(before, atExit), selectLogsBeforeProcessExit(entries, exit))
    }
}
