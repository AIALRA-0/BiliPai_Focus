package com.android.purebilibili.feature.video

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.android.purebilibili.feature.video.screen.VideoDetailBodyUpdates
import com.android.purebilibili.feature.video.screen.VideoDetailCommentUpdates
import com.android.purebilibili.feature.video.viewmodel.CommentSubjectKey
import com.android.purebilibili.feature.video.viewmodel.CommentUiState
import com.android.purebilibili.feature.video.viewmodel.resolveReplyCount
import kotlinx.collections.immutable.persistentSetOf
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VideoDetailBodyUpdatesUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun paginationAndLikesUpdateCommentsWithoutRecomposingIntroOrCountChrome() {
        val comments = mutableStateOf(CommentUiState(
            replyCount = 7,
            replyCountSubject = CommentSubjectKey(123L),
            replyCountIsAuthoritative = true,
        ))
        val progress = mutableFloatStateOf(0f)
        val bodyCommits = AtomicInteger()
        rule.setContent {
            Column {
                VideoDetailBodyUpdates(comments, progress) { state, _ ->
                    SideEffect { bodyCommits.incrementAndGet() }
                    Text("简介 评论 ${state.resolveReplyCount(CommentSubjectKey(123L), 99)}")
                }
                VideoDetailCommentUpdates(comments) { state ->
                    Text("列表 ${state?.isRepliesLoading} 页 ${state?.nextPage} 点赞 ${state?.likedComments?.size}")
                }
            }
        }
        rule.waitForIdle()
        val initialBodyCommits = bodyCommits.get()
        rule.runOnIdle {
            comments.value = comments.value.copy(
                isRepliesLoading = true,
                nextPage = 2,
                likedComments = persistentSetOf(42L),
            )
        }
        rule.onNodeWithText("列表 true 页 2 点赞 1").assertIsDisplayed()
        rule.runOnIdle { assertEquals(initialBodyCommits, bodyCommits.get()) }
        rule.onNodeWithText("简介 评论 7").assertIsDisplayed()
        rule.runOnIdle { comments.value = comments.value.copy(replyCount = 0) }
        rule.onNodeWithText("简介 评论 0").assertIsDisplayed()
    }

    @Test
    fun commentAndDownloadUpdatesStayInsideBodyAndRebindToNewContent() {
        val firstComments = mutableStateOf(CommentUiState(replyCount = 7))
        val secondComments = mutableStateOf(CommentUiState(replyCount = 0))
        val comments = mutableStateOf(firstComments)
        val progress = mutableFloatStateOf(0f)
        val shellCommits = AtomicInteger()
        rule.setContent {
            val currentComments = comments.value
            SideEffect { shellCommits.incrementAndGet() }
            Column {
                Text("播放器与转场壳")
                VideoDetailBodyUpdates(currentComments, progress) { state, value ->
                    Text("评论 ${state.replyCount} 下载 $value")
                }
            }
        }
        rule.waitForIdle()
        val initialCommits = shellCommits.get()
        rule.runOnIdle {
            firstComments.value = firstComments.value.copy(replyCount = 8)
            progress.floatValue = 0.5f
        }
        rule.onNodeWithText("评论 8 下载 0.5").assertIsDisplayed()
        rule.runOnIdle { assertEquals(initialCommits, shellCommits.get()) }

        // 新内容真实为0，不能保留前一内容的数量；旧状态迟到也不能再影响正文。
        rule.runOnIdle { comments.value = secondComments }
        rule.onNodeWithText("评论 0 下载 0.5").assertIsDisplayed()
        val reboundCommits = shellCommits.get()
        rule.runOnIdle { firstComments.value = firstComments.value.copy(replyCount = 99) }
        rule.onNodeWithText("评论 0 下载 0.5").assertIsDisplayed()
        rule.runOnIdle { assertEquals(reboundCommits, shellCommits.get()) }
    }
}
