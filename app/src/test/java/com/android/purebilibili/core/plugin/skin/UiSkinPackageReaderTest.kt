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
    fun validPackage_acceptsDeclaredProfileMp4Asset() {
        val manifest = sampleManifest(
            assets = UiSkinAssets(
                homeProfileVideoBackground = "assets/head_myself_mp4_bg.mp4"
            )
        )
        val bytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(manifest).toByteArray(),
            "assets/head_myself_mp4_bg.mp4" to mp4Bytes()
        )

        val preview = UiSkinPackageReader.preview(bytes).getOrThrow()

        assertEquals(
            listOf(
                UiSkinAssetEntry(
                    "assets/head_myself_mp4_bg.mp4",
                    UiSkinAssetType.MP4,
                    mp4Bytes().size.toLong()
                )
            ),
            preview.assetEntries
        )
    }

    @Test
    fun validPackage_acceptsDeclaredLottieJsonAssetsForLikeAndProgress() {
        val manifest = sampleManifest(
            assets = UiSkinAssets(
                likeEffectAnimation = "assets/like_effect.json",
                playerProgressIcon = "assets/progress_icon.json",
            )
        ).copy(
            surfaces = setOf(UiSkinSurface.LIKE_EFFECT, UiSkinSurface.PLAYER_PROGRESS)
        )
        val lottieBytes = "{\"v\":\"5.7.0\",\"layers\":[]}".toByteArray()
        val bytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(manifest).toByteArray(),
            "assets/like_effect.json" to lottieBytes,
            "assets/progress_icon.json" to lottieBytes,
        )

        val preview = UiSkinPackageReader.preview(bytes).getOrThrow()

        assertEquals(2, preview.assetEntries.size)
        assertTrue(preview.assetEntries.all { it.type == UiSkinAssetType.JSON })
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
            "assets/huge.png" to ByteArray(32 * 1024 * 1024 + 1)
        )

        val error = UiSkinPackageReader.preview(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("皮肤包解压后内容超过 33554432 字节", error.message)
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
        assertEquals("皮肤资源 assets/bottom_trim.png 不是受支持的图片或 MP4 格式", error.message)
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
                "side_bg_bottom.png" to pngBytes(),
                "head_bg.jpg" to jpegBytes(),
                "head_tab_bg.jpg" to jpegBytes(),
                "side_bg.jpg" to jpegBytes(),
                "head_myself_bg.jpg" to jpegBytes(),
                "head_myself_squared_bg.jpg" to jpegBytes(),
                "head_myself_mp4_bg.mp4" to mp4Bytes(),
                "tail_icon_pub_btn_bg.png" to pngBytes(),
                "tail_icon_selected_pub_btn_bg.png" to pngBytes(),
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
        assertEquals("assets/side_bg_bottom.png", preview.manifest.assets.drawerBottomTrim)
        assertEquals("assets/head_bg.jpg", preview.manifest.assets.topAtmosphere)
        assertEquals("assets/head_tab_bg.jpg", preview.manifest.assets.homeTopTabBackground)
        assertEquals("assets/side_bg.jpg", preview.manifest.assets.homeSideBackground)
        assertEquals("assets/head_myself_bg.jpg", preview.manifest.assets.homeProfileBackground)
        assertEquals(
            "assets/head_myself_squared_bg.jpg",
            preview.manifest.assets.homeProfileSquaredBackground
        )
        assertEquals(
            "assets/head_myself_mp4_bg.mp4",
            preview.manifest.assets.homeProfileVideoBackground
        )
        assertEquals("assets/tail_icon_pub_btn_bg.png", preview.manifest.assets.dynamicPublishIcon)
        assertEquals(
            "assets/tail_icon_selected_pub_btn_bg.png",
            preview.manifest.assets.dynamicPublishSelectedIcon
        )
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
        assertEquals("#778899", preview.manifest.colors.bottomBarIconTint)
        assertEquals("#AABBCC", preview.manifest.colors.bottomBarIconDarkTint)
        assertEquals("#336699", preview.manifest.colors.bottomBarSelectedTint)
        assertEquals("#654321", preview.manifest.colors.bottomBarSelectedDarkTint)
        assertEquals("#4e536a", preview.manifest.colors.topAtmosphereTint)
        assertEquals("#ffffff", preview.manifest.colors.searchCapsuleTint)
        assertEquals("#112233", preview.manifest.colors.sideBackgroundTint)
        assertEquals("#FFFFFF", preview.manifest.colors.dynamicPublishIconTint)
        assertEquals("#446688", preview.manifest.colors.dynamicPublishShadeTop)
        assertEquals("#224466", preview.manifest.colors.dynamicPublishShadeBottom)
        assertEquals("light", preview.manifest.colors.colorMode)
        assertEquals(true, preview.manifest.motion.bottomBarIconAnimated)
        assertEquals("once", preview.manifest.motion.bottomBarIconAnimationMode)
        assertEquals("img", preview.manifest.motion.bottomBarIconMode)
        assertEquals("loop", preview.manifest.motion.profileVideoPlayMode)
        assertEquals(
            setOf(
                UiSkinSurface.HOME_BOTTOM_BAR,
                UiSkinSurface.HOME_TOP_CHROME,
                UiSkinSurface.HOME_DRAWER,
                UiSkinSurface.PROFILE,
                UiSkinSurface.DYNAMIC_PUBLISH
            ),
            preview.manifest.surfaces
        )
        assertEquals("Rovniced/bilibili-skin", preview.manifest.styleSourceName)
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
    fun catalogPackageWithSeparateMetadataPreservesThemeTokens() {
        val packageBytes = skinPackage(
            "tail_bg.png" to pngBytes(),
            "head_bg.jpg" to jpegBytes(),
        )

        val importPackage = UiSkinImportPackageResolver.resolveBilibiliPackageWithMetadata(
            packageBytes = packageBytes,
            themeJsonBytes = convertedThemeJson().toByteArray(),
        ).getOrThrow()
        val preview = UiSkinPackageReader.preview(importPackage.packageBytes).getOrThrow()

        assertEquals("萧逸", preview.manifest.displayName)
        assertEquals("#778899", preview.manifest.colors.bottomBarIconTint)
        assertEquals("once", preview.manifest.motion.bottomBarIconAnimationMode)
    }

    @Test
    fun convertedArchiveKeepsSupportedLoadingLikeAndProgressAssets() {
        val lottieBytes = "{\"v\":\"5.7.0\",\"layers\":[]}".toByteArray()
        val packageBytes = skinPackage(
            "tail_bg.png" to pngBytes(),
            "loading.webp" to webpBytes(),
            "loading_frame.png" to pngBytes(),
            "like_effect.json" to lottieBytes,
            "progress_icon.json" to lottieBytes,
            "progress_drag_icon.json" to lottieBytes,
            "progress_static_icon.png" to pngBytes(),
        )

        val importPackage = UiSkinImportPackageResolver.resolveBilibiliPackageWithMetadata(
            packageBytes = packageBytes,
            themeJsonBytes = convertedThemeJson().toByteArray(),
        ).getOrThrow()
        val preview = UiSkinPackageReader.preview(importPackage.packageBytes).getOrThrow()

        assertTrue(UiSkinSurface.LOADING_INDICATOR in preview.manifest.surfaces)
        assertTrue(UiSkinSurface.LIKE_EFFECT in preview.manifest.surfaces)
        assertTrue(UiSkinSurface.PLAYER_PROGRESS in preview.manifest.surfaces)
        assertEquals("assets/loading.webp", preview.manifest.assets.loadingAnimation)
        assertEquals("assets/like_effect.json", preview.manifest.assets.likeEffectAnimation)
        assertEquals("assets/progress_icon.json", preview.manifest.assets.playerProgressIcon)
        assertEquals(
            "assets/progress_drag_icon.json",
            preview.manifest.assets.playerProgressDraggingIcon,
        )
    }

    @Test
    fun standaloneProgressSuitJsonDownloadsAndConvertsToEffectSkin() {
        val progressJson = """
            {
              "item_id": 2449,
              "name": "湊-阿库娅",
              "part_id": 11,
              "properties": {
                "icon": "https://example.com/icon.json",
                "drag_icon": "https://example.com/drag.json",
                "static_icon_image": "https://example.com/static.png",
                "ver": "1597737445"
              }
            }
        """.trimIndent().toByteArray()
        val lottieBytes = "{\"v\":\"5.7.0\",\"layers\":[]}".toByteArray()

        val importPackage = UiSkinImportPackageResolver.resolve(
            inputBytes = progressJson,
            remotePackageFetcher = { url ->
                when {
                    url.endsWith(".json") -> lottieBytes
                    else -> pngBytes()
                }
            },
        ).getOrThrow()
        val preview = UiSkinPackageReader.preview(importPackage.packageBytes).getOrThrow()

        assertEquals(setOf(UiSkinSurface.PLAYER_PROGRESS), preview.manifest.surfaces)
        assertEquals("湊-阿库娅", preview.manifest.displayName)
        assertEquals("assets/progress_icon.json", preview.manifest.assets.playerProgressIcon)
        assertEquals("assets/progress_drag_icon.json", preview.manifest.assets.playerProgressDraggingIcon)
        assertEquals("assets/progress_static_icon.png", preview.manifest.assets.playerProgressStaticIcon)
    }

    @Test
    fun onlineThemeMergeKeepsBaseThemeAndAddsSupplementalEffectSurfaces() {
        val basePackage = skinPackage(
            "skin-manifest.json" to Json.encodeToString(sampleManifest()).toByteArray(),
            "assets/bottom_trim.png" to pngBytes(),
            "assets/top_atmosphere.webp" to webpBytes(),
        )
        val lottieBytes = "{\"v\":\"5.7.0\",\"layers\":[]}".toByteArray()

        val mergedBytes = UiSkinImportPackageResolver.mergeSupplementalEffectAssets(
            packageBytes = basePackage,
            supplementalAssets = mapOf(
                "assets/loading.webp" to webpBytes(),
                "assets/like_effect_preview.jpg" to jpegBytes(),
                "assets/progress_icon.json" to lottieBytes,
            ),
        ).getOrThrow()
        val preview = UiSkinPackageReader.preview(mergedBytes).getOrThrow()

        assertTrue(UiSkinSurface.HOME_BOTTOM_BAR in preview.manifest.surfaces)
        assertTrue(UiSkinSurface.LOADING_INDICATOR in preview.manifest.surfaces)
        assertTrue(UiSkinSurface.LIKE_EFFECT in preview.manifest.surfaces)
        assertTrue(UiSkinSurface.PLAYER_PROGRESS in preview.manifest.surfaces)
        assertEquals("assets/bottom_trim.png", preview.manifest.assets.bottomBarTrim)
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
        assertEquals(null, preview.manifest.assets.topAtmosphere)
        assertEquals("assets/head_tab_bg.jpg", preview.manifest.assets.homeTopTabBackground)
    }

    @Test
    fun bilibiliColorModeThemeKeepsNativeBottomAndPublishSurfacesWithoutImageAssets() {
        val bytes = bilibiliThemeArchive(
            "哀悼主题/个性装扮.json" to colorModeThemeJson().toByteArray(),
            "哀悼主题/哀悼主题_package.zip" to skinPackage(
                "head_bg.jpg" to jpegBytes(),
                "head_tab_bg.jpg" to jpegBytes(),
            )
        )

        val importPackage = UiSkinImportPackageResolver.resolve(bytes).getOrThrow()
        val preview = UiSkinPackageReader.preview(importPackage.packageBytes).getOrThrow()

        assertTrue(UiSkinSurface.HOME_BOTTOM_BAR in preview.manifest.surfaces)
        assertTrue(UiSkinSurface.DYNAMIC_PUBLISH in preview.manifest.surfaces)
        assertTrue(preview.manifest.assets.bottomBarIcons.isEmpty())
        assertEquals(null, preview.manifest.assets.dynamicPublishIcon)
        assertEquals("#646569", preview.manifest.colors.bottomBarIconTint)
        assertEquals("#A4A5A7", preview.manifest.colors.bottomBarIconDarkTint)
        assertEquals("#222222", preview.manifest.colors.bottomBarSelectedTint)
        assertEquals("#F5F5F5", preview.manifest.colors.bottomBarSelectedDarkTint)
        assertEquals("color", preview.manifest.motion.bottomBarIconMode)
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
    fun bilibiliSkinDirectGb18030JsonPreservesChineseThemeName() {
        val packageBytes = skinPackage("tail_bg.png" to pngBytes())
        val input = officialUserEquipJson().toByteArray(java.nio.charset.Charset.forName("GB18030"))

        val importPackage = UiSkinImportPackageResolver.resolve(
            inputBytes = input,
            remotePackageFetcher = { packageBytes }
        ).getOrThrow()
        val preview = UiSkinPackageReader.preview(importPackage.packageBytes).getOrThrow()

        assertEquals("官方个性主题", preview.manifest.displayName)
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
        assertEquals(null, preview.manifest.assets.topAtmosphere)
        assertEquals("assets/head_tab_bg.jpg", preview.manifest.assets.homeTopTabBackground)
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
            "side_bg_bottom.png" to pngBytes(),
            "head_bg.jpg" to jpegBytes(),
            "head_tab_bg.jpg" to jpegBytes(),
            "side_bg.jpg" to jpegBytes(),
            "head_myself_bg.jpg" to jpegBytes(),
            "head_myself_squared_bg.jpg" to jpegBytes(),
            "head_myself_mp4_bg.mp4" to mp4Bytes(),
            "tail_icon_pub_btn_bg.png" to pngBytes(),
            "tail_icon_selected_pub_btn_bg.png" to pngBytes(),
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
        assertEquals("assets/side_bg_bottom.png", preview.manifest.assets.drawerBottomTrim)
        assertEquals("assets/head_bg.jpg", preview.manifest.assets.topAtmosphere)
        assertEquals("assets/head_tab_bg.jpg", preview.manifest.assets.homeTopTabBackground)
        assertEquals("assets/side_bg.jpg", preview.manifest.assets.homeSideBackground)
        assertEquals("assets/head_myself_bg.jpg", preview.manifest.assets.homeProfileBackground)
        assertEquals("assets/head_myself_squared_bg.jpg", preview.manifest.assets.homeProfileSquaredBackground)
        assertEquals("assets/head_myself_mp4_bg.mp4", preview.manifest.assets.homeProfileVideoBackground)
        assertEquals("assets/tail_icon_pub_btn_bg.png", preview.manifest.assets.dynamicPublishIcon)
        assertEquals(
            "assets/tail_icon_selected_pub_btn_bg.png",
            preview.manifest.assets.dynamicPublishSelectedIcon
        )
        assertEquals("assets/tail_icon_main.png", preview.manifest.assets.bottomBarIcons["home"])
        assertEquals("assets/tail_icon_selected_main.png", preview.manifest.assets.bottomBarIcons["home_selected"])
        assertEquals("assets/tail_icon_channel.png", preview.manifest.assets.bottomBarIcons["channel"])
        assertEquals(
            "assets/tail_icon_selected_channel.png",
            preview.manifest.assets.bottomBarIcons["channel_selected"]
        )
        assertEquals(null, preview.manifest.assets.homeChannelIcon)
        assertEquals(null, preview.manifest.assets.homeChannelSelectedIcon)
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

    private fun mp4Bytes(): ByteArray {
        return byteArrayOf(
            0x00, 0x00, 0x00, 0x18,
            0x66, 0x74, 0x79, 0x70,
            0x69, 0x73, 0x6F, 0x6D
        )
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
                "tail_color": "#6bb4ff",
                "tail_color_selected": "#336699",
                "tail_icon_color": "#778899",
                "tail_icon_color_dark": "#AABBCC",
                "tail_icon_color_selected": "#336699",
                "tail_icon_color_selected_dark": "654321",
                "side_bg_color": "#112233",
                "pub_btn_plus_color": "#FFFFFF",
                "pub_btn_shade_color_top": "#446688",
                "pub_btn_shade_color_bottom": "#224466",
                "color_mode": "light",
                "tail_icon_ani": "true",
                "tail_icon_ani_mode": "once",
                "tail_icon_mode": "img",
                "head_myself_mp4_play": "loop"
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

    private fun colorModeThemeJson(): String {
        return """
            {
              "item_id": 1213,
              "name": "全国哀悼日",
              "properties": {
                "ver": "1639381237",
                "color": "#ffffff",
                "color_second_page": "#797979",
                "tail_icon_mode": "color",
                "tail_icon_color": "#646569",
                "tail_icon_color_dark": "#A4A5A7",
                "tail_icon_color_selected": "#222222",
                "tail_icon_color_selected_dark": "#F5F5F5",
                "pub_btn_plus_color": "#ffffff",
                "pub_btn_shade_color_top": "#797979",
                "pub_btn_shade_color_bottom": "#666666"
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
