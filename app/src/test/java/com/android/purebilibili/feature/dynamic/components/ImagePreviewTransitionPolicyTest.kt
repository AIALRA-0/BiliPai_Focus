package com.android.purebilibili.feature.dynamic.components

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImagePreviewTransitionPolicyTest {

    @Test
    fun `live photo overlay controls retain their original corner radii`() {
        assertEquals(16, IMAGE_PREVIEW_LIVE_PHOTO_CONTROL_CORNER_RADIUS_DP)
        assertEquals(14, IMAGE_PREVIEW_LIVE_PHOTO_MENU_CORNER_RADIUS_DP)
    }

    @Test
    fun shouldHandleImagePreviewLongPressSave_respectsUserSwitchAndBusyState() {
        assertTrue(
            shouldHandleImagePreviewLongPressSave(
                longPressSaveEnabled = true,
                imageUrl = "https://i0.hdslb.com/test.jpg",
                isSaving = false
            )
        )
        assertFalse(
            shouldHandleImagePreviewLongPressSave(
                longPressSaveEnabled = false,
                imageUrl = "https://i0.hdslb.com/test.jpg",
                isSaving = false
            )
        )
        assertFalse(
            shouldHandleImagePreviewLongPressSave(
                longPressSaveEnabled = true,
                imageUrl = "",
                isSaving = false
            )
        )
        assertFalse(
            shouldHandleImagePreviewLongPressSave(
                longPressSaveEnabled = true,
                imageUrl = "https://i0.hdslb.com/test.jpg",
                isSaving = true
            )
        )
    }

    @Test
    fun resolveImagePreviewTransitionFrame_clampsProgressToClosedState() {
        val frame = resolveImagePreviewTransitionFrame(
            rawProgress = -0.2f,
            hasSourceRect = true,
            sourceCornerRadiusDp = 12f
        )

        assertEquals(0f, frame.layoutProgress)
        assertEquals(0f, frame.visualProgress)
        assertEquals(12f, frame.cornerRadiusDp)
    }

    @Test
    fun resolveImagePreviewTransitionFrame_interpolatesCornerRadiusTowardSourceOnReturn() {
        val open = resolveImagePreviewTransitionFrame(
            rawProgress = 1f,
            hasSourceRect = true,
            sourceCornerRadiusDp = 12f
        )
        val middle = resolveImagePreviewTransitionFrame(
            rawProgress = 0.5f,
            hasSourceRect = true,
            sourceCornerRadiusDp = 12f
        )
        val closed = resolveImagePreviewTransitionFrame(
            rawProgress = 0f,
            hasSourceRect = true,
            sourceCornerRadiusDp = 12f
        )

        assertEquals(0f, open.cornerRadiusDp, 0.0001f)
        assertEquals(6f, middle.cornerRadiusDp, 0.0001f)
        assertEquals(12f, closed.cornerRadiusDp, 0.0001f)
    }

    @Test
    fun resolveImagePreviewPresentedCornerRadius_lerpsCircularSourceWithoutOvershoot() {
        // 圆形源（头像）：半径等于短边一半，回位过程中圆角始终被钳在 [0, source] 区间
        val circleRadius = 40f
        for (progress in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
            val corner = resolveImagePreviewPresentedCornerRadiusDp(
                visualProgress = progress,
                verticalDragProgress = 0f,
                hasSourceRect = true,
                sourceCornerRadiusDp = circleRadius,
            )
            assertTrue(corner >= 0f && corner <= circleRadius, "corner out of range at $progress: $corner")
        }
        assertEquals(0f, resolveImagePreviewPresentedCornerRadiusDp(
            visualProgress = 1f,
            verticalDragProgress = 0f,
            hasSourceRect = true,
            sourceCornerRadiusDp = circleRadius,
        ))
        assertEquals(circleRadius, resolveImagePreviewPresentedCornerRadiusDp(
            visualProgress = 0f,
            verticalDragProgress = 0f,
            hasSourceRect = true,
            sourceCornerRadiusDp = circleRadius,
        ))
    }

    @Test
    fun resolveImagePreviewPresentedCornerRadius_verticalDragNeverGoesBelowMorphCorner() {
        val corner = resolveImagePreviewPresentedCornerRadiusDp(
            visualProgress = 1f,
            verticalDragProgress = 0.6f,
            hasSourceRect = true,
            sourceCornerRadiusDp = 10f,
        )
        assertTrue(corner > 0f, "dragging back from fullscreen should reveal source corner")
    }

    @Test
    fun resolveImagePreviewTransitionFrame_usesZeroCornerWhenNoSourceRect() {
        val frame = resolveImagePreviewTransitionFrame(
            rawProgress = 0.5f,
            hasSourceRect = false,
            sourceCornerRadiusDp = 12f
        )

        assertEquals(0f, frame.cornerRadiusDp)
    }

    @Test
    fun imagePreviewDismissMotion_returnsSingleShotSettleTargets() {
        val motion = imagePreviewDismissMotion()

        assertEquals(0f, motion.overshootTarget)
        assertEquals(0f, motion.settleTarget)
        assertEquals(180, motion.cancelRecoverDurationMillis)
        assertEquals(260, motion.openDurationMillis)
        assertEquals(motion.overshootTarget, motion.settleTarget)
    }

    @Test
    fun resolveImagePreviewDismissTransform_returnsIdentityWithoutSourceRect() {
        val transform = resolveImagePreviewDismissTransform(
            transitionProgress = 0.3f,
            sourceRect = null,
            displayedImageRect = null
        )

        assertEquals(1f, transform.scale)
        assertEquals(0f, transform.translationXPx)
        assertEquals(0f, transform.translationYPx)
    }

    @Test
    fun resolveImagePreviewDismissTransform_usesUniformScaleAndCenterTranslation() {
        val source = Rect(
            left = 100f,
            top = 400f,
            right = 300f,
            bottom = 500f
        )
        val start = resolveImagePreviewDismissTransform(
            transitionProgress = 1f,
            sourceRect = source,
            displayedImageRect = Rect(
                left = 0f,
                top = 660f,
                right = 1080f,
                bottom = 1260f
            )
        )
        val middle = resolveImagePreviewDismissTransform(
            transitionProgress = 0.5f,
            sourceRect = source,
            displayedImageRect = Rect(
                left = 0f,
                top = 660f,
                right = 1080f,
                bottom = 1260f
            )
        )
        val end = resolveImagePreviewDismissTransform(
            transitionProgress = 0f,
            sourceRect = source,
            displayedImageRect = Rect(
                left = 0f,
                top = 660f,
                right = 1080f,
                bottom = 1260f
            )
        )

        assertEquals(1f, start.scale, 0.0001f)
        assertEquals(0f, start.translationXPx, 0.0001f)
        assertEquals(0f, start.translationYPx, 0.0001f)

        // 线性 morph：中点约在起终点几何中点。
        assertTrue(middle.scale in 0.55f..0.65f)
        assertTrue(middle.translationXPx < 0f && middle.translationXPx > -340f)
        assertTrue(middle.translationYPx < 0f && middle.translationYPx > -535f)

        assertEquals(0.16666669f, end.scale, 0.0001f)
        assertEquals(-340f, end.translationXPx, 0.0001f)
        assertEquals(-510f, end.translationYPx, 0.0001f)
    }

    @Test
    fun resolveImagePreviewDismissRectFrame_startsFromDisplayedRect() {
        val displayed = Rect(
            left = 180f,
            top = 220f,
            right = 900f,
            bottom = 1500f
        )
        val source = Rect(
            left = 40f,
            top = 80f,
            right = 280f,
            bottom = 440f
        )

        val frame = resolveImagePreviewDismissRectFrame(
            transitionProgress = 1f,
            sourceRect = source,
            displayedImageRect = displayed
        ) ?: error("Expected start frame")

        assertEquals(displayed.left, frame.rect.left, 0.0001f)
        assertEquals(displayed.top, frame.rect.top, 0.0001f)
        assertEquals(displayed.right, frame.rect.right, 0.0001f)
        assertEquals(displayed.bottom, frame.rect.bottom, 0.0001f)
    }

    @Test
    fun resolveImagePreviewDismissRectFrame_endsAtSourceRect() {
        val displayed = Rect(
            left = 180f,
            top = 220f,
            right = 900f,
            bottom = 1500f
        )
        val source = Rect(
            left = 40f,
            top = 80f,
            right = 280f,
            bottom = 440f
        )

        val frame = resolveImagePreviewDismissRectFrame(
            transitionProgress = 0f,
            sourceRect = source,
            displayedImageRect = displayed
        ) ?: error("Expected end frame")

        assertEquals(source.left, frame.rect.left, 0.0001f)
        assertEquals(source.top, frame.rect.top, 0.0001f)
        assertEquals(source.right, frame.rect.right, 0.0001f)
        assertEquals(source.bottom, frame.rect.bottom, 0.0001f)
        assertEquals(source.width, frame.rect.width, 0.0001f)
        assertEquals(source.height, frame.rect.height, 0.0001f)
    }

    @Test
    fun resolveImagePreviewDismissRectFrame_clampsBelowZeroToSource() {
        val displayed = Rect(
            left = 0f,
            top = 0f,
            right = 1080f,
            bottom = 2400f
        )
        val source = Rect(
            left = 100f,
            top = 400f,
            right = 420f,
            bottom = 720f
        )

        val clamped = resolveImagePreviewDismissRectFrame(
            transitionProgress = -0.06f,
            sourceRect = source,
            displayedImageRect = displayed
        ) ?: error("Expected clamped frame")

        assertEquals(source.width, clamped.rect.width, 0.0001f)
        assertEquals(source.height, clamped.rect.height, 0.0001f)
        assertEquals(source.left, clamped.rect.left, 0.0001f)
        assertEquals(source.top, clamped.rect.top, 0.0001f)
    }

    @Test
    fun resolveImagePreviewDismissRectFrame_interpolatesWidthHeightAndCenter() {
        val displayed = Rect(
            left = 180f,
            top = 220f,
            right = 900f,
            bottom = 1500f
        )
        val source = Rect(
            left = 40f,
            top = 80f,
            right = 280f,
            bottom = 440f
        )

        val frame = resolveImagePreviewDismissRectFrame(
            transitionProgress = 0.5f,
            sourceRect = source,
            displayedImageRect = displayed
        )

        val rect = frame?.rect ?: error("Expected rect frame")
        assertTrue(rect.width < displayed.width && rect.width > source.width)
        assertTrue(rect.height < displayed.height && rect.height > source.height)
        val displayedCenterX = (displayed.left + displayed.right) / 2f
        val sourceCenterX = (source.left + source.right) / 2f
        val currentCenterX = (rect.left + rect.right) / 2f
        assertTrue(currentCenterX < displayedCenterX && currentCenterX > sourceCenterX)
    }

    @Test
    fun resolveImagePreviewDismissStartRect_prefersPreviewSurfaceForChromeDismiss() {
        val previewSurface = Rect(
            left = 0f,
            top = 0f,
            right = 1080f,
            bottom = 2400f
        )
        val displayed = Rect(
            left = 120f,
            top = 320f,
            right = 960f,
            bottom = 1760f
        )

        val startRect = resolveImagePreviewDismissStartRect(
            previewSurfaceRect = previewSurface,
            displayedImageRect = displayed,
            preferPreviewSurface = true
        )

        assertEquals(previewSurface, startRect)
    }

    @Test
    fun resolveImagePreviewDismissStartRect_prefersDisplayedImageForSmoothDefaultDismiss() {
        val previewSurface = Rect(
            left = 0f,
            top = 0f,
            right = 1080f,
            bottom = 2400f
        )
        val displayed = Rect(
            left = 120f,
            top = 320f,
            right = 960f,
            bottom = 1760f
        )

        val startRect = resolveImagePreviewDismissStartRect(
            previewSurfaceRect = previewSurface,
            displayedImageRect = displayed,
            preferPreviewSurface = false
        )

        assertEquals(displayed, startRect)
    }

    @Test
    fun resolveImagePreviewDismissStartRect_fallsBackToDisplayedImageWhenPreviewSurfaceMissing() {
        val displayed = Rect(
            left = 120f,
            top = 320f,
            right = 960f,
            bottom = 1760f
        )

        val startRect = resolveImagePreviewDismissStartRect(
            previewSurfaceRect = null,
            displayedImageRect = displayed,
            preferPreviewSurface = true
        )

        assertEquals(displayed, startRect)
    }

    @Test
    fun resolveImagePreviewDraggedDisplayRect_appliesTranslationAndScaleAroundCenter() {
        val displayed = Rect(
            left = 180f,
            top = 220f,
            right = 900f,
            bottom = 1500f
        )

        val dragged = resolveImagePreviewDraggedDisplayRect(
            displayedImageRect = displayed,
            translationYPx = 240f,
            scale = 0.9f
        ) ?: error("Expected dragged rect")

        assertEquals(displayed.width * 0.9f, dragged.width, 0.0001f)
        assertEquals(displayed.height * 0.9f, dragged.height, 0.0001f)
        val draggedCenterY = (dragged.top + dragged.bottom) / 2f
        val displayedCenterY = (displayed.top + displayed.bottom) / 2f
        assertEquals(displayedCenterY + 240f, draggedCenterY, 0.0001f)
    }

    @Test
    fun resolveImagePreviewDismissBackdropAlpha_fadesLinearlyWithMorph() {
        val start = resolveImagePreviewDismissBackdropAlpha(1f)
        val middle = resolveImagePreviewDismissBackdropAlpha(0.5f)
        val end = resolveImagePreviewDismissBackdropAlpha(0f)

        assertEquals(1f, start, 0.0001f)
        assertEquals(0.5f, middle, 0.0001f)
        assertEquals(0f, end, 0.0001f)
    }

    @Test
    fun resolveImagePreviewDismissContentAlpha_fadesOnlyOnFallbackDismiss() {
        // Rect 飞行路径图片全程不透明，自己飞回缩略图。
        assertEquals(1f, resolveImagePreviewDismissContentAlpha(hasRectFlight = true, isDismissing = true, visualProgress = 0f))
        assertEquals(1f, resolveImagePreviewDismissContentAlpha(hasRectFlight = true, isDismissing = true, visualProgress = 0.4f))
        // 非 dismiss 状态（打开中）不淡出。
        assertEquals(1f, resolveImagePreviewDismissContentAlpha(hasRectFlight = false, isDismissing = false, visualProgress = 0.4f))
        // fallback 关闭随进度同步淡出，避免窗口移除瞬间闪切。
        assertEquals(0.4f, resolveImagePreviewDismissContentAlpha(hasRectFlight = false, isDismissing = true, visualProgress = 0.4f), 0.0001f)
        assertEquals(0f, resolveImagePreviewDismissContentAlpha(hasRectFlight = false, isDismissing = true, visualProgress = -0.5f), 0.0001f)
    }

    @Test
    fun resolveImagePreviewChromeAlpha_fadesEarlierThanImageDuringDismiss() {
        assertEquals(1f, resolveImagePreviewChromeAlpha(visualProgress = 1f, isDismissing = false))
        assertEquals(0.5f, resolveImagePreviewChromeAlpha(visualProgress = 0.5f, isDismissing = false))

        // dismiss 时 0.05 以下 chrome 已清零，0.6 之后清零完成，与背景/图片节奏错位更小。
        assertEquals(0f, resolveImagePreviewChromeAlpha(visualProgress = 0.05f, isDismissing = true))
        assertEquals(1f, resolveImagePreviewChromeAlpha(visualProgress = 0.6f, isDismissing = true))
        assertEquals(1f, resolveImagePreviewChromeAlpha(visualProgress = 1f, isDismissing = true))
    }

    @Test
    fun resolveImagePreviewCounterScaledCornerRadii_keepsScreenCornerCircular() {
        // 非均匀缩放（x 压缩为 0.5）下，横向半径需反除以更大的比例分量。
        val radii = resolveImagePreviewCounterScaledCornerRadii(
            cornerRadiusDp = 12f,
            scaleX = 0.5f,
            scaleY = 0.1f
        )
        assertEquals(24f, radii.horizontalDp, 0.0001f)
        assertEquals(120f, radii.verticalDp, 0.0001f)
        // 非法缩放钳制到 0.01，半径带 64 倍上限防御极端比例。
        val guarded = resolveImagePreviewCounterScaledCornerRadii(
            cornerRadiusDp = 8f,
            scaleX = 0f,
            scaleY = -1f
        )
        assertEquals(512f, guarded.horizontalDp, 0.0001f)
        assertEquals(512f, guarded.verticalDp, 0.0001f)
    }

    @Test
    fun resolveImagePreviewGallery3DBlend_entersSmoothlyAfterThreshold() {
        assertEquals(0f, resolveImagePreviewGallery3DBlend(0f))
        assertEquals(0f, resolveImagePreviewGallery3DBlend(0.85f))
        assertEquals(0.5f, resolveImagePreviewGallery3DBlend(0.925f), 0.0001f)
        assertEquals(1f, resolveImagePreviewGallery3DBlend(1f))
        assertEquals(1f, resolveImagePreviewGallery3DBlend(1.5f))
    }

    @Test
    fun resolveImagePreviewLivePhotoAlpha_fadesInAcrossTail() {
        assertEquals(0f, resolveImagePreviewLivePhotoAlpha(0f))
        assertEquals(0f, resolveImagePreviewLivePhotoAlpha(0.7f))
        assertEquals(0.5f, resolveImagePreviewLivePhotoAlpha(0.85f), 0.0001f)
        assertEquals(1f, resolveImagePreviewLivePhotoAlpha(1f))
    }

    @Test
    fun clampImagePreviewDismissVelocity_limitsExtremeFlings() {
        assertEquals(0f, clampImagePreviewDismissVelocity(0f))
        assertEquals(3000f, clampImagePreviewDismissVelocity(9000f), 0.0001f)
        assertEquals(-3000f, clampImagePreviewDismissVelocity(-9000f), 0.0001f)
    }

    @Test
    fun dismissVelocity_convertsPixelsToProgressAndRespectsReturnDirection() {
        val start = Rect(0f, 0f, 200f, 200f)
        val below = Rect(0f, 1000f, 200f, 1200f)
        val above = Rect(0f, -1000f, 200f, -800f)
        assertEquals(-1f, resolveImagePreviewDismissProgressVelocity(1000f, start, below, 2000f), 0.0001f)
        assertEquals(-1f, resolveImagePreviewDismissProgressVelocity(-1000f, start, above, 2000f), 0.0001f)
        assertEquals(0f, resolveImagePreviewDismissProgressVelocity(1000f, start, above, 2000f))
        // A diagonal return only carries the part of the vertical velocity along its path.
        val diagonal = below.translate(androidx.compose.ui.geometry.Offset(1000f, 0f))
        assertEquals(-0.5f, resolveImagePreviewDismissProgressVelocity(1000f, start, diagonal, 2000f), 0.0001f)
    }

    @Test
    fun dismissVelocity_handlesMissingAndCoincidentAnchorsWithoutOvershoot() {
        val rect = Rect(0f, 0f, 200f, 200f)
        assertEquals(0f, resolveImagePreviewDismissProgressVelocity(3000f, rect, rect, 2000f))
        assertEquals(-0.75f, resolveImagePreviewDismissProgressVelocity(-3000f, rect, null, 2000f, 0.5f))
        assertEquals(-6f, resolveImagePreviewDismissProgressVelocity(3000f, null, null, 0f, 0.5f))
    }

    @Test
    fun dismissCorners_startAtDraggedCornerAndLandAtThumbnailCorner() {
        assertEquals(7f, resolveImagePreviewDismissCornerRadiusDp(1f, 7f, 12f))
        assertEquals(9.5f, resolveImagePreviewDismissCornerRadiusDp(0.5f, 7f, 12f))
        assertEquals(12f, resolveImagePreviewDismissCornerRadiusDp(0f, 7f, 12f))
        assertEquals(7f, resolveImagePreviewDismissCornerRadiusDp(2f, 7f, 12f))
    }

    @Test
    fun resolveImagePreviewBlurRadiusPx_growsLinearlyWithReturnProgress() {
        assertEquals(0f, resolveImagePreviewBlurRadiusPx(visualProgress = 1f, maxBlurRadiusPx = 20f))
        assertEquals(10f, resolveImagePreviewBlurRadiusPx(visualProgress = 0.5f, maxBlurRadiusPx = 20f), 0.0001f)
        assertEquals(20f, resolveImagePreviewBlurRadiusPx(visualProgress = 0f, maxBlurRadiusPx = 20f), 0.0001f)
    }

    @Test
    fun resolveImagePreviewText_usesPerImageCaptionWhenAvailable() {
        val resolved = resolveImagePreviewText(
            textContent = ImagePreviewTextContent(
                headline = "作者A",
                body = "这是正文",
                perImageCaptions = listOf("图1文案", "图2文案")
            ),
            currentPage = 1,
            totalPages = 3
        )

        assertEquals("作者A", resolved?.headline)
        assertEquals("图2文案", resolved?.body)
        assertEquals("2 / 3", resolved?.pageIndicator)
    }

    @Test
    fun resolveImagePreviewText_returnsNullWhenEverythingIsBlank() {
        val resolved = resolveImagePreviewText(
            textContent = ImagePreviewTextContent(),
            currentPage = 0,
            totalPages = 1
        )

        assertNull(resolved)
    }

    @Test
    fun resolveImagePreviewTextTransform_adds3dFeelDuringPagerSwipe() {
        val centered = resolveImagePreviewTextTransform(0f)
        val swiping = resolveImagePreviewTextTransform(0.55f)

        assertEquals(0f, centered.rotationX, 0.0001f)
        assertEquals(1f, centered.alpha, 0.0001f)
        assertTrue(swiping.rotationX > 10f)
        assertTrue(swiping.alpha < 1f)
        assertTrue(swiping.translateYDp > 0f)
    }
}
