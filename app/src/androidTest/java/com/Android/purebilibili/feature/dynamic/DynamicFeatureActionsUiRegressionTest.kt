package com.Android.purebilibili.feature.dynamic

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ImageLoader
import com.android.purebilibili.core.ui.AppThemeConfig
import com.android.purebilibili.core.ui.ProvideAppThemeConfig
import com.android.purebilibili.data.model.response.CoursesMajor
import com.android.purebilibili.data.model.response.DynamicAuthorModule
import com.android.purebilibili.data.model.response.DynamicAdditional
import com.android.purebilibili.data.model.response.DynamicAdditionalReserve
import com.android.purebilibili.data.model.response.DynamicAdditionalText
import com.android.purebilibili.data.model.response.DynamicBasic
import com.android.purebilibili.data.model.response.DynamicCardButton
import com.android.purebilibili.data.model.response.DynamicCardButtonStyle
import com.android.purebilibili.data.model.response.DynamicContentModule
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicMajor
import com.android.purebilibili.data.model.response.DynamicModules
import com.android.purebilibili.data.model.response.LiveRcmdMajor
import com.android.purebilibili.data.model.response.MedialistMajor
import com.android.purebilibili.data.model.response.MusicMajor
import com.android.purebilibili.data.model.response.SubscriptionNewMajor
import com.android.purebilibili.feature.dynamic.components.DynamicCardV2
import com.android.purebilibili.feature.dynamic.components.DynamicCardActions
import com.android.purebilibili.feature.dynamic.components.DynamicCardInteractionActions
import com.android.purebilibili.feature.dynamic.components.DynamicCardNavigationActions
import com.android.purebilibili.feature.dynamic.components.DynamicCardPresentation
import com.android.purebilibili.feature.dynamic.components.DynamicManageAction
import com.android.purebilibili.feature.dynamic.components.DynamicReserveResult
import com.android.purebilibili.feature.dynamic.components.DynamicShareSessionPresentation
import com.android.purebilibili.feature.dynamic.components.DynamicShareSessionRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DynamicFeatureActionsUiRegressionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun nativeMusicCollectionCourseAndSubscriptionCardsDispatchTheirCallbacks() {
        var musicId: Long? = null
        var collectionId: Long? = null
        var courseUrl: String? = null
        var liveRoomId: Long? = null
        val item = DynamicItem(
            id_str = "dynamic-1",
            type = "DYNAMIC_TYPE_MUSIC",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        music = MusicMajor(id = "au100", title = "原生音乐", label = "单曲"),
                        medialist = MedialistMajor(id = "200", title = "原生收藏夹"),
                        courses = CoursesMajor(
                            id = "300",
                            title = "原生课程",
                            jump_url = "https://www.bilibili.com/cheese/play/ss300",
                        ),
                        subscription_new = SubscriptionNewMajor(
                            live_rcmd = LiveRcmdMajor(
                                content = """{"live_play_info":{"title":"原生订阅直播","room_id":400,"online":5}}""",
                            ),
                        ),
                    ),
                ),
            ),
        )

        composeTestRule.setContent {
            val context = LocalContext.current
            val imageLoader = remember(context) { ImageLoader.Builder(context).build() }
            MaterialTheme {
                DynamicCardV2(
                    item = item,
                    gifImageLoader = imageLoader,
                    actions = DynamicCardActions(
                        navigation = DynamicCardNavigationActions(
                            onVideoClick = {},
                            onUserClick = {},
                            onMusicClick = { musicId = it },
                            onCollectionClick = { id, _, _, _ -> collectionId = id },
                            onCourseClick = { url, _ -> courseUrl = url },
                            onLiveClick = { roomId, _, _ -> liveRoomId = roomId },
                        ),
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("原生音乐").performClick()
        composeTestRule.onNodeWithText("原生收藏夹").performClick()
        composeTestRule.onNodeWithText("原生课程").performClick()
        composeTestRule.onNodeWithText("原生订阅直播").performClick()

        composeTestRule.runOnIdle {
            assertEquals(100L, musicId)
            assertEquals(200L, collectionId)
            assertEquals("https://www.bilibili.com/cheese/play/ss300", courseUrl)
            assertEquals(400L, liveRoomId)
        }
    }

    @Test
    fun ownDynamicOverflowMenuDispatchesSaveMessageShareAndCheck() {
        var saveClicks = 0
        var messageShareClicks = 0
        var checkClicks = 0
        val item = DynamicItem(
            id_str = "dynamic-own",
            basic = DynamicBasic(comment_type = 17),
            modules = DynamicModules(
                module_author = DynamicAuthorModule(mid = 42L, name = "作者"),
                module_dynamic = DynamicContentModule(),
            ),
        )

        composeTestRule.setContent {
            val context = LocalContext.current
            val imageLoader = remember(context) { ImageLoader.Builder(context).build() }
            ProvideAppThemeConfig(AppThemeConfig(nativeMiuixPopupsEnabled = false)) {
                MaterialTheme {
                    DynamicCardV2(
                        item = item,
                        gifImageLoader = imageLoader,
                        actions = DynamicCardActions(
                            navigation = DynamicCardNavigationActions(
                                onVideoClick = {},
                                onUserClick = {},
                            ),
                            interaction = DynamicCardInteractionActions(
                                onSaveDynamicClick = { saveClicks++ },
                                onShareToMessageClick = { messageShareClicks++ },
                                onCheckDynamicClick = { checkClicks++ },
                            ),
                        ),
                        presentation = DynamicCardPresentation(currentUserMid = 42L),
                    )
                }
            }
        }

        clickOverflowAction("保存动态")
        clickOverflowAction("分享至消息")
        clickOverflowAction("检查动态")

        composeTestRule.runOnIdle {
            assertEquals(1, saveClicks)
            assertEquals(1, messageShareClicks)
            assertEquals(1, checkClicks)
        }
    }

    @Test
    fun otherUserDynamicOverflowMenuDispatchesNotInterestedWithDynamicId() {
        var action: DynamicManageAction? = null
        val item = DynamicItem(
            id_str = "dynamic-other",
            modules = DynamicModules(
                module_author = DynamicAuthorModule(mid = 7L, name = "其他作者"),
                module_dynamic = DynamicContentModule(),
            ),
        )

        composeTestRule.setContent {
            val context = LocalContext.current
            val imageLoader = remember(context) { ImageLoader.Builder(context).build() }
            ProvideAppThemeConfig(AppThemeConfig(nativeMiuixPopupsEnabled = false)) {
                MaterialTheme {
                    DynamicCardV2(
                        item = item,
                        gifImageLoader = imageLoader,
                        actions = DynamicCardActions(
                            navigation = DynamicCardNavigationActions(
                                onVideoClick = {},
                                onUserClick = {},
                            ),
                            interaction = DynamicCardInteractionActions(
                                onManageAction = { action = it },
                            ),
                        ),
                        presentation = DynamicCardPresentation(currentUserMid = 42L),
                    )
                }
            }
        }

        clickOverflowAction("不感兴趣")

        composeTestRule.runOnIdle {
            assertTrue(action is DynamicManageAction.NotInterested)
            assertEquals("dynamic-other", (action as DynamicManageAction.NotInterested).dynamicId)
        }
    }

    @Test
    fun messageShareSessionRowShowsResolvedNameAndDispatchesClick() {
        var clicks = 0
        composeTestRule.setContent {
            MaterialTheme {
                DynamicShareSessionRow(
                    presentation = DynamicShareSessionPresentation(
                        name = "真实昵称",
                        avatarUrl = "",
                        resolvingUserInfo = false,
                    ),
                    sending = false,
                    enabled = true,
                    onClick = { clicks++ },
                )
            }
        }

        composeTestRule.onNodeWithText("真实昵称").assertIsDisplayed().performClick()
        composeTestRule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun reservationJumpButtonOpensItsUrlInsteadOfCallingToggleApi() {
        var openedUrl: String? = null
        var reserveCalls = 0
        val item = DynamicItem(
            id_str = "dynamic-reserve",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    additional = DynamicAdditional(
                        type = "ADDITIONAL_TYPE_RESERVE",
                        reserve = DynamicAdditionalReserve(
                            title = "首播预约",
                            rid = 66L,
                            button = DynamicCardButton(
                                jump_url = "https://www.bilibili.com/blackboard/reserve",
                                jump_style = DynamicCardButtonStyle(text = "查看详情"),
                            ),
                            desc3 = DynamicAdditionalText(
                                text = "预约抽奖",
                                jump_url = "https://www.bilibili.com/blackboard/gift",
                            ),
                        ),
                    ),
                ),
            ),
        )

        composeTestRule.setContent {
            val context = LocalContext.current
            val imageLoader = remember(context) { ImageLoader.Builder(context).build() }
            CompositionLocalProvider(
                LocalUriHandler provides object : UriHandler {
                    override fun openUri(uri: String) {
                        openedUrl = uri
                    }
                },
            ) {
                MaterialTheme {
                    DynamicCardV2(
                        item = item,
                        gifImageLoader = imageLoader,
                        actions = DynamicCardActions(
                            navigation = DynamicCardNavigationActions(
                                onVideoClick = {},
                                onUserClick = {},
                            ),
                            interaction = DynamicCardInteractionActions(
                                onReserveClick = { _, callback ->
                                    reserveCalls++
                                    callback(Result.success(DynamicReserveResult("已预约", 1L, 1)))
                                },
                            ),
                        ),
                    )
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("操作：查看详情").performClick()
        composeTestRule.runOnIdle {
            assertEquals("https://www.bilibili.com/blackboard/reserve", openedUrl)
            assertEquals(0, reserveCalls)
        }
    }

    private fun clickOverflowAction(label: String) {
        composeTestRule.onNodeWithContentDescription("更多").performClick()
        composeTestRule.onNodeWithText(label).assertIsDisplayed().performClick()
    }
}
