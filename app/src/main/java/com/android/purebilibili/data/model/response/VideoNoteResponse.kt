package com.android.purebilibili.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoNoteArchiveListResponse(
    val code: Int = 0,
    val message: String = "",
    val data: VideoNoteArchiveListData? = null
)

@Serializable
data class VideoNoteArchiveListData(
    val noteIds: List<String> = emptyList()
)

@Serializable
data class VideoNoteInfoResponse(
    val code: Int = 0,
    val message: String = "",
    val data: VideoNoteInfoData? = null
)

@Serializable
data class VideoNoteInfoData(
    val title: String = "",
    val summary: String = "",
    val content: String = "",
    @SerialName("cid_count")
    val cidCount: Int = 0,
    @SerialName("audit_status")
    val auditStatus: Int = 0,
    @SerialName("pub_status")
    val pubStatus: Int = 0,
    @SerialName("pub_reason")
    val pubReason: String = "",
    @SerialName("pub_version")
    val pubVersion: Int = 0,
    @SerialName("forbid_note_entrance")
    val forbidNoteEntrance: Boolean = false,
    val tags: List<VideoNoteTagData> = emptyList(),
    val arc: VideoNoteArc? = null
)

@Serializable
data class VideoNoteTagData(
    val cid: Long = 0L,
    val status: Int = 0,
    val index: Int = 0,
    val seconds: Long = 0L,
    val pos: Int = 0
)

@Serializable
data class VideoNoteArc(
    val oid: Long = 0L,
    @SerialName("oid_type")
    val oidType: Int = 0,
    val title: String = "",
    val pic: String = "",
    val status: Int = 0,
    val desc: String = ""
)

@Serializable
data class VideoNoteForbidResponse(
    val code: Int = 0,
    val message: String = "",
    val data: VideoNoteForbidData? = null
)

@Serializable
data class VideoNoteForbidData(
    @SerialName("forbid_note_entrance")
    val forbidNoteEntrance: Boolean = false
)

@Serializable
data class VideoNoteSaveResponse(
    val code: Int = 0,
    val message: String = "",
    val data: VideoNoteSaveData? = null
)

@Serializable
data class VideoNoteSaveData(
    @SerialName("note_id")
    val noteId: Long = 0L
)

@Serializable
data class PublicVideoNoteListResponse(
    val code: Int = 0,
    val message: String = "",
    val data: PublicVideoNoteListData? = null
)

@Serializable
data class PublicVideoNoteListData(
    val list: List<PublicVideoNoteItem> = emptyList(),
    val page: VideoNotePage = VideoNotePage(),
    @SerialName("show_public_note")
    val showPublicNote: Boolean = false,
    val message: String = ""
)

@Serializable
data class VideoNotePage(
    val total: Int = 0,
    val size: Int = 0,
    val num: Int = 0
)

@Serializable
data class PublicVideoNoteItem(
    val cvid: Long = 0L,
    val title: String = "",
    val summary: String = "",
    val pubtime: String = "",
    @SerialName("web_url")
    val webUrl: String = "",
    val message: String = "",
    val author: VideoNoteAuthor? = null,
    val likes: Int = 0,
    @SerialName("has_like")
    val hasLike: Boolean = false
)

@Serializable
data class VideoNoteAuthor(
    val mid: Long = 0L,
    val name: String = "",
    val face: String = "",
    val level: Int = 0
)

@Serializable
data class PublicVideoNoteInfoResponse(
    val code: Int = 0,
    val message: String = "",
    val data: PublicVideoNoteInfoData? = null
)

@Serializable
data class PublicVideoNoteInfoData(
    val cvid: Long = 0L,
    @SerialName("note_id")
    val noteId: Long = 0L,
    val title: String = "",
    val summary: String = "",
    val content: String = "",
    @SerialName("cid_count")
    val cidCount: Int = 0,
    @SerialName("pub_status")
    val pubStatus: Int = 0,
    val tags: List<VideoNoteTagData> = emptyList(),
    val arc: VideoNoteArc? = null,
    val author: VideoNoteAuthor? = null,
    @SerialName("forbid_note_entrance")
    val forbidNoteEntrance: Boolean = false
)
