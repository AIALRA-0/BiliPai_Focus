package com.android.purebilibili.core.ui

import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.ui.components.shouldUseCompactMiuixTabRow
import com.android.purebilibili.core.ui.components.resolveReadableNativeTabMinWidth
import com.android.purebilibili.core.ui.components.resolveCompactMiuixTabRowWidth
import com.android.purebilibili.core.ui.components.resolveLabelContentMinWidth
import com.android.purebilibili.core.ui.components.resolveAppMiuixTabContentColor
import com.android.purebilibili.core.ui.components.resolveAppMiuixTabTrackColor
import com.android.purebilibili.core.ui.components.resolveEqualMiuixNonGlassTabItemWidth
import com.android.purebilibili.core.ui.components.resolveMiuixTabMinWidth
import com.android.purebilibili.core.ui.components.resolvePiliPlusScrollableUnderlineMinWidth
import com.android.purebilibili.core.ui.components.resolveMiuixNonGlassContentTabItemWidths
import com.android.purebilibili.core.ui.components.shouldEqualizeMiuixNonGlassTabItems
import com.android.purebilibili.core.ui.components.shouldStretchMiuixNonGlassTabRowToTrack
import com.android.purebilibili.core.ui.components.MIUIX_NON_GLASS_TAB_ITEM_SPACING_DP
import com.android.purebilibili.core.ui.components.MiuixNonGlassTabItemWidthMode
import androidx.compose.ui.graphics.Color
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSegmentedControlPolicyTest {

    @Test
    fun `non glass Miuix removes outer dock while keeping readable labels`() {
        val track = Color(0xFF303030)
        val inactive = Color(0xFF8A8A8A)
        val readable = Color(0xFFF2F2F2)

        assertEquals(Color.Transparent, resolveAppMiuixTabTrackColor(true, track))
        assertEquals(track, resolveAppMiuixTabTrackColor(false, track))
        assertEquals(readable, resolveAppMiuixTabContentColor(true, inactive, readable))
        assertEquals(inactive, resolveAppMiuixTabContentColor(false, inactive, readable))

        val source = loadSource(
            "src/main/java/com/android/purebilibili/core/ui/renderer/miuix/" +
                "AppMiuixSegmentedControl.kt"
        )
        assertTrue(source.contains("else -> tabColors.backgroundColor"))
        assertFalse(source.contains("adaptiveSquircleBackground(\n                color = trackColor"))
        assertTrue(source.contains("AppMiuixNonGlassTabs("))
        assertTrue(source.contains("Arrangement.spacedBy(AppSpacingTokens.Small)"))
    }

    @Test
    fun `non glass tabs keep native geometry and grow for accessible text`() {
        assertEquals(RoundedControlVisualGeometry(42.dp, 12.dp),
            resolveMiuixNonGlassControlGeometry(false, 22.dp))
        assertEquals(RoundedControlVisualGeometry(36.dp, 10.dp),
            resolveMiuixNonGlassControlGeometry(true, 20.dp))
        assertEquals(RoundedControlVisualGeometry(64.dp, 12.dp),
            resolveMiuixNonGlassControlGeometry(false, 48.dp))
    }


    @Test
    fun `compact two option rows do not consume viewport`() {
        assertEquals(144.dp, resolveCompactMiuixTabRowWidth(400.dp, 72.dp, 2, false))
        assertEquals(400.dp, resolveCompactMiuixTabRowWidth(400.dp, 72.dp, 2, true))
        assertEquals(400.dp, resolveCompactMiuixTabRowWidth(400.dp, 72.dp, 3, false))
    }

    @Test
    fun `label content min width calculates safe minimum width to prevent ellipsis truncation`() {
        assertEquals(88.dp, resolveLabelContentMinWidth(listOf("相关推荐", "评论")))
        assertEquals(56.dp, resolveLabelContentMinWidth(listOf("简介", "评论")))
        assertEquals(72.dp, resolveLabelContentMinWidth(listOf("按热度", "按时间")))
        assertEquals(48.dp, resolveLabelContentMinWidth(listOf("A", "B")))
        assertEquals(0.dp, resolveLabelContentMinWidth(emptyList()))
    }

    @Test
    fun `native tabs expand shared item width across themes for complete long labels`() {
        assertEquals(
            56.dp,
            resolveReadableNativeTabMinWidth(
                requestedMinWidth = AppChromeSizeTokens.MinimumTouchTarget,
                labels = listOf("简介", "评论"),
                allowLabelOverflow = true,
            ),
        )
        assertEquals(
            88.dp,
            resolveReadableNativeTabMinWidth(
                requestedMinWidth = 72.dp,
                labels = listOf("播放多", "默认排序", "新发布"),
                allowLabelOverflow = true,
            ),
        )
        assertEquals(
            72.dp,
            resolveReadableNativeTabMinWidth(
                requestedMinWidth = 72.dp,
                labels = listOf("视频", "番剧"),
                allowLabelOverflow = true,
            ),
        )
        assertEquals(
            72.dp,
            resolveReadableNativeTabMinWidth(
                requestedMinWidth = 72.dp,
                labels = listOf("默认排序"),
                allowLabelOverflow = false,
            ),
        )
    }

    @Test
    fun `Miuix non glass equal tabs follow the longest measured label`() {
        assertEquals(
            88.dp,
            resolveEqualMiuixNonGlassTabItemWidth(
                longestLabelWidth = 72.dp,
                minTabWidth = AppChromeSizeTokens.MinimumTouchTarget,
            ),
        )
        assertEquals(
            64.dp,
            resolveEqualMiuixNonGlassTabItemWidth(
                longestLabelWidth = 20.dp,
                minTabWidth = 64.dp,
            ),
        )
    }

    @Test
    fun `Miuix non glass content tabs size each item from its own label`() {
        assertEquals(
            listOf(48.dp, 75.dp, 320.dp),
            resolveMiuixNonGlassContentTabItemWidths(
                labelWidths = listOf(20.dp, 51.dp, 400.dp),
                minTabWidth = 48.dp,
            ),
        )
    }

    @Test
    fun `long collection titles do not inflate short content sized tabs`() {
        val sharedWidth = resolveReadableNativeTabMinWidth(
            requestedMinWidth = 48.dp,
            labels = listOf("视频", "图文", "这是一个很长的合集标题"),
            allowLabelOverflow = true,
        )
        assertTrue(sharedWidth > 56.dp)
        val contentMinWidth = resolveMiuixTabMinWidth(
            requestedMinWidth = 48.dp,
            sharedMinWidth = sharedWidth,
            contentSizedItems = true,
        )
        assertEquals(
            listOf(56.dp, 56.dp, 200.dp),
            resolveMiuixNonGlassContentTabItemWidths(
                labelWidths = listOf(32.dp, 32.dp, 176.dp),
                minTabWidth = contentMinWidth,
            ),
        )
        assertEquals(sharedWidth, resolveMiuixTabMinWidth(48.dp, sharedWidth, false))
        assertEquals(72.dp, resolveMiuixTabMinWidth(72.dp, sharedWidth, true))
        assertEquals(0.dp, resolvePiliPlusScrollableUnderlineMinWidth())
        val source = loadSource("src/main/java/com/android/purebilibili/core/ui/components/AppSegmentedControl.kt")
        val materialRenderer = source
            .substringAfter("AppSegmentedRenderer.MATERIAL3 -> AppMaterial3TabRow(")
            .substringBefore("AppSegmentedRenderer.MIUIX -> AppMiuixTabRow(")
        assertTrue(materialRenderer.contains("minTabWidth = resolvePiliPlusScrollableUnderlineMinWidth()"))
        assertFalse(materialRenderer.contains("resolveMiuixTabMinWidth("))
        val renderer = source.substringAfter("AppSegmentedRenderer.MIUIX -> AppMiuixTabRow(")
        assertTrue(renderer.contains("minTabWidth = resolveMiuixTabMinWidth("))
        assertTrue(renderer.contains("contentSizedItems = useContentSizedMiuixItems"))
    }

    @Test
    fun `Miuix non glass content tabs can keep long labels complete`() {
        assertEquals(
            listOf(48.dp, 424.dp),
            resolveMiuixNonGlassContentTabItemWidths(
                labelWidths = listOf(20.dp, 400.dp),
                minTabWidth = 48.dp,
                maxTabWidth = androidx.compose.ui.unit.Dp.Infinity,
            ),
        )
    }

    @Test
    fun `equal longest label mode is isolated to Miuix non glass tabs`() {
        assertTrue(
            shouldEqualizeMiuixNonGlassTabItems(
                widthMode = MiuixNonGlassTabItemWidthMode.EQUAL_TO_LONGEST_LABEL,
                isMiuixNonGlass = true,
                optionCount = 2,
            )
        )
        assertFalse(
            shouldEqualizeMiuixNonGlassTabItems(
                widthMode = MiuixNonGlassTabItemWidthMode.EQUAL_TO_LONGEST_LABEL,
                isMiuixNonGlass = false,
                optionCount = 2,
            )
        )
        assertFalse(
            shouldEqualizeMiuixNonGlassTabItems(
                widthMode = MiuixNonGlassTabItemWidthMode.CONTENT,
                isMiuixNonGlass = true,
                optionCount = 2,
            )
        )
    }

    @Test
    fun `non glass Miuix page tabs stay content sized except compact two option tracks`() {
        assertTrue(
            shouldStretchMiuixNonGlassTabRowToTrack(
                compact = true,
                scrollable = false,
                optionCount = 2,
            )
        )
        assertFalse(
            shouldStretchMiuixNonGlassTabRowToTrack(
                compact = false,
                scrollable = false,
                optionCount = 3,
            )
        )
        assertFalse(
            shouldStretchMiuixNonGlassTabRowToTrack(
                compact = false,
                scrollable = true,
                optionCount = 6,
            )
        )
        assertEquals(9, MIUIX_NON_GLASS_TAB_ITEM_SPACING_DP)
        val miuixSource = loadSource(
            "src/main/java/com/android/purebilibili/core/ui/renderer/miuix/" +
                "AppMiuixSegmentedControl.kt"
        )
        val nonGlassTabs = miuixSource.substringAfter("private fun <T> AppMiuixNonGlassTabs(")
        assertTrue(nonGlassTabs.contains("TabRow("))
        assertTrue(nonGlassTabs.contains("itemSpacing = AppSpacingTokens.Small"))
        assertTrue(nonGlassTabs.contains("maxWidth = tabRowMaxWidth"))
    }

    @Test
    fun `only non scrollable two option Miuix rows use compact width`() {
        assertTrue(shouldUseCompactMiuixTabRow(2, scrollable = false, compactWhenTwoOptions = true))
        assertFalse(shouldUseCompactMiuixTabRow(2, scrollable = true, compactWhenTwoOptions = true))
        assertFalse(shouldUseCompactMiuixTabRow(3, scrollable = false, compactWhenTwoOptions = true))
        assertFalse(shouldUseCompactMiuixTabRow(2, scrollable = false, compactWhenTwoOptions = false))
    }

    @Test
    fun `material3 exposes material segmented capabilities`() {
        val policy = resolveAppSegmentedControlPolicy(AppUiStyle.MATERIAL3)

        assertTrue(policy.usesEmphasizedTitle)
        assertTrue(policy.usesMaterialFallback)
        assertTrue(policy.usesMaterialColorTokens)
        assertFalse(policy.usesNativeTabRow)
    }

    @Test
    fun `miuix exposes native tab row capability`() {
        val policy = resolveAppSegmentedControlPolicy(AppUiStyle.MIUIX)

        assertTrue(policy.usesEmphasizedTitle)
        assertTrue(policy.usesMaterialFallback)
        assertTrue(policy.usesNativeTabRow)
        assertFalse(policy.usesMaterialColorTokens)
    }

    @Test
    fun `segmented policy exposes semantic corners without a shared visual height`() {
        val material = resolveAppSegmentedControlPolicy(AppUiStyle.MATERIAL3)
        val miuix = resolveAppSegmentedControlPolicy(AppUiStyle.MIUIX)
        assertEquals(10.8.dp, material.preferredCornerRadius)
        assertEquals(16.dp, miuix.preferredCornerRadius)
    }

    @Test
    fun `oversized semantic corner is clamped instead of enlarging the control`() {
        val geometry = resolveRoundedControlVisualGeometry(
            preferredCornerRadius = 14.4.dp,
            nativeMinimumHeight = 40.dp,
        )

        assertEquals(48.dp, geometry.height)
        assertEquals(14.4.dp, geometry.cornerRadius)
        assertTrue(geometry.cornerRadius <= geometry.height * 0.3f + 0.1.dp)
    }

    @Test
    fun `native minimum wins when semantic corner already fits`() {
        val geometry = resolveRoundedControlVisualGeometry(
            preferredCornerRadius = 12.dp,
            nativeMinimumHeight = 42.dp,
        )

        assertEquals(42.dp, geometry.height)
        assertEquals(12.dp, geometry.cornerRadius)
    }

    @Test
    fun `native Miuix tabs keep compact visuals inside a 48dp touch target`() {
        val materialSource = loadSource(
            "src/main/java/com/android/purebilibili/core/ui/renderer/material3/" +
                "AppMaterial3SegmentedControl.kt"
        )
        val miuixSource = loadSource(
            "src/main/java/com/android/purebilibili/core/ui/renderer/miuix/" +
                "AppMiuixSegmentedControl.kt"
        )

        assertFalse(materialSource.contains("heightIn(min = 48.dp)"))
        assertTrue(miuixSource.contains("resolveRoundedControlVisualGeometry("))
        assertTrue(miuixSource.contains("TabRow("))
        assertTrue(miuixSource.contains("TabRowDefaults.TabRowMinWidth"))
        assertTrue(miuixSource.contains("TabRowDefaults.TabRowMaxWidth"))
        assertFalse(miuixSource.contains("AppMiuixNonGlassTabItem("))
    }

    @Test
    fun `md3 tab row overflows material text padding instead of ellipsizing compact labels`() {
        val materialSource = loadSource(
            "src/main/java/com/android/purebilibili/core/ui/renderer/material3/" +
                "AppMaterial3SegmentedControl.kt"
        )
        val tabRow = materialSource.substringAfter("internal fun <T> AppMaterial3TabRow(")

        assertTrue(tabRow.contains("text = {"))
        assertTrue(tabRow.contains("resolveAppSegmentedLabelFontSizeSp("))
        assertTrue(tabRow.contains("allowLabelOverflow"))
        assertTrue(tabRow.contains("wrapContentWidth("))
        assertTrue(tabRow.contains("unbounded = true"))
        assertTrue(tabRow.contains("softWrap = false"))
        assertTrue(tabRow.contains("TextOverflow.Visible"))
        assertFalse(tabRow.contains("TextOverflow.Ellipsis"))
        assertTrue(tabRow.contains("indicatorPositionProvider: (() -> Float)? = null"))
        assertTrue(tabRow.contains("indicatorPositionProvider = indicatorPositionProvider"))
    }

    @Test
    fun `miuix tab viewport clips scrolling content and background to its corners`() {
        val source = loadSource(
            "src/main/java/com/android/purebilibili/core/ui/renderer/miuix/" +
                "AppMiuixSegmentedControl.kt"
        )
        val tabRow = source.substringAfter("internal fun <T> AppMiuixTabRow(")

        assertTrue(source.contains("import top.yukonga.miuix.kmp.squircle.squircleClip"))
        assertTrue(tabRow.contains("modifier = modifier.squircleClip(geometry.cornerRadius)"))
        assertTrue(tabRow.contains("cornerRadius = geometry.cornerRadius"))
        assertTrue(tabRow.contains("listState = if (scrollable) scrollState else null"))
    }

    private fun loadSource(path: String): String = listOf(
        File(path),
        File("design-system/$path"),
    ).firstOrNull(File::exists)?.readText()
        ?: error("Cannot locate $path from ${File(".").absolutePath}")
}
