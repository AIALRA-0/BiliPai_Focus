package com.android.purebilibili.feature.video.player

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaSession
import com.android.purebilibili.core.lifecycle.BackgroundManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class MiniPlayerManagerLeakCleanupTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `dismiss releases media session when playback is closed`() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.registerReceiver(any(), any(), any(), any()) } returns null

        val context = mockContext()
        val manager = newMiniPlayerManager(context)
        val mediaSession = mockk<MediaSession>(relaxed = true)
        manager.mediaSession = mediaSession

        try {
            manager.dismiss()

            verify(exactly = 1) { mediaSession.release() }
        } finally {
            manager.release()
            BackgroundManager.removeListener(manager)
        }
    }

    @Test
    fun `release unregisters receiver and background listener`() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.registerReceiver(any(), any(), any(), any()) } returns null

        val context = mockContext()
        val manager = newMiniPlayerManager(context)

        manager.release()

        verify(exactly = 1) { context.unregisterReceiver(any()) }
        assertFalse(backgroundListeners().contains(manager))
    }

    private fun newMiniPlayerManager(context: Context): MiniPlayerManager {
        val constructor = MiniPlayerManager::class.java.getDeclaredConstructor(Context::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(context)
    }

    private fun mockContext(): Context {
        val context = mockk<Context>(relaxed = true)
        val notificationManager = mockk<NotificationManager>(relaxed = true)
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
        every { context.startService(any()) } returns ComponentName("com.android.purebilibili", "PlaybackService")
        return context
    }

    @Suppress("UNCHECKED_CAST")
    private fun backgroundListeners(): MutableList<BackgroundManager.BackgroundStateListener> {
        val field = BackgroundManager::class.java.getDeclaredField("listeners")
        field.isAccessible = true
        return field.get(BackgroundManager) as MutableList<BackgroundManager.BackgroundStateListener>
    }
}
