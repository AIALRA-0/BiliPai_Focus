package com.android.purebilibili.core.plugin.kotlinpkg

import com.android.purebilibili.core.plugin.ExternalPluginPackageDescriptor
import com.android.purebilibili.core.plugin.PluginCapabilityManifest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.zip.ZipInputStream

private const val PLUGIN_MANIFEST_ENTRY = "plugin-manifest.json"
private const val PLUGIN_SIGNATURE_ENTRY = "plugin-signature.json"
private const val CLASSES_JAR_ENTRY = "classes.jar"
private const val CLASSES_DEX_ENTRY = "classes.dex"
private const val MAX_ENTRY_COUNT = 256
private const val MAX_MANIFEST_BYTES = 64 * 1024
private const val MAX_SIGNATURE_BYTES = 16 * 1024
private const val MAX_TOTAL_UNCOMPRESSED_BYTES = 16 * 1024 * 1024

enum class ExternalKotlinPluginPayloadType {
    CLASSES_JAR,
    CLASSES_DEX,
    OTHER
}

data class ExternalKotlinPluginPayloadEntry(
    val path: String,
    val type: ExternalKotlinPluginPayloadType,
    val sizeBytes: Long
)

data class ExternalKotlinPluginPackagePreview(
    val descriptor: ExternalPluginPackageDescriptor,
    val dexPresent: Boolean,
    val payloadEntries: List<ExternalKotlinPluginPayloadEntry> = emptyList()
)

object ExternalKotlinPluginPackageReader {

    private val json = Json { ignoreUnknownKeys = true }

    fun preview(
        packageBytes: ByteArray,
        trustedPublicKeys: Map<String, ByteArray> = emptyMap()
    ): Result<ExternalKotlinPluginPackagePreview> {
        return runCatching {
            val packageSha256 = sha256Hex(packageBytes)
            val scan = scanPackage(packageBytes)
            val manifestBytes = scan.manifestBytes
                ?: throw IllegalArgumentException("插件包缺少 $PLUGIN_MANIFEST_ENTRY")
            val manifest = try {
                json.decodeFromString(PluginCapabilityManifest.serializer(), manifestBytes.decodeToString())
            } catch (e: Exception) {
                throw IllegalArgumentException("plugin-manifest.json 解析失败: ${e.message?.take(100)}")
            }
            val signerSha256 = resolveSignerSha256(
                signatureBytes = scan.signatureBytes,
                entryDigests = scan.entryDigests,
                trustedPublicKeys = trustedPublicKeys
            )

            ExternalKotlinPluginPackagePreview(
                descriptor = ExternalPluginPackageDescriptor(
                    manifest = manifest,
                    packageSha256 = packageSha256,
                    signerSha256 = signerSha256
                ),
                dexPresent = scan.dexPresent,
                payloadEntries = scan.payloadEntries
            )
        }
    }

    private fun scanPackage(packageBytes: ByteArray): PackageScanResult {
        val entryDigests = mutableListOf<EntryDigest>()
        val payloadEntries = mutableListOf<ExternalKotlinPluginPayloadEntry>()
        val seenEntries = mutableSetOf<String>()
        var manifestBytes: ByteArray? = null
        var signatureBytes: ByteArray? = null
        var dexPresent = false
        var entryCount = 0
        var totalUncompressedBytes = 0L

        ZipInputStream(ByteArrayInputStream(packageBytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }
                entryCount += 1
                if (entryCount > MAX_ENTRY_COUNT) {
                    throw IllegalArgumentException("插件包文件数量超过 $MAX_ENTRY_COUNT")
                }

                val normalizedName = normalizeEntryName(entry.name)
                if (!seenEntries.add(normalizedName)) {
                    if (normalizedName == PLUGIN_MANIFEST_ENTRY) {
                        throw IllegalArgumentException("插件包包含重复的 $PLUGIN_MANIFEST_ENTRY")
                    }
                    throw IllegalArgumentException("插件包包含重复路径: ${entry.name}")
                }

                when (normalizedName) {
                    PLUGIN_MANIFEST_ENTRY -> {
                        val bytes = zip.readLimited(MAX_MANIFEST_BYTES, PLUGIN_MANIFEST_ENTRY)
                        totalUncompressedBytes = totalUncompressedBytes.plusChecked(bytes.size.toLong())
                        manifestBytes = bytes
                        entryDigests += EntryDigest(
                            name = normalizedName,
                            size = bytes.size.toLong(),
                            sha256 = sha256Hex(bytes)
                        )
                    }
                    PLUGIN_SIGNATURE_ENTRY -> {
                        val bytes = zip.readLimited(MAX_SIGNATURE_BYTES, PLUGIN_SIGNATURE_ENTRY)
                        totalUncompressedBytes = totalUncompressedBytes.plusChecked(bytes.size.toLong())
                        signatureBytes = bytes
                    }
                    CLASSES_DEX_ENTRY -> {
                        dexPresent = true
                        val digest = MessageDigest.getInstance("SHA-256")
                        val size = zip.copyToDigest(
                            digest = digest,
                            remainingBytes = MAX_TOTAL_UNCOMPRESSED_BYTES - totalUncompressedBytes
                        )
                        totalUncompressedBytes += size
                        entryDigests += EntryDigest(
                            name = normalizedName,
                            size = size,
                            sha256 = digest.digest().toHex()
                        )
                        payloadEntries += ExternalKotlinPluginPayloadEntry(
                            path = normalizedName,
                            type = ExternalKotlinPluginPayloadType.CLASSES_DEX,
                            sizeBytes = size
                        )
                    }
                    else -> {
                        val digest = MessageDigest.getInstance("SHA-256")
                        val size = zip.copyToDigest(
                            digest = digest,
                            remainingBytes = MAX_TOTAL_UNCOMPRESSED_BYTES - totalUncompressedBytes
                        )
                        totalUncompressedBytes += size
                        entryDigests += EntryDigest(
                            name = normalizedName,
                            size = size,
                            sha256 = digest.digest().toHex()
                        )
                        payloadEntries += ExternalKotlinPluginPayloadEntry(
                            path = normalizedName,
                            type = normalizedName.toPayloadType(),
                            sizeBytes = size
                        )
                    }
                }
                zip.closeEntry()
            }
        }

