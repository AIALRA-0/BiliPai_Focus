package com.android.purebilibili.feature.video.note

fun hasVideoNoteBodyContent(document: VideoNoteEditorDocument): Boolean {
    return document.blocks.any { block ->
        when (block) {
            is VideoNoteBlock.Text -> block.text.isNotBlank()
            is VideoNoteBlock.Timestamp -> true
        }
    }
}

fun hasUnsavedVideoNoteDraft(noteState: VideoNoteUiState): Boolean {
    val editorDocument = noteState.editorDocument
    return hasVideoNoteBodyContent(editorDocument) &&
        editorDocument != noteState.privateNoteDocument
}

fun resolveVideoNotePrimaryActionLabel(noteState: VideoNoteUiState): String {
    return when {
        hasUnsavedVideoNoteDraft(noteState) -> "继续编辑"
        noteState.privateNoteDocument != null -> "编辑"
        else -> "新建"
    }
}

fun resolveVideoNoteEditableDocument(
    noteState: VideoNoteUiState,
    defaultTitle: String
): VideoNoteEditorDocument {
    return when {
        hasUnsavedVideoNoteDraft(noteState) -> noteState.editorDocument.copy(
            title = noteState.editorDocument.title.ifBlank { defaultTitle }
        )
        noteState.privateNoteDocument != null -> noteState.privateNoteDocument
        else -> VideoNoteEditorDocument(
            title = defaultTitle,
            blocks = listOf(VideoNoteBlock.Text(""))
        )
    }
}
