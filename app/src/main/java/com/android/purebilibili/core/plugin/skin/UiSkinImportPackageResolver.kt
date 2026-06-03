package com.android.purebilibili.core.plugin.skin

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val BILIBILI_SKIN_SOURCE_NAME = "KimmyXYC/bilibili-skin"
private const val BILIBILI_SKIN_SOURCE_URL = "https://github.com/KimmyXYC/bilibili-skin"
private const val BILIBILI_SKIN_LICENSE_NOTE =
    "由用户本地 KimmyXYC/bilibili-skin 主题目录转换，输出包包含原存档/官方装扮素材；" +
        "仅供本地私用或在已获得授权时分享，不要将官方付费主题原图、角色立绘、图标原件或动效资源作为社区包分发。"
private const val MAX_THEME_ENTRY_COUNT = 256
private const val MAX_THEME_TOTAL_BYTES = 128 * 1024 * 1024

enum class UiSkinImportSource {
    BP_SKIN,
    BILIBILI_SKIN_ARCHIVE
}

data class UiSkinImportPackage(
    val source: UiSkinImportSource,
    val packageBytes: ByteArray
)

object UiSkinImportPackageResolver {
    private val json = Json { ignoreUnknownKeys = true }
    private val iconMapping = mapOf(
        "tail_icon_main" to "home",
        "tail_icon_dynamic" to "following",
        "tail_icon_shop" to "member",
        "tail_icon_myself" to "profile"
    )
    private val selectedIconMapping = mapOf(
        "tail_icon_selected_main" to "home_selected",
        "tail_icon_selected_dynamic" to "following_selected",
        "tail_icon_selected_shop" to "member_selected",
        "tail_icon_selected_myself" to "profile_selected"
    )

    fun resolve(
        inputBytes: ByteArray,
        remotePackageFetcher: ((String) -> ByteArray)? = null
    ): Result<UiSkinImportPackage> {
        return runCatching {
            if (UiSkinPackageReader.preview(inputBytes).isSuccess) {
                return@runCatching UiSkinImportPackage(
                    source = UiSkinImportSource.BP_SKIN,
                    packageBytes = inputBytes
                )
            }
            UiSkinImportPackage(
                source = UiSkinImportSource.BILIBILI_SKIN_ARCHIVE,
                packageBytes = convertBilibiliThemeArchive(inputBytes, remotePackageFetcher)
            )
        }
    }

    private fun convertBilibiliThemeArchive(
        inputBytes: ByteArray,
        remotePackageFetcher: ((String) -> ByteArray)?
    ): ByteArray {
        if (looksLikeJson(inputBytes)) {
            return convertDirectThemeJson(inputBytes, remotePackageFetcher)
        }
        val outerEntries = scanZip(
            inputBytes = inputBytes,
            illegalPathMessage = "装扮存档包含非法路径"
        )
        val themeJson = selectThemeJsonOrNull(outerEntries)
        var packageZip = selectPackageZipOrNull(outerEntries)
        if (packageZip == null && themeJson != null) {
            val packageUrl = themeJson.packageUrlOrNull()
            packageZip = if (packageUrl != null) {
                fetchRemotePackage(packageUrl, remotePackageFetcher)
            } else {
                throw IllegalArgumentException("装扮存档缺少 _package.zip")
            }
        }
        val theme = if (packageZip == null) {
            BilibiliSkinTheme(
                id = "local_package",
                name = "本地装扮资源包",
                version = "1.0.0",
                color = null,
                colorSecondPage = null,
                tailColor = null
            )
        } else {
            parseThemeJson(themeJson ?: throw IllegalArgumentException("装扮存档缺少主题 JSON"))
        }
        val packageEntries = if (packageZip == null) {
            outerEntries
        } else {
            scanZip(
                inputBytes = packageZip,
                illegalPathMessage = "装扮资源包包含非法路径"
            )
        }
        val assetBytesByPath = buildAssetBytes(packageEntries)
        if (assetBytesByPath.isEmpty()) {
            throw IllegalArgumentException("装扮资源包缺少可转换资源")
        }
        val manifest = buildManifest(
            theme = theme,
            assetPaths = assetBytesByPath.keys.toSet()
        )
        return buildBpskinPackage(manifest, assetBytesByPath)
    }

