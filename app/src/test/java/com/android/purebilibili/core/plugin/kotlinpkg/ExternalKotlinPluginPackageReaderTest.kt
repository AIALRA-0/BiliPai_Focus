package com.android.purebilibili.core.plugin.kotlinpkg

import com.android.purebilibili.core.plugin.PluginCapability
import com.android.purebilibili.core.plugin.PluginCapabilityManifest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExternalKotlinPluginPackageReaderTest {

    @Test
    fun validPackage_readsManifestHashAndDexPresenceWithoutExecutingDex() {
        val manifest = sampleManifest()
        val bytes = pluginPackage(
            "plugin-manifest.json" to Json.encodeToString(manifest).toByteArray(),
            "classes.dex" to byteArrayOf(0x64, 0x65, 0x78)
        )

        val preview = ExternalKotlinPluginPackageReader.preview(bytes).getOrThrow()

        assertEquals(manifest, preview.descriptor.manifest)
        assertEquals(sha256Hex(bytes), preview.descriptor.packageSha256)
        assertTrue(preview.dexPresent)
        assertNull(preview.descriptor.signerSha256)
    }

    @Test
    fun validPackage_reportsJarDexAndOtherPayloadsWithoutExecutingThem() {
        val manifest = sampleManifest()
        val bytes = pluginPackage(
            "plugin-manifest.json" to Json.encodeToString(manifest).toByteArray(),
            "classes.jar" to byteArrayOf(0x50, 0x4b),
            "classes.dex" to byteArrayOf(0x64, 0x65, 0x78),
            "assets/compass.json" to byteArrayOf(1, 2, 3)
        )

        val preview = ExternalKotlinPluginPackageReader.preview(bytes).getOrThrow()

        assertTrue(preview.dexPresent)
        assertEquals(
            listOf(
                ExternalKotlinPluginPayloadEntry(
                    path = "classes.jar",
                    type = ExternalKotlinPluginPayloadType.CLASSES_JAR,
                    sizeBytes = 2
                ),
                ExternalKotlinPluginPayloadEntry(
                    path = "classes.dex",
                    type = ExternalKotlinPluginPayloadType.CLASSES_DEX,
                    sizeBytes = 3
                ),
                ExternalKotlinPluginPayloadEntry(
                    path = "assets/compass.json",
                    type = ExternalKotlinPluginPayloadType.OTHER,
                    sizeBytes = 3
                )
            ),
            preview.payloadEntries
        )
    }

    @Test
    fun missingManifest_rejectsPackageBeforeInstallDecision() {
        val bytes = pluginPackage("classes.dex" to byteArrayOf(1, 2, 3))

        val error = ExternalKotlinPluginPackageReader.preview(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("插件包缺少 plugin-manifest.json", error.message)
    }

    @Test
    fun duplicateManifest_rejectsPackageBeforeInstallDecision() {
        val manifest = Json.encodeToString(sampleManifest()).toByteArray()
        val bytes = pluginPackage(
            "plugin-manifest.json" to manifest,
            "./plugin-manifest.json" to manifest
        )

        val error = ExternalKotlinPluginPackageReader.preview(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("插件包包含重复的 plugin-manifest.json", error.message)
    }

    @Test
    fun pathTraversalEntry_rejectsPackage() {
        val bytes = pluginPackage(
            "plugin-manifest.json" to Json.encodeToString(sampleManifest()).toByteArray(),
            "../escape.dex" to byteArrayOf(1)
        )

        val error = ExternalKotlinPluginPackageReader.preview(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("插件包包含非法路径: ../escape.dex", error.message)
    }

    @Test
    fun oversizedManifest_rejectsPackage() {
        val bytes = pluginPackage(
            "plugin-manifest.json" to ByteArray(64 * 1024 + 1) { '{'.code.toByte() }
        )

        val error = ExternalKotlinPluginPackageReader.preview(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("plugin-manifest.json 超过 65536 字节", error.message)
    }

    @Test
    fun oversizedUncompressedPackage_rejectsPackage() {
        val bytes = pluginPackage(
            "plugin-manifest.json" to Json.encodeToString(sampleManifest()).toByteArray(),
            "assets/huge.bin" to ByteArray(16 * 1024 * 1024 + 1)
        )

        val error = ExternalKotlinPluginPackageReader.preview(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("插件包解压后内容超过 16777216 字节", error.message)
    }

    @Test
    fun validTrustedSignature_returnsSignerSha256() {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val manifestBytes = Json.encodeToString(sampleManifest()).toByteArray()
        val dexBytes = byteArrayOf(0x64, 0x65, 0x78)
        val signingPayload = signingPayload(
            mapOf(
                "plugin-manifest.json" to manifestBytes,
                "classes.dex" to dexBytes
            )
        )
        val signatureBytes = Signature.getInstance("SHA256withRSA").run {
            initSign(keyPair.private)
            update(signingPayload)
            sign()
        }
        val signatureJson = """
            {
              "formatVersion": 1,
              "keyId": "official",
              "algorithm": "SHA256withRSA",
              "signatureBase64": "${Base64.getEncoder().encodeToString(signatureBytes)}"
            }
        """.trimIndent().toByteArray()
        val bytes = pluginPackage(
            "plugin-manifest.json" to manifestBytes,
            "classes.dex" to dexBytes,
            "plugin-signature.json" to signatureJson
        )

        val preview = ExternalKotlinPluginPackageReader.preview(
            packageBytes = bytes,
            trustedPublicKeys = mapOf("official" to keyPair.public.encoded)
        ).getOrThrow()

        assertEquals(sha256Hex(keyPair.public.encoded), preview.descriptor.signerSha256)
    }

    @Test
    fun invalidSignature_doesNotTrustSignerButStillAllowsPreview() {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val manifestBytes = Json.encodeToString(sampleManifest()).toByteArray()
        val signatureJson = """
            {
              "formatVersion": 1,
              "keyId": "official",
              "algorithm": "SHA256withRSA",
              "signatureBase64": "${Base64.getEncoder().encodeToString(byteArrayOf(1, 2, 3))}"
            }
        """.trimIndent().toByteArray()
        val bytes = pluginPackage(
            "plugin-manifest.json" to manifestBytes,
            "plugin-signature.json" to signatureJson
        )

        val preview = ExternalKotlinPluginPackageReader.preview(
            packageBytes = bytes,
            trustedPublicKeys = mapOf("official" to keyPair.public.encoded)
        ).getOrThrow()

        assertFalse(preview.dexPresent)
        assertNull(preview.descriptor.signerSha256)
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

    private fun pluginPackage(vararg entries: Pair<String, ByteArray>): ByteArray {
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

    private fun sha256Hex(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }

    private fun signingPayload(entries: Map<String, ByteArray>): ByteArray {
        return entries
            .toList()
            .sortedBy { it.first }
            .joinToString(separator = "") { (name, bytes) ->
                "$name\n${bytes.size}\n${sha256Hex(bytes)}\n"
            }
            .toByteArray(Charsets.UTF_8)
    }
}
