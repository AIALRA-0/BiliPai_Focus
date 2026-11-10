package com.android.purebilibili.core.plugin.feed

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class FeedConditionalValidators(
    val etag: String? = null,
    val lastModified: String? = null,
)

/** Per-URL HTTP validators (ETag / Last-Modified) that let feed refreshes take the 304 fast path. */
object FeedConditionalStore {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    suspend fun load(context: Context): Map<String, FeedConditionalValidators> = lockedIo { read(context) }

    suspend fun update(context: Context, entries: Map<String, FeedConditionalValidators>) = lockedIo {
        if (entries.isEmpty()) return@lockedIo
        write(context, read(context) + entries)
    }

    suspend fun clear(context: Context, urls: Set<String>) = lockedIo {
        if (urls.isEmpty()) return@lockedIo
        write(context, read(context) - urls)
    }

    private suspend fun <T> lockedIo(block: () -> T): T {
        mutex.lock()
        try {
            return withContext(Dispatchers.IO) { block() }
        } finally {
            mutex.unlock()
        }
    }

    private fun read(context: Context): Map<String, FeedConditionalValidators> {
        val file = file(context)
        if (!file.baseFile.exists()) return emptyMap()
        return runCatching {
            file.openRead().bufferedReader().use { json.decodeFromString<Map<String, FeedConditionalValidators>>(it.readText()) }
        }.getOrDefault(emptyMap())
    }

    private fun write(context: Context, data: Map<String, FeedConditionalValidators>) {
        val file = file(context)
        file.baseFile.parentFile?.mkdirs()
        val stream = file.startWrite()
        try {
            stream.writer(Charsets.UTF_8).apply { write(json.encodeToString(data)); flush() }
            file.finishWrite(stream)
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
    }

    private fun file(context: Context): AtomicFile =
        AtomicFile(File(context.filesDir, "plugin/feed_validators.json"))
}
