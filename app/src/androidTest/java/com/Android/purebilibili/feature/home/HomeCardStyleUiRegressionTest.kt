package com.android.purebilibili.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.store.HomeDurationStyle
import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.feature.home.components.cards.StoryVideoCard
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeCardStyleUiRegressionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun storyCover_appliesConfiguredAspectRatio() {
        assertCoverRatio(16f / 9f)
        assertCoverRatio(4f / 3f)
    }

    private fun assertCoverRatio(ratio: Float) {
        composeTestRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.width(320.dp)) {
                    StoryVideoCard(
                        video = VideoItem(
                            bvid = "BV_TEST",
                            title = "封面比例测试",
                            owner = Owner(name = "UP"),
                            duration = 180
                        ),
                        animationEnabled = false,
                        transitionEnabled = false,
                        homeDurationStyle = HomeDurationStyle.OUTSIDE_COVER,
                        coverAspectRatio = ratio,
                        cardHorizontalPadding = 0.dp,
                        onClick = { _, _ -> }
                    )
                }
            }
        }

        val bounds = composeTestRule
            .onNodeWithTag("home_story_video_cover")
            .fetchSemanticsNode()
            .boundsInRoot
        assertEquals(ratio, bounds.width / bounds.height, 0.02f)
    }
}
