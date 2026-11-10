package com.android.purebilibili.feature.video.ui.section

import android.view.Surface
import androidx.media3.common.Player
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test

class VideoOutputRouterTest {
    @Test
    fun oldRouterClearsOnlyItsOwnSurfaceAfterOutputWasTransferred() {
        val player = mockk<Player>(relaxed = true)
        val oldSurface = mockk<Surface>()
        val newSurface = mockk<Surface>()
        val oldRouter = VideoOutputRouter(player)
        val newRouter = VideoOutputRouter(player)
        oldRouter.update(null, oldSurface, false, true)
        newRouter.update(null, newSurface, false, true)

        oldRouter.release()

        verify(exactly = 1) { player.clearVideoSurface(oldSurface) }
        verify(exactly = 0) { player.clearVideoSurface() }
        verify(exactly = 0) { player.clearVideoSurface(newSurface) }
    }

    @Test
    fun releasedRouterCannotRebindFromLateViewCallbacks() {
        val player = mockk<Player>(relaxed = true)
        val oldSurface = mockk<Surface>()
        val lateSurface = mockk<Surface>()
        val router = VideoOutputRouter(player)
        router.update(null, oldSurface, false, true)

        router.release()
        router.update(null, lateSurface, false, true)
        router.release()

        verify(exactly = 1) { player.clearVideoSurface(oldSurface) }
        verify(exactly = 0) { player.setVideoSurface(lateSurface) }
    }
}
