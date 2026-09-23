package com.android.purebilibili.core.util

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GalleryVisualMediaContractsStructureTest {

    @Test
    fun `gallery contract prefers media providers and keeps photo picker fallback`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/core/util/GalleryVisualMediaContracts.kt"
        )

        assertTrue(source.contains("Intent(Intent.ACTION_GET_CONTENT)"))
        assertTrue(source.contains("Intent.EXTRA_ALLOW_MULTIPLE"))
        assertTrue(source.contains("Intent.FLAG_GRANT_READ_URI_PERMISSION"))
        assertTrue(source.contains("ActivityResultContracts.PickVisualMedia()"))
        assertTrue(source.contains("ActivityResultContracts.PickMultipleVisualMedia(maxItems)"))
        assertTrue(source.contains("selectedUris.take(maxItems)"))
    }

    @Test
    fun `all visual media entry points use shared gallery contracts`() {
        val singlePickerFiles = listOf(
            "app/src/main/java/com/android/purebilibili/feature/message/ChatScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/profile/ProfileScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/profile/SplashWallpaperPickerSheet.kt",
            "app/src/main/java/com/android/purebilibili/feature/settings/LiquidGlassLivePreview.kt",
        )
        val multiplePickerFiles = listOf(
            "app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicPublishComposer.kt",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/CommentInputDialog.kt",
        )

        singlePickerFiles.forEach { path ->
            val source = loadSource(path)
            assertTrue(source.contains("PickGalleryVisualMedia()"), path)
            assertFalse(source.contains("contract = ActivityResultContracts.PickVisualMedia()"), path)
        }
        multiplePickerFiles.forEach { path ->
            val source = loadSource(path)
            assertTrue(source.contains("PickMultipleGalleryVisualMedia(maxItems = 9)"), path)
            assertFalse(source.contains("ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9)"), path)
        }
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(File(path), File(normalizedPath)).firstOrNull(File::exists)
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText().replace("\r\n", "\n")
    }
}
