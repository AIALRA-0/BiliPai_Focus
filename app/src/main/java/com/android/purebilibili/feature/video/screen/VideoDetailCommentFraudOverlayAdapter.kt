package com.android.purebilibili.feature.video.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.android.purebilibili.data.model.CommentFraudStatus
import com.android.purebilibili.data.repository.resolveCommentFraudLightMessage
import com.android.purebilibili.data.repository.shouldShowCommentFraudResultDialog
import com.android.purebilibili.feature.video.ui.components.CommentFraudResultDialog
import com.android.purebilibili.feature.video.viewmodel.VideoCommentViewModel
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackViewModel

@Composable
internal fun VideoDetailCommentFraudOverlayAdapter(
    context: Context,
    playbackViewModel: VideoPlaybackViewModel,
    commentViewModel: VideoCommentViewModel,
    aid: Long?,
    fraudDetectionEnabled: Boolean,
) {
    LaunchedEffect(playbackViewModel, commentViewModel, aid, fraudDetectionEnabled) {
        val activeAid = aid ?: return@LaunchedEffect
        playbackViewModel.commentSentEvent.collect { reply ->
            commentViewModel.onExternalCommentSent(
                aid = activeAid,
                newReply = reply,
                fraudDetectionEnabled = fraudDetectionEnabled,
            )
        }
    }

    var fraudDialogStatus by remember { mutableStateOf<CommentFraudStatus?>(null) }
    LaunchedEffect(commentViewModel, context.applicationContext) {
        commentViewModel.fraudEvent.collect { status ->
            val lightMessage = resolveCommentFraudLightMessage(status)
            if (lightMessage != null) {
                Toast.makeText(context, lightMessage, Toast.LENGTH_SHORT).show()
            } else if (shouldShowCommentFraudResultDialog(status)) {
                fraudDialogStatus = status
            }
        }
    }

    fraudDialogStatus?.let { status ->
        CommentFraudResultDialog(
            status = status,
            onDismiss = {
                fraudDialogStatus = null
                commentViewModel.dismissFraudResult()
            },
            onDeleteComment = if (status == CommentFraudStatus.SHADOW_BANNED) {
                {
                    val rpid = commentViewModel.commentState.value.fraudDetectRpid
                    if (rpid > 0L) {
                        commentViewModel.startDissolve(rpid)
                    }
                }
            } else {
                null
            },
        )
    }
}