        return PackageScanResult(
            manifestBytes = manifestBytes,
            signatureBytes = signatureBytes,
            dexPresent = dexPresent,
            payloadEntries = payloadEntries,
            entryDigests = entryDigests
        )
    }

    private fun resolveSignerSha256(
        signatureBytes: ByteArray?,
        entryDigests: List<EntryDigest>,
        trustedPublicKeys: Map<String, ByteArray>
    ): String? {
        if (signatureBytes == null) return null
        val signatureManifest = runCatching {
            json.decodeFromString(PluginSignatureManifest.serializer(), signatureBytes.decodeToString())
        }.getOrNull() ?: return null
        if (signatureManifest.formatVersion != 1) return null
        if (signatureManifest.algorithm !in setOf("SHA256withRSA", "SHA256withECDSA")) return null
        val publicKeyBytes = trustedPublicKeys[signatureManifest.keyId] ?: return null
        val keyFactoryAlgorithm = when (signatureManifest.algorithm) {
            "SHA256withRSA" -> "RSA"
            "SHA256withECDSA" -> "EC"
            else -> return null
        }
        val publicKey = runCatching {
            KeyFactory.getInstance(keyFactoryAlgorithm)
                .generatePublic(X509EncodedKeySpec(publicKeyBytes))
        }.getOrNull() ?: return null
        val decodedSignature = runCatching {
            Base64.getDecoder().decode(signatureManifest.signatureBase64)
        }.getOrNull() ?: return null

        val verified = runCatching {
            Signature.getInstance(signatureManifest.algorithm).run {
                initVerify(publicKey)
                update(buildSigningPayload(entryDigests))
                verify(decodedSignature)
            }
        }.getOrDefault(false)

        return if (verified) sha256Hex(publicKeyBytes) else null
    }

    private fun normalizeEntryName(rawName: String): String {
        if (rawName.isBlank() || rawName.startsWith("/") || rawName.startsWith("\\")) {
            throw IllegalArgumentException("插件包包含非法路径: $rawName")
        }

        val normalized = rawName
            .replace('\\', '/')
            .split('/')
            .filter { it.isNotEmpty() && it != "." }
            .also { parts ->
                if (parts.any { it == ".." }) {
                    throw IllegalArgumentException("插件包包含非法路径: $rawName")
                }
            }
            .joinToString("/")

        if (normalized.isBlank()) {
            throw IllegalArgumentException("插件包包含非法路径: $rawName")
        }
        return normalized
    }

    private fun buildSigningPayload(entryDigests: List<EntryDigest>): ByteArray {
        return entryDigests
            .sortedBy { it.name }
            .joinToString(separator = "") { entry ->
                "${entry.name}\n${entry.size}\n${entry.sha256}\n"
            }
            .toByteArray(Charsets.UTF_8)
    }
}

private data class PackageScanResult(
    val manifestBytes: ByteArray?,
    val signatureBytes: ByteArray?,
    val dexPresent: Boolean,
    val payloadEntries: List<ExternalKotlinPluginPayloadEntry>,
    val entryDigests: List<EntryDigest>
)

private data class EntryDigest(
    val name: String,
    val size: Long,
    val sha256: String
)

@Serializable
private data class PluginSignatureManifest(
    val formatVersion: Int,
    val keyId: String,
    val algorithm: String,
    val signatureBase64: String
)

private fun ZipInputStream.readLimited(limitBytes: Int, displayName: String): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val read = read(buffer)
        if (read == -1) break
        total += read
        if (total > limitBytes) {
            throw IllegalArgumentException("$displayName 超过 $limitBytes 字节")
        }
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

private fun ZipInputStream.copyToDigest(
    digest: MessageDigest,
    remainingBytes: Long
): Long {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = read(buffer)
        if (read == -1) break
        total += read
        if (total > remainingBytes) {
            throw IllegalArgumentException("插件包解压后内容超过 $MAX_TOTAL_UNCOMPRESSED_BYTES 字节")
        }
        digest.update(buffer, 0, read)
    }
    return total
}

private fun Long.plusChecked(delta: Long): Long {
    val total = this + delta
    if (total > MAX_TOTAL_UNCOMPRESSED_BYTES) {
        throw IllegalArgumentException("插件包解压后内容超过 $MAX_TOTAL_UNCOMPRESSED_BYTES 字节")
    }
    return total
}

private fun sha256Hex(bytes: ByteArray): String {
    return MessageDigest.getInstance("SHA-256").digest(bytes).toHex()
}

private fun ByteArray.toHex(): String {
    return joinToString("") { "%02x".format(it) }
}

private fun String.toPayloadType(): ExternalKotlinPluginPayloadType {
    return when (this) {
        CLASSES_JAR_ENTRY -> ExternalKotlinPluginPayloadType.CLASSES_JAR
        CLASSES_DEX_ENTRY -> ExternalKotlinPluginPayloadType.CLASSES_DEX
        else -> ExternalKotlinPluginPayloadType.OTHER
    }
}
