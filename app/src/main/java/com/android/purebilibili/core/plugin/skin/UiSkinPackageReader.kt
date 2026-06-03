package com.android.purebilibili.core.plugin.skin

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

private const val SKIN_MANIFEST_ENTRY = "skin-manifest.json"
private const val MAX_ENTRY_COUNT = 256
private const val MAX_MANIFEST_BYTES = 64 * 1024
private const val MAX_TOTAL_UNCOMPRESSED_BYTES = 16 * 1024 * 1024

object UiSkinPackageReader {

    private val json = Json { ignoreUnknownKeys = true }

    fun preview(packageBytes: ByteArray): Result<UiSkinPackagePreview> {
        return runCatching {
            val packageSha256 = sha256Hex(packageBytes)
            val scan = scanPackage(packageBytes)
            val manifestBytes = scan.manifestBytes
                ?: throw IllegalArgumentException("皮肤包缺少 $SKIN_MANIFEST_ENTRY")
            val rawManifest = try {
                json.decodeFromString(RawUiSkinManifest.serializer(), manifestBytes.decodeToString())
            } catch (e: Exception) {
                throw IllegalArgumentException("skin-manifest.json 解析失败: ${e.message?.take(100)}")
            }
            val manifest = rawManifest.toManifest()
            validateManifest(manifest)
            val assetEntries = validateAssets(
                manifest = manifest,
                assetBytesByPath = scan.assetBytesByPath
            )
            UiSkinPackagePreview(
                manifest = manifest,
                packageSha256 = packageSha256,
                assetEntries = assetEntries
            )
        }
    }

