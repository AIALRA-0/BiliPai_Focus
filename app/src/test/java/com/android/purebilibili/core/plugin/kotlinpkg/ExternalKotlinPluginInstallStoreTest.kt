package com.android.purebilibili.core.plugin.kotlinpkg

import com.android.purebilibili.core.plugin.PluginCapability
import com.android.purebilibili.core.plugin.PluginCapabilityManifest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExternalKotlinPluginInstallStoreTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun installPreviewPersistsPackageAndAuthorizationWithoutEnablingExecution() {
        val store = ExternalKotlinPluginInstallStore(rootDir = tempDir, clock = { 1234L })
        val bytes = pluginPackage(sampleManifest(), marker = "v1")
        val preview = store.previewPackage(bytes).getOrThrow()

        val installed = store.installPreview(
            preview = preview,
            packageBytes = bytes,
            grantedCapabilities = setOf(
                PluginCapability.RECOMMENDATION_CANDIDATES,
                PluginCapability.NETWORK
            )
        ).getOrThrow()

        assertEquals("dev.example.today_watch_remix", installed.manifest.pluginId)
        assertEquals(preview.descriptor.packageSha256, installed.packageSha256)
        assertEquals(setOf(PluginCapability.RECOMMENDATION_CANDIDATES), installed.grantedCapabilities)
        assertFalse(installed.enabled)
        assertTrue(File(installed.packagePath).exists())
        assertEquals(installed, store.listInstalledPackages().single())
        assertNotNull(store.getAuthorization(installed.manifest.pluginId, installed.packageSha256))
    }

    @Test
    fun packageHashChangeRequiresNewAuthorizationForSamePluginId() {
        val store = ExternalKotlinPluginInstallStore(rootDir = tempDir, clock = { 1234L })
        val manifest = sampleManifest()
        val firstBytes = pluginPackage(manifest, marker = "v1")
        val firstPreview = store.previewPackage(firstBytes).getOrThrow()
        store.installPreview(
            preview = firstPreview,
            packageBytes = firstBytes,
            grantedCapabilities = setOf(PluginCapability.RECOMMENDATION_CANDIDATES)
        ).getOrThrow()

        val secondPreview = store.previewPackage(pluginPackage(manifest, marker = "v2")).getOrThrow()

        assertNull(store.getAuthorization(manifest.pluginId, secondPreview.descriptor.packageSha256))
    }

    @Test
    fun removePackageAndRevokeAuthorizationClearPersistedRecords() {
        val store = ExternalKotlinPluginInstallStore(rootDir = tempDir, clock = { 1234L })
        val bytes = pluginPackage(sampleManifest(), marker = "v1")
        val installed = store.installPreview(
            preview = store.previewPackage(bytes).getOrThrow(),
            packageBytes = bytes,
            grantedCapabilities = setOf(PluginCapability.RECOMMENDATION_CANDIDATES)
        ).getOrThrow()

        store.revokeAuthorization(installed.manifest.pluginId, installed.packageSha256)
        store.removeInstalledPackage(installed.manifest.pluginId)

        assertNull(store.getAuthorization(installed.manifest.pluginId, installed.packageSha256))
        assertTrue(store.listInstalledPackages().isEmpty())
        assertFalse(File(installed.packagePath).exists())
    }

    private fun sampleManifest(): PluginCapabilityManifest {
        return PluginCapabilityManifest(
            pluginId = "dev.example.today_watch_remix",
            displayName = "今日推荐单 Remix",
            version = "1.0.0",
            apiVersion = 1,
            entryClassName = "dev.example.TodayWatchRemixPlugin",
            capabilities = setOf(
                PluginCapability.RECOMMENDATION_CANDIDATES,
                PluginCapability.LOCAL_HISTORY_READ
            )
        )
    }

    private fun pluginPackage(
        manifest: PluginCapabilityManifest,
        marker: String
    ): ByteArray {
        return ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("plugin-manifest.json"))
                zip.write(Json.encodeToString(manifest).toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("assets/$marker.txt"))
                zip.write(marker.toByteArray())
                zip.closeEntry()
            }
            output.toByteArray()
        }
    }
}
