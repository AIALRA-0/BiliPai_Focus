package com.android.purebilibili.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.components.AppNativeTabRow
import com.android.purebilibili.core.ui.components.AppSegmentOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(AndroidJUnit4::class)
class MiuixContentTabWidthUiRegressionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun overflowingTabKeepsItsOwnCornersWithoutRoundingTheViewport() {
        val title = "合集 · iPad 扩容与维修系列完整教程"
        rule.setContent {
            NativeTheme(1.4f) {
                val colors = top.yukonga.miuix.kmp.theme.lightColorScheme().copy(
                    surface = Color.Yellow,
                    surfaceContainer = Color.Blue,
                    onBackground = Color.White,
                )
                MiuixTheme(colors = colors) {
                    Box(Modifier.width(160.dp).background(Color.Magenta).testTag("overflow-viewport")) {
                        AppNativeTabRow(
                            options = listOf(AppSegmentOption(0, title)),
                            selectedValue = 0,
                            onSelectionChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            scrollable = true,
                            centerContent = false,
                        )
                    }
                }
            }
        }
        assertTextFits(title)
        val edgeInsetPx = with(rule.density) { 2.dp.roundToPx() }.coerceAtLeast(1)
        val pixels = rule.onNodeWithTag("overflow-viewport").captureToImage().toPixelMap()
        // 右边界位于长标签中部，应保留标签底色；左边界仍保留标签自身的圆角。
        assertEquals(Color.Blue.toArgb(), pixels[pixels.width - edgeInsetPx, edgeInsetPx].toArgb())
        assertEquals(Color.Magenta.toArgb(), pixels[edgeInsetPx, edgeInsetPx].toArgb())
    }

    @Test
    fun collectionLabelsKeepIndependentWidthsAtNormalAndLargeFontScale() {
        val fontScale = mutableStateOf(1f)
        val labels = listOf("视频", "图文", "合集 · 自律餐", "合集 · 过年系列", "直播")
        rule.setContent {
            NativeTheme(fontScale.value) {
                Box(Modifier.width(280.dp)) {
                    AppNativeTabRow(
                        options = labels.mapIndexed { index, label -> AppSegmentOption(index, label) },
                        selectedValue = 3,
                        onSelectionChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        scrollable = true,
                        minTabWidth = 48.dp,
                        contentSizedMiuixNonGlassMaxItemWidth = Dp.Infinity,
                    )
                }
            }
        }
        for (scale in listOf(1f, 1.4f)) {
            rule.runOnIdle { fontScale.value = scale }
            rule.waitForIdle()
            labels.forEach(::assertTextFits)
            rule.onNodeWithText(labels[3]).assertIsDisplayed()
            val shortWidth = rule.onNodeWithText(labels[0]).getUnclippedBoundsInRoot().width
            val longWidth = rule.onNodeWithText(labels[3]).getUnclippedBoundsInRoot().width
            assertTrue("长合集应有更宽的独立槽位", longWidth > shortWidth)
        }
    }

    @Test
    fun commentCountUpdateUsesReservedWidthWithoutEllipsis() {
        val countLabel = mutableStateOf("评论 0")
        rule.setContent {
            NativeTheme(1.4f) {
                Box(Modifier.width(260.dp)) {
                    AppNativeTabRow(
                        options = listOf(
                            AppSegmentOption(0, "简介"),
                            AppSegmentOption(1, countLabel.value, measurementLabel = "评论 9999.9万"),
                        ),
                        selectedValue = 1,
                        onSelectionChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        scrollable = true,
                    )
                }
            }
        }
        assertTextFits("评论 0")
        val reservedWidth = rule.onNodeWithText("评论 0").getUnclippedBoundsInRoot().width.value
        rule.runOnIdle { countLabel.value = "评论 12.3万" }
        rule.waitForIdle()
        assertTextFits("评论 12.3万")
        rule.onNodeWithText("评论 12.3万").assertIsDisplayed()
        assertEquals(
            reservedWidth,
            rule.onNodeWithText("评论 12.3万").getUnclippedBoundsInRoot().width.value,
            0.5f,
        )
    }

    @Test
    fun fittingTabsCenterWhileOverflowAndExplicitStartAlignmentRemainScrollable() {
        val hostWidth = mutableStateOf(300.dp)
        val center = mutableStateOf(true)
        val equalWidth = mutableStateOf(false)
        val labels = listOf("主页", "动态", "投稿")
        rule.setContent {
            NativeTheme(1f) {
                Box(Modifier.width(hostWidth.value).testTag("tab-host")) {
                    AppNativeTabRow(
                        options = labels.mapIndexed { index, label -> AppSegmentOption(index, label) },
                        selectedValue = 0,
                        onSelectionChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        scrollable = true,
                        centerContent = center.value,
                        forceEqualWidth = equalWidth.value,
                        minTabWidth = 48.dp,
                    )
                }
            }
        }
        rule.waitForIdle()
        val host = rule.onNodeWithTag("tab-host").getUnclippedBoundsInRoot()
        val first = rule.onNodeWithText(labels.first()).getUnclippedBoundsInRoot()
        val last = rule.onNodeWithText(labels.last()).getUnclippedBoundsInRoot()
        assertEquals((host.left.value + host.right.value) / 2f, (first.left.value + last.right.value) / 2f, 1f)

        rule.runOnIdle { center.value = false }
        assertEquals(host.left.value, rule.onNodeWithText(labels.first()).getUnclippedBoundsInRoot().left.value, 1f)

        rule.runOnIdle { center.value = true; hostWidth.value = 100.dp }
        rule.waitForIdle()
        val narrow = rule.onNodeWithTag("tab-host").getUnclippedBoundsInRoot()
        assertEquals(narrow.left.value, rule.onNodeWithText(labels.first()).getUnclippedBoundsInRoot().left.value, 1f)
        labels.forEach(::assertTextFits)

        rule.runOnIdle { hostWidth.value = 300.dp; equalWidth.value = true }
        rule.waitForIdle()
        val widths = labels.map { rule.onNodeWithText(it).getUnclippedBoundsInRoot().width.value }
        assertEquals(widths.first(), widths.last(), 1f)
        assertEquals(host.right.value, rule.onNodeWithText(labels.last()).getUnclippedBoundsInRoot().right.value, 1f)
    }

    private fun assertTextFits(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue("缺少文字排版结果：$text", layouts.isNotEmpty())
        layouts.forEach { layout ->
            assertFalse("文字不应溢出：$text", layout.didOverflowWidth)
            assertFalse("文字不应省略：$text", layout.isLineEllipsized(0))
        }
    }

    @Composable
    private fun NativeTheme(fontScale: Float, content: @Composable () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalAppUiStyle provides AppUiStyle.MIUIX,
            LocalAppThemeConfig provides AppThemeConfig(liquidGlassEnabled = false),
            LocalDensity provides Density(density.density, fontScale),
        ) {
            MaterialTheme { MiuixTheme { content() } }
        }
    }
}
