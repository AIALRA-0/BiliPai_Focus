package com.android.purebilibili.feature.dynamic.components

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.feature.dynamic.DYNAMIC_COMMENT_ROOT_INPUT_HINT
import com.android.purebilibili.feature.dynamic.DynamicViewModel
import com.android.purebilibili.feature.video.ui.components.CommentInputDialog
import com.android.purebilibili.feature.video.viewmodel.CommentComposerDraft
import com.android.purebilibili.feature.video.viewmodel.commentComposerDraftKey

/**
 * 动态评论的共享输入弹层宿主。
 *
 * 与评论列表弹窗/详情页是兄弟节点，避免把 [CommentInputDialog] 嵌套进底部 sheet。
 */
@Composable
internal fun DynamicCommentInputHost(
    draftKeyPrefix: String,
    viewModel: DynamicViewModel,
    onPostComment: (String, List<Uri>, (Boolean) -> Unit) -> Unit,
) {
    val visible by viewModel.commentComposerVisible.collectAsStateWithLifecycle()
    val replyTarget by viewModel.commentReplyTarget.collectAsStateWithLifecycle()
    val drafts by viewModel.commentComposerDrafts.collectAsStateWithLifecycle()
    val isSending by viewModel.isSendingComment.collectAsStateWithLifecycle()
    val mentionState by viewModel.commentMentionSearchState.collectAsStateWithLifecycle()

    val draftSessionId = drafts.sessionId
    val draftReplyRpid = replyTarget?.parentRpid
    val draftKey = remember(draftKeyPrefix, draftSessionId, draftReplyRpid) {
        "$draftKeyPrefix:$draftReplyRpid:$draftSessionId"
    }
    val draft = drafts.drafts[commentComposerDraftKey(draftReplyRpid)] ?: CommentComposerDraft()

    var emotePackages by remember { mutableStateOf(DynamicEmoteCatalog.snapshotPackages()) }
    LaunchedEffect(visible, DynamicEmoteCatalog.currentSessionKey()) {
        if (visible) {
            emotePackages = DynamicEmoteCatalog.ensurePackagesLoaded()
        }
    }

    CommentInputDialog(
        visible = visible,
        draftKey = draftKey,
        onDismiss = {
            if (viewModel.commentComposerDrafts.value.sessionId == draftSessionId) {
                viewModel.dismissCommentComposer()
            }
        },
        onSend = { text, images, _ ->
            val currentDrafts = viewModel.commentComposerDrafts.value
            if (
                visible &&
                currentDrafts.sessionId == draftSessionId &&
                viewModel.commentReplyTarget.value?.parentRpid == draftReplyRpid &&
                !viewModel.isSendingComment.value
            ) {
                onPostComment(text, images) { }
            }
        },
        isSending = isSending,
        replyToName = replyTarget?.uname,
        inputHint = DYNAMIC_COMMENT_ROOT_INPUT_HINT,
        canInputComment = true,
        emotePackages = emotePackages,
        mentionUsers = mentionState.users,
        isMentionSearching = mentionState.isLoading,
        mentionSearchError = mentionState.errorMessage,
        onMentionSearchQueryChange = viewModel::searchCommentMentionUsers,
        initialText = draft.text,
        initialImageUris = draft.imageUris,
        initialSyncToDynamic = false,
        onDraftChange = { text, images, sync ->
            viewModel.updateCommentDraft(draftSessionId, draftReplyRpid, text, images, sync)
        },
        allowImageAttachment = replyTarget == null,
        showSyncToDynamic = false,
        showProgressInsert = false,
    )
}
