package com.android.purebilibili.feature.list

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class FavoriteSubscribedFolderSharedTransitionStructureTest {

    @Test
    fun subscribedFolderRowAndDetailTopBarShareCollectionBounds() {
        val source = loadSource()

        assertTrue(source.contains("FavoriteSubscribedFolderRow("))
        assertTrue(source.contains("FavoriteSubscribedFolderPreview("))
        assertTrue(source.contains("model = FormatUtils.fixImageUrl(coverUrl)"))
        assertTrue(source.contains(".aspectRatio(16f / 9f)"))
        assertTrue(source.contains(".favoriteCollectionSharedBounds("))
        assertTrue(source.contains("shape = AppShapes.container(ContainerLevel.Card)"))
        assertTrue(source.contains("val shape = AppShapes.container(ContainerLevel.Card)"))
        assertTrue(source.contains("OverlayClip(AppShapes.container(ContainerLevel.Card))"))
        assertTrue(source.contains("namespace = \"favorite_collection\""))
        assertTrue(source.contains("modifier = Modifier.favoriteCollectionSharedBounds("))
        assertTrue(source.contains("route = favoriteCollectionSharedElementRoute"))
    }

    @Test
    fun subscribedFolderRouteCarriesSharedElementIntentToDetail() {
        val source = loadNavigationSource()

        assertTrue(source.contains("sharedElementTransition = route.sharedElementTransition"))
        assertTrue(source.contains("sharedElementTransition = seasonSeriesKey.sharedElementTransition"))
        assertTrue(source.contains("favoriteCollectionSharedElementRoute = FavoriteCollectionRoute("))
    }

    @Test
    fun seasonDetailVideoCardsKeepVideoSharedElementSource() {
        val commonListSource = loadSource()
        val navigationSource = loadNavigationSource()

        assertTrue(commonListSource.contains("transitionEnabled = cardTransitionEnabled"))
        assertTrue(navigationSource.contains("LocalVideoCardSharedElementSourceRoute provides"))
        assertTrue(navigationSource.contains("normalizeBiliPaiVideoSourceRoute("))
        assertTrue(navigationSource.contains("sourceRoute = seasonSeriesSourceRoute"))
    }

    private fun loadSource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/feature/list/CommonListScreen.kt"),
            File("src/main/java/com/android/purebilibili/feature/list/CommonListScreen.kt")
        ).first { it.exists() }.readText()
    }

    private fun loadNavigationSource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt"),
            File("src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")
        ).first { it.exists() }.readText()
    }
}
