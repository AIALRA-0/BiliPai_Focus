package com.android.purebilibili.feature.home.components

import androidx.compose.ui.graphics.Color
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
    fun `top tab long press drag only starts inside visible indicator bounds`() {
        val inside = shouldStartTopTabIndicatorLongPressDrag(
            pointerX = 134f,
            indicatorPosition = 2f,
            itemWidthPx = 72f,
            rowScrollOffsetPx = 64f,
            contentPaddingPx = 2f,
            indicatorWidthPx = 56f
        )
        val outside = shouldStartTopTabIndicatorLongPressDrag(
            pointerX = 80f,
            indicatorPosition = 2f,
            itemWidthPx = 72f,
            rowScrollOffsetPx = 64f,
            contentPaddingPx = 2f,
            indicatorWidthPx = 56f
        )

        assertEquals(true, inside)
        assertEquals(false, outside)
    }

    @Test
    fun `top tab indicator hit bounds account for row scroll offset`() {
        val indicatorLeft = resolveTopTabIndicatorHitLeftPx(
            indicatorPosition = 3f,
            itemWidthPx = 80f,
            rowScrollOffsetPx = 120f,
            contentPaddingPx = 0f,
            indicatorWidthPx = 32f
        )

        assertEquals(144f, indicatorLeft, 0.001f)
    }

    @Test
    fun `top tab long press drag is attached to selected item instead of lazy row scroll container`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt")
        val lazyRowSource = source
            .substringAfter("LazyRow(")
            .substringBefore("itemsIndexed(")

        assertTrue(source.contains("topTabSelectedItemLongPressDrag("))
        assertFalse(lazyRowSource.contains("topTabSelectedItemLongPressDrag("))
    }

    @Test
    fun `top tab drag does not change search or list layout clearance`() {
        val headerSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/iOSHomeHeader.kt")
        val homeSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")

        assertTrue(headerSource.contains("translationY = searchContentTranslationYPx"))
        assertFalse(headerSource.contains("onIndicatorClearanceChanged = { clearance ->"))
        assertFalse(homeSource.contains("topTabIndicatorClearance"))
        assertFalse(homeSource.contains("baseListTopPadding +"))
    }

    @Test
    fun `ios capsule uses moving shared container instead of per item fill`() {
        assertEquals(
            false,
            shouldDrawLightweightTopTabItemContainer(
                renderer = HomeTopTabRenderer.IOS,
                skinPlainStyle = false,
                hasSkinStickerIcon = false
            )
        )
        assertEquals(
            true,
            shouldDrawLightweightTopTabItemContainer(
                renderer = HomeTopTabRenderer.MD3,
                skinPlainStyle = false,
                hasSkinStickerIcon = false
            )
        )
        assertEquals(
            true,
            shouldDrawLightweightTopTabItemContainer(
                renderer = HomeTopTabRenderer.IOS,
                skinPlainStyle = false,
                hasSkinStickerIcon = true
            )
        )
    }

    @Test
    fun `capsule top tabs suppress rectangular item click indication`() {
        assertFalse(
            shouldUseLightweightTopTabItemClickIndication(
                renderer = HomeTopTabRenderer.IOS,
                skinPlainStyle = false,
                usesCapsuleIndicator = true
            )
        )
        assertFalse(
            shouldUseLightweightTopTabItemClickIndication(
                renderer = HomeTopTabRenderer.MD3,
                skinPlainStyle = false,
                usesCapsuleIndicator = true
            )
        )
        assertFalse(
            shouldUseLightweightTopTabItemClickIndication(
                renderer = HomeTopTabRenderer.MIUIX,
                skinPlainStyle = false,
                usesCapsuleIndicator = true
            )
        )
    }

    @Test
    fun `plain md3 top tabs keep item click indication`() {
        assertTrue(
            shouldUseLightweightTopTabItemClickIndication(
                renderer = HomeTopTabRenderer.MD3,
                skinPlainStyle = false,
                usesCapsuleIndicator = false
            )
        )
        assertTrue(
            shouldUseLightweightTopTabItemClickIndication(
                renderer = HomeTopTabRenderer.IOS,
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
