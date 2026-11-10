package com.android.purebilibili.feature.anime4k.gl

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import com.android.purebilibili.plugin.sdk.Anime4KConfig
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Anime4KDetachedInputReleaseTest {
    @AfterTest
    fun tearDown() = unmockkAll()

    @Test
    fun skippedGlCleanupStillReleasesNativeInputExactlyOnceWithoutGles() {
        mockkStatic(Matrix::class)
        every { Matrix.setIdentityM(any(), any()) } just Runs
        mockkStatic(GLES20::class)
        every { GLES20.glDeleteTextures(any(), any<IntArray>(), any()) } just Runs
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        val notifications = mutableListOf<android.view.Surface?>()
        val renderer = Anime4KPipelineRenderer(
            context, Anime4KConfig(), {}, notifications::add, {}, {},
        )
        val input = mockk<Anime4KInputSurface>(relaxed = true)
        val inputField = Anime4KPipelineRenderer::class.java.getDeclaredField("inputSurface")
            .apply { isAccessible = true }
        val textureField = Anime4KPipelineRenderer::class.java.getDeclaredField("externalTextureId")
            .apply { isAccessible = true }
        inputField.set(renderer, input)
        textureField.setInt(renderer, 42)

        renderer.releaseInputAfterThreadExit()
        renderer.releaseInputAfterThreadExit()

        verify(exactly = 1) { input.release() }
        verify(exactly = 0) { GLES20.glDeleteTextures(any(), any<IntArray>(), any()) }
        assertNull(inputField.get(renderer))
        assertEquals(0, textureField.getInt(renderer))
        assertEquals(listOf<android.view.Surface?>(null), notifications)
    }
}
