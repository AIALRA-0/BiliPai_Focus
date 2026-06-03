package com.android.purebilibili.core.plugin.skin

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UiSkinPackageReaderTest {

    @Test
    fun validPackage_readsManifestHashAndDeclaredImageAssetsWithoutExecutingCode() {
        val manifest = sampleManifest()
        val bytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(manifest).toByteArray(),
            "assets/bottom_trim.png" to pngBytes(),
            "assets/top_atmosphere.webp" to webpBytes()
        )

        val preview = UiSkinPackageReader.preview(bytes).getOrThrow()

        assertEquals(manifest, preview.manifest)
        assertEquals(sha256Hex(bytes), preview.packageSha256)
        assertEquals(
            listOf(
                UiSkinAssetEntry("assets/bottom_trim.png", UiSkinAssetType.PNG, pngBytes().size.toLong()),
                UiSkinAssetEntry("assets/top_atmosphere.webp", UiSkinAssetType.WEBP, webpBytes().size.toLong())
            ),
            preview.assetEntries
        )
    }

    @Test
    fun legacyPackageWithoutCommunityMetadata_stillImports() {
        val manifestJson = """
            {
              "formatVersion": 1,
              "skinId": "dev.example.legacy",
              "displayName": "旧版皮肤",
              "version": "1.0.0",
              "apiVersion": 1,
              "surfaces": ["HOME_BOTTOM_BAR"],
              "assets": {"bottomBarTrim": "assets/bottom_trim.png"}
            }
        """.trimIndent().toByteArray()
        val bytes = skinPackage(
            "skin-manifest.json" to manifestJson,
            "assets/bottom_trim.png" to pngBytes()
        )

        val preview = UiSkinPackageReader.preview(bytes).getOrThrow()

        assertEquals("dev.example.legacy", preview.manifest.skinId)
        assertEquals(null, preview.manifest.styleSourceName)
        assertEquals(false, preview.manifest.communityShareable)
    }

    @Test
    fun communityMetadata_readsSourceLicenseAndShareState() {
        val manifest = sampleManifest(
            styleSourceName = "KimmyXYC/bilibili-skin",
            styleSourceUrl = "https://github.com/KimmyXYC/bilibili-skin",
            licenseNote = "原创重绘资源，可作为社区包分享",
            communityShareable = true,
            containsOfficialAssets = false
        )
        val bytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(manifest).toByteArray(),
            "assets/bottom_trim.png" to pngBytes(),
            "assets/top_atmosphere.webp" to webpBytes()
        )

        val preview = UiSkinPackageReader.preview(bytes).getOrThrow()

        assertEquals("KimmyXYC/bilibili-skin", preview.manifest.styleSourceName)
        assertEquals("https://github.com/KimmyXYC/bilibili-skin", preview.manifest.styleSourceUrl)
        assertEquals("原创重绘资源，可作为社区包分享", preview.manifest.licenseNote)
        assertEquals(true, preview.manifest.communityShareable)
        assertEquals(false, preview.manifest.containsOfficialAssets)
    }

    @Test
    fun shareableCommunityPackageWithoutLicenseNote_rejectsSkin() {
        val manifest = sampleManifest(
            licenseNote = "",
            communityShareable = true
        )
        val bytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(manifest).toByteArray(),
            "assets/bottom_trim.png" to pngBytes(),
            "assets/top_atmosphere.webp" to webpBytes()
        )

        val error = UiSkinPackageReader.preview(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("社区可分享皮肤包必须声明 licenseNote", error.message)
    }

    @Test
    fun missingManifest_rejectsSkinBeforeInstallDecision() {
        val bytes = skinPackage("assets/bottom_trim.png" to pngBytes())

        val error = UiSkinPackageReader.preview(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("皮肤包缺少 skin-manifest.json", error.message)
    }

    @Test
    fun pathTraversalEntry_rejectsSkin() {
        val bytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(sampleManifest()).toByteArray(),
            "../escape.png" to pngBytes()
        )

        val error = UiSkinPackageReader.preview(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("皮肤包包含非法路径: ../escape.png", error.message)
    }

    @Test
    fun oversizedManifest_rejectsSkin() {
        val bytes = skinPackage(
            "skin-manifest.json" to ByteArray(64 * 1024 + 1) { '{'.code.toByte() }
        )

        val error = UiSkinPackageReader.preview(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("skin-manifest.json 超过 65536 字节", error.message)
    }

    @Test
    fun oversizedUncompressedPackage_rejectsSkin() {
        val bytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(sampleManifest()).toByteArray(),
            "assets/huge.png" to ByteArray(16 * 1024 * 1024 + 1)
        )

        val error = UiSkinPackageReader.preview(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("皮肤包解压后内容超过 16777216 字节", error.message)
    }

    @Test
    fun unknownSurface_rejectsSkinWithReadableMessage() {
        val manifestJson = """
            {
              "formatVersion": 1,
              "skinId": "dev.example.unknown",
              "displayName": "未知界面",
              "version": "1.0.0",
              "apiVersion": 1,
              "surfaces": ["HOME_BOTTOM_BAR", "PLAYER_OVERLAY"],
              "assets": {"bottomBarTrim": "assets/bottom_trim.png"}
            }
        """.trimIndent().toByteArray()
        val bytes = skinPackage(
            "skin-manifest.json" to manifestJson,
            "assets/bottom_trim.png" to pngBytes()
        )

        val error = UiSkinPackageReader.preview(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("皮肤包声明了未知界面: PLAYER_OVERLAY", error.message)
    }

    @Test
    fun damagedDeclaredImage_rejectsSkin() {
        val bytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(sampleManifest()).toByteArray(),
            "assets/bottom_trim.png" to byteArrayOf(1, 2, 3),
            "assets/top_atmosphere.webp" to webpBytes()
        )

        val error = UiSkinPackageReader.preview(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("皮肤资源 assets/bottom_trim.png 不是受支持的图片格式", error.message)
    }

    @Test
    fun duplicateAssetPath_rejectsSkin() {
        val manifest = sampleManifest(
            assets = UiSkinAssets(
                bottomBarTrim = "assets/shared.png",
                topAtmosphere = "assets/shared.png"
            )
        )
        val bytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(manifest).toByteArray(),
            "assets/shared.png" to pngBytes()
        )

        val error = UiSkinPackageReader.preview(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("皮肤包重复声明资源: assets/shared.png", error.message)
    }

    @Test
    fun nonAssetPayload_rejectsSkinCodeLikeEntries() {
        val bytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(sampleManifest()).toByteArray(),
            "classes.dex" to byteArrayOf(0x64, 0x65, 0x78),
            "assets/bottom_trim.png" to pngBytes()
        )

        val error = UiSkinPackageReader.preview(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("皮肤包只能包含 skin-manifest.json 和 assets/ 下的资源", error.message)
    }

    @Test
    fun bilibiliSkinThemeArchive_convertsToPreviewableBpskin() {
        val bytes = bilibiliThemeArchive(
            "萧逸/萧逸.json" to convertedThemeJson().toByteArray(),
            "萧逸/萧逸_package.zip" to skinPackage(
                "tail_bg.png" to pngBytes(),
                "head_bg.jpg" to jpegBytes(),
                "tail_icon_main.png" to pngBytes(),
                "tail_icon_selected_main.png" to pngBytes(),
                "tail_icon_dynamic.png" to pngBytes(),
                "tail_icon_selected_dynamic.png" to pngBytes(),
                "tail_icon_shop.png" to pngBytes(),
                "tail_icon_selected_shop.png" to pngBytes(),
                "tail_icon_myself.png" to pngBytes()
            )
        )

        val importPackage = UiSkinImportPackageResolver.resolve(bytes).getOrThrow()
        val preview = UiSkinPackageReader.preview(importPackage.packageBytes).getOrThrow()

        assertEquals(UiSkinImportSource.BILIBILI_SKIN_ARCHIVE, importPackage.source)
        assertEquals("local.bilibili_skin.34219", preview.manifest.skinId)
        assertEquals("萧逸", preview.manifest.displayName)
        assertEquals("1644150184", preview.manifest.version)
        assertEquals("assets/tail_bg.png", preview.manifest.assets.bottomBarTrim)
        assertEquals("assets/head_bg.jpg", preview.manifest.assets.topAtmosphere)
        assertEquals("assets/tail_icon_main.png", preview.manifest.assets.bottomBarIcons["home"])
        assertEquals("assets/tail_icon_selected_main.png", preview.manifest.assets.bottomBarIcons["home_selected"])
        assertEquals("assets/tail_icon_dynamic.png", preview.manifest.assets.bottomBarIcons["following"])
        assertEquals(
            "assets/tail_icon_selected_dynamic.png",
            preview.manifest.assets.bottomBarIcons["following_selected"]
        )
        assertEquals("assets/tail_icon_shop.png", preview.manifest.assets.bottomBarIcons["member"])
        assertEquals("assets/tail_icon_selected_shop.png", preview.manifest.assets.bottomBarIcons["member_selected"])
        assertEquals("assets/tail_icon_myself.png", preview.manifest.assets.bottomBarIcons["profile"])
        assertEquals("#6bb4ff", preview.manifest.colors.bottomBarTrimTint)
        assertEquals("#4e536a", preview.manifest.colors.topAtmosphereTint)
        assertEquals("#ffffff", preview.manifest.colors.searchCapsuleTint)
        assertEquals("KimmyXYC/bilibili-skin", preview.manifest.styleSourceName)
        assertEquals(false, preview.manifest.communityShareable)
        assertEquals(true, preview.manifest.containsOfficialAssets)
        assertTrue(preview.manifest.licenseNote.orEmpty().contains("本地"))
    }

    @Test
    fun standardBpskinImport_resolvesWithoutConversion() {
        val packageBytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(sampleManifest()).toByteArray(),
            "assets/bottom_trim.png" to pngBytes(),
            "assets/top_atmosphere.webp" to webpBytes()
        )

        val importPackage = UiSkinImportPackageResolver.resolve(packageBytes).getOrThrow()

        assertEquals(UiSkinImportSource.BP_SKIN, importPackage.source)
        assertEquals(packageBytes.toList(), importPackage.packageBytes.toList())
    }

    @Test
    fun bilibiliSkinThemeArchive_supportsRawGarbResponseJson() {
        val bytes = bilibiliThemeArchive(
            "萧逸/个性装扮.json" to rawGarbThemeJson().toByteArray(),
            "萧逸/萧逸_package.zip" to skinPackage(
                "tail_bg.png" to pngBytes(),
                "head_tab_bg.jpg" to jpegBytes()
            )
        )

        val importPackage = UiSkinImportPackageResolver.resolve(bytes).getOrThrow()
        val preview = UiSkinPackageReader.preview(importPackage.packageBytes).getOrThrow()

        assertEquals("local.bilibili_skin.34219", preview.manifest.skinId)
        assertEquals("萧逸", preview.manifest.displayName)
        assertEquals("assets/tail_bg.png", preview.manifest.assets.bottomBarTrim)
        assertEquals("assets/head_tab_bg.jpg", preview.manifest.assets.topAtmosphere)
    }

    @Test
    fun bilibiliSkinArchiveWithSkinSuitAndPackageUrlZip_readsOfficialNameAndItemId() {
        val bytes = bilibiliThemeArchive(
            "skin/skin_suit.json" to luotianyiSkinSuitJson().toByteArray(),
            "skin/package_urld1465d44aa720226f5b04d05348950c753552f10.zip" to skinPackage(
                "tail_bg.png" to pngBytes(),
                "head_bg.jpg" to jpegBytes(),
                "head_tab_bg.png" to pngBytes(),
                "tail_icon_main.png" to pngBytes(),
                "tail_icon_selected_main.png" to pngBytes(),
                "tail_icon_dynamic.png" to pngBytes(),
                "tail_icon_selected_dynamic.png" to pngBytes(),
                "tail_icon_shop.png" to pngBytes(),
                "tail_icon_selected_shop.png" to pngBytes(),
                "tail_icon_myself.png" to pngBytes(),
                "tail_icon_selected_myself.png" to pngBytes()
            )
        )

        val importPackage = UiSkinImportPackageResolver.resolve(bytes).getOrThrow()
        val preview = UiSkinPackageReader.preview(importPackage.packageBytes).getOrThrow()

        assertEquals(UiSkinImportSource.BILIBILI_SKIN_ARCHIVE, importPackage.source)
        assertEquals("local.bilibili_skin.1770793106001", preview.manifest.skinId)
        assertEquals("洛天依拜年纪个性主题", preview.manifest.displayName)
        assertEquals("1774972800", preview.manifest.version)
        assertEquals("assets/tail_bg.png", preview.manifest.assets.bottomBarTrim)
        assertEquals("assets/head_bg.jpg", preview.manifest.assets.topAtmosphere)
        assertEquals("assets/tail_icon_main.png", preview.manifest.assets.bottomBarIcons["home"])
        assertEquals("assets/tail_icon_selected_main.png", preview.manifest.assets.bottomBarIcons["home_selected"])
        assertEquals("#BEFFE4", preview.manifest.colors.bottomBarTrimTint)
        assertEquals("#51A2F0", preview.manifest.colors.topAtmosphereTint)
        assertEquals("#000000", preview.manifest.colors.searchCapsuleTint)
    }

    @Test
    fun bilibiliSkinDirectOfficialUserEquipJson_downloadsPackageUrlAndConverts() {
        val packageBytes = skinPackage(
            "tail_bg.png" to pngBytes(),
            "head_bg.jpg" to jpegBytes()
        )

        val importPackage = UiSkinImportPackageResolver.resolve(
            inputBytes = officialUserEquipJson().toByteArray(),
            remotePackageFetcher = { url ->
                assertEquals("https://i0.hdslb.com/bfs/garb/theme_package.zip", url)
                packageBytes
            }
        ).getOrThrow()
        val preview = UiSkinPackageReader.preview(importPackage.packageBytes).getOrThrow()

        assertEquals(UiSkinImportSource.BILIBILI_SKIN_ARCHIVE, importPackage.source)
        assertEquals("local.bilibili_skin.778899", preview.manifest.skinId)
        assertEquals("官方个性主题", preview.manifest.displayName)
        assertEquals("assets/tail_bg.png", preview.manifest.assets.bottomBarTrim)
        assertEquals("assets/head_bg.jpg", preview.manifest.assets.topAtmosphere)
        assertEquals("#223344", preview.manifest.colors.bottomBarTrimTint)
    }

    @Test
    fun bilibiliSkinDirectNestedJson_downloadsPackageUrlAndConverts() {
        val packageBytes = skinPackage(
            "tail_bg.png" to pngBytes(),
            "head_tab_bg.jpg" to jpegBytes()
        )

        val importPackage = UiSkinImportPackageResolver.resolve(
            inputBytes = nestedOfficialSkinJson().toByteArray(),
            remotePackageFetcher = { url ->
                assertEquals("https://i0.hdslb.com/bfs/garb/nested_theme_package.zip", url)
                packageBytes
            }
        ).getOrThrow()
        val preview = UiSkinPackageReader.preview(importPackage.packageBytes).getOrThrow()

        assertEquals(UiSkinImportSource.BILIBILI_SKIN_ARCHIVE, importPackage.source)
        assertEquals("local.bilibili_skin.998877", preview.manifest.skinId)
        assertEquals("嵌套官方主题", preview.manifest.displayName)
        assertEquals("assets/tail_bg.png", preview.manifest.assets.bottomBarTrim)
        assertEquals("assets/head_tab_bg.jpg", preview.manifest.assets.topAtmosphere)
    }

    @Test
    fun bilibiliSkinDirectJsonWithoutFetcherReturnsReadablePackageUrlError() {
        val error = UiSkinImportPackageResolver.resolve(
            inputBytes = officialUserEquipJson().toByteArray()
        ).exceptionOrNull()

        assertNotNull(error)
        assertEquals("皮肤 JSON 需要下载 package_url，请检查网络后重试", error.message)
    }

    @Test
    fun bilibiliSkinArchiveConversion_isStableForSameInput() {
        val bytes = bilibiliThemeArchive(
            "skin/skin_suit.json" to luotianyiSkinSuitJson().toByteArray(),
            "skin/package_urlabcdef.zip" to skinPackage(
                "tail_bg.png" to pngBytes(),
                "head_bg.jpg" to jpegBytes()
            )
        )

        val first = UiSkinImportPackageResolver.resolve(bytes).getOrThrow().packageBytes
        Thread.sleep(1100)
        val second = UiSkinImportPackageResolver.resolve(bytes).getOrThrow().packageBytes

        assertEquals(first.toList(), second.toList())
        assertEquals(
            UiSkinPackageReader.preview(first).getOrThrow().packageSha256,
            UiSkinPackageReader.preview(second).getOrThrow().packageSha256
        )
    }

    @Test
    fun bilibiliSkinDirectPackageZip_convertsWithoutThemeJson() {
        val bytes = skinPackage(
            "tail_bg.png" to pngBytes(),
            "head_bg.jpg" to jpegBytes(),
            "head_tab_bg.jpg" to jpegBytes(),
            "side_bg.jpg" to jpegBytes(),
            "head_myself_bg.jpg" to jpegBytes(),
            "head_myself_squared_bg.jpg" to jpegBytes(),
            "tail_icon_main.png" to pngBytes(),
            "tail_icon_selected_main.png" to pngBytes(),
            "tail_icon_dynamic.png" to pngBytes(),
            "tail_icon_channel.png" to pngBytes(),
            "tail_icon_selected_channel.png" to pngBytes()
        )

        val importPackage = UiSkinImportPackageResolver.resolve(bytes).getOrThrow()
        val preview = UiSkinPackageReader.preview(importPackage.packageBytes).getOrThrow()

        assertEquals(UiSkinImportSource.BILIBILI_SKIN_ARCHIVE, importPackage.source)
        assertEquals("local.bilibili_skin.local_package", preview.manifest.skinId)
        assertEquals("本地装扮资源包", preview.manifest.displayName)
        assertEquals("assets/tail_bg.png", preview.manifest.assets.bottomBarTrim)
        assertEquals("assets/head_bg.jpg", preview.manifest.assets.topAtmosphere)
        assertEquals("assets/head_tab_bg.jpg", preview.manifest.assets.homeTopTabBackground)
        assertEquals("assets/side_bg.jpg", preview.manifest.assets.homeSideBackground)
        assertEquals("assets/head_myself_bg.jpg", preview.manifest.assets.homeProfileBackground)
        assertEquals("assets/head_myself_squared_bg.jpg", preview.manifest.assets.homeProfileSquaredBackground)
        assertEquals("assets/tail_icon_main.png", preview.manifest.assets.bottomBarIcons["home"])
        assertEquals("assets/tail_icon_selected_main.png", preview.manifest.assets.bottomBarIcons["home_selected"])
        assertEquals("assets/tail_icon_channel.png", preview.manifest.assets.homeChannelIcon)
        assertEquals("assets/tail_icon_selected_channel.png", preview.manifest.assets.homeChannelSelectedIcon)
        assertEquals(null, preview.manifest.assets.bottomBarIcons["story"])
        assertEquals(null, preview.manifest.assets.bottomBarIcons["story_selected"])
        assertEquals(false, preview.manifest.communityShareable)
        assertEquals(true, preview.manifest.containsOfficialAssets)
    }

    @Test
    fun bilibiliSkinThemeArchive_missingThemeJsonReturnsReadableError() {
        val bytes = bilibiliThemeArchive(
            "萧逸/萧逸_package.zip" to skinPackage("tail_bg.png" to pngBytes())
        )

        val error = UiSkinImportPackageResolver.resolve(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("装扮存档缺少主题 JSON", error.message)
    }

    @Test
    fun bilibiliSkinThemeArchive_missingPackageZipReturnsReadableError() {
        val bytes = bilibiliThemeArchive(
            "萧逸/萧逸.json" to convertedThemeJson().toByteArray()
        )

        val error = UiSkinImportPackageResolver.resolve(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("装扮存档缺少 _package.zip", error.message)
    }

    @Test
    fun bilibiliSkinThemeArchive_rejectsInnerZipPathTraversal() {
        val bytes = bilibiliThemeArchive(
            "萧逸/萧逸.json" to convertedThemeJson().toByteArray(),
            "萧逸/萧逸_package.zip" to skinPackage("../tail_bg.png" to pngBytes())
        )

        val error = UiSkinImportPackageResolver.resolve(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("装扮资源包包含非法路径: ../tail_bg.png", error.message)
    }

    private fun sampleManifest(
        assets: UiSkinAssets = UiSkinAssets(
            bottomBarTrim = "assets/bottom_trim.png",
            topAtmosphere = "assets/top_atmosphere.webp"
        ),
        styleSourceName: String? = null,
        styleSourceUrl: String? = null,
        licenseNote: String? = null,
        communityShareable: Boolean = false,
        containsOfficialAssets: Boolean = false
    ): UiSkinManifest {
        return UiSkinManifest(
            formatVersion = 1,
            skinId = "dev.example.winter_cloud",
            displayName = "冬日云朵",
            version = "1.0.0",
            apiVersion = 1,
            author = "BiliPai",
            surfaces = setOf(UiSkinSurface.HOME_BOTTOM_BAR, UiSkinSurface.HOME_TOP_CHROME),
            assets = assets,
            colors = UiSkinColorTokens(
                bottomBarTrimTint = "#EAF8FF",
                topAtmosphereTint = "#DFF5FF"
            ),
            styleSourceName = styleSourceName,
            styleSourceUrl = styleSourceUrl,
            licenseNote = licenseNote,
            communityShareable = communityShareable,
            containsOfficialAssets = containsOfficialAssets
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

    private fun bilibiliThemeArchive(vararg entries: Pair<String, ByteArray>): ByteArray {
        return skinPackage(*entries)
    }

    private fun pngBytes(): ByteArray {
        return byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D
        )
    }

    private fun webpBytes(): ByteArray {
        return byteArrayOf(
            0x52, 0x49, 0x46, 0x46,
            0x04, 0x00, 0x00, 0x00,
            0x57, 0x45, 0x42, 0x50
        )
    }

    private fun jpegBytes(): ByteArray {
        return byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x00)
    }

    private fun convertedThemeJson(): String {
        return """
            {
              "id": "34219",
              "name": "萧逸",
              "ver": "1644150184",
              "data": {
                "color": "#ffffff",
                "color_second_page": "#4e536a",
                "tail_color": "#6bb4ff"
              }
            }
        """.trimIndent()
    }

    private fun rawGarbThemeJson(): String {
        return """
            {
              "code": 0,
              "data": {
                "item_id": 34219,
                "name": "萧逸",
                "properties": {
                  "ver": "1644150184",
                  "color": "#ffffff",
                  "color_second_page": "#4e536a",
                  "tail_color": "#6bb4ff"
                }
              }
            }
        """.trimIndent()
    }

    private fun luotianyiSkinSuitJson(): String {
        return """
            {
              "item_id": 1770793106001,
              "name": "洛天依拜年纪个性主题",
              "properties": {
                "ver": "1774972800",
                "color": "#000000",
                "color_second_page": "#51A2F0",
                "tail_color": "#BEFFE4"
              }
            }
        """.trimIndent()
    }

    private fun officialUserEquipJson(): String {
        return """
            {
              "code": 0,
              "data": {
                "user_equip": {
                  "item_id": 778899,
                  "name": "官方个性主题",
                  "package_url": "https://i0.hdslb.com/bfs/garb/theme_package.zip",
                  "package_md5": "abc",
                  "properties": {
                    "ver": "1774972800",
                    "color": "#112233",
                    "color_second_page": "#445566",
                    "tail_color": "#223344"
                  }
                }
              }
            }
        """.trimIndent()
    }

    private fun nestedOfficialSkinJson(): String {
        return """
            {
              "code": 0,
              "data": {
                "skin_suit": {
                  "item": {
                    "item_id": 998877,
                    "name": "嵌套官方主题",
                    "properties": {
                      "ver": "1774972801",
                      "package_url": "https://i0.hdslb.com/bfs/garb/nested_theme_package.zip",
                      "color": "#334455",
                      "color_second_page": "#556677",
                      "tail_color": "#778899"
                    }
                  }
                }
              }
            }
        """.trimIndent()
    }

    private fun sha256Hex(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }
}
