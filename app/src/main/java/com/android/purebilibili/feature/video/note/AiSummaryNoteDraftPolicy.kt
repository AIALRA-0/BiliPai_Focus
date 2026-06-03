package com.android.purebilibili.feature.video.note

import com.android.purebilibili.data.model.response.AiSummaryData

fun buildVideoNoteDraftFromAiSummary(
    title: String,
    aiSummary: AiSummaryData,
    cid: Long,
    pageIndex: Int,
    cidCount: Int,
    existingDocument: VideoNoteEditorDocument? = null
): VideoNoteEditorDocument {
    val modelResult = aiSummary.modelResult
    val generatedBlocks = buildList {
        add(VideoNoteBlock.Text("AI 总结\n", bold = true))
        val summary = modelResult?.summary.orEmpty().trim()
        if (summary.isNotBlank()) {
            add(VideoNoteBlock.Text("$summary\n\n"))
        }
        modelResult?.outline.orEmpty().forEach { outline ->
            add(
                VideoNoteBlock.Timestamp(
                    seconds = outline.timestamp,
                    cid = cid,
                    index = pageIndex,
                    cidCount = cidCount
                )
            )
            add(VideoNoteBlock.Text(" ${outline.title}\n", bold = true))
            outline.partOutline.forEach { part ->
                add(
                    VideoNoteBlock.Timestamp(
                        seconds = part.timestamp,
                        cid = cid,
                        index = pageIndex,
                        cidCount = cidCount
                    )
                )
                add(VideoNoteBlock.Text(" ${part.content}\n", unorderedList = true))
            }
            add(VideoNoteBlock.Text("\n"))
        }
        if (size == 1) {
            add(VideoNoteBlock.Text("这条视频暂时没有可写入的 AI 总结内容。\n"))
        }
    }

    val existing = existingDocument
    return if (existing != null && existing.blocks.isNotEmpty()) {
        existing.copy(
            blocks = existing.blocks +
                VideoNoteBlock.Text("\n\n来自 AI 总结的草稿，尚未保存：\n", bold = true, highlight = true) +
                generatedBlocks
        )
    } else {
        VideoNoteEditorDocument(
            title = title.ifBlank { "视频笔记" },
            blocks = generatedBlocks
        )
    }
}
