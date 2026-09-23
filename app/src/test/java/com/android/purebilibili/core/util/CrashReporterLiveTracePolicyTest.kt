package com.android.purebilibili.core.util

import java.io.IOException
import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReporterLiveTracePolicyTest {

    @Test
    fun sanitizeLiveTraceStage_collapsesWhitespaceAndLimitsLength() {
        val sanitized = sanitizeLiveTraceStage("  player   prepare   success  ")
        assertEquals("player_prepare_success", sanitized)

        val longInput = "a".repeat(120)
        assertEquals(80, sanitizeLiveTraceStage(longInput).length)
    }

    @Test
    fun shouldUpdateLiveTraceStage_onlyWhenStageChangesAndNotBlank() {
        assertFalse(shouldUpdateLiveTraceStage(lastStage = "prepare", nextStage = "prepare"))
        assertFalse(shouldUpdateLiveTraceStage(lastStage = "prepare", nextStage = "  "))
        assertTrue(shouldUpdateLiveTraceStage(lastStage = "prepare", nextStage = "playing"))
    }

    @Test
    fun liveSessionDurationMs_neverReturnsNegative() {
        assertEquals(0L, liveSessionDurationMs(nowElapsedMs = 100L, sessionStartElapsedMs = 120L))
        assertEquals(350L, liveSessionDurationMs(nowElapsedMs = 470L, sessionStartElapsedMs = 120L))
    }

    @Test
    fun shouldWriteCrashCustomKey_skipsRedundantWrites() {
        assertFalse(shouldWriteCrashCustomKey(previousValue = "same", nextValue = "same"))
        assertFalse(shouldWriteCrashCustomKey(previousValue = 12L, nextValue = 12L))
        assertTrue(shouldWriteCrashCustomKey(previousValue = "old", nextValue = "new"))
        assertTrue(shouldWriteCrashCustomKey(previousValue = null, nextValue = "first"))
    }

    @Test
    fun localCrashSnapshotPersistsRegardlessOfCrashTrackingConsent() {
        assertTrue(shouldPersistLocalCrashSnapshot(crashTrackingEnabled = true))
        assertTrue(shouldPersistLocalCrashSnapshot(crashTrackingEnabled = false))
    }

    @Test
    fun remoteCrashlyticsRequiresFirebaseAndCrashTrackingConsent() {
        assertTrue(
            shouldUseRemoteCrashlytics(
                firebaseConfigured = true,
                crashTrackingEnabled = true
            )
        )
        assertFalse(
            shouldUseRemoteCrashlytics(
                firebaseConfigured = false,
                crashTrackingEnabled = true
            )
        )
        assertFalse(
            shouldUseRemoteCrashlytics(
                firebaseConfigured = true,
                crashTrackingEnabled = false
            )
        )
    }

    @Test
    fun sensitiveCrashFieldsAndUserIdAreBlocked() {
        assertTrue(isSensitiveCrashCustomKey("video_bvid"))
        assertTrue(isSensitiveCrashCustomKey("danmaku_cid"))
        assertTrue(isSensitiveCrashCustomKey("live_room_id"))
        assertTrue(isSensitiveCrashCustomKey("live_room_title"))
        assertTrue(isSensitiveCrashCustomKey("live_anchor_name"))
        assertFalse(isSensitiveCrashCustomKey("video_error_type"))
        assertFalse(isSensitiveCrashCustomKey("live_last_stage"))

        assertEquals("", resolveCrashlyticsUserId(mid = 123456L))
        assertEquals("", resolveCrashlyticsUserId(mid = null))
    }

    @Test
    fun canceledApiRequestsAreNotReportedAsFailures() {
        assertFalse(
            shouldReportApiFailure(
                callCanceled = true,
                throwable = IOException("Socket closed")
            )
        )
        assertFalse(
            shouldReportApiFailure(
                callCanceled = false,
                throwable = IOException("Canceled")
            )
        )
        assertFalse(
            shouldReportApiFailure(
                callCanceled = false,
                throwable = IOException("request failed", CancellationException())
            )
        )
        assertTrue(
            shouldReportApiFailure(
                callCanceled = false,
                throwable = IOException("Unable to resolve host")
            )
        )
    }

    @Test
    fun mediaAssetEndpointsAreGroupedForRateLimiting() {
        assertEquals(
            "GET /bfs/face",
            normalizeApiErrorEndpoint("GET /bfs/face/a1/b2/avatar.jpg")
        )
        assertEquals(
            "GET /videoshotpvhdboss",
            normalizeApiErrorEndpoint("GET /videoshotpvhdboss/38581767200_wn163b-0001.jpg")
        )
        assertEquals(
            "GET /x/polymer/web-dynamic/v1/feed/all",
            normalizeApiErrorEndpoint("GET /x/polymer/web-dynamic/v1/feed/all")
        )
    }

    @Test
    fun nonFatalReportingStopsBeforeHeapIsExhausted() {
        val heapLimit = 256L * 1024 * 1024

        assertFalse(
            shouldRecordNonFatalEvent(
                maxMemoryBytes = heapLimit,
                totalMemoryBytes = heapLimit,
                freeMemoryBytes = 700L * 1024
            )
        )
        assertTrue(
            shouldRecordNonFatalEvent(
                maxMemoryBytes = heapLimit,
                totalMemoryBytes = 180L * 1024 * 1024,
                freeMemoryBytes = 12L * 1024 * 1024
            )
        )
    }
}