    private fun convertDirectThemeJson(
        inputBytes: ByteArray,
        remotePackageFetcher: ((String) -> ByteArray)?
    ): ByteArray {
        val packageUrl = inputBytes.packageUrlOrNull()
            ?: throw IllegalArgumentException("皮肤 JSON 缺少 package_url，无法导入资源包")
        val packageZip = fetchRemotePackage(packageUrl, remotePackageFetcher)
        val theme = parseThemeJson(inputBytes)
        val packageEntries = scanZip(
            inputBytes = packageZip,
            illegalPathMessage = "装扮资源包包含非法路径"
        )
        val assetBytesByPath = buildAssetBytes(packageEntries)
        if (assetBytesByPath.isEmpty()) {
            throw IllegalArgumentException("装扮资源包缺少可转换资源")
        }
        val manifest = buildManifest(
            theme = theme,
            assetPaths = assetBytesByPath.keys.toSet()
        )
        return buildBpskinPackage(manifest, assetBytesByPath)
    }

    private fun fetchRemotePackage(
        packageUrl: String,
        remotePackageFetcher: ((String) -> ByteArray)?
    ): ByteArray {
        val normalizedUrl = packageUrl.trim()
        if (!normalizedUrl.startsWith("https://")) {
            throw IllegalArgumentException("皮肤 package_url 不是安全 HTTPS 链接")
        }
        return remotePackageFetcher?.invoke(normalizedUrl)
            ?: throw IllegalArgumentException("皮肤 JSON 需要下载 package_url，请检查网络后重试")
    }

    private fun selectThemeJsonOrNull(entries: Map<String, ByteArray>): ByteArray? {
        val jsonEntries = entries
            .filterKeys { path ->
                val name = path.substringAfterLast("/")
                path.endsWith(".json") && name != "个性装扮-套装.json" && name != "原始.json"
            }
            .toList()
            .sortedWith(compareBy(
                { (path, _) -> if (path.substringAfterLast("/") == "${path.parentName()}.json") 0 else 1 },
                { (path, _) -> if (path.substringAfterLast("/") == "个性装扮.json") 1 else 0 },
                { (path, _) -> path }
            ))
        return jsonEntries.firstOrNull()?.second
    }

    private fun selectPackageZipOrNull(entries: Map<String, ByteArray>): ByteArray? {
        return entries
            .filterKeys { path ->
                val name = path.substringAfterLast("/")
                name.endsWith("_package.zip") ||
                    (name.startsWith("package_url") && name.endsWith(".zip"))
            }
            .toList()
            .sortedWith(compareBy(
                { (path, _) -> if (path.substringAfterLast("/").endsWith("_package.zip")) 0 else 1 },
                { (path, _) -> path }
            ))
            .map { (_, bytes) -> bytes }
            .firstOrNull()
    }

    private fun parseThemeJson(bytes: ByteArray): BilibiliSkinTheme {
        val root = json.parseToJsonElement(bytes.decodeToString()).jsonObject
        val dataObject = root.objectOrNull("data")
        val themeObject = root.resolveThemeObject()
        val properties = themeObject?.objectOrNull("properties")
            ?: dataObject?.objectOrNull("properties")
            ?: root.objectOrNull("properties")
            ?: dataObject
                ?.takeIf { it.looksLikeThemeProperties() }
            ?: themeObject
                ?.takeIf { it.looksLikeThemeProperties() }
            ?: dataObject
            ?: JsonObject(emptyMap())
        val id = themeObject?.stringOrNull("item_id")
            ?: themeObject?.stringOrNull("id")
            ?: root.stringOrNull("item_id")
            ?: root.stringOrNull("id")
            ?: dataObject?.stringOrNull("item_id")
            ?: dataObject?.stringOrNull("id")
            ?: properties.stringOrNull("item_id")
            ?: properties.stringOrNull("id")
        val name = themeObject?.stringOrNull("name")
            ?: root.stringOrNull("name")
            ?: dataObject?.stringOrNull("name")
            ?: "Bilibili Skin"
        val version = themeObject?.stringOrNull("ver")
            ?: root.stringOrNull("ver")
            ?: properties.stringOrNull("ver")
            ?: dataObject?.stringOrNull("ver")
            ?: "1.0.0"
        return BilibiliSkinTheme(
            id = id,
            name = name,
            version = version,
            color = properties.stringOrNull("color"),
            colorSecondPage = properties.stringOrNull("color_second_page"),
            tailColor = properties.stringOrNull("tail_color")
        )
    }

    private fun looksLikeJson(bytes: ByteArray): Boolean {
        return bytes.decodeToString().trimStart().startsWith("{")
    }

