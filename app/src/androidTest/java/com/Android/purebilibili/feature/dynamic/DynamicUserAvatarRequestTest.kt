package com.android.purebilibili.feature.dynamic

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import com.android.purebilibili.feature.dynamic.components.buildDynamicUserAvatarRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DynamicUserAvatarRequestTest {
    @Test
    fun displayAndPrefetchNormalizeToTheSameBoundedRequest() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val display = buildDynamicUserAvatarRequest(context, "//avatar/user.jpg", 144)
        val prefetch = buildDynamicUserAvatarRequest(context, "https://avatar/user.jpg", 144)
        assertEquals(display.data, prefetch.data)
        assertEquals(Size(144, 144), display.sizeResolver.size())
        assertEquals(display.sizeResolver.size(), prefetch.sizeResolver.size())
        assertEquals(Precision.EXACT, display.precision)
        assertEquals(Scale.FILL, display.scale)
        assertEquals(Size(256, 256), buildDynamicUserAvatarRequest(context, "https://avatar/user.jpg", 4096).sizeResolver.size())
    }
}
