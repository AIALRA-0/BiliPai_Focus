package com.android.purebilibili.core.plugin.skin

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WinterCloudSkinSampleTest {

    @Test
    fun winterCloudSampleCanBePreviewedAsBpskin() {
        val sampleDir = locateRepoFile("plugins/samples/winter-cloud-skin")
        val assetDir = File(sampleDir, "assets")
        val packageBytes = ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                zip.writeEntry(
                    name = "skin-manifest.json",
                    bytes = File(sampleDir, "skin-manifest.json").readBytes()
                )
                assetDir
                    .listFiles()
                    .orEmpty()
                    .filter { it.isFile }
                    .sortedBy { it.name }
                    .forEach { file ->
                        zip.writeEntry(
                            name = "assets/${file.name}",
                            bytes = file.readBytes()
                        )
                    }
            }
            output.toByteArray()
        }

        val preview = UiSkinPackageReader.preview(packageBytes).getOrThrow()

        assertEquals("samples.winter_cloud", preview.manifest.skinId)
        assertEquals("assets/bottom_trim.png", preview.manifest.assets.bottomBarTrim)
        assertTrue(UiSkinSurface.HOME_BOTTOM_BAR in preview.manifest.surfaces)
        assertTrue(UiSkinSurface.HOME_TOP_CHROME in preview.manifest.surfaces)
        assertEquals(preview.manifest.assets.declaredPaths().size, preview.assetEntries.size)
    }

    private fun ZipOutputStream.writeEntry(name: String, bytes: ByteArray) {
        putNextEntry(ZipEntry(name))
        write(bytes)
        closeEntry()
    }

    private fun locateRepoFile(path: String): File {
        return listOf(
            File(path),
            File("../$path")
        ).firstOrNull { it.exists() }
            ?: error("Cannot locate $path from ${File(".").absolutePath}")
    }
}
