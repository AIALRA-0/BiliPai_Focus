package com.android.purebilibili.core.plugin

import com.android.purebilibili.plugin.sdk.PlayerPluginApi
import com.android.purebilibili.plugin.sdk.PlayerPluginSession
import com.android.purebilibili.plugin.sdk.PluginCapability
import com.android.purebilibili.plugin.sdk.PluginCapabilityManifest
import com.android.purebilibili.plugin.sdk.SkipAction
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerPluginSessionHostTest {
    @Test
    fun factoryFailureKeepsOtherPluginsAndLaterVideoUpdatesWorking() = runTest {
        val fixture = Fixture(this)
        val before = Definition("before")
        val broken = Definition("broken").apply { creating = { error("factory failed") } }
        val after = Definition("after")
        val definitions = listOf(before, broken, after)
        val first = PluginVideoKey("BV-first", 1)

        fixture.host.update(definitions, first)
        runCurrent()
        assertEquals(listOf("before", "after"), fixture.host.readySessions(first).map { it.definition.id })
        assertTrue(broken.sessions.isEmpty())

        broken.creating = {}
        fixture.host.update(definitions, first)
        runCurrent()
        assertEquals(listOf("before", "broken", "after"), fixture.host.readySessions(first).map { it.definition.id })
        assertEquals(1, before.sessions.size)
        assertEquals(1, after.sessions.size)

        val second = PluginVideoKey("BV-second", 2)
        fixture.host.update(definitions, second)
        runCurrent()
        assertEquals(3, fixture.host.readySessions(second).size)
        assertTrue(fixture.host.readySessions(first).isEmpty())
        definitions.forEach { assertEquals(1, it.sessions.first().endCount) }
        fixture.host.close()
        definitions.forEach { assertEquals(1, it.sessions.last().endCount) }
    }

    @Test
    fun factoryCancellationPropagatesAndCleansUnpublishedSessions() = runTest {
        val fixture = Fixture(this)
        val before = Definition("before")
        val cancelled = Definition("cancelled").apply {
            creating = { throw kotlinx.coroutines.CancellationException("cancel creation") }
        }
        val after = Definition("after")
        assertFailsWith<kotlinx.coroutines.CancellationException> {
            fixture.host.update(listOf(before, cancelled, after), PluginVideoKey("BV-cancel", 1))
        }
        assertEquals(1, before.sessions.single().endCount)
        assertTrue(after.sessions.isEmpty())
        assertTrue(fixture.host.currentSessions().isEmpty())
        fixture.host.close()
        assertEquals(1, before.sessions.single().endCount)
    }

    @Test
    fun twoPlayersSharingOneDefinitionOwnIndependentSessions() = runTest {
        val definition = Definition("shared")
        val first = Fixture(this)
        val second = Fixture(this)
        val key = PluginVideoKey("BV-shared", 1)

        first.host.update(listOf(definition), key)
        second.host.update(listOf(definition), key)
        runCurrent()

        val firstEntry = first.host.readySessions(key).single()
        val secondEntry = second.host.readySessions(key).single()
        assertSame(definition, firstEntry.definition)
        assertSame(definition, secondEntry.definition)
        assertNotSame(firstEntry.session, secondEntry.session)
        firstEntry.session.onPositionUpdate(500)
        assertEquals(listOf(500L), definition.sessions[0].positions)
        assertTrue(definition.sessions[1].positions.isEmpty())
        assertEquals(listOf(key), definition.sessions[0].loads)
        assertEquals(listOf(key), definition.sessions[1].loads)

        first.host.close()
        assertEquals(1, definition.sessions[0].endCount)
        assertEquals(0, definition.sessions[1].endCount)
        assertTrue(second.host.isCurrent(secondEntry))
        assertEquals(listOf(secondEntry), second.host.readySessions(key))
        second.host.close()
        assertEquals(1, definition.sessions[1].endCount)
    }

    @Test
    fun equalKeyUpdatesKeepTheSessionLoadAndImmutableCachedSnapshots() = runTest {
        val fixture = Fixture(this)
        val definition = Definition("one")
        val key = PluginVideoKey("BV-same", 10)
        fixture.host.update(listOf(definition), key)
        val current = fixture.host.currentSessions()
        val loading = fixture.host.readySessions(key)
        fixture.host.update(listOf(definition), key.copy())
        assertSame(current, fixture.host.currentSessions())
        assertSame(loading, fixture.host.readySessions(key))
        runCurrent()
        val ready = fixture.host.readySessions(key)

        fixture.host.update(listOf(definition), key.copy())
        fixture.host.retryLoad(definition.id)
        fixture.host.retryLoad("missing")
        runCurrent()

        assertEquals(1, definition.sessions.size)
        assertEquals(listOf(key), definition.sessions.single().loads)
        assertSame(current, fixture.host.currentSessions())
        assertSame(ready, fixture.host.readySessions(key))
        assertSame(ready, fixture.host.readySessions(key.copy()))
        assertTrue(loading.isEmpty())
        assertFailsWith<UnsupportedOperationException> {
            (current as MutableList<PlayerPluginSessionEntry>).clear()
        }
        assertFailsWith<UnsupportedOperationException> {
            (ready as MutableList<PlayerPluginSessionEntry>).clear()
        }
        assertTrue(fixture.host.readySessions(key.copy(cid = 11)).isEmpty())
        fixture.host.close()
        assertEquals(1, current.size)
        assertEquals(1, ready.size)
    }

    @Test
    fun changingOnlyCidEndsTheOldSessionBeforeConstructingItsReplacement() = runTest {
        val fixture = Fixture(this)
        val definition = Definition("one")
        val firstKey = PluginVideoKey("BV-parts", 100)
        val secondKey = firstKey.copy(cid = 101)
        fixture.host.update(listOf(definition), firstKey)
        runCurrent()
        val firstEntry = fixture.host.currentSessions().single()
        definition.sessions.single().ending = {
            assertFalse(fixture.host.isCurrent(firstEntry))
            assertTrue(fixture.host.readySessions(firstKey).isEmpty())
            assertTrue(fixture.host.currentSessions().isEmpty())
        }
        definition.creating = {
            assertFalse(fixture.host.isCurrent(firstEntry))
            assertTrue(fixture.host.currentSessions().isEmpty())
            assertEquals(1, definition.sessions.single().endCount)
            assertEquals(listOf(firstEntry), fixture.ended)
        }

        fixture.host.update(listOf(definition), secondKey)

        val secondEntry = fixture.host.currentSessions().single()
        assertNotSame(firstEntry.session, secondEntry.session)
        assertEquals(1, definition.sessions[0].endCount)
        assertTrue(definition.sessions[1].loads.isEmpty())
        assertTrue(fixture.host.readySessions(secondKey).isEmpty())
        assertEquals(listOf(firstEntry), fixture.ended)
        runCurrent()
        assertEquals(listOf(secondKey), definition.sessions[1].loads)
        assertEquals(listOf(secondEntry), fixture.host.readySessions(secondKey))
        fixture.host.update(listOf(definition), null)
        fixture.host.close()
        assertEquals(listOf(firstEntry, secondEntry), fixture.ended)
        assertEquals(listOf(1, 1), definition.sessions.map { it.endCount })
    }

    @Test
    fun addingEnabledDefinitionLoadsAtTheSameKeyWithoutWaitingAndKeepsRegistrationOrder() = runTest {
        val fixture = Fixture(this)
        val gate = CompletableDeferred<Unit>()
        val first = Definition("first") { it.loading = { _, _ -> gate.await() } }
        val second = Definition("second")
        val key = PluginVideoKey("BV-added", 20)
        fixture.host.update(listOf(first), key)
        runCurrent()
        val firstEntry = fixture.host.currentSessions().single()

        fixture.host.update(listOf(first, second), key)
        runCurrent()

        val secondEntry = fixture.host.currentSessions()[1]
        assertSame(firstEntry, fixture.host.currentSessions()[0])
        assertEquals(listOf(secondEntry), fixture.host.readySessions(key))
        assertEquals(listOf(secondEntry), fixture.loaded)
        assertEquals(1, first.sessions.single().loads.size)
        gate.complete(Unit)
        runCurrent()
        assertEquals(listOf(firstEntry, secondEntry), fixture.host.readySessions(key))
        assertEquals(listOf(secondEntry, firstEntry), fixture.loaded)

        fixture.host.update(listOf(second, first), key)
        assertEquals(listOf(secondEntry, firstEntry), fixture.host.currentSessions())
        assertEquals(listOf(secondEntry, firstEntry), fixture.host.readySessions(key))
        assertEquals(1, first.sessions.single().loads.size)
        assertEquals(1, second.sessions.single().loads.size)
        assertTrue(fixture.ended.isEmpty())
        fixture.host.close()
    }

    @Test
    fun positionPollingOnlyDeliversProgressToSuccessfullyLoadedSessions() = runTest {
        val fixture = Fixture(this)
        val gate = CompletableDeferred<Unit>()
        val loading = Definition("loading") { it.loading = { _, _ -> gate.await() } }
        val failed = Definition("failed") { it.loading = { _, _ -> error("load failed") } }
        val ready = Definition("ready")
        val key = PluginVideoKey("BV-progress", 30)
        fixture.host.update(listOf(loading, failed, ready), key)
        runCurrent()

        for (entry in fixture.host.readySessions(key)) entry.session.onPositionUpdate(500)

        assertTrue(loading.sessions.single().positions.isEmpty())
        assertTrue(failed.sessions.single().positions.isEmpty())
        assertEquals(listOf(500L), ready.sessions.single().positions)
        assertEquals(listOf("ready"), fixture.host.readySessions(key).map { it.definition.id })
        assertEquals("failed", fixture.failures.single().first.definition.id)
        gate.complete(Unit)
        runCurrent()
        for (entry in fixture.host.readySessions(key)) entry.session.onPositionUpdate(1_000)
        assertEquals(listOf(1_000L), loading.sessions.single().positions)
        assertTrue(failed.sessions.single().positions.isEmpty())
        assertEquals(listOf(500L, 1_000L), ready.sessions.single().positions)
        fixture.host.close()
    }

    @Test
    fun lateNoncooperativeSuccessOrFailureAfterDisableCannotPublishOrNotify() = runTest {
        for (failLate in listOf(false, true)) {
            val fixture = Fixture(this)
            lateinit var continuation: Continuation<Unit>
            val definition = Definition("late") { session ->
                session.loading = { _, _ -> suspendCoroutine { continuation = it } }
            }
            val key = PluginVideoKey("BV-late", 40)
            fixture.host.update(listOf(definition), key)
            runCurrent()
            val removed = fixture.host.currentSessions().single()
            fixture.host.update(emptyList(), key)
            assertFalse(fixture.host.isCurrent(removed))
            assertEquals(1, definition.sessions.single().endCount)
            assertEquals(listOf(removed), fixture.ended)

            if (failLate) continuation.resumeWithException(IllegalStateException("late failure"))
            else continuation.resume(Unit)
            runCurrent()

            assertTrue(fixture.host.currentSessions().isEmpty())
            assertTrue(fixture.host.readySessions(key).isEmpty())
            assertTrue(fixture.loaded.isEmpty())
            assertTrue(fixture.failures.isEmpty())
            fixture.host.close()
            assertEquals(1, definition.sessions.single().endCount)
            assertEquals(listOf(removed), fixture.ended)
        }
    }

    @Test
    fun oneFailureDoesNotBlockOtherLoadsAndOnlyExplicitRetryReloadsTheFailedEntry() = runTest {
        val fixture = Fixture(this)
        val failure = IllegalStateException("independent failure")
        var shouldFail = true
        val failing = Definition("failing") { session ->
            session.loading = { _, _ -> if (shouldFail) throw failure }
        }
        val healthy = Definition("healthy")
        val pendingGate = CompletableDeferred<Unit>()
        val pending = Definition("pending") { it.loading = { _, _ -> pendingGate.await() } }
        val definitions = listOf(failing, healthy, pending)
        val key = PluginVideoKey("BV-retry", 50)
        fixture.host.update(definitions, key)
        runCurrent()
        val failedEntry = fixture.host.currentSessions()[0]
        val healthyEntry = fixture.host.currentSessions()[1]
        val pendingEntry = fixture.host.currentSessions()[2]
        assertSame(failure, fixture.failures.single().second)
        assertEquals(listOf(healthyEntry), fixture.host.readySessions(key))
        assertEquals(listOf(healthyEntry), fixture.loaded)

        fixture.host.update(definitions.toList(), key.copy())
        fixture.host.retryLoad(healthy.id)
        fixture.host.retryLoad(pending.id)
        fixture.host.retryLoad("missing")
        runCurrent()
        assertEquals(1, failing.sessions.single().loads.size)
        assertEquals(1, pending.sessions.single().loads.size)
        assertEquals(1, healthy.sessions.single().loads.size)
        assertEquals(1, fixture.failures.size)

        shouldFail = false
        fixture.host.retryLoad(failing.id)
        fixture.host.retryLoad(failing.id)
        runCurrent()

        assertSame(failedEntry, fixture.host.currentSessions()[0])
        assertEquals(1, failing.sessions.size)
        assertEquals(2, failing.sessions.single().loads.size)
        assertEquals(listOf(failedEntry, healthyEntry), fixture.host.readySessions(key))
        assertEquals(listOf(healthyEntry, failedEntry), fixture.loaded)
        pendingGate.complete(Unit)
        runCurrent()
        assertEquals(listOf(failedEntry, healthyEntry, pendingEntry), fixture.host.readySessions(key))
        assertTrue(fixture.ended.isEmpty())
        fixture.host.close()
    }

    @Test
    fun disableThenReenableCreatesANewSessionAndAnOldLoadCannotReadyIt() = runTest {
        val fixture = Fixture(this)
        lateinit var oldLoad: Continuation<Unit>
        var firstSession = true
        val definition = Definition("reenabled") { session ->
            if (firstSession) {
                firstSession = false
                session.loading = { _, _ -> suspendCoroutine { oldLoad = it } }
            }
        }
        val key = PluginVideoKey("BV-reenable", 60)
        fixture.host.update(listOf(definition), key)
        runCurrent()
        val oldEntry = fixture.host.currentSessions().single()
        fixture.host.update(emptyList(), key)
        fixture.host.retryLoad(definition.id)
        fixture.host.update(listOf(definition), key)
        val newEntry = fixture.host.currentSessions().single()
        assertNotSame(oldEntry.session, newEntry.session)
        assertFalse(fixture.host.isCurrent(oldEntry))
        assertTrue(fixture.host.isCurrent(newEntry))
        runCurrent()
        val ready = fixture.host.readySessions(key)
        assertEquals(listOf(newEntry), ready)

        oldLoad.resume(Unit)
        runCurrent()

        assertSame(ready, fixture.host.readySessions(key))
        assertEquals(listOf(newEntry), fixture.loaded)
        assertTrue(fixture.failures.isEmpty())
        assertEquals(listOf(oldEntry), fixture.ended)
        assertEquals(listOf(1, 0), definition.sessions.map { it.endCount })
        fixture.host.close()
        assertEquals(listOf(oldEntry, newEntry), fixture.ended)
        assertEquals(listOf(1, 1), definition.sessions.map { it.endCount })
    }

    @Test
    fun removalInvalidatesSnapshotsBeforeCancellationAndBothEndCallbacks() = runTest {
        val fixture = Fixture(this)
        val events = mutableListOf<String>()
        val key = PluginVideoKey("BV-invalidation", 70)
        lateinit var entry: PlayerPluginSessionEntry
        fun assertRemoved() {
            assertFalse(fixture.host.isCurrent(entry))
            assertTrue(fixture.host.currentSessions().isEmpty())
            assertTrue(fixture.host.readySessions(key).isEmpty())
        }
        val definition = Definition("cancelled") { session ->
            session.loading = { _, _ ->
                suspendCancellableCoroutine<Unit> { continuation ->
                    continuation.invokeOnCancellation {
                        assertRemoved()
                        events += "cancelled"
                    }
                }
            }
            session.ending = {
                assertRemoved()
                events += "video end"
            }
        }
        fixture.ending = {
            assertRemoved()
            events += "ended"
        }
        fixture.host.update(listOf(definition), key)
        entry = fixture.host.currentSessions().single()
        runCurrent()

        fixture.host.update(emptyList(), key)
        runCurrent()

        assertEquals(listOf("cancelled", "video end", "ended"), events)
        assertEquals(listOf(entry), fixture.ended)
        fixture.host.close()
        assertEquals(1, definition.sessions.single().endCount)
    }

    @Test
    fun endingFailuresAreIsolatedAndCloseIsIdempotentAndFinal() = runTest {
        val fixture = Fixture(this)
        val first = Definition("first") { it.ending = { error("session end failed") } }
        val second = Definition("second")
        val key = PluginVideoKey("BV-close", 80)
        fixture.ending = { if (it.definition === first) error("notification failed") }
        fixture.host.update(listOf(first, second), key)
        runCurrent()
        val entries = fixture.host.currentSessions()

        fixture.host.close()
        fixture.host.close()
        fixture.host.update(listOf(first, second), key)
        fixture.host.retryLoad(first.id)
        runCurrent()

        assertEquals(entries, fixture.ended)
        assertEquals(1, first.sessions.single().endCount)
        assertEquals(1, second.sessions.single().endCount)
        assertTrue(fixture.host.currentSessions().isEmpty())
        assertTrue(fixture.host.readySessions(key).isEmpty())
        assertTrue(entries.none(fixture.host::isCurrent))
        assertEquals(1, first.sessions.size)
        assertEquals(1, second.sessions.size)
    }

    @Test
    fun loadNotificationFailuresDoNotChangeReadinessOrBlockOtherSessions() = runTest {
        val fixture = Fixture(this)
        val failed = Definition("failed") { it.loading = { _, _ -> error("load failed") } }
        val healthy = Definition("healthy")
        fixture.loading = { error("loaded notification failed") }
        fixture.failing = { _, _ -> error("failure notification failed") }
        val key = PluginVideoKey("BV-callbacks", 90)

        fixture.host.update(listOf(failed, healthy), key)
        runCurrent()

        assertEquals(listOf("healthy"), fixture.host.readySessions(key).map { it.definition.id })
        assertEquals(1, fixture.loaded.size)
        assertEquals(1, fixture.failures.size)
        fixture.host.close()
        assertEquals(2, fixture.ended.size)
    }

    @Test
    fun cancellingTheSuppliedScopeCancelsLoadsWithoutReportingPluginFailure() = runTest {
        val owner = Job()
        val fixture = Fixture(CoroutineScope(coroutineContext + owner))
        var cancelled = false
        val definition = Definition("scope") { session ->
            session.loading = { _, _ ->
                suspendCancellableCoroutine<Unit> { continuation ->
                    continuation.invokeOnCancellation { cancelled = true }
                }
            }
        }
        val key = PluginVideoKey("BV-scope", 100)
        fixture.host.update(listOf(definition), key)
        runCurrent()

        owner.cancelAndJoin()
        runCurrent()
        fixture.host.retryLoad(definition.id)
        runCurrent()

        assertTrue(cancelled)
        assertTrue(fixture.loaded.isEmpty())
        assertTrue(fixture.failures.isEmpty())
        assertTrue(fixture.host.readySessions(key).isEmpty())
        assertEquals(1, definition.sessions.single().loads.size)
        fixture.host.close()
        assertEquals(1, definition.sessions.single().endCount)
        assertEquals(1, fixture.ended.size)
    }

    private class Fixture(scope: CoroutineScope) {
        val loaded = mutableListOf<PlayerPluginSessionEntry>()
        val failures = mutableListOf<Pair<PlayerPluginSessionEntry, Exception>>()
        val ended = mutableListOf<PlayerPluginSessionEntry>()
        var loading: (PlayerPluginSessionEntry) -> Unit = {}
        var failing: (PlayerPluginSessionEntry, Exception) -> Unit = { _, _ -> }
        var ending: (PlayerPluginSessionEntry) -> Unit = {}
        val host = PlayerPluginSessionHost(
            scope = scope,
            onLoaded = {
                loaded += it
                loading(it)
            },
            onLoadFailed = { entry, error ->
                failures += entry to error
                failing(entry, error)
            },
            onEnded = {
                ended += it
                ending(it)
            },
        )
    }

    private class Definition(
        id: String,
        private val configure: (RecordingSession) -> Unit = {},
    ) : PlayerPluginApi {
        override val capabilityManifest = PluginCapabilityManifest(
            pluginId = id,
            displayName = id,
            version = "1.0.0",
            apiVersion = 1,
            entryClassName = "test.$id",
            capabilities = setOf(PluginCapability.PLAYER_STATE, PluginCapability.PLAYER_CONTROL),
        )
        val sessions = mutableListOf<RecordingSession>()
        var creating: () -> Unit = {}

        override fun createSession(): PlayerPluginSession {
            creating()
            return RecordingSession().also {
                configure(it)
                sessions += it
            }
        }
    }

    private class RecordingSession : PlayerPluginSession {
        val loads = mutableListOf<PluginVideoKey>()
        val positions = mutableListOf<Long>()
        var loading: suspend (String, Long) -> Unit = { _, _ -> }
        var ending: () -> Unit = {}
        var endCount = 0
            private set

        override suspend fun onVideoLoad(bvid: String, cid: Long) {
            loads += PluginVideoKey(bvid, cid)
            loading(bvid, cid)
        }

        override suspend fun onPositionUpdate(positionMs: Long): SkipAction? {
            positions += positionMs
            return null
        }

        override fun onVideoEnd() {
            endCount++
            ending()
        }
    }
}