    private fun ByteArray.packageUrlOrNull(): String? {
        val root = runCatching { json.parseToJsonElement(decodeToString()).jsonObject }.getOrNull()
            ?: return null
        val dataObject = root.objectOrNull("data")
        val themeObject = root.resolveThemeObject()
        return themeObject?.stringOrNull("package_url")
            ?: themeObject?.stringOrNull("packageUrl")
            ?: themeObject?.objectOrNull("properties")?.stringOrNull("package_url")
            ?: themeObject?.objectOrNull("properties")?.stringOrNull("packageUrl")
            ?: dataObject?.stringOrNull("package_url")
            ?: dataObject?.stringOrNull("packageUrl")
            ?: root.stringOrNull("package_url")
            ?: root.stringOrNull("packageUrl")
            ?: root.findStringDeep("package_url", "packageUrl")
    }

    private fun buildAssetBytes(packageEntries: Map<String, ByteArray>): Map<String, ByteArray> {
        val assetBytes = linkedMapOf<String, ByteArray>()
        firstExisting(packageEntries, "tail_bg.png", "tail_bg.jpg", "side_bg_bottom.png", "side_bg_bottom.jpg")?.let { (path, bytes) ->
            assetBytes["assets/${path.substringAfterLast("/")}"] = bytes
        }
        firstExisting(packageEntries, "head_bg.jpg", "head_tab_bg.jpg", "side_bg.jpg")?.let { (path, bytes) ->
            assetBytes["assets/${path.substringAfterLast("/")}"] = bytes
        }
        firstExisting(packageEntries, "head_tab_bg.jpg", "head_tab_bg.png")?.let { (path, bytes) ->
            assetBytes["assets/${path.substringAfterLast("/")}"] = bytes
        }
        firstExisting(packageEntries, "side_bg.jpg", "side_bg.png")?.let { (path, bytes) ->
            assetBytes["assets/${path.substringAfterLast("/")}"] = bytes
        }
        firstExisting(packageEntries, "head_myself_bg.jpg", "head_myself_bg.png")?.let { (path, bytes) ->
            assetBytes["assets/${path.substringAfterLast("/")}"] = bytes
        }
        firstExisting(
            packageEntries,
            "head_myself_squared_bg.jpg",
            "head_myself_squared_bg.png"
        )?.let { (path, bytes) ->
            assetBytes["assets/${path.substringAfterLast("/")}"] = bytes
        }
        firstExisting(packageEntries, "tail_icon_channel.png", "tail_icon_channel.jpg")?.let { (path, bytes) ->
            assetBytes["assets/${path.substringAfterLast("/")}"] = bytes
        }
        firstExisting(
            packageEntries,
            "tail_icon_selected_channel.png",
            "tail_icon_selected_channel.jpg"
        )?.let { (path, bytes) ->
            assetBytes["assets/${path.substringAfterLast("/")}"] = bytes
        }
        iconMapping.forEach { (packageStem, _) ->
            firstExisting(packageEntries, "$packageStem.png", "$packageStem.jpg")?.let { (path, bytes) ->
                assetBytes["assets/${path.substringAfterLast("/")}"] = bytes
            }
        }
        selectedIconMapping.forEach { (packageStem, _) ->
            firstExisting(packageEntries, "$packageStem.png", "$packageStem.jpg")?.let { (path, bytes) ->
                assetBytes["assets/${path.substringAfterLast("/")}"] = bytes
            }
        }
        return assetBytes
    }

