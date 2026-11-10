package com.android.purebilibili.data.repository

import org.junit.Assert.*
import org.junit.Test

class VideoInteractionStatusCacheTest {
    @Test
    fun completedLikeOperationCanConfirmStatusOnReentry() {
        val cache = VideoInteractionStatusCache()
        val version = cache.beginOperation("a", "like:1")
        assertTrue(cache.publish("a", "like:1", true, version))
        cache.finishOperation("a", "like:1", version)
        assertNull(cache.queryVersion("a", "like:1"))
        assertEquals(true, cache.confirmedOperationValue("a", "like:1"))
    }

    @Test
    fun pendingLikeCannotConfirmThePreviousCachedValue() {
        val cache = VideoInteractionStatusCache()
        val version = cache.beginOperation("a", "like:1")
        assertTrue(cache.publish("a", "like:1", false, version))
        cache.finishOperation("a", "like:1", version)
        cache.beginOperation("a", "like:1")
        assertNull(cache.confirmedOperationValue("a", "like:1"))
        assertNull(cache.confirmedOperationValue("another-account", "like:1"))
    }

    @Test
    fun expiryInvalidatesValueAndInFlightVersion() {
        var now = 0L
        val cache = VideoInteractionStatusCache(ttlMs = 100L, nowMs = { now })
        val version = cache.read("a", "like:1").second
        assertTrue(cache.publish("a", "like:1", true, version))
        now = 99L
        assertEquals(true, cache.read("a", "like:1").first)
        now = 100L
        val expired = cache.read("a", "like:1")
        assertNull(expired.first)
        assertTrue(expired.second > version)
        assertFalse(cache.publish("a", "like:1", false, version))
    }

    @Test
    fun readingRefreshesLruButNotTtl() {
        val cache = VideoInteractionStatusCache(capacity = 2, nowMs = { 0L })
        val first = cache.read("a", "like:1").second
        val second = cache.read("a", "like:2").second
        cache.read("a", "like:1")
        cache.read("a", "like:3")
        assertEquals(first, cache.read("a", "like:1").second)
        assertTrue(cache.read("a", "like:2").second > second)
    }

    @Test
    fun operationRejectsOldAndPendingQueriesEvenForSameValue() {
        val cache = VideoInteractionStatusCache(nowMs = { 0L })
        val query = cache.read("a", "like:1").second
        cache.publish("a", "like:1", true, query)
        val operation = cache.beginOperation("a", "like:1")
        assertFalse(cache.publish("a", "like:1", true, query, query = true))
        assertNull(cache.queryVersion("a", "like:1"))
        assertFalse(cache.publish("a", "like:1", true, operation, query = true))
        assertTrue(cache.publish("a", "like:1", false, operation))
        assertEquals(false, cache.read("a", "like:1").first)
    }

    @Test
    fun sessionChangeAndEvictionRejectOldReplies() {
        val cache = VideoInteractionStatusCache(capacity = 1, nowMs = { 0L })
        val old = cache.beginOperation("a", "follow:1")
        cache.read("a", "follow:2")
        assertFalse(cache.publish("a", "follow:1", true, old))
        val version = cache.read("a", "follow:1").second
        cache.read("b", "follow:1")
        assertFalse(cache.publish("a", "follow:1", true, version))
        assertNull(cache.read("b", "follow:1").first)
    }

    @Test
    fun operationExpiryAndReleaseDoNotRestoreStaleVersion() {
        var now = 0L
        val cache = VideoInteractionStatusCache(ttlMs = 100L, pendingTtlMs = 100L, nowMs = { now })
        val version = cache.beginOperation("a", "like:1")
        now = 100L
        assertFalse(cache.publish("a", "like:1", true, version))
        cache.finishOperation("a", "like:1", version)
        assertTrue(requireNotNull(cache.queryVersion("a", "like:1")) > version)
    }

    @Test
    fun completedOperationRejectsSameValueQueriesUntilExpiry() {
        var now = 0L
        val cache = VideoInteractionStatusCache(ttlMs = 100L, nowMs = { now })
        val version = cache.beginOperation("a", "like:1")
        assertTrue(cache.publish("a", "like:1", true, version))
        cache.finishOperation("a", "like:1", version)
        now = 99L
        assertNull(cache.queryVersion("a", "like:1"))
        assertFalse(cache.publish("a", "like:1", true, version, query = true))
        now = 100L
        assertNull(cache.read("a", "like:1").first)
        assertTrue(requireNotNull(cache.queryVersion("a", "like:1")) > version)
    }

    @Test
    fun failedUnknownOperationReleasesQueryWithNewVersion() {
        val cache = VideoInteractionStatusCache(nowMs = { 0L })
        val operation = cache.beginOperation("a", "follow:1")
        cache.finishOperation("a", "follow:1", operation)
        val query = requireNotNull(cache.queryVersion("a", "follow:1"))
        assertTrue(query > operation)
        assertFalse(cache.publish("a", "follow:1", true, operation))
        assertTrue(cache.publish("a", "follow:1", false, query, query = true))
    }

