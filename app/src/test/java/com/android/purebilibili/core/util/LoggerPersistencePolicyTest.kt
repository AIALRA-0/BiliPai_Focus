package com.android.purebilibili.core.util

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoggerPersistencePolicyTest {

    @Test
    fun verboseRuntimeLogsAllowDebugBuildOrExplicitReleaseOptIn() {
        assertFalse(
            shouldEnableVerboseRuntimeLogs(
                isDebugBuild = false,
                verboseDebugLogsEnabled = true,
                enhancedDiagnosticLoggingEnabled = false,
            )
        )
        assertFalse(
            shouldEnableVerboseRuntimeLogs(
                isDebugBuild = true,
                verboseDebugLogsEnabled = false,
                enhancedDiagnosticLoggingEnabled = false,
            )
        )
        assertTrue(
            shouldEnableVerboseRuntimeLogs(
                isDebugBuild = true,
                verboseDebugLogsEnabled = true,
                enhancedDiagnosticLoggingEnabled = false,
            )
        )
        assertTrue(
            shouldEnableVerboseRuntimeLogs(
                isDebugBuild = false,
                verboseDebugLogsEnabled = false,
                enhancedDiagnosticLoggingEnabled = true,
            )
        )
    }

    @Test
    fun verboseLogcatOutputRemainsDebugOnly() {
        assertTrue(
            shouldEmitVerboseLogcat(
                isDebugBuild = true,
                verboseDebugLogsEnabled = true,
            )
        )
        assertFalse(
            shouldEmitVerboseLogcat(
                isDebugBuild = false,
                verboseDebugLogsEnabled = true,
            )
        )
    }

    @Test
    fun warningsAndErrorsPersistWithoutEnhancedLoggingButVerboseLogsRequireOptIn() {
        assertFalse(
            shouldPersistRuntimeLogEntry(
                level = "D",
                verboseRuntimeLogPersistenceEnabled = false
            )
        )
        assertTrue(
            shouldPersistRuntimeLogEntry(
                level = "W",
                verboseRuntimeLogPersistenceEnabled = false
            )
        )
        assertTrue(
            shouldPersistRuntimeLogEntry(
                level = "E",
                verboseRuntimeLogPersistenceEnabled = false
            )
        )
        assertTrue(
            shouldPersistRuntimeLogEntry(
                level = "D",
                verboseRuntimeLogPersistenceEnabled = true
            )
        )
        assertTrue(
            shouldPersistRuntimeLogEntry(
                level = "E",
                verboseRuntimeLogPersistenceEnabled = true
            )
        )
    }

    @Test
    fun runtimeLogCaptureKeepsWarningsAndErrorsInMemory() {
        assertFalse(
            shouldCaptureRuntimeLogEntry(
                level = "D",
                verboseRuntimeLogsEnabled = false
            )
        )
        assertTrue(
            shouldCaptureRuntimeLogEntry(
                level = "W",
                verboseRuntimeLogsEnabled = false
            )
        )
        assertTrue(
            shouldCaptureRuntimeLogEntry(
                level = "D",
                verboseRuntimeLogsEnabled = true
            )
        )
    }

    @Test
    fun resolvesStableFilePathsUnderLogDirectory() {
        val baseDir = File("/tmp/bilipai")

        assertEquals(
            File("/tmp/bilipai/logs"),
            resolveLogPersistenceDir(baseDir)
        )
        assertEquals(
            File("/tmp/bilipai/logs/basic.log"),
            resolveBasicLogFile(baseDir)
        )
        assertEquals(
            File("/tmp/bilipai/logs/runtime.log"),
            resolveRuntimeLogFile(baseDir)
        )
        assertEquals(
            File("/tmp/bilipai/logs/last_crash_log.txt"),
            resolveCrashSnapshotFile(baseDir)
        )
        assertEquals(
            File("/tmp/bilipai/logs/pending_crash.marker"),
            resolveCrashSnapshotMarkerFile(baseDir)
        )
        assertEquals(
            "player_diagnostic_20260329_155725.txt",
            resolvePlayerDiagnosticExportFileName(1_774_771_045_000L)
        )
    }

    @Test
    fun exportIncludesCrashOnlyOrSystemOnlyEvidenceWithoutRuntimeEntries() {
        assertTrue(hasExportableDiagnostics(0, true, 0, 0))
        assertTrue(hasExportableDiagnostics(0, false, 1, 0))
        assertTrue(hasExportableDiagnostics(0, false, 0, 1))
        assertTrue(hasExportableDiagnostics(1, false, 0, 0))
        assertFalse(hasExportableDiagnostics(0, false, 0, 0))
    }

    @Test
    fun rollingFilesBoundUtf8BytesAndKeepMostRecentEvidence() {
        val file = File.createTempFile("bilipai-log", ".txt")
        try {
            repeat(30) { appendRollingDiagnosticLog(file, "故障线索-$it\n", 128) }
            assertTrue(file.length() <= 128)
            assertTrue(file.readText().contains("故障线索-29"))
            assertFalse(file.readText().contains('\uFFFD'))
            appendRollingDiagnosticLog(file, "异常".repeat(200) + "END", 128)
            assertTrue(file.length() <= 128)
            assertTrue(file.readText().endsWith("END"))
            assertFalse(file.readText().contains('\uFFFD'))
        } finally {
            file.delete()
        }
    }

    @Test
    fun pendingCrashSnapshotRequiresMarkerAndSnapshotFile() {
        assertTrue(
            hasPendingCrashSnapshot(
                markerExists = true,
                snapshotExists = true
            )
        )
        assertFalse(
            hasPendingCrashSnapshot(
                markerExists = true,
                snapshotExists = false
            )
        )
        assertFalse(
            hasPendingCrashSnapshot(
                markerExists = false,
                snapshotExists = true
            )
        )
    }

    @Test
    fun crashSnapshotContentIncludesThrowableAndRecentLogs() {
        val entries = listOf(
            LogCollector.LogEntry(
                timestamp = 1_741_334_800_000L,
                level = "D",
                tag = "MainActivity",
                message = "before crash"
            ),
            LogCollector.LogEntry(
                timestamp = 1_741_334_801_000L,
                level = "E",
                tag = "VideoDetailScreen",
                message = "boom soon"
            )
        )

        val content = buildCrashSnapshotContent(
            throwable = IllegalStateException("Player exploded"),
            entries = entries,
            exportedAtMillis = 1_741_334_802_000L,
            appVersionName = "6.9.0",
            versionCode = 103,
            manufacturer = "nubia",
            model = "NX769J",
            androidRelease = "16",
            apiLevel = 36
        )

        assertTrue(content.contains("BiliPai 崩溃日志快照"))
        assertTrue(content.contains("IllegalStateException"))
        assertTrue(content.contains("Player exploded"))
        assertTrue(content.contains("before crash"))
        assertTrue(content.contains("boom soon"))
    }

    @Test
    fun sensitiveValuesAreRemovedBeforeLogsAreStoredOrExported() {
        val sanitized = sanitizeLogMessage(
            "SESSDATA=secret access_token=token123 mid=123456 " +
                "content=private-message keyword=private-search BV1AB411c7mD " +
                "Authorization: Bearer super-secret"
        )

        assertFalse(sanitized.contains("secret"))
        assertFalse(sanitized.contains("token123"))
        assertFalse(sanitized.contains("123456"))
        assertFalse(sanitized.contains("private-message"))
        assertFalse(sanitized.contains("private-search"))
        assertFalse(sanitized.contains("BV1AB411c7mD"))
        assertTrue(sanitized.contains("content=***"))
    }

    @Test
    fun crashSnapshotSanitizesThrowableTextToo() {
        val content = buildCrashSnapshotContent(
            throwable = IllegalStateException("access_token=secret-token mid=123456"),
            entries = emptyList(),
            exportedAtMillis = 1_741_334_802_000L,
            appVersionName = "6.9.0",
            versionCode = 103,
            manufacturer = "nubia",
            model = "NX769J",
            androidRelease = "16",
            apiLevel = 36,
        )

        assertFalse(content.contains("secret-token"))
        assertFalse(content.contains("123456"))
        assertTrue(content.contains("access_token=***"))
    }

    @Test
    fun logArtifactsToClear_includePrivateFilesAndCacheLogDirs() {
        val filesDir = File("/tmp/bilipai/files")
        val cacheDir = File("/tmp/bilipai/cache")

        assertEquals(
            listOf(
                File("/tmp/bilipai/files/logs"),
                File("/tmp/bilipai/cache/logs")
            ),
            resolveLogArtifactDirsToClear(
                filesDir = filesDir,
                cacheDir = cacheDir
            )
        )
    }
}
