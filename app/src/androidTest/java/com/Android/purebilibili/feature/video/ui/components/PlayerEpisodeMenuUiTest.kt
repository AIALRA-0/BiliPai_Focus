package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.android.purebilibili.feature.video.player.ExternalPlaylistSource
import com.android.purebilibili.feature.video.player.PlaylistItem
import com.android.purebilibili.feature.video.screen.ExternalPlaylistQueueSheetContent
import com.android.purebilibili.feature.video.screen.resolveExternalPlaylistQueueTitle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.AppThemeConfig
import com.android.purebilibili.core.ui.AppPopupWindowHostRenderer
import com.android.purebilibili.core.ui.LocalAppPopupWindowHostRenderer
import com.android.purebilibili.core.ui.LocalComponentMotionEnabled
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.data.model.response.Page
import com.android.purebilibili.data.model.response.UgcEpisode
import com.android.purebilibili.data.model.response.UgcSeason
import com.android.purebilibili.data.model.response.UgcSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(AndroidJUnit4::class)
class PlayerEpisodeMenuUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun currentPartIsFullyVisibleWhenThemeSheetOpens() {
        val pages = List(60) { index ->
            Page(cid = index + 1L, page = index + 1, part = "第${index + 1}段", duration = 65)
        }
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalComponentMotionEnabled provides false,
                LocalAppThemeConfig provides AppThemeConfig(liquidGlassEnabled = false),
            ) {
                MaterialTheme {
                    Box(Modifier.fillMaxSize()) {
                        PlayerEpisodeMenu(
                            pages = pages, currentPageIndex = 30,
                            onDismiss = {}, onPageSelect = {},
                        )
                    }
                }
            }
        }
        val current = composeTestRule.onNodeWithText(pages[30].part)
            .assertIsSelected().assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val close = composeTestRule.onNodeWithContentDescription("关闭")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(current.top >= close.bottom)
        assertEquals(with(composeTestRule.density) { 110.dp.toPx() }, current.height, 1f)
    }

    @Test
    fun partsUseThemeSheetInsteadOfPopupAndCloseExactlyOnceAcrossMaterials() {
        val visible = mutableStateOf(false)
        val glass = mutableStateOf(false)
        val style = mutableStateOf(AppUiStyle.MATERIAL3)
        var dismissed = 0
        val pages = listOf(
            Page(cid = 1L, page = 1, part = "第一段", duration = 60),
            Page(cid = 2L, page = 2, part = "第二段", duration = 60),
        )
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAppThemeConfig provides AppThemeConfig(liquidGlassEnabled = glass.value),
                LocalAppUiStyle provides style.value,
            ) {
                MaterialTheme { MiuixTheme {
                    Box(Modifier.fillMaxSize()) {
                        if (visible.value) PlayerEpisodeMenu(
                            pages = pages, currentPageIndex = 1,
                            onPageSelect = {},
                            onDismiss = { dismissed++; visible.value = false },
                        )
                    }
                } }
            }
        }
        var expectedDismissals = 0
        for (uiStyle in AppUiStyle.entries) {
            for (useGlass in listOf(false, true)) {
                composeTestRule.runOnIdle {
                    style.value = uiStyle; glass.value = useGlass; visible.value = true
                }
                composeTestRule.onNode(isDialog()).assertExists()
                composeTestRule.onNode(isPopup()).assertDoesNotExist()
                composeTestRule.onNodeWithText("第二段").assertIsSelected().assertIsDisplayed()
                composeTestRule.onNodeWithContentDescription("关闭").performClick()
                composeTestRule.onNode(isDialog()).assertDoesNotExist()
                expectedDismissals++
                composeTestRule.runOnIdle { assertEquals(expectedDismissals, dismissed) }
            }
        }
    }

    @Test
    fun glassWindowHostStaysWithinCompactPanelInsteadOfExpandingToWindow() {
        val glass = mutableStateOf(false)
        val renderer = object : AppPopupWindowHostRenderer {
            @Composable
            override fun Render(content: @Composable () -> Unit) {
                // 模拟真实玻璃菜单宿主的填满约束行为。
                Box(Modifier.fillMaxSize().testTag("panel_window_host")) { content() }
            }
        }
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAppThemeConfig provides AppThemeConfig(liquidGlassEnabled = glass.value),
                LocalAppPopupWindowHostRenderer provides renderer,
                LocalComponentMotionEnabled provides false,
            ) {
                MaterialTheme {
                    Box(Modifier.fillMaxSize()) {
                        PlayerDetailPanel(
                            title = "分P", isFullscreen = false, playerBounds = null,
                            compactHeight = true, onDismiss = {},
                            modifier = Modifier.testTag("compact_panel"),
                        ) { _ -> Box(Modifier.fillMaxSize()) }
                    }
                }
            }
        }
        val nativePanel = composeTestRule.onNodeWithTag("compact_panel")
            .fetchSemanticsNode().boundsInRoot
        composeTestRule.runOnIdle { glass.value = true }
        val glassPanel = composeTestRule.onNodeWithTag("compact_panel")
            .fetchSemanticsNode().boundsInRoot
        val host = composeTestRule.onNodeWithTag("panel_window_host")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(glassPanel.height > 0f)
        assertEquals(nativePanel.width, glassPanel.width, 1f)
        assertEquals(nativePanel.height, glassPanel.height, 1f)
        assertEquals(glassPanel.width, host.width, 1f)
        assertEquals(glassPanel.height, host.height, 1f)
    }

    @Test
    fun longPartsMenu_initiallyRevealsCurrentAndCanSelectLastThenFirstAcrossPopupHosts() {
        val nativePopup = mutableStateOf(false)
        val visible = mutableStateOf(false)
        val selected = mutableListOf<Int>()
        val pages = List(60) { index ->
            Page(cid = index.toLong() + 1, page = index + 1, part = "很长的第${index + 1}段标题，在窄菜单中最多展示两行", duration = 65)
        }
        composeTestRule.setContent {
            EpisodeMenuTheme(nativePopup.value) {
                Box(Modifier.fillMaxSize()) {
                    if (visible.value) {
                        PlayerEpisodeMenu(
                            pages = pages, currentPageIndex = 58, ugcSeason = null,
                            currentBvid = "BVparts", currentCid = 59,
                            isFullscreen = true,
                            onDismiss = { visible.value = false },
                            onPageSelect = { selected.add(it) },
                            onEpisodeSelect = { error("Parts must not invoke collection callback") },
                        )
                    }
                }
            }
        }
        for (native in listOf(false, true)) {
            composeTestRule.runOnIdle { nativePopup.value = native; visible.value = true; selected.clear() }
            composeTestRule.onNodeWithText(pages[58].part).assertIsSelected().assertIsDisplayed()
            composeTestRule.onNodeWithContentDescription("跳至底部").performClick()
            composeTestRule.onNodeWithText(pages.last().part).assertIsDisplayed()
                .performTouchInput { click() }
            composeTestRule.onNodeWithContentDescription("关闭").assertDoesNotExist()
            composeTestRule.runOnIdle { visible.value = true }
            composeTestRule.onNodeWithContentDescription("跳至顶部").performClick()
            composeTestRule.onNodeWithText(pages.first().part).assertIsDisplayed()
                .performTouchInput { click() }
            composeTestRule.onNodeWithContentDescription("关闭").assertDoesNotExist()
            composeTestRule.runOnIdle { assertEquals(listOf(59, 0), selected) }
        }
    }

    @Test
    fun collectionMenu_opensCurrentSectionAndSectionSwitchPreservesMultipartBvidAndCid() {
        val nativePopup = mutableStateOf(false)
        val visible = mutableStateOf(false)
        val multipart = UgcEpisode(
            id = 10, aid = 100, bvid = "BVcourse", cid = 101, title = "课程",
            pages = listOf(Page(cid = 101, page = 1, part = "前半"), Page(cid = 102, page = 2, part = "后半")),
        )
        val season = UgcSeason(sections = listOf(
            UgcSection(title = "上篇", episodes = listOf(multipart)),
            UgcSection(title = "下篇", episodes = listOf(UgcEpisode(bvid = "BVcurrent", cid = 200, title = "当前播放"))),
        ))
        var selected: UgcEpisode? = null
        composeTestRule.setContent {
            EpisodeMenuTheme(nativePopup.value) {
                Box(Modifier.fillMaxSize()) {
                    if (visible.value) {
                        PlayerEpisodeMenu(
                            pages = emptyList(), currentPageIndex = 0, ugcSeason = season,
                            currentBvid = "BVcurrent", currentCid = 200,
                            isFullscreen = true,
                            onDismiss = { visible.value = false },
                            onPageSelect = { error("Collection must not invoke page callback") },
                            onEpisodeSelect = { selected = it },
                        )
                    }
                }
            }
        }
        for (native in listOf(false, true)) {
            composeTestRule.runOnIdle { nativePopup.value = native; visible.value = true; selected = null }
            composeTestRule.onNodeWithText("当前播放").assertIsSelected().assertIsDisplayed()
            composeTestRule.onNodeWithText("上篇").performClick()
            composeTestRule.onNodeWithText("后半").performClick()
            composeTestRule.onNodeWithContentDescription("关闭").assertDoesNotExist()
            composeTestRule.runOnIdle { assertEquals(multipart.copy(cid = 102), selected) }
        }
    }


    @Test
    fun sourceMenuKeepsCurrentVideoPartsAndExternalQueueSeparatelyReachable() {
        val nativePopup = mutableStateOf(false)
        val sourceVisible = mutableStateOf(false)
        val partsVisible = mutableStateOf(false)
        val queueVisible = mutableStateOf(false)
        val currentPage = mutableStateOf(0)
        val currentQueueIndex = mutableStateOf(0)
        val selectedVideo = mutableStateOf("A" to 11L)
        val pages = listOf(
            Page(cid = 11, page = 1, part = "A的前半"),
            Page(cid = 12, page = 2, part = "A的后半"),
        )
        val queue = listOf(
            PlaylistItem(bvid = "A", cid = 11, title = "队列视频A", cover = "", owner = ""),
            PlaylistItem(bvid = "B", cid = 29, title = "队列视频B", cover = "", owner = ""),
        )
        val queueTitle = resolveExternalPlaylistQueueTitle(ExternalPlaylistSource.WATCH_LATER)
        composeTestRule.setContent {
            EpisodeMenuTheme(nativePopup.value) {
                Box(Modifier.fillMaxSize()) {
                    if (sourceVisible.value) {
                        PlayerEpisodeSourceMenu(
                            externalPlaylistTitle = queueTitle,
                            isFullscreen = true,
                            onDismiss = { sourceVisible.value = false },
                            onPartsClick = { partsVisible.value = true },
                            onExternalPlaylistClick = { queueVisible.value = true },
                        )
                    }
                    if (partsVisible.value) {
                        PlayerEpisodeMenu(
                            pages = pages, currentPageIndex = currentPage.value,
                            currentBvid = "A", currentCid = selectedVideo.value.second,
                            isFullscreen = true,
                            onDismiss = { partsVisible.value = false },
                            onPageSelect = {
                                currentPage.value = it
                                selectedVideo.value = "A" to pages[it].cid
                            },
                            onEpisodeSelect = { error("Part selection must not navigate to a season video") },
                        )
                    }
                    if (queueVisible.value) {
                        ExternalPlaylistQueueSheetContent(
                            title = queueTitle, playlist = queue,
                            currentIndex = currentQueueIndex.value,
                            listMaxHeight = 440.dp, bottomSpacerHeight = 0.dp,
                            onDismiss = { queueVisible.value = false },
                            onVideoSelected = { index, item ->
                                currentQueueIndex.value = index
                                selectedVideo.value = item.bvid to item.cid
                            },
                        )
                    }
                }
            }
        }
        for (native in listOf(false, true)) {
            composeTestRule.runOnIdle {
                nativePopup.value = native
                sourceVisible.value = true
                currentPage.value = 0
                currentQueueIndex.value = 0
                selectedVideo.value = "A" to 11L
            }
            composeTestRule.onNodeWithText("分P选集").assertIsDisplayed().performClick()
            composeTestRule.onNodeWithText("A的前半").assertIsSelected().assertIsDisplayed()
            composeTestRule.onNodeWithText("队列视频B").assertDoesNotExist()
            composeTestRule.onNodeWithText("A的后半").performClick()
            composeTestRule.runOnIdle {
                assertEquals("A" to 12L, selectedVideo.value)
                partsVisible.value = true
            }
            composeTestRule.onNodeWithText("A的后半").assertIsSelected().assertIsDisplayed()
            composeTestRule.onNodeWithContentDescription("关闭").performClick()
            composeTestRule.runOnIdle { sourceVisible.value = true }
            composeTestRule.onNodeWithText(queueTitle).assertIsDisplayed().performClick()
            composeTestRule.onNodeWithText("队列视频A").assertIsDisplayed()
            composeTestRule.onNodeWithText("A的后半").assertDoesNotExist()
            composeTestRule.onNodeWithText("队列视频B").assertIsDisplayed().performClick()
            composeTestRule.onNodeWithContentDescription("当前播放").assertIsDisplayed()
            composeTestRule.runOnIdle {
                assertEquals("B" to 29L, selectedVideo.value)
                assertFalse(sourceVisible.value)
                assertFalse(partsVisible.value)
                queueVisible.value = false
            }
        }
    }

    @Composable
    private fun EpisodeMenuTheme(nativePopup: Boolean, content: @Composable () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalAppUiStyle provides AppUiStyle.MIUIX,
            LocalAppThemeConfig provides AppThemeConfig(nativeMiuixPopupsEnabled = nativePopup),
            LocalDensity provides Density(density.density, 1.4f),
        ) {
            MaterialTheme { MiuixTheme { content() } }
        }
    }
}
