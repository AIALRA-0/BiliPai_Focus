package com.android.purebilibili.core.plugin.skin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SkinCatalogPolicyTest {

    private fun entry(
        packageZipUrl: String? = null,
        packageUrlCdn: String? = null,
        colorMode: String? = null,
        capabilities: SkinCatalogCapabilities = SkinCatalogCapabilities()
    ) = SkinCatalogEntry(
        id = "t",
        name = "主题",
        previewUrl = "https://example.com/p.jpg",
        packageZipUrl = packageZipUrl,
        packageUrlCdn = packageUrlCdn,
        colorMode = colorMode,
        capabilities = capabilities
    )

    @Test
    fun preferredPackageUrlPrefersRawZipOverCdn() {
        assertEquals(
            "https://raw.githubusercontent.com/x.zip",
            entry(
                packageZipUrl = "https://raw.githubusercontent.com/x.zip",
                packageUrlCdn = "http://i0.hdslb.com/y.zip"
            ).preferredPackageUrl()
        )
    }

    @Test
    fun preferredPackageUrlFallsBackToCdnWhenNoRawZip() {
        assertEquals(
            "http://i0.hdslb.com/y.zip",
            entry(packageUrlCdn = "http://i0.hdslb.com/y.zip").preferredPackageUrl()
        )
    }

    @Test
    fun preferredPackageUrlIsNullWhenNeitherPresent() {
        assertNull(entry().preferredPackageUrl())
    }

    @Test
    fun themeMetadataUrlUsesPackageZipDirectoryAndSupportsLegacyCatalog() {
        assertEquals(
            "https://raw.githubusercontent.com/Rovniced/bilibili-skin/main/%E8%90%A7%E9%80%B8/" +
                "%E4%B8%AA%E6%80%A7%E8%A3%85%E6%89%AE.json",
            entry(
                packageZipUrl = "https://raw.githubusercontent.com/Rovniced/bilibili-skin/main/" +
                    "%E8%90%A7%E9%80%B8/%E8%90%A7%E9%80%B8_package.zip"
            ).resolvedThemeMetadataUrl()
        )
        assertEquals(
            "https://example.com/%E4%B8%AA%E6%80%A7%E8%A3%85%E6%89%AE.json",
            entry(packageUrlCdn = "https://i0.hdslb.com/package.zip").resolvedThemeMetadataUrl()
        )
    }

    @Test
    fun isDarkReflectsColorMode() {
        assertTrue(entry(colorMode = "dark").isDark)
        assertFalse(entry(colorMode = "light").isDark)
        assertFalse(entry(colorMode = null).isDark)
        assertFalse(entry(colorMode = "DARK").isDark.not())
    }

    @Test
    fun displayNameFallsBackToUtf8RepositoryDirectoryForLegacyMojibake() {
        val corrupted = SkinCatalogEntry(
            id = "12周年夏日狂欢",
            name = "12周\uFFFD\uFFFD\uFFFD",
            previewUrl = "https://example.com/p.jpg"
        )

        assertEquals("12周年夏日狂欢", corrupted.displayName)
        assertEquals("主题", entry().displayName)
    }

    @Test
    fun capabilityLabelsListedInDisplayOrder() {
        val caps = SkinCatalogCapabilities(
            bottomBarIcons = true,
            bottomBarTrim = true,
            profileBackground = true,
            profileVideo = true,
            topAtmosphere = true,
            topTabBackground = true,
            sideBackground = true,
            drawerBottomTrim = true,
            publishIcon = true,
            animatedIcons = true,
            loadingAnimation = true,
            likeEffect = true,
            playerProgress = true,
        )
        assertEquals(
            listOf(
                "底栏图标",
                "底栏饰面",
                "个人页背景",
                "个人页动效",
                "顶部氛围",
                "标签背景",
                "侧栏背景",
                "侧栏底饰",
                "发布图标",
                "底栏动效",
                "加载动画",
                "点赞效果",
                "进度条",
            ),
            caps.labels()
        )
    }

    @Test
    fun emptyCapabilitiesHaveNoLabels() {
        val caps = SkinCatalogCapabilities()
        assertTrue(caps.isEmpty)
        assertTrue(caps.labels().isEmpty())
    }
}