    @Test
    fun failedKnownOperationReleasesQueryWithoutExtendingTtl() {
        var now = 0L
        val cache = VideoInteractionStatusCache(ttlMs = 100L, nowMs = { now })
        val initial = requireNotNull(cache.queryVersion("a", "follow:1"))
        assertTrue(cache.publish("a", "follow:1", true, initial, query = true))
        now = 40L
        val operation = cache.beginOperation("a", "follow:1")
        now = 50L
        cache.finishOperation("a", "follow:1", operation)
        val query = requireNotNull(cache.queryVersion("a", "follow:1"))
        assertTrue(query > operation)
        assertEquals(true, cache.read("a", "follow:1").first)
        assertFalse(cache.publish("a", "follow:1", false, operation))
        now = 100L
        assertNull(cache.read("a", "follow:1").first)
    }

    @Test
    fun valueExpiryDoesNotExpireNewerPendingOperation() {
        var now = 0L
        val cache = VideoInteractionStatusCache(ttlMs = 100L, pendingTtlMs = 100L, nowMs = { now })
        val query = requireNotNull(cache.queryVersion("a", "like:1"))
        assertTrue(cache.publish("a", "like:1", false, query, query = true))
        now = 90L
        val operation = cache.beginOperation("a", "like:1")
        now = 100L
        assertNull(cache.read("a", "like:1").first)
        assertEquals(operation, cache.read("a", "like:1").second)
        assertNull(cache.queryVersion("a", "like:1"))
        assertTrue(cache.publish("a", "like:1", true, operation))
        now = 150L
        assertFalse(cache.publish("a", "like:1", true, operation))
        now = 200L
        assertNull(cache.read("a", "like:1").first)
    }

    @Test
    fun pendingOperationHasItsOwnExpiryAndFailureDoesNotReviveValue() {
        var now = 0L
        val cache = VideoInteractionStatusCache(ttlMs = 100L, pendingTtlMs = 100L, nowMs = { now })
        val initial = requireNotNull(cache.queryVersion("a", "follow:1"))
        assertTrue(cache.publish("a", "follow:1", true, initial, query = true))
        now = 90L
        val operation = cache.beginOperation("a", "follow:1")
        now = 100L
        cache.finishOperation("a", "follow:1", operation)
        assertNull(cache.read("a", "follow:1").first)
        assertNotNull(cache.queryVersion("a", "follow:1"))
        val next = cache.beginOperation("a", "follow:1")
        now = 200L
        assertFalse(cache.publish("a", "follow:1", false, next))
    }

    @Test
    fun followingSnapshotsRejectPendingConcurrentAndCompletedOperations() {
        val guard = FollowingSnapshotGuard()
        val account = "a"
        val original = requireNotNull(guard.snapshotVersion(account))
        var publications = 0
        guard.beginOperation(account)
        guard.beginOperation(account)
        assertNull(guard.snapshotVersion(account))
        assertFalse(guard.accept(account, original) { ++publications })
        guard.finishOperation(account)
        assertNull(guard.snapshotVersion(account))
        guard.finishOperation(account)
        assertFalse(guard.accept(account, original) { ++publications })
        val current = requireNotNull(guard.snapshotVersion(account))
        assertTrue(guard.accept(account, current) { ++publications })
        assertEquals(1, publications)
        guard.beginOperation(account)
        guard.finishOperation(account)
        assertFalse(guard.accept(account, current) { ++publications })
    }

    @Test
    fun followingSnapshotsAreIsolatedByAccount() {
        val guard = FollowingSnapshotGuard()
        val first = "first"
        val second = "second"
        val firstRevision = requireNotNull(guard.snapshotVersion(first))
        guard.beginOperation(first)
        val secondRevision = requireNotNull(guard.snapshotVersion(second))
        assertNull(guard.snapshotVersion(first))
        assertEquals(secondRevision, guard.snapshotVersion(second))
        assertFalse(guard.accept(first, firstRevision) {})
        assertTrue(guard.accept(second, secondRevision) {})
        guard.finishOperation(first)
        val rebuilt = requireNotNull(guard.snapshotVersion(first))
        assertTrue(rebuilt > firstRevision)
        assertFalse(guard.accept(first, firstRevision) {})
        assertTrue(guard.accept(first, rebuilt) {})
        assertTrue(guard.accept(second, secondRevision) {})
        assertEquals(secondRevision, guard.snapshotVersion(second))
    }

    @Test
    fun knownSnapshotIncludesFalseAndExpires() {
        var now = 0L
        val cache = VideoInteractionStatusCache(ttlMs = 100L, nowMs = { now })
        val version = cache.beginOperation("a", "follow:1")
        cache.publish("a", "follow:1", false, version)
        assertEquals(mapOf("follow:1" to false), cache.knownValues("a", "follow:"))
        assertTrue(cache.knownValues("b", "follow:").isEmpty())
        now = 100L
        assertTrue(cache.knownValues("a", "follow:").isEmpty())
    }
}
