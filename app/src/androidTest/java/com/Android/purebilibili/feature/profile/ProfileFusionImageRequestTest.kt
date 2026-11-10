package com.android.purebilibili.feature.profile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ProfileFusionImageRequestTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun warmHardwareCacheIsRejectedAndSoftwareResultCanBeDrawnAndReused() = runBlocking {
        val source = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.BLUE)
        val hardware = requireNotNull(source.copy(Bitmap.Config.HARDWARE, false))
        val file = File.createTempFile("fusion-image", ".png", context.cacheDir)
        val loader = ImageLoader.Builder(context)
            .memoryCache { MemoryCache.Builder().maxSizeBytes(1024 * 1024).build() }
            .build()
        try {
            file.outputStream().use { source.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val key = MemoryCache.Key("shared-preview-key")
            requireNotNull(loader.memoryCache)[key] = MemoryCache.Value(hardware.asImage())
            val request = ImageRequest.Builder(context)
                .data(file)
                .size(32, 32)
                .memoryCacheKey(key.key)
                .allowHardware(true)
                .buildForProfileFusion()

            val first = loader.execute(request) as SuccessResult
            val decoded = (first.image as BitmapImage).bitmap
            assertNotSame(hardware, decoded)
            assertFalse(decoded.config == Bitmap.Config.HARDWARE)
            val target = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            try {
                Canvas(target).drawBitmap(decoded, 0f, 0f, null)
                assertEquals(Color.BLUE, target.getPixel(16, 16))
            } finally {
                target.recycle()
            }

            val second = loader.execute(request) as SuccessResult
            assertEquals(DataSource.MEMORY_CACHE, second.dataSource)
            assertSame(first.image, second.image)
            assertEquals(key, second.memoryCacheKey)
        } finally {
            loader.shutdown()
            file.delete()
            hardware.recycle()
            source.recycle()
        }
    }
}
