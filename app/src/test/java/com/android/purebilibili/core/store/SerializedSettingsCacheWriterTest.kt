package com.android.purebilibili.core.store

import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

class SerializedSettingsCacheWriterTest {

    @Test
    fun `concurrent writes keep each datastore and cache pair together`() = runBlocking {
        val writer = SerializedSettingsCacheWriter()
        val events = Collections.synchronizedList(mutableListOf<String>())
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()

        val first = async(Dispatchers.Default) {
            writer.write(
                writeDataStore = {
                    events += "first-datastore"
                    firstStarted.complete(Unit)
                    releaseFirst.await()
                },
                writeCache = {
                    events += "first-cache"
                },
            )
        }
        firstStarted.await()
        val second = async(Dispatchers.Default) {
            writer.write(
                writeDataStore = { events += "second-datastore" },
                writeCache = { events += "second-cache" },
            )
        }

        yield()
        assertEquals(listOf("first-datastore"), events.toList())
        releaseFirst.complete(Unit)
        first.await()
        second.await()

        assertEquals(
            listOf("first-datastore", "first-cache", "second-datastore", "second-cache"),
            events.toList(),
        )
    }

    @Test
    fun `cancellation after datastore starts still completes cache write`() = runBlocking {
        val writer = SerializedSettingsCacheWriter()
        val dataStoreStarted = CompletableDeferred<Unit>()
        val releaseDataStore = CompletableDeferred<Unit>()
        val cacheCompleted = CompletableDeferred<Unit>()

        val job = launch(Dispatchers.Default) {
            writer.write(
                writeDataStore = {
                    dataStoreStarted.complete(Unit)
                    releaseDataStore.await()
                },
                writeCache = { cacheCompleted.complete(Unit) },
            )
        }

        dataStoreStarted.await()
        job.cancel()
        releaseDataStore.complete(Unit)
        job.cancelAndJoin()

        assertTrue(cacheCompleted.isCompleted)
    }
}
