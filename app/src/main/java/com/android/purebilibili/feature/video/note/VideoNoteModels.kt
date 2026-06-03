package com.android.purebilibili.feature.video.note

data class VideoNoteEditorDocument(
    val title: String = "",
    val blocks: List<VideoNoteBlock> = emptyList()
)

sealed interface VideoNoteBlock {
    data class Text(
        val text: String,
        val bold: Boolean = false,
        val highlight: Boolean = false,
        val unorderedList: Boolean = false
    ) : VideoNoteBlock

    data class Timestamp(
        val seconds: Long,
        val cid: Long,
        val index: Int,
        val cidCount: Int,
        val label: String = formatVideoNoteTimestamp(seconds)
    ) : VideoNoteBlock
}

data class EncodedVideoNoteContent(
    val content: String,
    val tags: String,
    val summary: String,
    val contentLength: Int
)

enum class VideoNoteLoadStatus {
    IDLE,
    LOADING,
    READY,
    ERROR
}

data class VideoNoteUiState(
    val status: VideoNoteLoadStatus = VideoNoteLoadStatus.IDLE,
    val forbidNoteEntrance: Boolean = false,
    val privateNoteId: String? = null,
    val privateNoteTitle: String = "",
    val privateNoteSummary: String = "",
    val privateNoteDocument: VideoNoteEditorDocument? = null,
    val publicNoteCount: Int = 0,
    val publicNotes: List<VideoNotePublicPreview> = emptyList(),
    val editorVisible: Boolean = false,
    val editorDocument: VideoNoteEditorDocument = VideoNoteEditorDocument(),
    val editorFromAiSummary: Boolean = false,
    val saving: Boolean = false,
    val deleting: Boolean = false,
    val errorMessage: String? = null,
    val feedbackMessage: String? = null
)

data class VideoNotePublicPreview(
    val cvid: Long,
    val title: String,
    val summary: String,
    val authorName: String,
    val webUrl: String,
    val likes: Int
)

fun formatVideoNoteTimestamp(seconds: Long): String {
    val safeSeconds = seconds.coerceAtLeast(0L)
    val minutes = safeSeconds / 60
    val secondPart = safeSeconds % 60
    return "%02d:%02d".format(minutes, secondPart)
}
