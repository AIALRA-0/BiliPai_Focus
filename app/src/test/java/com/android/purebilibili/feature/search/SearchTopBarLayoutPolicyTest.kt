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
    fun topBarRowMinHeight_matchesHomeSearchRowHeight() {
        // 首页搜索胶囊 36dp、行高 48dp；搜索页输入框同为 compactChrome.primaryHeightDp，
        // 行高应保持与首页一致（48dp），不再被 64dp 下限抬高。
        assertEquals(48, resolveSearchTopBarRowMinHeightDp(inputHeightDp = 36))
        assertEquals(52, resolveSearchTopBarRowMinHeightDp(inputHeightDp = 44))
        assertEquals(64, resolveSearchTopBarRowMinHeightDp(inputHeightDp = 56))
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
