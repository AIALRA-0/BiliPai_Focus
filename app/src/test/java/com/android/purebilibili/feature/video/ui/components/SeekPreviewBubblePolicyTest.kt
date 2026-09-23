package com.android.purebilibili.feature.video.ui.components

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.android.purebilibili.data.model.response.VideoshotData
import kotlin.test.Test
import kotlin.test.assertEquals

class SeekPreviewBubblePolicyTest {

    @Test
    fun anchoredPreview_followsProgressAndClampsAtTrackEdges() {
        assertEquals(
            0,
            resolveSeekPreviewBubbleOffsetPx(
                placement = SeekPreviewBubblePlacement.Anchored,
                offsetX = 0f,
                containerWidth = 400f,
                bubbleWidthPx = 120f
            )
        )
        assertEquals(
            140,
            resolveSeekPreviewBubbleOffsetPx(
                placement = SeekPreviewBubblePlacement.Anchored,
                offsetX = 200f,
                containerWidth = 400f,
                bubbleWidthPx = 120f
            )
        )
        assertEquals(
            280,
            resolveSeekPreviewBubbleOffsetPx(
                placement = SeekPreviewBubblePlacement.Anchored,
                offsetX = 400f,
                containerWidth = 400f,
                bubbleWidthPx = 120f
            )
        )
    }

    @Test
    fun seekPreviewAnchor_quantizesToCurrentVideoshotFrameBoundary() {
        val videoshotData = VideoshotData(
            img_x_len = 2,
            img_y_len = 2,
            image = listOf("sprite-1"),
            index = listOf(0L, 1_000L, 2_000L, 3_000L)
        )

        assertEquals(
            2_000L,
            resolveSeekPreviewAnchorPositionMs(
                videoshotData = videoshotData,
                targetPositionMs = 2_850L,
                durationMs = 4_000L
            )
        )
    }

    @Test
    fun seekPreviewAnchor_keepsTargetPositionWhenVideoshotUnavailable() {
        assertEquals(
            2_850L,
            resolveSeekPreviewAnchorPositionMs(
                videoshotData = null,
                targetPositionMs = 2_850L,
                durationMs = 4_000L
            )
        )
    }

    @Test
    fun seekPreviewAnchor_estimatesFrameBoundaryWhenTimelineMissing() {
        val videoshotData = VideoshotData(
            img_x_len = 2,
            img_y_len = 2,
            image = listOf("sprite-1"),
            index = emptyList()
        )

        assertEquals(
            3_000L,
            resolveSeekPreviewAnchorPositionMs(
                videoshotData = videoshotData,
                targetPositionMs = 3_700L,
                durationMs = 4_000L
            )
        )
    }

    @Test
    fun seekPreviewDestinationRect_preservesLandscapeSourceAspectRatio() {
        assertEquals(
            SeekPreviewDestinationRect(
                offset = IntOffset(14, 0),
                size = IntSize(213, 120)
            ),
            resolveSeekPreviewDestinationRect(
                sourceWidthPx = 160,
                sourceHeightPx = 90,
                containerWidthPx = 240,
                containerHeightPx = 120
            )
        )
    }

    @Test
    fun seekPreviewDestinationRect_centersPortraitSourceWithoutStretching() {
        assertEquals(
            SeekPreviewDestinationRect(
                offset = IntOffset(64, 0),
                size = IntSize(60, 106)
            ),
            resolveSeekPreviewDestinationRect(
                sourceWidthPx = 90,
                sourceHeightPx = 160,
                containerWidthPx = 188,
                containerHeightPx = 106
            )
        )
    }

    @Test
    fun compactPortraitPreview_usesReadablePortraitFrame() {
        assertEquals(
            CompactSeekPreviewSize(widthDp = 120, heightDp = 213),
            resolveCompactSeekPreviewSize(
                sourceWidthPx = 160,
                sourceHeightPx = 90,
                screenWidthDp = 393,
                videoAspectRatio = 9f / 16f
            )
        )
    }

    @Test
    fun portraitVideoPreview_cropsLetterboxedSpriteCellToVideoRatio() {
        assertEquals(
            SeekPreviewSourceCrop(
                offsetX = 55,
                offsetY = 0,
                width = 51,
                height = 90
            ),
            resolveSeekPreviewSourceCrop(
                sourceWidthPx = 160,
                sourceHeightPx = 90,
                videoAspectRatio = 9f / 16f
            )
        )
    }

    @Test
    fun landscapeVideoPreview_keepsWholeSpriteCell() {
        assertEquals(
            SeekPreviewSourceCrop(
                offsetX = 0,
                offsetY = 0,
                width = 160,
                height = 90
            ),
            resolveSeekPreviewSourceCrop(
                sourceWidthPx = 160,
                sourceHeightPx = 90,
                videoAspectRatio = 16f / 9f
            )
        )
    }

    @Test
    fun compactLandscapePreview_keepsLandscapeRatio() {
        assertEquals(
            CompactSeekPreviewSize(widthDp = 144, heightDp = 81),
            resolveCompactSeekPreviewSize(
                sourceWidthPx = 160,
                sourceHeightPx = 90,
                screenWidthDp = 393
            )
        )
    }

    @Test
    fun portraitPlayerLandscapeContent_usesDetectedVideoAspectRatio() {
        assertEquals(
            CompactSeekPreviewSize(widthDp = 144, heightDp = 81),
            resolveCompactSeekPreviewSize(
                sourceWidthPx = 160,
                sourceHeightPx = 90,
                screenWidthDp = 393,
                videoAspectRatio = 16f / 9f
            )
        )
    }
}
