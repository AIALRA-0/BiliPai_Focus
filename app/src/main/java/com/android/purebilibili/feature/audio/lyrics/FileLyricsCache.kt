package com.android.purebilibili.feature.audio.lyrics

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class FileLyricsCache(
    private val directory: File,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : LyricsCache {
    override suspend fun read(key: String): LyricDocument? = withContext(Dispatchers.IO) {
        val file = lyricCacheFileNamesForKey(key)
            .asSequence()
            .map { File(directory, it) }
            .firstOrNull { it.isFile }
            ?: return@withContext null
        runCatching { json.decodeFromString<LyricDocument>(file.readText()) }.getOrNull()
    }

    override suspend fun write(key: String, document: LyricDocument) = withContext(Dispatchers.IO) {
        if (!directory.exists()) directory.mkdirs()
        val destination = File(directory, lyricCacheFileNamesForKey(key).first())
        val temporary = File(directory, "${destination.name}.tmp")
        temporary.writeText(json.encodeToString(document))
        if (!temporary.renameTo(destination)) {
            destination.writeText(temporary.readText())
            temporary.delete()
        }
    }

}

/**
 * The first name is portable. The second preserves access to caches created before
 * colon was excluded for Windows compatibility.
 */
internal fun lyricCacheFileNamesForKey(key: String): List<String> {
    val portableName = key.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".json"
    val legacyName = key.replace(Regex("[^A-Za-z0-9._:-]"), "_") + ".json"
    return listOf(portableName, legacyName).distinct()
}
