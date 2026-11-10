package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.SplashItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class OfficialWallpaperCatalogPolicyTest {
    @Test
    fun mergesBothSourcesAndPreservesDifferentImagesWithTheSameId() {
        val brand = SplashItem(id = 17, thumb = "https://example.com/brand.jpg")
        val splash = SplashItem(id = 17, image = "https://example.com/other.jpg")
        assertEquals(listOf(brand, splash), mergeOfficialWallpaperCatalogs(listOf(brand), listOf(splash)))
    }

    @Test
    fun duplicatesUseOriginalImageAndNormalizeUrlSchemes() {
        val brand = SplashItem(id = 17, thumb = "//example.com/thumb.jpg", title = "官方壁纸 #17")
        val splash = SplashItem(id = 99, thumb = "http://example.com/thumb.jpg", image = "https://example.com/full.jpg")
        val merged = mergeOfficialWallpaperCatalogs(listOf(brand), listOf(splash))
        assertEquals(1, merged.size)
        assertEquals("https://example.com/thumb.jpg", merged.single().thumb)
        assertEquals("https://example.com/full.jpg", merged.single().image)
        assertEquals("官方壁纸 #17", merged.single().title)
    }

    @Test
    fun filtersAdsAndMissingImagesEvenWhenThereAreNoOtherWallpapers() {
        assertEquals(
            emptyList(),
            mergeOfficialWallpaperCatalogs(listOf(SplashItem(id = 1, thumb = "https://example.com/ad.jpg", isAd = true), SplashItem(id = 2))),
        )
    }
    @Test
    fun archiveTreeEncodesChinesePathsAndDeduplicatesContentWithoutImportingLogos() {
        val root = Json.parseToJsonElement("""
            {"truncated":false,"tree":[
                {"type":"blob","path":"app_splash/春节 壁纸.PNG","sha":"same"},
                {"type":"blob","path":"bizhiniang/2022/duplicate.jpg","sha":"same"},
                {"type":"blob","path":"app_splash/images.json","sha":"index"},
                {"type":"blob","path":"assets/logo.png","sha":"logo"}
            ]}
        """).jsonObject
        val items = parseWallpaperArchiveTree(root, "owner/repo", setOf("app_splash", "bizhiniang"), "历史归档")
        assertEquals(1, items.size)
        assertEquals("https://raw.githubusercontent.com/owner/repo/main/app_splash/%E6%98%A5%E8%8A%82%20%E5%A3%81%E7%BA%B8.PNG", items.single().image)
    }

    @Test
    fun truncatedArchiveIsReportedAsIncomplete() {
        assertFailsWith<IllegalStateException> {
            parseWallpaperArchiveTree(
                Json.parseToJsonElement("""{"truncated":true,"tree":[]}""").jsonObject,
                "owner/repo", setOf("app_splash"), "历史归档",
            )
        }
    }

    @Test
    fun duplicateArchiveContentAcrossSourcesKeepsOneEntry() {
        val first = SplashItem(id = -1, image = "https://example.com/a.png", archiveContentHash = "same")
        val second = SplashItem(id = -2, image = "https://example.com/b.png", archiveContentHash = "same")
        assertEquals(listOf(first), mergeOfficialWallpaperCatalogs(listOf(first), listOf(second)))
    }
}
