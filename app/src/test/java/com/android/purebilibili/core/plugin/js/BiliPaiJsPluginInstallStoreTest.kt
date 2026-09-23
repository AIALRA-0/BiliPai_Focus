package com.android.purebilibili.core.plugin.js

import com.android.purebilibili.core.plugin.PluginCapability
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BiliPaiJsPluginInstallStoreTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun installPreviewPersistsScriptManifestSourceAndDisabledState() {
        val store = BiliPaiJsPluginInstallStore(rootDir = tempDir, clock = { 1234L })
        val manifest = sampleManifest()

        val installed = store.installPlugin(
            manifest = manifest,
            script = "globalThis.BiliPaiPlugin = {};",
            sourceUrl = "https://example.com/live.js",
            grantedCapabilities = setOf(
                PluginCapability.NETWORK,
                PluginCapability.EXTERNAL_MEDIA_PLAYBACK
            )
        ).getOrThrow()

        assertEquals("live.tv", installed.manifest.id)
        assertEquals("https://example.com/live.js", installed.sourceUrl)
        assertEquals(
            setOf(PluginCapability.NETWORK, PluginCapability.EXTERNAL_MEDIA_PLAYBACK),
            installed.grantedCapabilities
        )
        assertFalse(installed.enabled)
        assertEquals("globalThis.BiliPaiPlugin = {};", store.readScript(installed).getOrThrow())
        assertEquals(installed, store.listInstalledPlugins().single())
    }

    @Test
    fun installingSamePluginIdOverwritesPreviousScriptAndMetadata() {
        val store = BiliPaiJsPluginInstallStore(rootDir = tempDir, clock = { 1234L })
        val manifest = sampleManifest()

        store.installPlugin(
            manifest = manifest,
            script = "v1",
            sourceUrl = "https://example.com/v1.js",
            grantedCapabilities = setOf(PluginCapability.NETWORK)
        ).getOrThrow()
        val updated = store.installPlugin(
            manifest = manifest.copy(version = "1.1.0"),
            script = "v2",
            sourceUrl = "https://example.com/v2.js",
            grantedCapabilities = setOf(PluginCapability.EXTERNAL_MEDIA_PLAYBACK)
        ).getOrThrow()

        assertEquals(listOf(updated), store.listInstalledPlugins())
        assertEquals("1.1.0", updated.manifest.version)
        assertEquals("v2", store.readScript(updated).getOrThrow())
        assertEquals(setOf(PluginCapability.EXTERNAL_MEDIA_PLAYBACK), updated.grantedCapabilities)
    }

    @Test
    fun toggleAndRemovePluginUpdatePersistedRecords() {
        val store = BiliPaiJsPluginInstallStore(rootDir = tempDir, clock = { 1234L })
        val installed = store.installPlugin(
            manifest = sampleManifest(),
            script = "script",
            sourceUrl = null,
            grantedCapabilities = setOf(PluginCapability.NETWORK)
        ).getOrThrow()

        store.setEnabled(installed.manifest.id, true)
        assertTrue(store.listInstalledPlugins().single().enabled)

        store.removePlugin(installed.manifest.id)
        assertTrue(store.listInstalledPlugins().isEmpty())
        assertFalse(File(installed.scriptPath).exists())
    }

    private fun sampleManifest(): BiliPaiJsPluginManifest {
        return BiliPaiJsPluginManifest(
            id = "live.tv",
            title = "电视台",
            version = "1.0.0",
            author = "BiliPai",
            modules = listOf(BiliPaiJsModule(title = "直播", functionName = "loadChannels")),
            permissions = setOf(PluginCapability.NETWORK, PluginCapability.EXTERNAL_MEDIA_PLAYBACK)
        )
    }
}
