package com.android.purebilibili.feature.video.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.feature.video.viewmodel.CommentUiState
import com.android.purebilibili.feature.video.viewmodel.SubReplyUiState
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VideoDetailRouteCommentEffectTest {
    @get:Rule val rule = createComposeRule()
    private var hostCompositions = 0
    private var fallbackCalls = 0
    private var opened: Pair<Long, Long>? = null

    @Composable
    private fun RouteHost(
        comments: State<CommentUiState>,
        thread: State<SubReplyUiState>,
        handled: MutableState<Boolean>,
        fallbackSucceeds: Boolean = true,
    ) {
        SideEffect { hostCompositions++ }
        VideoDetailRouteCommentEffect(
            rootReplyId = 42L,
            targetReplyId = 43L,
            handled = handled.value,
            commentState = comments,
            subReplyState = thread,
            onOpenReply = { reply, target -> opened = reply.rpid to target },
            onOpenReplyFromRoute = { _, _ -> fallbackCalls++; fallbackSucceeds },
            onHandled = { handled.value = true },
        )
    }

    @Test
    fun commentUpdatesDoNotRecomposeRouteHostAndLoadedRootOpensOnce() {
        val comments = mutableStateOf(CommentUiState(isRepliesLoading = true))
        val thread = mutableStateOf(SubReplyUiState())
        val handled = mutableStateOf(false)
        rule.setContent { RouteHost(comments, thread, handled) }
        var baseline = 0
        rule.runOnIdle { baseline = hostCompositions }
        rule.runOnIdle { comments.value = comments.value.copy(replyCount = 100) }
        rule.runOnIdle {
            assertEquals(baseline, hostCompositions)
            assertEquals(0, fallbackCalls)
            comments.value = comments.value.copy(replies = persistentListOf(ReplyItem(rpid = 42L)))
        }
        rule.runOnIdle {
            assertEquals(42L to 43L, opened)
            assertEquals(true, handled.value)
            assertEquals(0, fallbackCalls)
        }
        rule.runOnIdle { comments.value = comments.value.copy(isRepliesLoading = false) }
        rule.runOnIdle { assertEquals(0, fallbackCalls) }
    }

    @Test
    fun fallbackWaitsForLoadingAndExistingThreadToFinish() {
        val comments = mutableStateOf(CommentUiState(isRepliesLoading = true))
        val thread = mutableStateOf(SubReplyUiState(visible = true))
        val handled = mutableStateOf(false)
        rule.setContent { RouteHost(comments, thread, handled) }
        rule.runOnIdle { comments.value = comments.value.copy(isRepliesLoading = false) }
        rule.runOnIdle {
            assertEquals(0, fallbackCalls)
            thread.value = SubReplyUiState()
        }
        rule.runOnIdle {
            assertEquals(1, fallbackCalls)
            assertEquals(true, handled.value)
        }
        rule.runOnIdle { comments.value = comments.value.copy(replies = persistentListOf(ReplyItem(rpid = 99L))) }
        rule.runOnIdle { assertEquals(1, fallbackCalls) }
    }

    @Test
    fun failedFallbackCanRetryWhenTheLoadedListChanges() {
        val comments = mutableStateOf(CommentUiState())
        val thread = mutableStateOf(SubReplyUiState())
        val handled = mutableStateOf(false)
        rule.setContent { RouteHost(comments, thread, handled, fallbackSucceeds = false) }
        rule.runOnIdle {
            assertEquals(1, fallbackCalls)
            assertEquals(false, handled.value)
            comments.value = comments.value.copy(replies = persistentListOf(ReplyItem(rpid = 99L)))
        }
        rule.runOnIdle {
            assertEquals(2, fallbackCalls)
            comments.value = comments.value.copy(replies = persistentListOf(ReplyItem(rpid = 42L)))
        }
        rule.runOnIdle {
            assertEquals(42L to 43L, opened)
            assertEquals(true, handled.value)
            assertEquals(2, fallbackCalls)
        }
    }
}
