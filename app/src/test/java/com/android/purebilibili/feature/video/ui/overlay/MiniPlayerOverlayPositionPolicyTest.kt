package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals

class MiniPlayerOverlayPositionPolicyTest {

    @Test
    fun initialOffset_isClampedInsideVisibleBounds() {
        val result = clampMiniPlayerOverlayOffset(
            offsetX = 980f,
            offsetY = 40f,
            screenWidthPx = 1080f,
            screenHeightPx = 2400f,
            miniPlayerWidthPx = 420f,
            miniPlayerHeightPx = 240f,
            outerPaddingPx = 24f,
            topInsetPx = 50f,
            bottomInsetPx = 100f
        )

        assertEquals(636f, result.x, 0.001f)
        assertEquals(74f, result.y, 0.001f)
    }

    @Test
    fun initialOffset_usesCurrentMiniPlayerSizeWhenUserPreviouslyResized() {
        val result = resolveMiniPlayerInitialOverlayOffset(
            cardLeftPx = 760f,
            entryFromLeft = false,
            screenWidthPx = 1080f,
            screenHeightPx = 2400f,
            miniPlayerWidthPx = 640f,
            miniPlayerHeightPx = 360f,
            outerPaddingPx = 24f,
            topInsetPx = 50f,
            bottomInsetPx = 100f
        )

        assertEquals(416f, result.x, 0.001f)
        assertEquals(1916f, result.y, 0.001f)
    }

    @Test
    fun resizeFromRightEdge_keepsOffsetInsideBoundsForNewWidth() {
        val result = resolveMiniPlayerOffsetAfterSizeChanged(
            offsetX = 636f,
            offsetY = 320f,
            screenWidthPx = 1080f,
            screenHeightPx = 2400f,
            miniPlayerWidthPx = 640f,
            miniPlayerHeightPx = 360f,
            outerPaddingPx = 24f,
            topInsetPx = 50f,
            bottomInsetPx = 100f
        )

        assertEquals(416f, result.x, 0.001f)
        assertEquals(320f, result.y, 0.001f)
    }

    @Test
    fun offsetClamp_preservesInBoundsPosition() {
        val result = clampMiniPlayerOverlayOffset(
            offsetX = 120f,
            offsetY = 320f,
            screenWidthPx = 1080f,
            screenHeightPx = 2400f,
            miniPlayerWidthPx = 420f,
            miniPlayerHeightPx = 240f,
            outerPaddingPx = 24f,
            topInsetPx = 50f,
            bottomInsetPx = 100f
        )

        assertEquals(120f, result.x, 0.001f)
        assertEquals(320f, result.y, 0.001f)
    }

    @Test
    fun dockedBottomOffset_keepsMiniPlayerAboveHomeBottomBarArea() {
        assertEquals(
            2030f,
            resolveMiniPlayerDockedBottomOffsetY(
                screenHeightPx = 2400f,
                miniPlayerHeightPx = 240f,
                outerPaddingPx = 24f,
                bottomInsetPx = 106f
            ),
            0.001f
        )
    }

    @Test
    fun dragIntent_prefersSeekOnlyForClearlyHorizontalDrags() {
        assertEquals(
            MiniPlayerContentDragIntent.SEEK,
            resolveMiniPlayerContentDragIntent(
                totalDragX = 48f,
                totalDragY = 8f,
                seekEnabled = true,
                touchSlopPx = 12f
            )
        )
        assertEquals(
            MiniPlayerContentDragIntent.MOVE,
            resolveMiniPlayerContentDragIntent(
                totalDragX = 26f,
                totalDragY = 22f,
                seekEnabled = true,
                touchSlopPx = 12f
            )
        )
    }

    @Test
    fun dragIntent_defaultsToMoveWhenSeekUnavailable() {
        assertEquals(
            MiniPlayerContentDragIntent.MOVE,
            resolveMiniPlayerContentDragIntent(
                totalDragX = 40f,
                totalDragY = 2f,
                seekEnabled = false,
                touchSlopPx = 12f
            )
        )
    }

    @Test
    fun dragIntent_waitsUntilMotionPassesTouchSlop() {
        assertEquals(
            MiniPlayerContentDragIntent.UNDECIDED,
            resolveMiniPlayerContentDragIntent(
                totalDragX = 6f,
                totalDragY = 5f,
                seekEnabled = true,
                touchSlopPx = 12f
            )
        )
    }

    @Test
    fun seekTarget_usesStableDragStartPosition() {
        assertEquals(
            51_000L,
            resolveMiniPlayerSeekTargetPosition(
                dragStartPositionMs = 30_000L,
                dragDeltaPx = 120f,
                miniPlayerWidthPx = 400f,
                durationMs = 70_000L
            )
        )
    }

    @Test
    fun seekTarget_clampsToDuration() {
        assertEquals(
            70_000L,
            resolveMiniPlayerSeekTargetPosition(
                dragStartPositionMs = 65_000L,
                dragDeltaPx = 300f,
                miniPlayerWidthPx = 400f,
                durationMs = 70_000L
            )
        )
        assertEquals(
            0L,
            resolveMiniPlayerSeekTargetPosition(
                dragStartPositionMs = 5_000L,
                dragDeltaPx = -300f,
                miniPlayerWidthPx = 400f,
                durationMs = 70_000L
            )
        )
    }

    @Test
    fun seekTarget_ignoresInvalidWidthOrDuration() {
        assertEquals(
            30_000L,
            resolveMiniPlayerSeekTargetPosition(
                dragStartPositionMs = 30_000L,
                dragDeltaPx = 120f,
                miniPlayerWidthPx = 0f,
                durationMs = 70_000L
            )
        )
        assertEquals(
            0L,
            resolveMiniPlayerSeekTargetPosition(
                dragStartPositionMs = 30_000L,
                dragDeltaPx = 120f,
                miniPlayerWidthPx = 400f,
                durationMs = 0L
            )
        )
    }

    @Test
    fun resizeBounds_keepMiniPlayerVisibleAndAllowUsefulScaling() {
        val bounds = resolveMiniPlayerResizeBounds(
            defaultWidthDp = 220,
            defaultHeightDp = 130,
            screenWidthDp = 393,
            screenHeightDp = 852,
            outerPaddingDp = 12,
            topInsetDp = 50,
            bottomInsetDp = 100
        )

        assertEquals(168f, bounds.minWidthDp, 0.001f)
        assertEquals(369f, bounds.maxWidthDp, 0.001f)
    }

    @Test
    fun resizeDrag_projectsBothAxesAndClampsWidth() {
        assertEquals(
            300.76923f,
            resolveResizedMiniPlayerWidth(
                currentWidthPx = 220f,
                dragDeltaX = 60f,
                dragDeltaY = 60f,
                aspectRatio = 220f / 130f,
                minWidthPx = 168f,
                maxWidthPx = 360f
            ),
            0.001f
        )
        assertEquals(
            168f,
            resolveResizedMiniPlayerWidth(
                currentWidthPx = 220f,
                dragDeltaX = -200f,
                dragDeltaY = -200f,
                aspectRatio = 220f / 130f,
                minWidthPx = 168f,
                maxWidthPx = 360f
            ),
            0.001f
        )
    }
}
