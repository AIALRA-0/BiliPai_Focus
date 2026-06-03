package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.PublicVideoNoteInfoData
import com.android.purebilibili.data.model.response.PublicVideoNoteItem
import com.android.purebilibili.data.model.response.VideoNoteInfoData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class VideoNoteSnapshot(
    val forbidNoteEntrance: Boolean,
    val privateNote: VideoNoteInfoData?,
    val privateNoteId: String?,
    val publicNotes: List<PublicVideoNoteItem>,
    val publicNoteTotal: Int
)

data class VideoNoteSavePayload(
    val aid: Long,
    val noteId: String?,
    val title: String,
    val summary: String,
    val content: String,
    val tags: String,
    val contentLength: Int,
    val publish: Boolean = false
)

sealed class VideoNoteRepositoryError(message: String) : Exception(message) {
    data object NotLoggedIn : VideoNoteRepositoryError("请先登录")
    data object MissingCsrf : VideoNoteRepositoryError("登录态缺少 CSRF，请重新登录后再试")
    data object Forbidden : VideoNoteRepositoryError("该视频暂不支持笔记")
    data class ApiError(val code: Int, val apiMessage: String) :
        VideoNoteRepositoryError(apiMessage.ifBlank { "笔记接口返回异常：$code" })
}

object VideoNoteRepository {
    private val api = NetworkModule.api

    suspend fun getVideoNoteSnapshot(aid: Long): Result<VideoNoteSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val forbid = api.getVideoNoteForbidState(aid)
                .takeIf { it.code == 0 }
                ?.data
                ?.forbidNoteEntrance ?: false

            val privateNoteResult = if (hasSession()) {
                loadPrivateNote(aid)
            } else {
                null to null
            }
            val publicList = api.getPublicVideoNoteList(oid = aid)
            val publicData = publicList.data

            VideoNoteSnapshot(
                forbidNoteEntrance = forbid,
                privateNote = privateNoteResult.first,
                privateNoteId = privateNoteResult.second,
                publicNotes = publicData?.list.orEmpty(),
                publicNoteTotal = publicData?.page?.total ?: publicData?.list.orEmpty().size
            )
        }
    }

    suspend fun savePrivateNote(payload: VideoNoteSavePayload): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!hasSession()) throw VideoNoteRepositoryError.NotLoggedIn
            val csrf = TokenManager.csrfCache?.takeIf { it.isNotBlank() }
                ?: throw VideoNoteRepositoryError.MissingCsrf
            val fields = linkedMapOf(
                "oid" to payload.aid.toString(),
                "oid_type" to "0",
                "title" to payload.title,
                "summary" to payload.summary,
                "content" to payload.content,
                "tags" to payload.tags,
                "cls" to "1",
                "from" to "save",
                "cont_len" to payload.contentLength.toString(),
                "platform" to "web",
                "publish" to if (payload.publish) "1" else "0",
                "csrf" to csrf
            )
            payload.noteId?.takeIf { it.isNotBlank() }?.let { fields["note_id"] = it }

            val response = api.saveVideoNote(fields)
            if (response.code != 0) {
                throw classifyApiError(response.code, response.message)
            }
            response.data?.noteId?.takeIf { it > 0L }?.toString()
                ?: payload.noteId.orEmpty().ifBlank { throw VideoNoteRepositoryError.ApiError(response.code, "保存成功但缺少笔记 ID") }
        }
    }

    suspend fun deletePrivateNote(aid: Long, noteId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!hasSession()) throw VideoNoteRepositoryError.NotLoggedIn
            val csrf = TokenManager.csrfCache?.takeIf { it.isNotBlank() }
                ?: throw VideoNoteRepositoryError.MissingCsrf
            val response = api.deleteVideoNote(oid = aid, noteId = noteId, csrf = csrf)
            if (response.code != 0) {
                throw classifyApiError(response.code, response.message)
            }
        }
    }

    suspend fun getPublicNoteInfo(cvid: Long): Result<PublicVideoNoteInfoData> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.getPublicVideoNoteInfo(cvid)
            if (response.code != 0) throw classifyApiError(response.code, response.message)
            response.data ?: throw VideoNoteRepositoryError.ApiError(response.code, "公开笔记详情为空")
        }
    }

    private suspend fun loadPrivateNote(aid: Long): Pair<VideoNoteInfoData?, String?> {
        val listResponse = api.getPrivateVideoNoteIds(
            oid = aid,
            csrf = TokenManager.csrfCache?.takeIf { it.isNotBlank() }
        )
        if (listResponse.code != 0) return null to null
        val noteId = listResponse.data?.noteIds?.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: return null to null
        val infoResponse = api.getPrivateVideoNoteInfo(oid = aid, noteId = noteId)
        if (infoResponse.code != 0) return null to noteId
        return infoResponse.data to noteId
    }

    private fun hasSession(): Boolean {
        return !TokenManager.sessDataCache.isNullOrBlank()
    }

    private fun classifyApiError(code: Int, message: String): VideoNoteRepositoryError {
        return when (code) {
            -101 -> VideoNoteRepositoryError.NotLoggedIn
            -111 -> VideoNoteRepositoryError.MissingCsrf
            79513 -> VideoNoteRepositoryError.Forbidden
            else -> VideoNoteRepositoryError.ApiError(code, message)
        }
    }
}
