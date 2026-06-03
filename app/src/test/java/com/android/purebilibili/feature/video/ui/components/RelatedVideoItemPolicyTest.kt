package com.android.purebilibili.feature.video.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RelatedVideoItemPolicyTest {

    @Test
    fun `shared transition mode keeps related card scale stable`() {
        assertEquals(
            1f,
            resolveRelatedVideoCardPressScaleTarget(
                isPressed = true,
                transitionEnabled = true
            )
        )
    }

    @Test
    fun `normal mode also keeps related card scale stable`() {
        assertEquals(
            1f,
            resolveRelatedVideoCardPressScaleTarget(
                isPressed = true,
                transitionEnabled = false
            )
        )
        assertEquals(
            1f,
            resolveRelatedVideoCardPressScaleTarget(
                isPressed = false,
                transitionEnabled = false
            )
        )
    }

    @Test
    fun `cover crossfade is disabled in all modes for list stability`() {
        assertFalse(shouldEnableRelatedVideoCoverCrossfade(transitionEnabled = true))
        assertFalse(shouldEnableRelatedVideoCoverCrossfade(transitionEnabled = false))
    }

    @Test
    fun `metadata shared bounds stay disabled for related cards`() {
        assertFalse(shouldEnableRelatedVideoMetadataSharedBounds(transitionEnabled = true))
        assertFalse(shouldEnableRelatedVideoMetadataSharedBounds(transitionEnabled = false))
    }

    @Test
    fun `related cards default to video source route for detail to detail shared element`() {
        assertEquals("video", resolveRelatedVideoSharedElementSourceRoute(null))
        assertEquals("video", resolveRelatedVideoSharedElementSourceRoute(""))
        assertEquals("video", resolveRelatedVideoSharedElementSourceRoute("video?from=related"))
        assertEquals("home", resolveRelatedVideoSharedElementSourceRoute("home"))
    }

    @Test
    fun `related card shell shared bounds are owned by the whole card`() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/ui/components/RelatedVideoItem.kt")
            .readText()
        val surfaceBlock = source
            .substringAfter("Surface(")
            .substringBefore("val relatedCoverWidth")
        val coverBlock = source
            .substringAfter("// Video cover")
            .substringBefore("AsyncImage(")

        assertTrue(source.contains("val cardShellModifier = if (coverSharedEnabled)"))
        assertTrue(surfaceBlock.contains(".then(cardShellModifier)"))
        assertFalse(coverBlock.contains("videoCardShellSharedElementKey("))
    }

    @Test
    fun `related video cover uses shared CDN sizing policy`() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/ui/components/RelatedVideoItem.kt")
            .readText()

        assertTrue(source.contains("FormatUtils.resolveVideoCoverUrl(video.pic, useLowQuality = false)"))
        assertFalse(source.contains("FormatUtils.fixImageUrl(video.pic)"))
    }

    @Test
    fun `press haptic is disabled for related cards`() {
        assertFalse(
            shouldTriggerRelatedVideoPressHaptic(
                isPressed = true,
                transitionEnabled = true
            )
        )
        assertFalse(
            shouldTriggerRelatedVideoPressHaptic(
                isPressed = true,
                transitionEnabled = false
            )
        )
    }
}
