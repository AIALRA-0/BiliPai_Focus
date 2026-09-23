package com.Android.purebilibili.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.AppScaffold
import com.android.purebilibili.core.ui.AppTopBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class AdaptiveTopBarScrollUiRegressionTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var scrollBehavior: TopAppBarScrollBehavior
    private lateinit var gridState: LazyGridState
    private var style by mutableStateOf(AppUiStyle.MIUIX)

    private fun render(columns: Int) {
        rule.setContent {
            CompositionLocalProvider(LocalAppUiStyle provides style) {
                MaterialTheme {
                    MiuixTheme {
                        scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
                        gridState = rememberLazyGridState()
                        // Same scroll ownership as WatchLaterScreen, without glass or network data.
                        AppScaffold(
                            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                            topBar = {
                                AppTopBar(title = "稍后再看", scrollBehavior = scrollBehavior)
                            },
                        ) { padding ->
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(columns),
                                contentPadding = padding,
                                modifier = Modifier.fillMaxSize().testTag("watch-later-grid"),
                            ) {
                                items(100, key = { it }) {
                                    Box(Modifier.fillMaxWidth().height(100.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun miuixWithoutGlass_singleColumnScrollsInBothDirections() {
        assertMiuixScrolls(columns = 1)
    }

    @Test
    fun miuixWithoutGlass_multipleColumnsScrollInBothDirections() {
        assertMiuixScrolls(columns = 2)
    }

    private fun assertMiuixScrolls(columns: Int) {
        render(columns)
        rule.runOnIdle {
            assertEquals(0f, scrollBehavior.state.heightOffsetLimit, 0f)
            for (delta in listOf(-120f, 120f)) {
                assertEquals(
                    Offset.Zero,
                    scrollBehavior.nestedScrollConnection.onPreScroll(
                        Offset(0f, delta), NestedScrollSource.UserInput,
                    ),
                )
            }
        }
        rule.onNodeWithTag("watch-later-grid").performTouchInput { swipeUp() }
        var afterUp = 0
        rule.runOnIdle {
            afterUp = gridState.firstVisibleItemIndex * 10000 + gridState.firstVisibleItemScrollOffset
            assertTrue("Upward drag must reach the video grid", afterUp > 0)
        }
        rule.onNodeWithTag("watch-later-grid").performTouchInput { swipeDown() }
        rule.runOnIdle {
            val afterDown = gridState.firstVisibleItemIndex * 10000 + gridState.firstVisibleItemScrollOffset
            assertTrue("Downward drag must reach the video grid", afterDown < afterUp)
        }
    }

    @Test
    fun themeSwitchClearsStaleCollapseAndRestoresMaterialBehavior() {
        style = AppUiStyle.MATERIAL3
        render(columns = 1)
        rule.runOnIdle {
            assertTrue(scrollBehavior.state.heightOffsetLimit < 0f)
            assertTrue(scrollBehavior.state.heightOffsetLimit > -Float.MAX_VALUE)
            scrollBehavior.nestedScrollConnection.onPreScroll(
                Offset(0f, -24f), NestedScrollSource.UserInput,
            )
            assertTrue(scrollBehavior.state.heightOffset < 0f)
            style = AppUiStyle.MIUIX
        }
        rule.runOnIdle {
            assertEquals(0f, scrollBehavior.state.heightOffsetLimit, 0f)
            assertEquals(0f, scrollBehavior.state.heightOffset, 0f)
            style = AppUiStyle.MATERIAL3
        }
        rule.runOnIdle {
            assertTrue("Material bars must still have a measured collapse range", scrollBehavior.state.heightOffsetLimit < 0f)
            assertEquals(
                Offset(0f, -24f),
                scrollBehavior.nestedScrollConnection.onPreScroll(
                    Offset(0f, -24f), NestedScrollSource.UserInput,
                ),
            )
        }
    }
}