    private fun buildManifest(
        theme: BilibiliSkinTheme,
        assetPaths: Set<String>
    ): UiSkinManifest {
        val iconPaths = iconMapping.mapNotNull { (packageStem, hostKey) ->
            val path = assetPaths.firstOrNull {
                it.endsWith("$packageStem.png") || it.endsWith("$packageStem.jpg")
            }
            path?.let { hostKey to it }
        }.toMap() + selectedIconMapping.mapNotNull { (packageStem, hostKey) ->
            val path = assetPaths.firstOrNull {
                it.endsWith("$packageStem.png") || it.endsWith("$packageStem.jpg")
            }
            path?.let { hostKey to it }
        }.toMap()
        return UiSkinManifest(
            formatVersion = 1,
            skinId = "local.bilibili_skin.${theme.safeSkinIdSegment()}",
            displayName = theme.name,
            version = theme.version,
            apiVersion = 1,
            author = "BiliPai local converter",
            surfaces = setOf(UiSkinSurface.HOME_BOTTOM_BAR, UiSkinSurface.HOME_TOP_CHROME),
            assets = UiSkinAssets(
                bottomBarTrim = assetPaths.firstOrNull {
                    it.endsWith("tail_bg.png") ||
                        it.endsWith("tail_bg.jpg") ||
                        it.endsWith("side_bg_bottom.png") ||
                        it.endsWith("side_bg_bottom.jpg")
                },
                topAtmosphere = assetPaths.firstOrNull {
                    it.endsWith("head_bg.jpg") || it.endsWith("head_tab_bg.jpg")
                },
                homeTopTabBackground = assetPaths.firstOrNull {
                    it.endsWith("head_tab_bg.jpg") || it.endsWith("head_tab_bg.png")
                }?.takeUnless { path ->
                    path == assetPaths.firstOrNull {
                        it.endsWith("head_bg.jpg") || it.endsWith("head_tab_bg.jpg")
                    }
                },
                homeSideBackground = assetPaths.firstOrNull {
                    it.endsWith("side_bg.jpg") || it.endsWith("side_bg.png")
                },
                homeProfileBackground = assetPaths.firstOrNull {
                    it.endsWith("head_myself_bg.jpg") || it.endsWith("head_myself_bg.png")
                },
                homeProfileSquaredBackground = assetPaths.firstOrNull {
                    it.endsWith("head_myself_squared_bg.jpg") || it.endsWith("head_myself_squared_bg.png")
                },
                homeChannelIcon = assetPaths.firstOrNull {
                    it.endsWith("tail_icon_channel.png") || it.endsWith("tail_icon_channel.jpg")
                },
                homeChannelSelectedIcon = assetPaths.firstOrNull {
                    it.endsWith("tail_icon_selected_channel.png") ||
                        it.endsWith("tail_icon_selected_channel.jpg")
                },
                bottomBarIcons = iconPaths
            ),
            colors = UiSkinColorTokens(
                bottomBarTrimTint = theme.tailColor.validColorOrNull(),
                topAtmosphereTint = (theme.colorSecondPage ?: theme.color).validColorOrNull(),
                searchCapsuleTint = theme.color.validColorOrNull()
            ),
            styleSourceName = BILIBILI_SKIN_SOURCE_NAME,
            styleSourceUrl = BILIBILI_SKIN_SOURCE_URL,
            licenseNote = BILIBILI_SKIN_LICENSE_NOTE,
            communityShareable = false,
            containsOfficialAssets = true
        )
    }

    private fun buildBpskinPackage(
        manifest: UiSkinManifest,
        assetBytesByPath: Map<String, ByteArray>
    ): ByteArray {
        return ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putStableEntry("skin-manifest.json")
                zip.write(json.encodeToString(UiSkinManifest.serializer(), manifest).toByteArray())
                zip.closeEntry()
                assetBytesByPath.forEach { (path, bytes) ->
                    zip.putStableEntry(path)
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
            output.toByteArray()
        }
    }

