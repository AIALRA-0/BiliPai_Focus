package com.android.purebilibili.core.plugin.skin

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UiSkinInstallStoreTest {

    @Test
    fun installPreview_persistsBpskinRecordButKeepsItDisabledByDefault() {
        val rootDir = createTempDirectory("ui-skin-store").toFile()
        val store = UiSkinInstallStore(
            rootDir = rootDir,
            clock = { 42L }
        )
        val manifest = UiSkinManifest(
            formatVersion = 1,
            skinId = "dev.example.cloud",
            displayName = "云朵底栏",
            version = "1.0.0",
            apiVersion = 1,
            surfaces = setOf(UiSkinSurface.HOME_BOTTOM_BAR),
            assets = UiSkinAssets(bottomBarTrim = "assets/bottom_trim.png")
        )
        val bytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(manifest).toByteArray(),
            "assets/bottom_trim.png" to pngBytes()
        )
        val preview = store.previewPackage(bytes).getOrThrow()

        val installed = store.installPreview(preview, bytes).getOrThrow()
        val installedPackages = store.listInstalledPackages()

        assertEquals(manifest, installed.manifest)
        assertFalse(installed.enabled)
        assertEquals(42L, installed.installedAtMillis)
        assertTrue(installed.packagePath.endsWith(".bpskin"))
        val bottomTrimPath = assertNotNull(installed.assetFiles["assets/bottom_trim.png"])
        assertTrue(File(bottomTrimPath).exists())
        assertEquals(pngBytes().toList(), File(bottomTrimPath).readBytes().toList())
        assertTrue(installedPackages.any { it.skinId == manifest.skinId && !it.enabled })
    }

    @Test
    fun previewAssetFiles_extractsDeclaredAssetsForVisualPreviewWithoutInstalling() {
        val rootDir = createTempDirectory("ui-skin-store").toFile()
        val store = UiSkinInstallStore(rootDir = rootDir)
        val bytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(
                manifest(
                    displayName = "预览皮肤",
                    version = "1.0.0",
                    bottomBarTrim = "assets/bottom_trim.png"
                )
            ).toByteArray(),
            "assets/bottom_trim.png" to pngBytes()
        )
        val preview = store.previewPackage(bytes).getOrThrow()

        val previewFiles = store.extractPreviewAssetFiles(preview, bytes).getOrThrow()

        val previewPath = assertNotNull(previewFiles["assets/bottom_trim.png"])
        assertTrue(File(previewPath).exists())
        assertEquals(pngBytes().toList(), File(previewPath).readBytes().toList())
        assertTrue(previewPath.contains("preview_assets"))
    }

    @Test
    fun installPreview_replacesOlderPackageWithSameSkinId() {
        val rootDir = createTempDirectory("ui-skin-store").toFile()
        var now = 100L
        val store = UiSkinInstallStore(
            rootDir = rootDir,
            clock = { now++ }
        )
        val firstBytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(
                manifest(
                    displayName = "本地装扮资源包",
                    version = "1.0.0",
                    bottomBarTrim = "assets/first.png"
                )
            ).toByteArray(),
            "assets/first.png" to pngBytes()
        )
        val secondBytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(
                manifest(
                    displayName = "本地装扮资源包",
                    version = "1.0.1",
                    bottomBarTrim = "assets/second.png"
                )
            ).toByteArray(),
            "assets/second.png" to pngBytes(0x01)
        )

        val firstPreview = store.previewPackage(firstBytes).getOrThrow()
        val secondPreview = store.previewPackage(secondBytes).getOrThrow()
        val firstInstalled = store.installPreview(firstPreview, firstBytes).getOrThrow()
        val secondInstalled = store.installPreview(secondPreview, secondBytes).getOrThrow()
        val installedPackages = store.listInstalledPackages()
            .filter { it.skinId == "local.bilibili_skin.local_package" }

        assertEquals(1, installedPackages.size)
        assertEquals(secondInstalled.installId, installedPackages.single().installId)
        assertTrue(firstInstalled.installId != secondInstalled.installId)
        assertNull(
            rootDir.resolve("installed/${firstInstalled.installId}.json").takeIf { it.exists() }
        )
        assertTrue(File(checkNotNull(secondInstalled.assetFiles["assets/second.png"])).exists())
    }

    @Test
    fun listInstalledPackages_collapsesHistoricalDuplicatesBySkinId() {
        val rootDir = createTempDirectory("ui-skin-store").toFile()
        val installedDir = rootDir.resolve("installed").also { it.mkdirs() }
        val oldRecord = InstalledUiSkinPackage(
            manifest = manifest(
                displayName = "本地装扮资源包",
                version = "1.0.0",
                bottomBarTrim = "assets/old.png"
            ),
            packageSha256 = "old-sha",
            packagePath = "/tmp/old.bpskin",
            installedAtMillis = 100L
        )
        val latestRecord = InstalledUiSkinPackage(
            manifest = manifest(
                displayName = "洛天依拜年纪个性主题",
                version = "1774972800",
                bottomBarTrim = "assets/latest.png"
            ),
            packageSha256 = "latest-sha",
            packagePath = "/tmp/latest.bpskin",
            installedAtMillis = 200L
        )
        installedDir.resolve("old.json").writeText(Json.encodeToString(oldRecord))
        installedDir.resolve("latest.json").writeText(Json.encodeToString(latestRecord))
        val store = UiSkinInstallStore(rootDir = rootDir)

        val installedPackages = store.listInstalledPackages()
            .filter { it.skinId == "local.bilibili_skin.local_package" }

        assertEquals(1, installedPackages.size)
        assertEquals("洛天依拜年纪个性主题", installedPackages.single().displayName)
        assertEquals(200L, installedPackages.single().installedAtMillis)
    }

    @Test
    fun deleteInstalledPackage_removesRecordPackageAndExtractedAssets() {
        val rootDir = createTempDirectory("ui-skin-store").toFile()
        val store = UiSkinInstallStore(
            rootDir = rootDir,
            clock = { 42L }
        )
        val bytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(
                manifest(
                    displayName = "可删除皮肤",
                    version = "1.0.0",
                    bottomBarTrim = "assets/bottom_trim.png"
                )
            ).toByteArray(),
            "assets/bottom_trim.png" to pngBytes()
        )
        val preview = store.previewPackage(bytes).getOrThrow()
        val installed = store.installPreview(preview, bytes).getOrThrow()
        val packageFile = File(installed.packagePath)
        val assetFile = File(checkNotNull(installed.assetFiles["assets/bottom_trim.png"]))

        val deleted = store.deleteInstalledPackage(installed.installId).getOrThrow()

        assertTrue(deleted)
        assertFalse(packageFile.exists())
        assertFalse(assetFile.exists())
        assertTrue(store.listInstalledPackages().none { it.installId == installed.installId })
    }

    @Test
    fun deleteInstalledPackage_returnsFalseWhenRecordIsMissing() {
        val rootDir = createTempDirectory("ui-skin-store").toFile()
        val store = UiSkinInstallStore(rootDir = rootDir)

        val deleted = store.deleteInstalledPackage("missing-install-id").getOrThrow()

        assertFalse(deleted)
    }

    private fun manifest(
        displayName: String,
        version: String,
        bottomBarTrim: String
    ): UiSkinManifest {
        return UiSkinManifest(
            formatVersion = 1,
            skinId = "local.bilibili_skin.local_package",
            displayName = displayName,
            version = version,
            apiVersion = 1,
            surfaces = setOf(UiSkinSurface.HOME_BOTTOM_BAR),
            assets = UiSkinAssets(bottomBarTrim = bottomBarTrim)
        )
    }

    private fun skinPackage(vararg entries: Pair<String, ByteArray>): ByteArray {
        return ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                entries.forEach { (name, bytes) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
            output.toByteArray()
        }
    }

    private fun pngBytes(seed: Int = 0x00): ByteArray {
        return byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            seed.toByte(), 0x00, 0x00, 0x0D
        )
    }
}
