package com.android.purebilibili.core.plugin.feed

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 文章（RSS）本地笔记。B 站官方笔记 API 只接受视频 oid，文章笔记仅存本地；
 * 内容复用视频笔记的 delta 编码（VideoNoteContentCodec），保证两端样式模型一致。
 */
@Serializable
data class SavedArticleNote(
    val link: String,
    val articleTitle: String,
    val sourceTitle: String,
    val noteTitle: String,
    val content: String,
    val updatedAtEpochMs: Long
)

object ArticleNoteStore {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    fun list(context: Context): List<SavedArticleNote> {
        val file = file(context)
        if (!file.exists()) return emptyList()
        return runCatching {
            json.decodeFromString<List<SavedArticleNote>>(file.readText())
        }.getOrDefault(emptyList())
    }

    fun get(context: Context, link: String): SavedArticleNote? {
        return list(context).firstOrNull { it.link == link }
    }

    fun save(context: Context, note: SavedArticleNote) {
        val current = list(context).filterNot { it.link == note.link }
        write(context, listOf(note) + current)
    }

    fun remove(context: Context, link: String): Boolean {
        val current = list(context)
        val remaining = current.filterNot { it.link == link }
        val removed = remaining.size != current.size
        if (removed) write(context, remaining)
        return removed
    }

    private fun write(context: Context, notes: List<SavedArticleNote>) {
        val file = file(context)
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(notes))
        _revision.value += 1
    }

    private fun file(context: Context): File {
        return File(context.filesDir, "plugin/article_notes.json")
    }
}
