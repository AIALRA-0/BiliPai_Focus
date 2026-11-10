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
        val unorderedList: Boolean = false,
        val italic: Boolean = false,
        val underline: Boolean = false,
        val strikethrough: Boolean = false
    ) : VideoNoteBlock

    data class Timestamp(
        val seconds: Long,
        val cid: Long,
        val index: Int,
        val cidCount: Int,
        val label: String = formatVideoNoteTimestamp(seconds)
    ) : VideoNoteBlock

    /** 引用块：视频笔记可作摘录，文章笔记（RSS）用于原文摘录。Quill delta 的 blockquote 属性。 */
    data class Quote(
        val text: String
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
    val publicNotesLoadingMore: Boolean = false,
    val publicNotesEnd: Boolean = false,
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
    val authorMid: Long = 0L,
    val authorFace: String = "",
    val authorLevel: Int = 0,
    val authorSenior: Boolean = false,
    val pubtime: String = "",
    val webUrl: String,
    val likes: Int
)

fun formatVideoNoteTimestamp(seconds: Long): String {
    val safeSeconds = seconds.coerceAtLeast(0L)
    val minutes = safeSeconds / 60
    val secondPart = safeSeconds % 60
    return "%02d:%02d".format(minutes, secondPart)
}
