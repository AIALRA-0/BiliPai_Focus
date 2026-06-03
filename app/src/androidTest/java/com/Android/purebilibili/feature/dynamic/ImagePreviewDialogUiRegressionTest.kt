package com.Android.purebilibili.feature.dynamic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.feature.dynamic.components.IMAGE_PREVIEW_COMMENT_PANEL_TAG
import com.android.purebilibili.feature.dynamic.components.IMAGE_PREVIEW_PAGE_TAG
import com.android.purebilibili.feature.dynamic.components.ImagePreviewDialog
import com.android.purebilibili.feature.dynamic.components.ImagePreviewCommentContext
import com.android.purebilibili.feature.dynamic.components.ImagePreviewOverlayHost
import com.android.purebilibili.feature.dynamic.components.ImagePreviewTextContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImagePreviewDialogUiRegressionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun doubleTapOnPreviewImage_keepsPreviewOpen() {
        composeTestRule.setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .size(width = 390.dp, height = 844.dp)
                ) {
                    ImagePreviewDialog(
                        images = listOf("https://example.com/demo.jpg"),
                        initialIndex = 0,
                        sourceRect = Rect(24f, 60f, 196f, 232f),
                        onDismiss = {}
                    )
                    ImagePreviewOverlayHost(modifier = Modifier.fillMaxSize())
                }
            }
        }

        composeTestRule.onNodeWithTag(IMAGE_PREVIEW_PAGE_TAG).assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(IMAGE_PREVIEW_PAGE_TAG)
            .performTouchInput {
                doubleClick(center)
            }

        composeTestRule.onNodeWithTag(IMAGE_PREVIEW_PAGE_TAG).assertIsDisplayed()
    }

    @Test
    fun longPressOnPreviewImage_triggersSaveActionWithoutClosingPreview() {
        var savedUrl: String? = null

        composeTestRule.setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .size(width = 390.dp, height = 844.dp)
                ) {
                    ImagePreviewDialog(
                        images = listOf("https://example.com/demo.jpg"),
                        initialIndex = 0,
                        sourceRect = Rect(24f, 60f, 196f, 232f),
                        onImageLongPress = { savedUrl = it },
                        onDismiss = {}
                    )
                    ImagePreviewOverlayHost(modifier = Modifier.fillMaxSize())
                }
            }
        }

        composeTestRule
            .onNodeWithTag(IMAGE_PREVIEW_PAGE_TAG)
            .performTouchInput {
                longClick(center)
            }

        composeTestRule.runOnIdle {
            assertEquals("https://example.com/demo.jpg", savedUrl)
        }
        composeTestRule.onNodeWithTag(IMAGE_PREVIEW_PAGE_TAG).assertIsDisplayed()
    }

    @Test
    fun commentPreview_keepsOpenWhenImageTappedAndShowsCommentChrome() {
        var dismissed = false
        var liked = false
        var replied = false

        composeTestRule.setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .size(width = 390.dp, height = 844.dp)
                ) {
                    ImagePreviewDialog(
                        images = listOf("https://example.com/comment.jpg"),
                        initialIndex = 0,
                        sourceRect = Rect(24f, 60f, 196f, 232f),
                        textContent = ImagePreviewTextContent(
                            headline = "江南大学的 Simple 本人",
                            body = "哈哈哈",
                            commentContext = ImagePreviewCommentContext(
                                authorName = "江南大学的 Simple 本人",
                                avatarUrl = "https://example.com/avatar.jpg",
                                timeText = "4小时前",
                                body = "哈哈哈",
                                originalSizeLabels = listOf("查看原图 (1.1M)"),
                                likeCount = 34,
                                liked = false,
                                onLikeClick = { liked = true },
                                onReplyClick = { replied = true }
                            )
                        ),
                        onDismiss = { dismissed = true }
                    )
                    ImagePreviewOverlayHost(modifier = Modifier.fillMaxSize())
                }
            }
        }

        composeTestRule.onNodeWithTag(IMAGE_PREVIEW_COMMENT_PANEL_TAG).assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(IMAGE_PREVIEW_PAGE_TAG)
            .performTouchInput {
                click(center)
            }

        composeTestRule.runOnIdle {
            assertEquals(false, dismissed)
            assertEquals(false, liked)
            assertEquals(false, replied)
        }
        composeTestRule.onNodeWithTag(IMAGE_PREVIEW_PAGE_TAG).assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("image_preview_also_post").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("image_preview_dislike").assertCountEquals(0)
    }
}
