package com.Android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.data.model.response.ReplyContent
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.data.model.response.ReplyMember
import com.android.purebilibili.data.model.response.ReplyPicture
import com.android.purebilibili.feature.video.ui.components.SubReplySheet
import com.android.purebilibili.feature.video.viewmodel.SubReplyUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val SUB_REPLY_DETAIL_HEADER_TAG = "subreply_detail_header"
private const val SUB_REPLY_DETAIL_ROOT_TAG = "subreply_detail_root"
private const val SUB_REPLY_DETAIL_LIST_TAG = "subreply_detail_reply_list"

@RunWith(AndroidJUnit4::class)
class SubReplyDetailUiRegressionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun subReplyDetail_exposesDedicatedStructureTags() {
        composeTestRule.setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier.size(width = 390.dp, height = 844.dp)
                ) {
                    SubReplySheet(
                        state = buildSubReplyState(),
                        emoteMap = emptyMap(),
                        onDismiss = {},
                        onLoadMore = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(SUB_REPLY_DETAIL_HEADER_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SUB_REPLY_DETAIL_ROOT_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SUB_REPLY_DETAIL_LIST_TAG).assertIsDisplayed()
    }

    @Test
    fun clickingChildReplyPicture_usesPerReplyImageTag() {
        var previewedImage: String? = null

        composeTestRule.setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier.size(width = 390.dp, height = 844.dp)
                ) {
                    SubReplySheet(
                        state = buildSubReplyState(),
                        emoteMap = emptyMap(),
                        onDismiss = {},
                        onLoadMore = {},
                        onImagePreview = { images, index, _, _ ->
                            previewedImage = images[index]
                        }
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithTag("subreply_detail_image_201_0")
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals("https://example.com/reply-image.jpg", previewedImage)
        }
    }

    private fun buildSubReplyState(): SubReplyUiState {
        return SubReplyUiState(
            visible = true,
            rootReply = ReplyItem(
                rpid = 101L,
                mid = 11L,
                ctime = 1_700_000_000L,
                member = ReplyMember(
                    mid = "11",
                    uname = "RootAuthor"
                ),
                content = ReplyContent(
                    message = "root comment with picture",
                    pictures = listOf(
                        ReplyPicture(
                            imgSrc = "https://example.com/root-image.jpg",
                            imgWidth = 720,
                            imgHeight = 720
                        )
                    )
                )
            ),
            items = listOf(
                ReplyItem(
                    rpid = 201L,
                    mid = 12L,
                    root = 101L,
                    ctime = 1_700_000_060L,
                    member = ReplyMember(
                        mid = "12",
                        uname = "ReplyWithPicture"
                    ),
                    content = ReplyContent(
                        message = "reply image",
                        pictures = listOf(
                            ReplyPicture(
                                imgSrc = "https://example.com/reply-image.jpg",
                                imgWidth = 640,
                                imgHeight = 640
                            )
                        )
                    )
                ),
                ReplyItem(
                    rpid = 202L,
                    mid = 13L,
                    root = 101L,
                    ctime = 1_700_000_090L,
                    member = ReplyMember(
                        mid = "13",
                        uname = "ReplyTextOnly"
                    ),
                    content = ReplyContent(message = "plain reply")
                )
            )
        )
    }
}
