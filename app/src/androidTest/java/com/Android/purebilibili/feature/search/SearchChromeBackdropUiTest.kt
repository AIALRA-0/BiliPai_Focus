package com.android.purebilibili.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.blur.rememberChromeBackdropSource
import com.android.purebilibili.feature.home.components.liquid.rememberCombinedBackdrop
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchChromeBackdropUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun foregroundSamplesColoredCategoryAndPageWithoutCapturingItself() {
        rule.setContent {
            val page = rememberChromeBackdropSource()
            val categories = rememberChromeBackdropSource()
            val combined = rememberCombinedBackdrop(page.backdrop, categories.backdrop)
            val coordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }
            Box(Modifier.size(120.dp, 120.dp)) {
                Box(Modifier.size(120.dp, 120.dp).then(page.modifier).background(Color.Black))
                Box(
                    Modifier.offset(y = 60.dp).size(120.dp, 40.dp)
                        .then(categories.modifier).background(Color.Blue),
                )
                if (page.isReady && categories.isReady) {
                    // 前景覆盖源区域；它不属于任一捕获源，因此不会录回自己的红色底板。
                    Box(
                        Modifier.offset(x = 20.dp, y = 40.dp).size(80.dp, 70.dp)
                            .testTag("foreground")
                            .onGloballyPositioned { coordinates.value = it }
                            .drawBehind {
                                drawRect(Color.Red)
                                with(combined) {
                                    drawBackdrop(
                                        density = this@drawBehind,
                                        coordinates = coordinates.value,
                                    )
                                }
                            },
                    )
                }
            }
        }
        rule.waitForIdle()
        val pixels = rule.onNodeWithTag("foreground").captureToImage().toPixelMap()
        val page = pixels[pixels.width / 2, pixels.height / 10]
        val category = pixels[pixels.width / 2, pixels.height / 2]
        assertEquals(0f, page.red, 0.02f)
        assertEquals(0f, page.blue, 0.02f)
        assertEquals(0f, category.red, 0.02f)
        assertEquals(1f, category.blue, 0.02f)
    }
}
