package com.android.purebilibili.feature.list

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FavoriteContentModeResolverTest {

    @Test
    fun nonFavoritePageUsesBaseMode() {
        assertEquals(
            FavoriteContentMode.BASE_LIST,
            resolveFavoriteContentMode(isFavoritePage = false, folderCount = 3)
        )
    }

    @Test
    fun singleFolderUsesFolderStateMode() {
        assertEquals(
            FavoriteContentMode.SINGLE_FOLDER,
            resolveFavoriteContentMode(isFavoritePage = true, folderCount = 1)
        )
    }

    @Test
    fun multipleFoldersUsePagerMode() {
        assertEquals(
            FavoriteContentMode.PAGER,
            resolveFavoriteContentMode(isFavoritePage = true, folderCount = 2)
        )
    }

    @Test
    fun favoriteBrowseSegmentedControlForcesLiquidIndicatorFromPageSettings() {
        val listSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/list/CommonListScreen.kt"
        )
        val segmentedSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/settings/IOSSlidingSegmentedControl.kt"
        )
        val bottomBarSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBarLiquidSegmentedControl.kt"
        )

        assertTrue(
            listSource.contains("forceLiquidIndicator = homeSettings.androidNativeLiquidGlassEnabled"),
            "Favorite page should pass its already-collected Android native glass setting into the top segmented control"
        )
        assertTrue(
            listSource.contains("height = favoriteHeaderLayout.browseToggleHeightDp.dp"),
            "Favorite page should use the compact header segmented-control height instead of the bottom-bar default"
        )
        assertTrue(
            listSource.contains("indicatorHeight = favoriteHeaderLayout.browseToggleIndicatorHeightDp.dp"),
            "Favorite page should size the selected indicator from the header layout policy"
        )
        assertTrue(
            listSource.contains("labelFontSize = favoriteHeaderLayout.browseToggleLabelFontSizeSp.sp"),
            "Favorite page should size segmented labels from the header layout policy"
        )
        assertTrue(
            listSource.contains("tapPressRefractionEnabled = false"),
            "Favorite page should not inject tap press into liquid-glass refraction because it causes selection ghosting"
        )
        assertTrue(
            segmentedSource.contains("forceLiquidIndicator: Boolean = false"),
            "Shared iOS segmented control should expose an explicit liquid-indicator override"
        )
        assertTrue(
            segmentedSource.contains("tapPressRefractionEnabled: Boolean = true"),
            "Shared iOS segmented control should expose tap refraction control to callers"
        )
        assertTrue(
            segmentedSource.contains("forceLiquidChrome = forceLiquidIndicator"),
            "Shared iOS segmented control should forward the override into the bottom-bar liquid implementation"
        )
        assertTrue(
            segmentedSource.contains("tapPressRefractionEnabled = tapPressRefractionEnabled"),
            "Shared iOS segmented control should forward tap refraction control into the bottom-bar liquid implementation"
        )
        assertTrue(
            bottomBarSource.contains("forceLiquidChrome: Boolean = false"),
            "BottomBarLiquidSegmentedControl should allow parents with settled settings to bypass the async default fallback"
        )
        assertTrue(
            bottomBarSource.contains("forceLiquidChrome || homeSettings.androidNativeLiquidGlassEnabled"),
            "BottomBarLiquidSegmentedControl should treat forced liquid chrome the same as the global Android native glass setting"
        )
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
