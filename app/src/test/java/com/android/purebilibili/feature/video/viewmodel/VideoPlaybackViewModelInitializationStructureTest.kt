package com.android.purebilibili.feature.video.viewmodel

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoPlaybackViewModelInitializationStructureTest {

    @Test
    fun `view model startup preferences do not depend on deferred plugin initialization`() {
        val source = loadSource()

        assertTrue(source.contains("class VideoPlaybackViewModel(application: Application) : AndroidViewModel(application)"))
        assertTrue(source.contains("getPlaybackCdnPreference(application.applicationContext)"))
        assertFalse(source.contains("getPlaybackCdnPreference(PluginManager.getContext())"))
    }

    private fun loadSource(): String = listOf(
        File("app/src/main/java/com/android/purebilibili/feature/video/viewmodel/VideoPlaybackViewModel.kt"),
        File("src/main/java/com/android/purebilibili/feature/video/viewmodel/VideoPlaybackViewModel.kt"),
    ).first { it.exists() }.readText()
}
