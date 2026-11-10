package com.android.purebilibili.feature.search

import com.android.purebilibili.core.ui.AppTopTabPresentation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchTopBarLayoutPolicyTest {

    @Test
    fun topBarLayout_removesInlineHotToggleAndKeepsPlaceholderSingleLine() {
        val spec = resolveSearchTopBarLayoutSpec()

        assertFalse(spec.showInlineHotToggle)
        assertEquals(1, spec.placeholderMaxLines)
    }

    @Test
    fun topBarRow_reservesTouchHeightAndVerticalPadding() {
        val paddingDp = 8
        val compactRowHeight = resolveSearchTopBarRowMinHeightDp(inputHeightDp = 36, verticalPaddingDp = paddingDp)
        val tallRowHeight = resolveSearchTopBarRowMinHeightDp(inputHeightDp = 72, verticalPaddingDp = paddingDp)

        assertTrue(compactRowHeight >= 48 + paddingDp)
        assertTrue(tallRowHeight >= 72 + paddingDp)
    }

    @Test
    fun inputHeight_doesNotGrowWhenTypographyFits() {
        val minimumHeightDp = 48
        assertEquals(minimumHeightDp, resolveSearchInputHeightDp(minimumHeightDp, lineHeightDp = 20f, fontSizeDp = 16f))
    }

    @Test
    fun inputHeight_accommodatesLargeLinesAndOversizedGlyphs() {
        assertTrue(resolveSearchInputHeightDp(48, lineHeightDp = 60f, fontSizeDp = 48f) > 60)
        assertTrue(resolveSearchInputHeightDp(48, lineHeightDp = 20f, fontSizeDp = 64f) > 64)
    }

    @Test
    fun material3SearchInput_omitsLeadingIconToPreservePlaceholderWidth() {
        assertTrue(
            shouldOmitSearchInputLeadingIcon(
                tabPresentation = AppTopTabPresentation.MATERIAL_UNDERLINE,
            )
        )
        assertFalse(
            shouldOmitSearchInputLeadingIcon(
                tabPresentation = AppTopTabPresentation.TONAL_CAPSULE,
            )
        )
        assertFalse(
            shouldOmitSearchInputLeadingIcon(
                tabPresentation = AppTopTabPresentation.MOVING_CAPSULE,
            )
        )
    }
}
