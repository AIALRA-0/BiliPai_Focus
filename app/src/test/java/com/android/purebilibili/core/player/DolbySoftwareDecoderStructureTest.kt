package com.android.purebilibili.core.player

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DolbySoftwareDecoderStructureTest {

    @Test
    fun `all independent players enable platform first ffmpeg fallback`() {
        val playerSources = listOf(
            loadRepositoryFile(
                "app/src/main/java/com/android/purebilibili/feature/video/state/VideoPlayerState.kt"
            ),
            loadRepositoryFile(
                "app/src/main/java/com/android/purebilibili/feature/video/ui/pager/PortraitVideoPager.kt"
            ),
            loadRepositoryFile(
                "app/src/main/java/com/android/purebilibili/feature/bangumi/BangumiPlayerScreen.kt"
            ),
            loadRepositoryFile(
                "app/src/main/java/com/android/purebilibili/feature/video/player/MiniPlayerManager.kt"
            )
        )

        playerSources.forEach { source ->
            assertTrue(source.contains("EXTENSION_RENDERER_MODE_ON"))
        }
        assertFalse(playerSources.first().contains("EXTENSION_RENDERER_MODE_PREFER"))
    }

    @Test
    fun `app packages the arm64 eac3 decoder module`() {
        val settings = loadRepositoryFile("settings.gradle.kts")
        val appBuild = loadRepositoryFile("app/build.gradle.kts")
        val decoderReadme = loadRepositoryFile("dolby-ffmpeg-decoder/README.md")
        val nativeLibrary = locateRepositoryFile(
            "dolby-ffmpeg-decoder/src/main/jniLibs/arm64-v8a/libffmpegJNI.so"
        )

        assertTrue(settings.contains("include(\":dolby-ffmpeg-decoder\")"))
        assertTrue(appBuild.contains("implementation(project(\":dolby-ffmpeg-decoder\"))"))
        assertTrue(decoderReadme.contains("只显式启用 `eac3`"))
        assertTrue(nativeLibrary.length() > 0L)
    }

    @Test
    fun `software decoder capability changes dolby presentation only when platform decoder is absent`() {
        val mediaUtils = loadRepositoryFile(
            "app/src/main/java/com/android/purebilibili/core/util/MediaUtils.kt"
        )
        val audioPolicy = loadRepositoryFile(
            "app/src/main/java/com/android/purebilibili/feature/video/playback/audio/AudioStreamSelectionPolicy.kt"
        )

        assertTrue(mediaUtils.contains("FfmpegLibrary.supportsFormat(MimeTypes.AUDIO_E_AC3)"))
        assertTrue(
            mediaUtils.contains(
                "!isPlatformDolbyAudioDecoderSupported() && isDolbySoftwareAudioDecoderSupported()"
            )
        )
        assertTrue(audioPolicy.contains("\"杜比音频\" else \"杜比全景声\""))
    }

    private fun loadRepositoryFile(relativePath: String): String {
        return locateRepositoryFile(relativePath).readText()
    }

    private fun locateRepositoryFile(relativePath: String): File {
        val direct = File(relativePath)
        val fromModule = File("../$relativePath")
        return listOf(direct, fromModule)
            .firstOrNull { it.exists() }
            ?: error("Cannot locate $relativePath from ${File(".").absolutePath}")
    }
}
