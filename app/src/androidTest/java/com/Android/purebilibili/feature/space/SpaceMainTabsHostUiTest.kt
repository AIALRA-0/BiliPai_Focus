package com.android.purebilibili.feature.space

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpaceMainTabsHostUiTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun pinningAndViewportReentryKeepTheSameRail() {
        val position = mutableStateOf<IntOffset?>(IntOffset(0, 160))
        var mounts = 0
        var disposals = 0
        rule.setContent {
            SpaceMainTabsHost(
                sharedGlassTabs = true,
                position = { position.value },
                modifier = Modifier.fillMaxSize(),
                tabs = {
                    DisposableEffect(Unit) {
                        mounts++
                        onDispose { disposals++ }
                    }
                    Box(Modifier.fillMaxWidth().height(40.dp).testTag("shared-tabs"))
                },
            ) { slot ->
                Column { checkNotNull(slot).invoke() }
            }
        }
        rule.onNodeWithTag("shared-tabs").assertIsDisplayed()
        rule.runOnIdle { position.value = IntOffset(0, 40) }
        rule.onNodeWithTag("shared-tabs").assertIsDisplayed()
        rule.runOnIdle { position.value = null }
        rule.onNodeWithTag("shared-tabs").assertIsNotDisplayed()
        rule.runOnIdle { position.value = IntOffset(0, 160) }
        rule.onNodeWithTag("shared-tabs").assertIsDisplayed()
        rule.runOnIdle {
            assertEquals(1, mounts)
            assertEquals(0, disposals)
        }
    }

    @Test
    fun nativeMaterialKeepsPageOwnedTabs() {
        rule.setContent {
            SpaceMainTabsHost(
                sharedGlassTabs = false,
                position = { error("Native tabs do not use the glass position") },
                tabs = { error("Native tabs must not mount the shared glass rail") },
            ) { slot ->
                assertNull(slot)
                Box(Modifier.fillMaxWidth().height(40.dp).testTag("native-tabs"))
            }
        }
        rule.onNodeWithTag("native-tabs").assertIsDisplayed()
    }

    @Test
    fun changingPagerPageKeepsTheSamePointerOwnerAndDragCoordinates() {
        lateinit var pager: PagerState
        lateinit var scope: CoroutineScope
        var mounts = 0
        var disposals = 0
        var starts = 0
        var finishes = 0
        var dragX = 0f
        rule.setContent {
            pager = rememberPagerState { 5 }
            scope = rememberCoroutineScope()
            SpaceMainTabsHost(
                sharedGlassTabs = true,
                position = { IntOffset(0, 80) },
                tabs = {
                    DisposableEffect(Unit) {
                        mounts++
                        onDispose { disposals++ }
                    }
                    Box(
                        Modifier.fillMaxWidth().height(40.dp).testTag("shared-tabs")
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { starts++ },
                                    onDragEnd = { finishes++ },
                                    onDragCancel = { finishes++ },
                                ) { change, amount ->
                                    change.consume()
                                    dragX += amount.x
                                }
                            },
                    )
                },
                modifier = Modifier.fillMaxSize(),
            ) { tabSlot ->
                HorizontalPager(state = pager, userScrollEnabled = false) { _ ->
                    Column(Modifier.fillMaxSize()) {
                        Spacer(Modifier.height(80.dp))
                        checkNotNull(tabSlot).invoke()
                    }
                }
            }
        }
        rule.onNodeWithTag("shared-tabs").performTouchInput {
            down(Offset(20f, centerY))
            moveBy(Offset(80f, 0f))
        }
        var before = 0f
        rule.runOnIdle {
            before = dragX
            scope.launch { pager.scrollToPage(4) }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("shared-tabs").performTouchInput {
            moveBy(Offset(20f, 0f))
            up()
        }
        rule.runOnIdle {
            assertEquals(1, mounts)
            assertEquals(0, disposals)
            assertEquals(1, starts)
            assertEquals(1, finishes)
            assertEquals(before + 20f, dragX, 0.5f)
        }
    }
}
