package com.android.purebilibili.core.plugin

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BuiltinPluginRegistryTest {
    @Test
    fun lastSuccessfulPreRegistrationRequestOverridesStoredRead() = runTest {
        val fixture = Fixture()
        fixture.registry.setEnabled("test", true)
        fixture.registry.setEnabled("test", false)
        fixture.registry.setEnabled("test", true)
        // A stale read must not override the last successfully persisted request.
        fixture.read = { false }
        val plugin = LifecyclePlugin()

        fixture.registry.register(plugin)

        assertTrue(plugin.active)
        assertTrue(fixture.registry.pluginsFlow.value.single().enabled)
        assertEquals(listOf(true, false, true), fixture.writes.map { it.second })
        assertTrue(fixture.notifications.isEmpty())
    }

    @Test
    fun failedPreRegistrationWritePropagatesAndPreservesLastSuccessfulRequest() = runTest {
        val fixture = Fixture()
        fixture.registry.setEnabled("test", true)
        val failure = IllegalStateException("write failed")
        fixture.beforeWrite = { _, _ -> throw failure }

        assertSame(failure, assertFailsWith<IllegalStateException> {
            fixture.registry.setEnabled("test", false)
        })
        fixture.read = { false }
        val plugin = LifecyclePlugin()
        fixture.registry.register(plugin)

        assertTrue(plugin.active)
        assertTrue(fixture.registry.pluginsFlow.value.single().enabled)
        assertEquals(listOf<Pair<String, Exception>>("test" to failure), fixture.failures)
        assertEquals(listOf("test" to true), fixture.writes)
    }

    @Test
    fun failedFirstPreRegistrationWriteDoesNotCreatePendingSuccess() = runTest {
        val fixture = Fixture()
        fixture.beforeWrite = { _, _ -> error("write failed") }
        assertFailsWith<IllegalStateException> {
            fixture.registry.setEnabled("test", true)
        }
        val plugin = LifecyclePlugin()

        fixture.registry.register(plugin)

        assertFalse(plugin.active)
        assertFalse(fixture.registry.pluginsFlow.value.single().enabled)
        assertTrue(plugin.transitions.isEmpty())
    }

    @Test
    fun preRegistrationWriteMustFinishBeforeRegistrationCanConsumeItsRequest() = runTest {
        val fixture = Fixture()
        val writeGate = CompletableDeferred<Unit>()
        fixture.beforeWrite = { _, _ -> writeGate.await() }
        fixture.read = { false }
        val request = launch { fixture.registry.setEnabled("test", true) }
        runCurrent()
        val plugin = LifecyclePlugin()
        val registration = launch { fixture.registry.register(plugin) }
        runCurrent()

        assertEquals(0, fixture.reads)
        assertFalse(registration.isCompleted)
        assertTrue(fixture.registry.pluginsFlow.value.isEmpty())
        assertFalse(plugin.active)
        writeGate.complete(Unit)
        request.join()
        registration.join()

        assertEquals(listOf(plugin.id to true), fixture.writes)
        assertTrue(plugin.active)
        assertTrue(fixture.registry.pluginsFlow.value.single().enabled)
    }

    @Test
    fun registrationDoesNotPublishOrBecomeReadyUntilLoadFinishes() = runTest {
        val fixture = Fixture(initialEnabled = true)
        val readGate = CompletableDeferred<Unit>()
        val enableGate = CompletableDeferred<Unit>()
        fixture.read = { readGate.await(); true }
        val plugin = LifecyclePlugin().apply { enabling = { enableGate.await() } }
        val ready = async { fixture.registry.awaitPluginReady(plugin.id) }
        val registration = launch { fixture.registry.register(plugin) }
        runCurrent()

        assertTrue(fixture.registry.pluginsFlow.value.isEmpty())
        assertFalse(plugin.active)
        assertFalse(ready.isCompleted)
        readGate.complete(Unit)
        runCurrent()
        assertTrue(plugin.active)
        assertTrue(fixture.registry.pluginsFlow.value.isEmpty())
        assertFalse(ready.isCompleted)
        assertFalse(registration.isCompleted)

        enableGate.complete(Unit)
        registration.join()
        ready.await()
        assertTrue(fixture.registry.pluginsFlow.value.single().enabled)
        assertTrue(fixture.notifications.isEmpty())
    }

    @Test
    fun duplicateIdDoesNotReadOrRunAnotherLifecycle() = runTest {
        val fixture = Fixture(initialEnabled = true)
        val first = LifecyclePlugin()
        val duplicate = LifecyclePlugin()
        fixture.registry.register(first)
        val original = fixture.registry.pluginsFlow.value
        fixture.registry.register(first)
        fixture.registry.register(duplicate)

        assertSame(original, fixture.registry.pluginsFlow.value)
        assertSame(first, fixture.registry.pluginsFlow.value.single().plugin)
        assertEquals(1, fixture.reads)
        assertEquals(listOf(true), first.transitions)
        assertTrue(duplicate.transitions.isEmpty())
    }

    @Test
    fun readFailureLeavesNoEntryButCompletesReadyBarrierAndAllowsLaterRegistration() = runTest {
        val fixture = Fixture(initialEnabled = true)
        val failure = IllegalStateException("read failed")
        fixture.read = { throw failure }
        val ready = async { fixture.registry.awaitPluginReady("test") }
        val plugin = LifecyclePlugin()
        fixture.registry.register(plugin)
        ready.await()

        assertTrue(fixture.registry.pluginsFlow.value.isEmpty())
        assertTrue(plugin.transitions.isEmpty())
        assertEquals(listOf<Pair<String, Exception>>("test" to failure), fixture.failures)
        fixture.read = { true }
        fixture.registry.register(plugin)
        assertTrue(fixture.registry.pluginsFlow.value.single().enabled)
    }

    @Test
    fun registrationEnableFailureCleansPartialEffectsWithoutOverwritingPersistedRequest() = runTest {
        val fixture = Fixture(initialEnabled = true)
        val failure = IllegalStateException("enable failed")
        val plugin = LifecyclePlugin().apply { enabling = { throw failure } }
        fixture.registry.register(plugin)
        fixture.registry.awaitPluginReady(plugin.id)

        assertFalse(plugin.active)
        assertEquals(listOf(true, false), plugin.transitions)
        assertFalse(fixture.registry.pluginsFlow.value.single().enabled)
        assertEquals(true, fixture.saved[plugin.id])
        assertTrue(fixture.writes.isEmpty())
        assertTrue(fixture.notifications.isEmpty())
        assertEquals(listOf<Pair<String, Exception>>(plugin.id to failure), fixture.failures)
    }

    @Test
    fun registrationCleanupFailureIsReportedSeparatelyAndDoesNotStrandReady() = runTest {
        val fixture = Fixture(initialEnabled = true)
        val enableFailure = IllegalStateException("enable failed")
        val cleanupFailure = IllegalArgumentException("cleanup failed")
        val plugin = LifecyclePlugin().apply {
            enabling = { throw enableFailure }
            disabling = { throw cleanupFailure }
        }
        fixture.registry.register(plugin)
        fixture.registry.awaitPluginReady(plugin.id)

        assertFalse(fixture.registry.pluginsFlow.value.single().enabled)
        assertEquals(setOf(enableFailure, cleanupFailure), fixture.failures.map { it.second }.toSet())
        assertTrue(fixture.writes.isEmpty())
    }

    @Test
    fun failedEnableAndDisableRestorePreviousExecutionStateWithoutPersisting() = runTest {
        for (initialEnabled in listOf(false, true)) {
            val fixture = Fixture(initialEnabled)
            val plugin = LifecyclePlugin()
            fixture.registry.register(plugin)
            val original = fixture.registry.pluginsFlow.value
            val failure = IllegalStateException("lifecycle failed")
            if (initialEnabled) plugin.disabling = { throw failure }
            else plugin.enabling = { throw failure }

            fixture.registry.setEnabled(plugin.id, !initialEnabled)

            assertEquals(initialEnabled, plugin.active)
            assertSame(original, fixture.registry.pluginsFlow.value)
            assertEquals(initialEnabled, fixture.saved[plugin.id])
            assertTrue(fixture.writes.isEmpty())
            assertTrue(fixture.notifications.isEmpty())
            assertEquals(listOf<Pair<String, Exception>>(plugin.id to failure), fixture.failures)
            assertEquals(listOf(!initialEnabled, initialEnabled), plugin.transitions.takeLast(2))
        }
    }

    @Test
    fun failedPersistenceRollsBackBothToggleDirectionsAndKeepsOriginalSnapshot() = runTest {
        for (initialEnabled in listOf(false, true)) {
            val fixture = Fixture(initialEnabled)
            val plugin = LifecyclePlugin()
            fixture.registry.register(plugin)
            val original = fixture.registry.pluginsFlow.value
            val failure = IllegalStateException("write failed")
            fixture.beforeWrite = { _, _ -> throw failure }

            fixture.registry.setEnabled(plugin.id, !initialEnabled)

            assertEquals(initialEnabled, plugin.active)
            assertSame(original, fixture.registry.pluginsFlow.value)
            assertEquals(initialEnabled, fixture.saved[plugin.id])
            assertTrue(fixture.notifications.isEmpty())
            assertEquals(listOf<Pair<String, Exception>>(plugin.id to failure), fixture.failures)
            assertEquals(listOf(!initialEnabled, initialEnabled), plugin.transitions.takeLast(2))
        }
    }

    @Test
    fun failedRollbackIsReportedWithoutPublishingRequestedState() = runTest {
        val fixture = Fixture()
        val plugin = LifecyclePlugin()
        fixture.registry.register(plugin)
        val writeFailure = IllegalStateException("write failed")
        val rollbackFailure = IllegalArgumentException("rollback failed")
        fixture.beforeWrite = { _, _ -> throw writeFailure }
        plugin.disabling = { throw rollbackFailure }

        fixture.registry.setEnabled(plugin.id, true)

        assertFalse(fixture.registry.pluginsFlow.value.single().enabled)
        assertEquals(false, fixture.saved[plugin.id])
        assertEquals(setOf(writeFailure, rollbackFailure), fixture.failures.map { it.second }.toSet())
        assertTrue(fixture.notifications.isEmpty())
    }

    @Test
    fun persistenceMustFinishBeforeNewStateAndNotificationArePublished() = runTest {
        val fixture = Fixture()
        val plugin = LifecyclePlugin()
        fixture.registry.register(plugin)
        val writeGate = CompletableDeferred<Unit>()
        fixture.beforeWrite = { _, _ -> writeGate.await() }
        val toggle = launch { fixture.registry.setEnabled(plugin.id, true) }
        runCurrent()

        assertTrue(plugin.active)
        assertFalse(fixture.registry.pluginsFlow.value.single().enabled)
        assertTrue(fixture.notifications.isEmpty())
        writeGate.complete(Unit)
        toggle.join()
        assertTrue(fixture.registry.pluginsFlow.value.single().enabled)
        assertEquals(listOf(plugin.id to true), fixture.notifications)
    }

    @Test
    fun notificationFailureDoesNotUndoCommittedStateOrPersistence() = runTest {
        val fixture = Fixture()
        val plugin = LifecyclePlugin()
        fixture.registry.register(plugin)
        val failure = IllegalStateException("notification failed")
        fixture.notify = { _, _ -> throw failure }
        fixture.registry.setEnabled(plugin.id, true)

        assertTrue(plugin.active)
        assertTrue(fixture.registry.pluginsFlow.value.single().enabled)
        assertEquals(true, fixture.saved[plugin.id])
        assertEquals(listOf(plugin.id to true), fixture.writes)
        assertEquals(listOf<Pair<String, Exception>>(plugin.id to failure), fixture.failures)
        assertEquals(listOf(true), plugin.transitions)
    }

    @Test
    fun successfulTogglePublishesNewSnapshotsWithoutChangingOlderOnes() = runTest {
        val fixture = Fixture()
        val first = LifecyclePlugin()
        val second = LifecyclePlugin("second")
        fixture.registry.register(first)
        val beforeRegistration = fixture.registry.pluginsFlow.value
        fixture.registry.register(second)
        val beforeEnable = fixture.registry.pluginsFlow.value
        fixture.registry.setEnabled(first.id, true)
        val beforeDisable = fixture.registry.pluginsFlow.value
        fixture.registry.setEnabled(first.id, false)
        val current = fixture.registry.pluginsFlow.value

        assertEquals(listOf(first), beforeRegistration.map { it.plugin })
        assertEquals(listOf(first, second), current.map { it.plugin })
        assertNotSame(beforeEnable, beforeDisable)
        assertNotSame(beforeDisable, current)
        assertFalse(beforeEnable.first().enabled)
        assertTrue(beforeDisable.first().enabled)
        assertFalse(current.first().enabled)
        assertFalse(current.last().enabled)
        assertEquals(listOf(first.id to true, first.id to false), fixture.notifications)
    }

    @Test
    fun repeatedStateDoesNotRunLifecyclePersistOrNotify() = runTest {
        val fixture = Fixture()
        val plugin = LifecyclePlugin()
        fixture.registry.register(plugin)
        val disabledSnapshot = fixture.registry.pluginsFlow.value
        fixture.registry.setEnabled(plugin.id, false)
        assertSame(disabledSnapshot, fixture.registry.pluginsFlow.value)
        assertTrue(plugin.transitions.isEmpty())
        assertTrue(fixture.writes.isEmpty())
        fixture.registry.setEnabled(plugin.id, true)
        val enabledSnapshot = fixture.registry.pluginsFlow.value
        fixture.registry.setEnabled(plugin.id, true)

        assertSame(enabledSnapshot, fixture.registry.pluginsFlow.value)
        assertEquals(listOf(true), plugin.transitions)
        assertEquals(listOf(plugin.id to true), fixture.writes)
        assertEquals(listOf(plugin.id to true), fixture.notifications)
    }

    @Test
    fun concurrentRegistrationAndToggleSerializeLifecycleWithoutReentry() = runTest {
        val fixture = Fixture(initialEnabled = true)
        val gate = CompletableDeferred<Unit>()
        val plugin = LifecyclePlugin().apply { enabling = { gate.await() } }
        val duplicate = LifecyclePlugin()
        val registration = launch { fixture.registry.register(plugin) }
        runCurrent()
        val toggle = launch { fixture.registry.setEnabled(plugin.id, false) }
        val duplicateRegistration = launch { fixture.registry.register(duplicate) }
        runCurrent()

        assertFalse(toggle.isCompleted)
        assertFalse(duplicateRegistration.isCompleted)
        assertEquals(listOf(true), plugin.transitions)
        assertTrue(fixture.writes.isEmpty())
        gate.complete(Unit)
        registration.join()
        toggle.join()
        duplicateRegistration.join()

        assertEquals(listOf(true, false), plugin.transitions)
        assertFalse(plugin.active)
        assertFalse(fixture.registry.pluginsFlow.value.single().enabled)
        assertTrue(duplicate.transitions.isEmpty())
        assertEquals(listOf(plugin.id to false), fixture.writes)
    }

    @Test
    fun cancelledDuplicateWaitingForLockCannotReleaseOriginalReadinessBarrier() = runTest {
        val fixture = Fixture(initialEnabled = true)
        val gate = CompletableDeferred<Unit>()
        val plugin = LifecyclePlugin().apply { enabling = { gate.await() } }
        val registration = launch { fixture.registry.register(plugin) }
        val ready = async { fixture.registry.awaitPluginReady(plugin.id) }
        runCurrent()
        val duplicate = launch { fixture.registry.register(LifecyclePlugin()) }
        runCurrent()
        duplicate.cancel()
        duplicate.join()
        runCurrent()

        assertFalse(ready.isCompleted)
        assertTrue(fixture.registry.pluginsFlow.value.isEmpty())
        gate.complete(Unit)
        registration.join()
        ready.await()
        assertTrue(fixture.registry.pluginsFlow.value.single().enabled)
    }

    @Test
    fun cancelledRegistrationWaitsForNonCancellableCleanupThenReleasesReadiness() = runTest {
        val fixture = Fixture(initialEnabled = true)
        val enableGate = CompletableDeferred<Unit>()
        val cleanupGate = CompletableDeferred<Unit>()
        val plugin = LifecyclePlugin().apply {
            enabling = { enableGate.await() }
            disabling = { cleanupGate.await() }
        }
        val registration = async { fixture.registry.register(plugin) }
        val ready = async { fixture.registry.awaitPluginReady(plugin.id) }
        runCurrent()
        registration.cancel()
        runCurrent()

        assertFalse(registration.isCompleted)
        assertFalse(ready.isCompleted)
        assertFalse(plugin.active)
        assertTrue(fixture.registry.pluginsFlow.value.isEmpty())
        cleanupGate.complete(Unit)
        assertFailsWith<CancellationException> { registration.await() }
        ready.await()
        assertFalse(fixture.registry.pluginsFlow.value.single().enabled)
        assertEquals(true, fixture.saved[plugin.id])
        assertTrue(fixture.notifications.isEmpty())
    }

    @Test
    fun cancelledToggleRestoresPriorStateInBothDirectionsAndRethrows() = runTest {
        for (initialEnabled in listOf(false, true)) {
            val fixture = Fixture(initialEnabled)
            val plugin = LifecyclePlugin()
            fixture.registry.register(plugin)
            val original = fixture.registry.pluginsFlow.value
            val lifecycleGate = CompletableDeferred<Unit>()
            val cleanupGate = CompletableDeferred<Unit>()
            if (initialEnabled) {
                plugin.disabling = { lifecycleGate.await() }
                plugin.enabling = { cleanupGate.await() }
            } else {
                plugin.enabling = { lifecycleGate.await() }
                plugin.disabling = { cleanupGate.await() }
            }
            val toggle = async { fixture.registry.setEnabled(plugin.id, !initialEnabled) }
            runCurrent()
            toggle.cancel()
            runCurrent()

            assertEquals(initialEnabled, plugin.active)
            assertFalse(toggle.isCompleted)
            assertSame(original, fixture.registry.pluginsFlow.value)
            cleanupGate.complete(Unit)
            assertFailsWith<CancellationException> { toggle.await() }
            assertSame(original, fixture.registry.pluginsFlow.value)
            assertEquals(initialEnabled, fixture.saved[plugin.id])
            assertTrue(fixture.writes.isEmpty())
            assertTrue(fixture.notifications.isEmpty())
        }
    }

    @Test
    fun cancelledPersistenceRollsBackLifecycleWithoutPublishingSuccess() = runTest {
        val fixture = Fixture()
        val plugin = LifecyclePlugin()
        fixture.registry.register(plugin)
        val gate = CompletableDeferred<Unit>()
        fixture.beforeWrite = { _, _ -> gate.await() }
        val toggle = async { fixture.registry.setEnabled(plugin.id, true) }
        runCurrent()
        assertTrue(plugin.active)
        toggle.cancel()
        assertFailsWith<CancellationException> { toggle.await() }

        assertFalse(plugin.active)
        assertFalse(fixture.registry.pluginsFlow.value.single().enabled)
        assertEquals(false, fixture.saved[plugin.id])
        assertTrue(fixture.notifications.isEmpty())
    }

    @Test
    fun cancelledUnknownWriteCannotLeaveAPendingEnabledRequest() = runTest {
        val fixture = Fixture()
        val gate = CompletableDeferred<Unit>()
        fixture.beforeWrite = { _, _ -> gate.await() }
        val toggle = async { fixture.registry.setEnabled("test", true) }
        runCurrent()
        toggle.cancel()
        assertFailsWith<CancellationException> { toggle.await() }
        fixture.beforeWrite = { _, _ -> }
        val plugin = LifecyclePlugin()
        fixture.registry.register(plugin)

        assertFalse(plugin.active)
        assertFalse(fixture.registry.pluginsFlow.value.single().enabled)
        assertTrue(fixture.writes.isEmpty())
        assertTrue(fixture.failures.single().second is CancellationException)
    }

    private class Fixture(initialEnabled: Boolean = false) {
        val saved = mutableMapOf("test" to initialEnabled)
        val writes = mutableListOf<Pair<String, Boolean>>()
        val failures = mutableListOf<Pair<String, Exception>>()
        val notifications = mutableListOf<Pair<String, Boolean>>()
        var reads = 0
        var read: suspend (String) -> Boolean = { saved[it] ?: false }
        var beforeWrite: suspend (String, Boolean) -> Unit = { _, _ -> }
        var notify: (BuiltinPlugin, Boolean) -> Unit = { _, _ -> }
        val registry = BuiltinPluginRegistry(
            readEnabled = { id ->
                reads++
                read(id)
            },
            writeEnabled = { id, enabled ->
                beforeWrite(id, enabled)
                saved[id] = enabled
                writes += id to enabled
            },
            onFailure = { id, error -> failures += id to error },
            onEnabledChanged = { plugin, enabled ->
                notifications += plugin.id to enabled
                notify(plugin, enabled)
            }
        )
    }

    /** Models side effects before suspension/failure rather than only counting callbacks. */
    private class LifecyclePlugin(override val id: String = "test") : BuiltinPlugin {
        override val name = id
        override val description = "Registry lifecycle test"
        override val version = "1.0.0"
        var active = false
            private set
        val transitions = mutableListOf<Boolean>()
        var enabling: suspend () -> Unit = {}
        var disabling: suspend () -> Unit = {}
        private var inLifecycle = false

        override suspend fun onEnable() = transition(true, enabling)
        override suspend fun onDisable() = transition(false, disabling)

        private suspend fun transition(enabled: Boolean, action: suspend () -> Unit) {
            check(!inLifecycle) { "Concurrent lifecycle entry" }
            inLifecycle = true
            try {
                active = enabled
                transitions += enabled
                action()
            } finally {
                inLifecycle = false
            }
        }
    }
}
