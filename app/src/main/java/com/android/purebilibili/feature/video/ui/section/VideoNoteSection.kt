package com.android.purebilibili.feature.video.ui.section

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.purebilibili.feature.video.note.VideoNoteBlock
import com.android.purebilibili.feature.video.note.VideoNoteEditorDocument
import com.android.purebilibili.feature.video.note.VideoNoteLoadStatus
import com.android.purebilibili.feature.video.note.VideoNoteUiState
import com.android.purebilibili.feature.video.note.hasUnsavedVideoNoteDraft
import com.android.purebilibili.feature.video.note.resolveVideoNotePrimaryActionLabel
import com.android.purebilibili.feature.video.note.resolveVideoNoteEmptyMessage
import com.android.purebilibili.feature.video.note.shouldShowVideoNoteBody
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor

@Composable
fun VideoNoteCard(
    noteState: VideoNoteUiState,
    isLoggedIn: Boolean,
    onCreateOrEditClick: () -> Unit,
    onRetryClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: (VideoNoteEditorDocument) -> Unit,
    onPublicNoteClick: (Long, String) -> Unit,
    defaultCollapsed: Boolean = false,
    modifier: Modifier = Modifier
) {
    val hasUnsavedDraft = hasUnsavedVideoNoteDraft(noteState)
    val primaryActionLabel = resolveVideoNotePrimaryActionLabel(noteState)
    var userExpanded by remember(defaultCollapsed) { mutableStateOf(!defaultCollapsed) }
    val showBody = shouldShowVideoNoteBody(
        defaultCollapsed = defaultCollapsed,
        userExpanded = userExpanded
    )
    val primaryActionEnabled = (isLoggedIn || hasUnsavedDraft) &&
        !noteState.forbidNoteEntrance &&
        !noteState.saving
    val showSecondaryActions = noteState.privateNoteDocument != null ||
        noteState.status == VideoNoteLoadStatus.ERROR
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBox()
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "视频笔记",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = resolveNoteSubtitle(noteState, isLoggedIn),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (showBody) {
                            VideoNotePrimaryActionButton(
                                label = primaryActionLabel,
                                enabled = primaryActionEnabled,
                                onClick = onCreateOrEditClick
                            )
                        }
                    }
                }
                if (noteState.status == VideoNoteLoadStatus.LOADING) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
                if (defaultCollapsed) {
                    TextButton(onClick = { userExpanded = !userExpanded }) {
                        Text(if (showBody) "收起" else "展开")
                    }
                }
            }

            if (showBody) {
                if (!noteState.feedbackMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = noteState.feedbackMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (!noteState.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = noteState.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (showSecondaryActions) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        noteState.privateNoteDocument?.let { privateDocument ->
                            OutlinedButton(
                                onClick = { onShareClick(privateDocument) }
                            ) {
                                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("分享")
                            }
                            OutlinedButton(
                                onClick = onDeleteClick,
                                enabled = !noteState.deleting
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("删除")
                            }
                        }
                        if (noteState.status == VideoNoteLoadStatus.ERROR) {
                            TextButton(onClick = onRetryClick) {
                                Text("重试")
                            }
                        }
                    }
                }

                if (noteState.publicNotes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "公开笔记 ${noteState.publicNoteCount} 篇",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    noteState.publicNotes.take(2).forEach { note ->
                        Text(
                            text = note.title.ifBlank { note.summary },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onPublicNoteClick(note.cvid, note.webUrl) }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoNotePrimaryActionButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled
    ) {
        Icon(
            imageVector = if (label == "新建") Icons.Outlined.Add else Icons.Outlined.Edit,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoNoteEditorSheet(
    noteState: VideoNoteUiState,
    onDismiss: () -> Unit,
    onDocumentChange: (VideoNoteEditorDocument) -> Unit,
    onInsertTimestamp: () -> Unit,
    onTimestampClick: (Long) -> Unit,
    onShare: (VideoNoteEditorDocument) -> Unit,
    onSave: (VideoNoteEditorDocument) -> Unit
) {
    if (!noteState.editorVisible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(noteState.editorDocument.title) { mutableStateOf(noteState.editorDocument.title) }
    var markdown by remember(noteState.editorDocument) { mutableStateOf(documentToMarkdown(noteState.editorDocument)) }
    val richTextState = rememberRichTextState()

    LaunchedEffect(markdown) {
        richTextState.setMarkdown(markdown)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(560.dp)
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (noteState.editorFromAiSummary) "AI 笔记草稿" else "视频笔记",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            VideoNoteEditorToolbar(
                onBoldClick = {
                    markdown = wrapSelectionFallback(richTextState.toMarkdown(), "**")
                    richTextState.setMarkdown(markdown)
                },
                onHighlightClick = {
                    markdown = richTextState.toMarkdown() + "\n==高亮重点=="
                    richTextState.setMarkdown(markdown)
                },
                onBulletClick = {
                    markdown = richTextState.toMarkdown() + "\n- "
                    richTextState.setMarkdown(markdown)
                },
                onTimestampClick = onInsertTimestamp,
                onUndoClick = {
                    richTextState.history.undo()
                    markdown = richTextState.toMarkdown()
                },
                onRedoClick = {
                    richTextState.history.redo()
                    markdown = richTextState.toMarkdown()
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            RichTextEditor(
                state = richTextState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(10.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            VideoNoteTimestampChips(
                document = noteState.editorDocument,
                onTimestampClick = onTimestampClick
            )
            if (!noteState.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = noteState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = {
                        val document = markdownToDocument(
                            title = title,
                            markdown = richTextState.toMarkdown(),
                            timestamps = noteState.editorDocument.blocks.filterIsInstance<VideoNoteBlock.Timestamp>()
                        )
                        onDocumentChange(document)
                        onShare(document)
                    },
                    enabled = !noteState.saving
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("分享")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val nextMarkdown = richTextState.toMarkdown()
                        val document = markdownToDocument(
                            title = title,
                            markdown = nextMarkdown,
                            timestamps = noteState.editorDocument.blocks.filterIsInstance<VideoNoteBlock.Timestamp>()
                        )
                        onDocumentChange(document)
                        onSave(document)
                    },
                    enabled = !noteState.saving
                ) {
                    if (noteState.saving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("保存")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun VideoNoteDeleteConfirmDialog(
    visible: Boolean,
    deleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除视频笔记") },
        text = { Text("删除后无法在 BiliPai 内恢复。确认要删除这条笔记吗？") },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !deleting) {
                Text(if (deleting) "删除中" else "删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !deleting) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun IconBox() {
    Surface(
        modifier = Modifier.size(32.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Icon(
            imageVector = Icons.Outlined.BookmarkBorder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
private fun VideoNoteEditorToolbar(
    onBoldClick: () -> Unit,
    onHighlightClick: () -> Unit,
    onBulletClick: () -> Unit,
    onTimestampClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        IconButton(onClick = onBoldClick) { Text("B", fontWeight = FontWeight.Bold) }
        IconButton(onClick = onHighlightClick) {
            Icon(Icons.Outlined.FormatColorFill, contentDescription = "高亮")
        }
        IconButton(onClick = onBulletClick) { Text("-") }
        IconButton(onClick = onTimestampClick) {
            Icon(Icons.Outlined.AccessTime, contentDescription = "插入时间点")
        }
        IconButton(onClick = onUndoClick) {
            Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = "撤销")
        }
        IconButton(onClick = onRedoClick) {
            Icon(Icons.AutoMirrored.Outlined.Redo, contentDescription = "重做")
        }
    }
}

@Composable
private fun VideoNoteTimestampChips(
    document: VideoNoteEditorDocument,
    onTimestampClick: (Long) -> Unit
) {
    val timestamps = document.blocks.filterIsInstance<VideoNoteBlock.Timestamp>()
    if (timestamps.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        timestamps.forEach { timestamp ->
            AssistChip(
                onClick = { onTimestampClick(timestamp.seconds * 1000L) },
                label = { Text(timestamp.label) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            )
        }
    }
}

private fun resolveNoteSubtitle(noteState: VideoNoteUiState, isLoggedIn: Boolean): String {
    return when {
        noteState.privateNoteDocument != null -> noteState.privateNoteSummary.ifBlank {
            "这条视频已有私有笔记。"
        }
        hasUnsavedVideoNoteDraft(noteState) -> "草稿还在，点继续编辑可以把这一段认真留下来。"
        else -> resolveVideoNoteEmptyMessage(isLoggedIn, noteState.forbidNoteEntrance)
    }
}

private fun documentToMarkdown(document: VideoNoteEditorDocument): String {
    return document.blocks.joinToString(separator = "") { block ->
        when (block) {
            is VideoNoteBlock.Text -> when {
                block.unorderedList -> "- ${block.text.removePrefix("- ")}"
                block.bold -> "**${block.text}**"
                block.highlight -> "==${block.text}=="
                else -> block.text
            }
            is VideoNoteBlock.Timestamp -> "[${block.label}]"
        }
    }
}

private fun markdownToDocument(
    title: String,
    markdown: String,
    timestamps: List<VideoNoteBlock.Timestamp>
): VideoNoteEditorDocument {
    val blocks = mutableListOf<VideoNoteBlock>()
    var remaining = markdown
    timestamps.forEach { timestamp ->
        val marker = "[${timestamp.label}]"
        val index = remaining.indexOf(marker)
        if (index >= 0) {
            remaining.take(index).takeIf { it.isNotEmpty() }?.let { blocks += textBlockFromMarkdown(it) }
            blocks += timestamp
            remaining = remaining.drop(index + marker.length)
        }
    }
    if (remaining.isNotEmpty()) blocks += textBlockFromMarkdown(remaining)
    return VideoNoteEditorDocument(title = title, blocks = blocks.ifEmpty { listOf(VideoNoteBlock.Text("")) })
}

private fun textBlockFromMarkdown(markdown: String): VideoNoteBlock.Text {
    val trimmed = markdown.trim('\n')
    return when {
        trimmed.startsWith("- ") -> VideoNoteBlock.Text(trimmed.removePrefix("- ") + "\n", unorderedList = true)
        trimmed.startsWith("**") && trimmed.endsWith("**") -> VideoNoteBlock.Text(trimmed.removeSurrounding("**"), bold = true)
        trimmed.startsWith("==") && trimmed.endsWith("==") -> VideoNoteBlock.Text(trimmed.removeSurrounding("=="), highlight = true)
        else -> VideoNoteBlock.Text(markdown)
    }
}

private fun wrapSelectionFallback(value: String, wrapper: String): String {
    return if (value.isBlank()) "$wrapper$wrapper" else "$wrapper$value$wrapper"
}
