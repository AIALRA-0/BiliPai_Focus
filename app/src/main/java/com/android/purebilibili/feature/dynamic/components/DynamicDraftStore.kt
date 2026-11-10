package com.android.purebilibili.feature.dynamic.components

import android.content.Context
import android.net.Uri
import com.android.purebilibili.data.model.response.DynamicPublishDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/** Draft images belong to the current account and are kept until the draft is replaced or cleared. */
internal object DynamicDraftStore {
    private val json = Json { ignoreUnknownKeys = true }
    private const val MAX_IMAGE_BYTES = 15L * 1024 * 1024

    fun load(context: Context, uid: Long, key: String = "main"): DynamicPublishDraft? {
        if (uid <= 0) return null
        val stored = context.getSharedPreferences("dynamic_publish_drafts", Context.MODE_PRIVATE)
            .getString("$uid:$key", null) ?: return null
        return runCatching { json.decodeFromString<DynamicPublishDraft>(stored) }.getOrNull()
    }

    suspend fun save(context: Context, uid: Long, draft: DynamicPublishDraft, key: String = "main"): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(uid > 0) { "请先登录" }
            val folder = File(context.filesDir, "dynamic_drafts/$uid/$key")
            check(folder.isDirectory || folder.mkdirs()) { "无法保存草稿图片" }
            val created = mutableListOf<File>()
            try {
                val images = draft.imageUris.map { value ->
                    val uri = Uri.parse(value)
                    if (uri.scheme == "https" || uri.scheme == "http") return@map value
                    val original = uri.path?.let(::File)
                    if (uri.scheme == "file" && original?.parentFile?.canonicalFile == folder.canonicalFile && original?.isFile == true) return@map value
                    val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()
                        ?: uri.path?.substringAfterLast('.', "")?.let { android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(it.lowercase()) }
                    val extension = when (mime) {
                        "image/png" -> "png"
                        "image/webp" -> "webp"
                        "image/gif" -> "gif"
                        "image/heic" -> "heic"
                        "image/heif" -> "heif"
                        else -> "jpg"
                    }
                    val destination = File(folder, "${UUID.randomUUID()}.$extension")
                    created += destination
                    val source = context.contentResolver.openInputStream(uri) ?: error("无法读取草稿图片")
                    source.use { input -> destination.outputStream().use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var total = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            check(total <= MAX_IMAGE_BYTES) { "图片超过 15 MB" }
                            output.write(buffer, 0, count)
                        }
                        check(total > 0) { "图片内容为空" }
                    } }
                    Uri.fromFile(destination).toString()
                }
                val saved = draft.copy(imageUris = images)
                val preferences = context.getSharedPreferences("dynamic_publish_drafts", Context.MODE_PRIVATE)
                check(preferences.edit().putString("$uid:$key", json.encodeToString(saved)).commit()) { "草稿保存失败" }
                val keep = images.mapNotNull { value -> Uri.parse(value).takeIf { it.scheme == "file" }?.path }.toSet()
                // The new draft is already committed; cleanup must not roll back its images.
                runCatching { folder.listFiles()?.forEach { if (it.absolutePath !in keep) it.delete() } }
                Unit
            } catch (e: Exception) {
                created.forEach(File::delete)
                throw e
            }
        }
    }

    suspend fun clear(context: Context, uid: Long, key: String = "main"): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (uid > 0) {
                val preferences = context.getSharedPreferences("dynamic_publish_drafts", Context.MODE_PRIVATE)
                check(preferences.edit().remove("$uid:$key").commit()) { "无法清除草稿" }
                File(context.filesDir, "dynamic_drafts/$uid/$key").deleteRecursively()
            }
            Unit
        }
    }
}
