package com.android.purebilibili.feature.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.ui.blur.rememberChromeBackdropSource
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FloatingBottomBarHitTestRegressionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun hiddenGlassExportDoesNotStealVisibleTabTaps() {
        val visibleClicks = mutableListOf<Int>()
        val exportClicks = mutableListOf<Int>()
        rule.setContent {
            val export = rememberChromeBackdropSource(drawContentToScreen = false)
            Box(Modifier.size(240.dp, 64.dp).testTag("dock")) {
                Row(Modifier.size(240.dp, 64.dp)) {
                    repeat(3) { index ->
                        FloatingBottomBarItem(onClick = { visibleClicks += index }, itemIndex = index) {
                            Spacer(Modifier.size(24.dp))
                        }
                    }
                }
                // Same drawing order as the real dock: optical export above the visible row.
                CompositionLocalProvider(LocalFloatingBottomBarActiveContent provides true) {
                    Row(Modifier.size(240.dp, 64.dp).then(export.modifier)) {
                        repeat(3) { index ->
                            FloatingBottomBarItem(onClick = { exportClicks += index }, itemIndex = index) {
                                Spacer(Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
        repeat(3) { index ->
            // Real pointer hit testing is essential: a semantic click bypasses overlapping rows.
            rule.onNodeWithTag("dock").performTouchInput {
                click(Offset(width * (index + 0.5f) / 3f, height / 2f))
            }
        }
        rule.runOnIdle {
            assertEquals(listOf(0, 1, 2), visibleClicks)
            assertEquals(emptyList<Int>(), exportClicks)
        }
    }
}
