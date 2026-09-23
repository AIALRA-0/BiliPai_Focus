package com.android.purebilibili.feature.home.components

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.ui.AppTopTabPresentation
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopTabMotionVelocityTest {

    @Test
    fun `horizontal only when liquid glass disabled`() {
        val velocity = resolveTopTabIndicatorVelocity(
            horizontalVelocityPxPerSecond = 1200f
        )

        assertEquals(1200f, velocity, 0.001f)
    }

    @Test
    fun `vertical does not contribute when liquid glass enabled`() {
        val velocity = resolveTopTabIndicatorVelocity(
            horizontalVelocityPxPerSecond = 1000f
        )

        assertEquals(1000f, velocity, 0.001f)
    }

    @Test
    fun `result is clamped to avoid excessive distortion`() {
        val velocity = resolveTopTabIndicatorVelocity(
            horizontalVelocityPxPerSecond = 5000f
        )

        assertEquals(4200f, velocity, 0.001f)
    }

    @Test
    fun `pager position velocity feeds capsule deformation`() {
        val velocity = resolveTopTabPagerVelocityItemsPerSecond(
            currentPosition = 2.4f,
            previousPosition = 2.0f,
            elapsedNanos = 100_000_000L
        )

        assertEquals(4f, velocity, 0.001f)
    }

    @Test
    fun `pager position velocity is clamped for capsule deformation`() {
        val velocity = resolveTopTabPagerVelocityItemsPerSecond(
            currentPosition = 4f,
            previousPosition = 0f,
            elapsedNanos = 100_000_000L
        )

        assertEquals(12f, velocity, 0.001f)
    }

    @Test
    fun `segmented pager matches home velocity cap`() {
        val velocity = resolveSegmentedControlExternalPagerVelocityItemsPerSecond(
            currentPosition = 1f,
            previousPosition = 0f,
            elapsedNanos = 50_000_000L,
        )

        assertEquals(12f, velocity, 0.001f)
    }

    @Test
    fun `segmented pager stretches during travel and releases at destination`() {
        assertTrue(
            shouldStretchSegmentedControlExternalPagerIndicator(
                position = 0.45f,
                externalPagerMotionActive = true,
            )
        )
        assertFalse(
            shouldStretchSegmentedControlExternalPagerIndicator(
                position = 1f,
                externalPagerMotionActive = true,
            )
        )
        assertFalse(
            shouldStretchSegmentedControlExternalPagerIndicator(
                position = 0.45f,
                externalPagerMotionActive = false,
            )
        )
    }

    @Test
    fun `direct drag keeps bottom bar velocity deformation`() {
        assertEquals(
            4f,
            resolveTopTabIndicatorLayerVelocityItemsPerSecond(
                motionVelocityItemsPerSecond = 4f
            ),
            0.001f
        )
        assertEquals(
            4f,
            resolveTopTabIndicatorLayerVelocityItemsPerSecond(
                motionVelocityItemsPerSecond = 4f
            ),
            0.001f
        )
    }

    @Test
    fun `vertical motion alone does not mark interacting when liquid glass enabled`() {
        val interacting = shouldTopTabIndicatorBeInteracting(
            pagerIsScrolling = false,
            combinedVelocityPxPerSecond = 10f,
            liquidGlassEnabled = true
        )

        assertEquals(false, interacting)
    }

    @Test
    fun `vertical motion ignored when liquid glass disabled`() {
        val interacting = shouldTopTabIndicatorBeInteracting(
            pagerIsScrolling = false,
            combinedVelocityPxPerSecond = 10f,
            liquidGlassEnabled = false
        )

        assertEquals(false, interacting)
    }

    @Test
    fun `held pager drag keeps top tab indicator interacting even when scroll flag drops`() {
        val interacting = shouldTopTabIndicatorBeInteracting(
            pagerIsDragging = true,
            pagerIsScrolling = false,
            combinedVelocityPxPerSecond = 0f,
            liquidGlassEnabled = true
        )

        assertEquals(true, interacting)
    }

    @Test
    fun `liquid glass top tab keeps enlarged interaction briefly after pager stops`() {
        assertEquals(
            140L,
            resolveTopTabIndicatorInteractionReleaseDelayMillis(liquidGlassEnabled = true)
        )
        assertEquals(
            0L,
            resolveTopTabIndicatorInteractionReleaseDelayMillis(liquidGlassEnabled = false)
        )
    }

    @Test
    fun `tiny pager jitter is ignored by horizontal delta resolver`() {
        val delta = resolveTopTabHorizontalDeltaPx(
            positionDeltaPages = 0.0008f,
            tabWidthPx = 92f
        )

        assertEquals(0f, delta, 0.0001f)
    }

    @Test
    fun `meaningful page movement produces horizontal delta`() {
        val delta = resolveTopTabHorizontalDeltaPx(
            positionDeltaPages = 0.25f,
            tabWidthPx = 100f
        )

        assertEquals(25f, delta, 0.0001f)
    }

    @Test
    fun `viewport shift uses first visible item index and offset`() {
        val shift = resolveTopTabIndicatorViewportShiftPx(
            firstVisibleItemIndex = 2,
            firstVisibleItemScrollOffsetPx = 24,
            tabWidthPx = 92f
        )

        assertEquals(208f, shift, 0.0001f)
    }

    @Test
    fun `viewport shift returns zero for invalid width`() {
        val shift = resolveTopTabIndicatorViewportShiftPx(
            firstVisibleItemIndex = 2,
            firstVisibleItemScrollOffsetPx = 24,
            tabWidthPx = 0f
        )

        assertEquals(0f, shift, 0.0001f)
    }

    @Test
    fun `indicator clamp shift ignores manual top tab row scroll`() {
        val shift = resolveTopTabIndicatorViewportClampShiftPx(
            rowScrollOffsetPx = 240f,
            indicatorPanelOffsetPx = 8f
        )

        assertEquals(0f, shift, 0.0001f)
    }

    @Test
    fun `static top tab indicator policy keeps neutral color without motion effects`() {
        val policy = resolveTopTabStaticIndicatorVisualPolicy(useNeutralIndicatorTint = true)

        assertEquals(false, policy.isInMotion)
        assertEquals(false, policy.shouldRefract)
        assertEquals(true, policy.useNeutralTint)
    }

    @Test
    fun `top tab neutral indicator color stays muted over wallpaper`() {
        assertEquals(
            Color(0xFFEAF2EF).copy(alpha = 0.42f),
            resolveTopTabNeutralIndicatorColor(isDarkTheme = false, alpha = 0.42f)
        )
        assertEquals(
            Color(0xFFE1E8E5).copy(alpha = 0.38f),
            resolveTopTabNeutralIndicatorColor(isDarkTheme = true, alpha = 0.38f)
        )
    }

    @Test
    fun `top tab neutral indicator alpha avoids bottom bar opacity floor`() {
        assertEquals(
            0.42f,
            resolveTopTabNeutralIndicatorTintAlpha(isDarkTheme = false, configuredAlpha = 0.16f),
            0.001f
        )
        assertEquals(
            0.38f,
            resolveTopTabNeutralIndicatorTintAlpha(isDarkTheme = true, configuredAlpha = 0.16f),
            0.001f
        )
        assertEquals(
            0.72f,
            resolveTopTabNeutralIndicatorTintAlpha(isDarkTheme = false, configuredAlpha = 0.72f),
            0.001f
        )
    }

    @Test
    fun `ios capsule translation follows fractional pager position with viewport offset`() {
        val translation = resolveIosTopTabCapsuleTranslationPx(
            absolutePagerPosition = 1.4f,
            itemWidthPx = 100f,
            rowScrollOffsetPx = 20f,
            contentPaddingPx = 2f
        )

        assertEquals(122f, translation, 0.001f)
    }

    @Test
    fun `top tab indicator supports direct horizontal drag while screen pager remains available`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt")
        val lazyRowSource = source
            .substringAfter("LazyRow(")
            .substringBefore("itemsIndexed(")

        assertTrue(source.contains(".zIndex(3f)"))
        assertTrue(source.contains(".then(indicatorGestureModifier)"))
        assertTrue(source.contains(".then(indicatorDragModifier)"))
        assertTrue(source.contains("Modifier.draggable("))
        assertTrue(source.contains("orientation = Orientation.Horizontal"))
        assertTrue(source.contains("topTabIndicatorDirectDragPosition"))
        assertFalse(lazyRowSource.contains("topTabIndicatorDrag("))
        assertFalse(source.contains("topTabIndicatorDrag("))
        assertFalse(source.contains("awaitHorizontalTouchSlopOrCancellation"))
    }

    @Test
    fun `top tab direct drag snaps to nearest bounded item`() {
        assertEquals(0, resolveTopTabIndicatorDragTargetIndex(-0.4f, itemCount = 5))
        assertEquals(2, resolveTopTabIndicatorDragTargetIndex(1.6f, itemCount = 5))
        assertEquals(4, resolveTopTabIndicatorDragTargetIndex(5.2f, itemCount = 5))
        assertEquals(0, resolveTopTabIndicatorDragTargetIndex(1f, itemCount = 0))
    }

    @Test
    fun `top tab row disables unsynchronized stretch overscroll`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt")
        val lazyRowSource = source
            .substringAfter("LazyRow(")
            .substringBefore("itemsIndexed(")

        assertTrue(lazyRowSource.contains("overscrollEffect = null"))
    }

    @Test
    fun `top tab export defers lazy row scroll reads to its graphics layer`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt")
        val exportSource = source
            .substringAfter("val topTabListScrollOffsetPxProvider = {")
            .substringBefore("LazyRow(")

        assertTrue(exportSource.contains("listState.firstVisibleItemScrollOffset"))
        assertEquals(3, exportSource.split("topTabListScrollOffsetPxProvider()").size - 1)
    }

    @Test
    fun `top tab liquid panel offset moves indicator without rebounding labels`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt")
        val stableContentGroup = source
            .substringAfter("val topTabIndicatorPanelOffsetPx =")
            .substringBefore("// stable export + visible content with indicator-only motion")

        assertFalse(stableContentGroup.contains("translationX = topTabIndicatorPanelOffsetPx"))
        assertEquals(
            3,
            stableContentGroup.split("indicatorPanelOffsetPx = topTabIndicatorPanelOffsetPx").size - 1
        )
    }

    @Test
    fun `top tab drag does not change search or list layout clearance`() {
        val headerSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/HomeHeader.kt")
        val homeSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")

        assertTrue(headerSource.contains("alpha = searchContentAlphaProvider.value"))
        assertTrue(headerSource.contains("translationY = searchContentTranslationProvider.value()"))
        assertFalse(headerSource.contains("searchContentTranslationYPx"))
        assertFalse(headerSource.contains("onIndicatorClearanceChanged = { clearance ->"))
        assertFalse(homeSource.contains("topTabIndicatorClearance"))
        assertFalse(homeSource.contains("baseListTopPadding +"))
    }

    @Test
    fun `ios capsule uses moving shared container instead of per item fill`() {
        assertEquals(
            false,
            shouldDrawLightweightTopTabItemContainer(
                presentation = AppTopTabPresentation.MOVING_CAPSULE,
                skinPlainStyle = false,
                hasSkinStickerIcon = false
            )
        )
        assertEquals(
            true,
            shouldDrawLightweightTopTabItemContainer(
                presentation = AppTopTabPresentation.MATERIAL_UNDERLINE,
                skinPlainStyle = false,
                hasSkinStickerIcon = false
            )
        )
        assertEquals(
            true,
            shouldDrawLightweightTopTabItemContainer(
                presentation = AppTopTabPresentation.MOVING_CAPSULE,
                skinPlainStyle = false,
                hasSkinStickerIcon = true
            )
        )
    }

    @Test
    fun `capsule top tabs suppress rectangular item click indication`() {
        assertFalse(
            shouldUseLightweightTopTabItemClickIndication(
                presentation = AppTopTabPresentation.MOVING_CAPSULE,
                skinPlainStyle = false,
                usesCapsuleIndicator = true
            )
        )
        assertFalse(
            shouldUseLightweightTopTabItemClickIndication(
                presentation = AppTopTabPresentation.MATERIAL_UNDERLINE,
                skinPlainStyle = false,
                usesCapsuleIndicator = true
            )
        )
        assertFalse(
            shouldUseLightweightTopTabItemClickIndication(
                presentation = AppTopTabPresentation.TONAL_CAPSULE,
                skinPlainStyle = false,
                usesCapsuleIndicator = true
            )
        )
    }

    @Test
    fun `plain md3 top tabs keep item click indication`() {
        assertTrue(
            shouldUseLightweightTopTabItemClickIndication(
                presentation = AppTopTabPresentation.MATERIAL_UNDERLINE,
                skinPlainStyle = false,
                usesCapsuleIndicator = false
            )
        )
        assertTrue(
            shouldUseLightweightTopTabItemClickIndication(
                presentation = AppTopTabPresentation.MOVING_CAPSULE,
                skinPlainStyle = true,
                usesCapsuleIndicator = true
            )
        )
    }

    @Test
    fun `follow scroll centers selected item on item boundaries while moving right`() {
        val target = resolveTopTabFollowScrollTarget(
            indicatorPosition = 4.2f,
            itemWidthPx = 100f,
            itemCount = 8,
            viewportWidthPx = 300f,
            currentFirstVisibleItemIndex = 0,
            currentFirstVisibleItemScrollOffsetPx = 0,
            maxScrollPx = 500f,
            edgeBufferPx = 20f
        )

        assertEquals(TopTabScrollTarget(firstVisibleItemIndex = 3, firstVisibleItemScrollOffsetPx = 0), target)
    }

    @Test
    fun `follow scroll centers selected item on item boundaries while moving left`() {
        val target = resolveTopTabFollowScrollTarget(
            indicatorPosition = 1f,
            itemWidthPx = 100f,
            itemCount = 8,
            viewportWidthPx = 300f,
            currentFirstVisibleItemIndex = 2,
            currentFirstVisibleItemScrollOffsetPx = 50,
            maxScrollPx = 500f,
            edgeBufferPx = 20f
        )

        assertEquals(TopTabScrollTarget(firstVisibleItemIndex = 0, firstVisibleItemScrollOffsetPx = 0), target)
    }

    @Test
    fun `follow scroll keeps middle selected category in the center slot`() {
        val target = resolveTopTabFollowScrollTarget(
            indicatorPosition = 3f,
            itemWidthPx = 100f,
            itemCount = 8,
            viewportWidthPx = 500f,
            currentFirstVisibleItemIndex = 0,
            currentFirstVisibleItemScrollOffsetPx = 0,
            maxScrollPx = 300f,
            edgeBufferPx = 20f
        )

        assertEquals(TopTabScrollTarget(firstVisibleItemIndex = 1, firstVisibleItemScrollOffsetPx = 0), target)
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
