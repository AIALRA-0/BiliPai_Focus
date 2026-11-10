package com.android.purebilibili.feature.video

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.feature.video.ambient.AmbientFrameController
import com.android.purebilibili.feature.video.ambient.AmbientFrameSource
import com.android.purebilibili.feature.video.ambient.AmbientPresentation
import com.android.purebilibili.feature.video.ambient.AmbientRunPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AmbientLegacyApiUiRegressionTest {
    @Test
    fun enablingGlowOnAndroid13ProducesCpuBlurAndReleasesSource() = runTest {
        assumeTrue(Build.VERSION.SDK_INT == 33)
        val presentation = AmbientPresentation()
        var released = false
        val source = object : AmbientFrameSource {
            override suspend fun capture(width: Int, height: Int): Bitmap =
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(Color.RED)
                }
            override fun release() { released = true }
        }
        val controller = AmbientFrameController(presentation)
        val job = launch {
            controller.run(source, 160, 90) {
                AmbientRunPolicy(
                    active = true, glow = true, playing = false,
                    saving = false, refreshGeneration = 0, diagnosticLogging = false,
                )
            }
        }
        try {
            // The analyzer runs on a real worker; its deadline must not use virtual time.
            val frame = withContext(Dispatchers.Default) {
                withTimeout(5_000) {
                    while (presentation.current == null) delay(10)
                    checkNotNull(presentation.current)
                }
            }
            val glow = frame.glow.asAndroidBitmap()
            assertEquals(frame.raw.width, glow.width)
            assertEquals(frame.raw.height, glow.height)
            assertEquals(Color.RED, glow.getPixel(glow.width / 2, glow.height / 2))
        } finally {
            job.cancelAndJoin()
        }
        assertTrue(released)
    }
}