    private fun scanPackage(packageBytes: ByteArray): UiSkinScanResult {
        val seenEntries = mutableSetOf<String>()
        val assetBytesByPath = linkedMapOf<String, ByteArray>()
        var manifestBytes: ByteArray? = null
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
                    throw IllegalArgumentException("皮肤包文件数量超过 $MAX_ENTRY_COUNT")
                }
                val normalizedName = normalizeEntryName(entry.name)
                if (!seenEntries.add(normalizedName)) {
                    throw IllegalArgumentException("皮肤包包含重复路径: ${entry.name}")
                }
                when {
                    normalizedName == SKIN_MANIFEST_ENTRY -> {
                        val bytes = zip.readLimited(MAX_MANIFEST_BYTES, SKIN_MANIFEST_ENTRY)
                        totalUncompressedBytes = totalUncompressedBytes.plusChecked(bytes.size.toLong())
                        manifestBytes = bytes
                    }
                    normalizedName.startsWith("assets/") -> {
                        val bytes = zip.readRemainingLimited(
                            remainingBytes = MAX_TOTAL_UNCOMPRESSED_BYTES - totalUncompressedBytes
                        )
                        totalUncompressedBytes += bytes.size
                        assetBytesByPath[normalizedName] = bytes
                    }
                    else -> throw IllegalArgumentException(
                        "皮肤包只能包含 skin-manifest.json 和 assets/ 下的资源"
                    )
                }
                zip.closeEntry()
            }
        }

        return UiSkinScanResult(
            manifestBytes = manifestBytes,
            assetBytesByPath = assetBytesByPath
        )
    }

    private fun validateManifest(manifest: UiSkinManifest) {
        if (manifest.formatVersion != 1) {
            throw IllegalArgumentException("皮肤包格式版本不受支持: ${manifest.formatVersion}")
        }
        if (manifest.apiVersion != 1) {
            throw IllegalArgumentException("皮肤包 API 版本不受支持: ${manifest.apiVersion}")
        }
        if (manifest.skinId.isBlank()) {
            throw IllegalArgumentException("皮肤包缺少 skinId")
        }
        if (manifest.displayName.isBlank()) {
            throw IllegalArgumentException("皮肤包缺少 displayName")
        }
        if (manifest.surfaces.isEmpty()) {
            throw IllegalArgumentException("皮肤包没有声明适用界面")
        }
        if (manifest.communityShareable && manifest.licenseNote.isNullOrBlank()) {
            throw IllegalArgumentException("社区可分享皮肤包必须声明 licenseNote")
        }
    }

    private fun validateAssets(
        manifest: UiSkinManifest,
        assetBytesByPath: Map<String, ByteArray>
    ): List<UiSkinAssetEntry> {
        val declaredPaths = manifest.assets.declaredPaths()
        val distinctPaths = linkedSetOf<String>()
        declaredPaths.forEach { path ->
            if (!distinctPaths.add(path)) {
                throw IllegalArgumentException("皮肤包重复声明资源: $path")
            }
            if (!path.startsWith("assets/")) {
                throw IllegalArgumentException("皮肤资源必须位于 assets/ 下: $path")
            }
            if (path !in assetBytesByPath) {
                throw IllegalArgumentException("皮肤包缺少资源: $path")
            }
        }
        assetBytesByPath.keys.forEach { path ->
            if (path !in distinctPaths) {
                throw IllegalArgumentException("皮肤包包含未声明资源: $path")
            }
        }
        return declaredPaths.map { path ->
            val bytes = assetBytesByPath.getValue(path)
            val type = detectImageType(bytes)
                ?: throw IllegalArgumentException("皮肤资源 $path 不是受支持的图片格式")
            UiSkinAssetEntry(
                path = path,
                type = type,
                sizeBytes = bytes.size.toLong()
            )
        }
    }

    private fun RawUiSkinManifest.toManifest(): UiSkinManifest {
        return UiSkinManifest(
            formatVersion = formatVersion,
            skinId = skinId,
            displayName = displayName,
            version = version,
            apiVersion = apiVersion,
            author = author,
            surfaces = surfaces.mapTo(linkedSetOf()) { surface ->
                UiSkinSurface.entries.find { it.name == surface }
                    ?: throw IllegalArgumentException("皮肤包声明了未知界面: $surface")
            },
            assets = assets,
            colors = colors,
            styleSourceName = styleSourceName,
            styleSourceUrl = styleSourceUrl,
            licenseNote = licenseNote,
            communityShareable = communityShareable,
            containsOfficialAssets = containsOfficialAssets
        )
    }

    private fun detectImageType(bytes: ByteArray): UiSkinAssetType? {
        return when {
            bytes.size >= 8 &&
                bytes[0] == 0x89.toByte() &&
                bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() &&
                bytes[3] == 0x47.toByte() &&
                bytes[4] == 0x0D.toByte() &&
                bytes[5] == 0x0A.toByte() &&
                bytes[6] == 0x1A.toByte() &&
                bytes[7] == 0x0A.toByte() -> UiSkinAssetType.PNG
            bytes.size >= 12 &&
                bytes[0] == 0x52.toByte() &&
                bytes[1] == 0x49.toByte() &&
                bytes[2] == 0x46.toByte() &&
                bytes[3] == 0x46.toByte() &&
                bytes[8] == 0x57.toByte() &&
                bytes[9] == 0x45.toByte() &&
                bytes[10] == 0x42.toByte() &&
                bytes[11] == 0x50.toByte() -> UiSkinAssetType.WEBP
            bytes.size >= 3 &&
                bytes[0] == 0xFF.toByte() &&
                bytes[1] == 0xD8.toByte() &&
                bytes[2] == 0xFF.toByte() -> UiSkinAssetType.JPEG
            else -> null
        }
    }

    private fun normalizeEntryName(rawName: String): String {
        if (rawName.isBlank() || rawName.startsWith("/") || rawName.startsWith("\\")) {
            throw IllegalArgumentException("皮肤包包含非法路径: $rawName")
        }
        val normalized = rawName
            .replace('\\', '/')
            .split('/')
            .filter { it.isNotEmpty() && it != "." }
            .also { parts ->
                if (parts.any { it == ".." }) {
                    throw IllegalArgumentException("皮肤包包含非法路径: $rawName")
                }
            }
            .joinToString("/")
        if (normalized.isBlank()) {
            throw IllegalArgumentException("皮肤包包含非法路径: $rawName")
        }
        return normalized
    }
}

private data class UiSkinScanResult(
    val manifestBytes: ByteArray?,
    val assetBytesByPath: Map<String, ByteArray>
)

@Serializable
private data class RawUiSkinManifest(
    val formatVersion: Int,
    val skinId: String,
    val displayName: String,
    val version: String,
    val apiVersion: Int,
    val author: String? = null,
    val surfaces: Set<String>,
    val assets: UiSkinAssets = UiSkinAssets(),
    val colors: UiSkinColorTokens = UiSkinColorTokens(),
    val styleSourceName: String? = null,
    val styleSourceUrl: String? = null,
    val licenseNote: String? = null,
    val communityShareable: Boolean = false,
    val containsOfficialAssets: Boolean = false
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

private fun ZipInputStream.readRemainingLimited(remainingBytes: Long): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = read(buffer)
        if (read == -1) break
        total += read
        if (total > remainingBytes) {
            throw IllegalArgumentException("皮肤包解压后内容超过 $MAX_TOTAL_UNCOMPRESSED_BYTES 字节")
        }
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

private fun Long.plusChecked(delta: Long): Long {
    val total = this + delta
    if (total > MAX_TOTAL_UNCOMPRESSED_BYTES) {
        throw IllegalArgumentException("皮肤包解压后内容超过 $MAX_TOTAL_UNCOMPRESSED_BYTES 字节")
    }
    return total
}

private fun sha256Hex(bytes: ByteArray): String {
    return MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
}
