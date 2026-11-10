package com.android.purebilibili.feature.settings.highlights

import com.android.purebilibili.core.store.AppThemeSettings
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.SeedPaletteStyle
import com.android.purebilibili.feature.settings.Md3ColorSource
import com.materialkolor.PaletteStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdateHighlightPolicyTest {
    private val feature = updateHighlightCatalog.first { it.id == "cmf_spec_2026" }

    @Test
    fun newInstallationEstablishesBaselineWithoutInvitations() {
        val result = prepareUpdateHighlights(UpdateHighlightHistory(), feature.introducedVersionCode, false)
        assertTrue(result.pendingIds.isEmpty())
        assertTrue(feature.id in result.handledIds)
        assertEquals(feature.introducedVersionCode, result.lastVersionCode)
    }

    @Test
    fun existingInstallCanReceiveTheFirstReleaseAndReentryDoesNotRequeueHandledItems() {
        val initial = prepareUpdateHighlights(UpdateHighlightHistory(), feature.introducedVersionCode, true)
        assertEquals(setOf(feature.id), initial.pendingIds)
        val dismissed = initial.copy(pendingIds = emptySet(), handledIds = setOf(feature.id))
        assertEquals(dismissed, prepareUpdateHighlights(dismissed, feature.introducedVersionCode, true))
        assertTrue(prepareUpdateHighlights(dismissed, feature.introducedVersionCode + 1, true).pendingIds.isEmpty())
    }

    @Test
    fun skippedVersionsMergeNewFeaturesAndDowngradesRetainTheHighWaterMark() {
        val catalog = (100..104).map { UpdateHighlight("feature_$it", it, "Title", "Description") }
        val result = prepareUpdateHighlights(UpdateHighlightHistory(lastVersionCode = 99), 104, true, catalog)
        assertEquals(catalog.map { it.id }.toSet(), result.pendingIds)
        assertEquals(result, prepareUpdateHighlights(result, 102, true, catalog))
        assertEquals(listOf(catalog.last()), latestReleaseHighlights(104, catalog))
        assertEquals(listOf(catalog[2]), latestReleaseHighlights(102, catalog))
    }

    @Test
    fun eligibilityNeverChangesTheUsersStyleOrColorSource() {
        val base = AppThemeSettings(uiStyle = AppUiStyle.MATERIAL3, md3ColorSource = Md3ColorSource.CUSTOM)
        assertTrue(canPreviewUpdateHighlight(feature, base))
        assertFalse(canPreviewUpdateHighlight(feature, base.copy(uiStyle = AppUiStyle.MIUIX)))
        assertFalse(canPreviewUpdateHighlight(feature, base.copy(md3ColorSource = Md3ColorSource.FOLLOW_WALLPAPER)))
        assertTrue(isUpdateHighlightAlreadyEnabled(feature, base.copy(colorStyle = PaletteStyle.Cmf())))
        assertFalse(isUpdateHighlightAlreadyEnabled(feature, base.copy(
            uiStyle = AppUiStyle.MIUIX, colorStyle = PaletteStyle.Cmf(),
        )))
        assertFalse(isUpdateHighlightAlreadyEnabled(feature, base.copy(
            colorStyle = PaletteStyle.Cmf(), seedPaletteStyle = SeedPaletteStyle.CandyPop,
        )))
    }

    @Test
    fun stableFeatureIdsAreUniqueAndReleaseOverviewIsBounded() {
        assertEquals(updateHighlightCatalog.size, updateHighlightCatalog.map { it.id }.toSet().size)
        val catalog = (0..5).map { UpdateHighlight("item_$it", 486, "Title", "Description") }
        assertEquals(3, latestReleaseHighlights(486, catalog).size)
    }
}
