@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.android.purebilibili.feature.home.subscription

import com.android.purebilibili.core.ui.AppContentColors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.AppModalBottomSheet
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppOutlinedTextField
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.core.util.WindowWidthSizeClass
import com.android.purebilibili.feature.video.note.VideoNoteBlock
import com.android.purebilibili.feature.video.note.VideoNoteContentCodec
import com.android.purebilibili.feature.video.note.VideoNoteEditorDocument
import com.android.purebilibili.feature.video.ui.section.VideoNoteEditorField
import com.android.purebilibili.feature.video.ui.section.VideoNoteEditorToolbar
import com.android.purebilibili.feature.video.ui.section.VideoNoteHighlightColor
import com.android.purebilibili.feature.video.ui.section.VideoNotePreviewPane
import com.android.purebilibili.feature.video.ui.section.documentToHtml
import com.android.purebilibili.feature.video.ui.section.htmlToDocument
import com.mohamedrejeb.richeditor.model.rememberRichTextState

/**
 * 文章（RSS）笔记编辑器：复用视频笔记编辑器的构件（工具栏/编辑区/HTML 往返/实时预览），
 * 但无时间戳（视频特化能力），保存走 [ArticleNoteStore] 本地仓库。
 */
@Composable
internal fun ArticleNoteEditorSheet(
    visible: Boolean,
    articleTitle: String,
    sourceTitle: String,
    savedNoteTitle: String?,
    savedNoteContent: String?,
    prefillDocument: VideoNoteEditorDocument?,
    summaryDraft: VideoNoteEditorDocument?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (VideoNoteEditorDocument) -> Unit,
    onDelete: () -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(articleTitle) }
    val richTextState = rememberRichTextState()
    var previewMode by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(visible, savedNoteContent, prefillDocument, summaryDraft) {
        if (!visible || initialized) return@LaunchedEffect
        initialized = true
        val document = when {
            prefillDocument != null -> prefillDocument
            summaryDraft != null -> summaryDraft
            savedNoteContent != null ->
                VideoNoteContentCodec.decode(
                    title = savedNoteTitle ?: articleTitle,
                    content = savedNoteContent
                )
            else -> VideoNoteEditorDocument(
                title = articleTitle,
                blocks = listOf(VideoNoteBlock.Text(""))
            )
        }
        title = document.title.ifBlank { articleTitle }
        richTextState.setHtml(documentToHtmlForArticle(document))
    }

    val isBold = richTextState.currentSpanStyle.fontWeight == FontWeight.Bold
    val isItalic = richTextState.currentSpanStyle.fontStyle == FontStyle.Italic
    val isUnderline = richTextState.currentSpanStyle.textDecoration?.contains(TextDecoration.Underline) == true
    val isStrikethrough = richTextState.currentSpanStyle.textDecoration?.contains(TextDecoration.LineThrough) == true
    val isHighlighted = richTextState.currentSpanStyle.background == VideoNoteHighlightColor
    val isBulletList = richTextState.isUnorderedList
    val charCount = richTextState.annotatedString.text.length
    val isWideEditor = LocalWindowSizeClass.current.widthSizeClass != WindowWidthSizeClass.Compact

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .imePadding()
                .padding(horizontal = 18.dp)
        ) {
            AppText(
                text = "文章笔记",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            AppText(
                text = sourceTitle,
                style = MaterialTheme.typography.labelSmall,
                color = AppContentColors.secondaryText(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            AppOutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { AppText("标题") },
                labelText = "标题",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AppIconButton(
                    onClick = {
                        summaryDraft?.let { draft ->
                            richTextState.setHtml(documentToHtmlForArticle(draft))
                            title = draft.title.ifBlank { title }
                        }
                    },
                    enabled = summaryDraft != null,
                ) {
                    AppIcon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = "生成摘要草稿",
                        modifier = Modifier.size(18.dp),
                    )
                }
                AppText(
                    text = if (summaryDraft != null) "从原文生成摘要草稿" else "原文内容不足以生成摘要",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppContentColors.secondaryText(),
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            VideoNoteEditorToolbar(
                isBold = isBold,
                isItalic = isItalic,
                isUnderline = isUnderline,
                isStrikethrough = isStrikethrough,
                isHighlighted = isHighlighted,
                isBulletList = isBulletList,
                previewMode = previewMode && !isWideEditor,
                onPreviewToggle = { previewMode = !previewMode },
                showTimestampButton = false,
                onBoldClick = { richTextState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) },
                onItalicClick = { richTextState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(fontStyle = FontStyle.Italic)) },
                onUnderlineClick = {
                    richTextState.toggleSpanStyle(
                        androidx.compose.ui.text.SpanStyle(textDecoration = TextDecoration.Underline)
                    )
                },
                onStrikethroughClick = {
                    richTextState.toggleSpanStyle(
                        androidx.compose.ui.text.SpanStyle(textDecoration = TextDecoration.LineThrough)
                    )
                },
                onHighlightClick = {
                    richTextState.toggleSpanStyle(
                        androidx.compose.ui.text.SpanStyle(background = VideoNoteHighlightColor)
                    )
                },
                onBulletClick = { richTextState.toggleUnorderedList() },
                onUndoClick = { richTextState.history.undo() },
                onRedoClick = { richTextState.history.redo() }
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (isWideEditor) {
                Row(modifier = Modifier.weight(1f)) {
                    VideoNoteEditorField(
                        richTextState = richTextState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    VideoNotePreviewPane(
                        richTextState = richTextState,
                        modifier = Modifier
                            .width(260.dp)
                            .fillMaxHeight()
                    )
                }
            } else if (previewMode) {
                VideoNotePreviewPane(
                    richTextState = richTextState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                VideoNoteEditorField(
                    richTextState = richTextState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(
                    text = "$charCount 字",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppContentColors.secondaryText(),
                    modifier = Modifier.weight(1f)
                )
                if (savedNoteContent != null) {
                    AppIconButton(onClick = { confirmDelete = true }) {
                        AppIcon(
                            Icons.Outlined.Delete,
                            contentDescription = "删除笔记",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                AppTextButton(onClick = onDismiss) {
                    AppText("取消")
                }
                Spacer(modifier = Modifier.width(8.dp))
                AppButton(
                    onClick = {
                        val document = htmlToDocumentForArticle(
                            title = title,
                            html = richTextState.toHtml()
                        )
                        onSave(document)
                    },
                    enabled = !saving
                ) {
                    AppText(if (saving) "保存中" else "保存")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        AppAlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { AppText("删除文章笔记") },
            text = { AppText("本地保存的这篇笔记将被删除，确认要删除吗？") },
            confirmButton = {
                AppTextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) {
                    AppText("删除")
                }
            },
            dismissButton = {
                AppTextButton(onClick = { confirmDelete = false }) {
                    AppText("取消")
                }
            }
        )
    }
}

internal fun documentToHtmlForArticle(document: VideoNoteEditorDocument): String {
    return documentToHtml(document)
}

internal fun htmlToDocumentForArticle(
    title: String,
    html: String
): VideoNoteEditorDocument {
    return htmlToDocument(title = title, html = html, timestamps = emptyList())
}
