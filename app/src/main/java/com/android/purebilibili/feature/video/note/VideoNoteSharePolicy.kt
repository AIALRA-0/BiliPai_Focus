package com.android.purebilibili.feature.video.note

fun buildVideoNoteShareText(
    videoTitle: String,
    bvid: String,
    document: VideoNoteEditorDocument,
    isDraft: Boolean = false
): String {
    val title = document.title.ifBlank { videoTitle }.trim()
    val body = VideoNoteContentCodec.toPlainText(document)
    val videoUrl = "https://www.bilibili.com/video/$bvid"
    return buildString {
        appendLine(title)
        if (isDraft) {
            appendLine("AI 整理草稿，分享前还可以继续改。")
        }
        appendLine()
        appendLine(body)
        appendLine()
        appendLine("来自视频：$videoTitle")
        append(videoUrl)
    }.trim()
}
