package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AppUiStyle
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContentCardSurfacePolicyTest {

    @Test
    fun miuixContentCardsUseTokenSurfaceAndFlatElevation() {
        val spec = resolveContentCardSurfaceSpec(AppUiStyle.MIUIX)
        assertTrue(spec.usesTonalContainerTreatment)
        assertEquals(ContainerLevel.Card, spec.cornerLevel)
        assertEquals(0.8f, spec.borderWidthDp)
        assertEquals(0.22f, spec.borderAlpha)
        assertEquals(0f, spec.tonalElevationDp)
        assertEquals(0f, spec.shadowElevationDp)
    }

    @Test
    fun materialContentCardsKeepLegacyGlassShellDefaults() {
        val spec = resolveContentCardSurfaceSpec(AppUiStyle.MATERIAL3)
        assertFalse(spec.usesTonalContainerTreatment)
        assertEquals(0f, spec.borderWidthDp)
    }

    @Test
    fun messageFeedAndSearchSurfacesAdoptSharedPolicy() {
        val messageSource = load("app/src/main/java/com/android/purebilibili/feature/message/feed/MessageFeedCommon.kt")
        val searchSource = load("app/src/main/java/com/android/purebilibili/feature/search/SearchScreen.kt")
        val dynamicSource = load(
            "app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicComponents.kt"
        )

        assertTrue(messageSource.contains("rememberContentCardSurfaceSpec("))
        assertTrue(messageSource.contains("AppShapes.borderedContainer("))
        assertTrue(searchSource.contains("rememberContentCardSurfaceSpec("))
        assertTrue(searchSource.contains("AppShapes.borderedContainer("))
        assertTrue(dynamicSource.contains("rememberContentCardSurfaceSpec("))
        assertTrue(dynamicSource.contains("AppShapes.borderedContainer("))
    }

    private fun load(path: String): String {
        val normalized = path.removePrefix("app/")
        return listOf(File(path), File(normalized))
            .first { it.exists() }
            .readText()
    }
}