    private fun scanZip(
        inputBytes: ByteArray,
        illegalPathMessage: String
    ): Map<String, ByteArray> {
        val entries = linkedMapOf<String, ByteArray>()
        var entryCount = 0
        var totalBytes = 0
        ZipInputStream(ByteArrayInputStream(inputBytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory) {
                    entryCount += 1
                    if (entryCount > MAX_THEME_ENTRY_COUNT) {
                        throw IllegalArgumentException("装扮存档文件数量超过 $MAX_THEME_ENTRY_COUNT")
                    }
                    val normalizedName = normalizeEntryName(entry.name, illegalPathMessage)
                    val bytes = zip.readBytes()
                    totalBytes += bytes.size
                    if (totalBytes > MAX_THEME_TOTAL_BYTES) {
                        throw IllegalArgumentException("装扮存档解压后内容超过 $MAX_THEME_TOTAL_BYTES 字节")
                    }
                    if (entries.put(normalizedName, bytes) != null) {
                        throw IllegalArgumentException("装扮存档包含重复路径: ${entry.name}")
                    }
                }
                zip.closeEntry()
            }
        }
        return entries
    }

    private fun normalizeEntryName(rawName: String, illegalPathMessage: String): String {
        if (rawName.isBlank() || rawName.startsWith("/") || rawName.startsWith("\\")) {
            throw IllegalArgumentException("$illegalPathMessage: $rawName")
        }
        val normalized = rawName
            .replace('\\', '/')
            .split('/')
            .filter { it.isNotEmpty() && it != "." }
            .also { parts ->
                if (parts.any { it == ".." }) {
                    throw IllegalArgumentException("$illegalPathMessage: $rawName")
                }
            }
            .joinToString("/")
        if (normalized.isBlank()) {
            throw IllegalArgumentException("$illegalPathMessage: $rawName")
        }
        return normalized
    }

    private fun firstExisting(
        entries: Map<String, ByteArray>,
        vararg names: String
    ): Pair<String, ByteArray>? {
        names.forEach { name ->
            entries.entries.firstOrNull { it.key.substringAfterLast("/") == name }?.let {
                return it.key to it.value
            }
        }
        return null
    }

    private fun JsonObject.objectOrNull(key: String): JsonObject? {
        return get(key)?.runCatching { jsonObject }?.getOrNull()
    }

    private fun JsonObject.stringOrNull(key: String): String? {
        val primitive = get(key)?.jsonPrimitive ?: return null
        return primitive.contentOrNull ?: primitive.booleanOrNull?.toString()
    }

    private fun JsonObject.resolveThemeObject(): JsonObject? {
        val dataObject = objectOrNull("data")
        val knownCandidates = listOfNotNull(
            dataObject?.objectOrNull("user_equip"),
            objectOrNull("user_equip"),
            dataObject?.objectOrNull("skin_suit")?.objectOrNull("item"),
            objectOrNull("skin_suit")?.objectOrNull("item"),
            dataObject?.objectOrNull("item"),
            objectOrNull("item"),
            dataObject?.objectOrNull("skin_suit"),
            objectOrNull("skin_suit"),
            dataObject
        )
        return knownCandidates.firstOrNull { it.looksLikeThemeObject() }
            ?: findObjectDeep { it.looksLikeThemeObject() }
    }

    private fun JsonObject.looksLikeThemeObject(): Boolean {
        return stringOrNull("item_id") != null ||
            stringOrNull("id") != null ||
            stringOrNull("name") != null ||
            objectOrNull("properties") != null
    }

    private fun JsonObject.looksLikeThemeProperties(): Boolean {
        return stringOrNull("color") != null ||
            stringOrNull("color_second_page") != null ||
            stringOrNull("tail_color") != null ||
            stringOrNull("ver") != null ||
            stringOrNull("package_url") != null ||
            stringOrNull("packageUrl") != null
    }

    private fun JsonObject.findStringDeep(vararg keys: String): String? {
        keys.forEach { key ->
            stringOrNull(key)?.let { return it }
        }
        values.forEach { value ->
            when (value) {
                is JsonObject -> value.findStringDeep(*keys)?.let { return it }
                else -> runCatching { value.jsonArray }.getOrNull()?.forEach { item ->
                    (item as? JsonObject)?.findStringDeep(*keys)?.let { return it }
                }
            }
        }
        return null
    }

    private fun JsonObject.findObjectDeep(predicate: (JsonObject) -> Boolean): JsonObject? {
        if (predicate(this)) return this
        values.forEach { value ->
            when (value) {
                is JsonObject -> value.findObjectDeep(predicate)?.let { return it }
                else -> runCatching { value.jsonArray }.getOrNull()?.forEach { item ->
                    (item as? JsonObject)?.findObjectDeep(predicate)?.let { return it }
                }
            }
        }
        return null
    }

    private fun String.parentName(): String {
        return substringBeforeLast("/", "").substringAfterLast("/")
    }

    private fun String?.validColorOrNull(): String? {
        val value = this?.trim() ?: return null
        return if (Regex("#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?").matches(value)) value else null
    }

    private fun BilibiliSkinTheme.safeSkinIdSegment(): String {
        val nameSlug = name
            .replace(Regex("[^A-Za-z0-9_.-]"), "_")
            .trim('_')
            .lowercase()
        val idSlug = id
            ?.replace(Regex("[^A-Za-z0-9_.-]"), "_")
            ?.trim('_')
            ?.lowercase()
        return idSlug?.takeIf { it.isNotBlank() }
            ?: nameSlug.takeIf { it.isNotBlank() }
            ?: "theme"
    }
}

private data class BilibiliSkinTheme(
    val id: String?,
    val name: String,
    val version: String,
    val color: String?,
    val colorSecondPage: String?,
    val tailColor: String?
)

private fun ZipOutputStream.putStableEntry(name: String) {
    putNextEntry(
        ZipEntry(name).apply {
            time = 0L
        }
    )
}
