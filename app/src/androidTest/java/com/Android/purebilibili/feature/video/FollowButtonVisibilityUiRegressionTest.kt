package com.android.purebilibili.feature.video

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.ViewInfo
import com.android.purebilibili.feature.video.ui.section.UpInfoSection
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FollowButtonVisibilityUiRegressionTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun hidingFollowButton_keepsOwnerIdentityAndHomeEntry() {
        runBlocking { SettingsManager.setShowVideoFollowButton(context, false) }

        composeTestRule.setContent {
            MaterialTheme {
                UpInfoSection(
                    info = ViewInfo(
                        bvid = "BV_TEST",
                        owner = Owner(mid = 1L, name = "测试UP主")
                    )
                )
            }
        }

        composeTestRule.waitUntil {
            composeTestRule.onAllNodesWithText("关注").fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithText("测试UP主").assertIsDisplayed()
        composeTestRule.onNodeWithText("关注").assertDoesNotExist()
    }

    @After
    fun restoreDefaultVisibility() {
        runBlocking { SettingsManager.setShowVideoFollowButton(context, true) }
    }
}
