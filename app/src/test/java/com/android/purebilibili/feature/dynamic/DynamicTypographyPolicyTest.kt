package com.android.purebilibili.feature.dynamic

import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicTypographyPolicyTest {

    @Test
    fun `compact dynamic typography keeps its established dimensions`() {
        assertEquals(13f, DynamicTypographyPolicy.segmentedControlLabelFontSize.value)
        assertEquals(8f, DynamicTypographyPolicy.sidebarLiveBadgeMinFontSize.value)
        assertEquals(0.5f, DynamicTypographyPolicy.sidebarLiveBadgeFontSizeStep.value)
        assertEquals(20f, DynamicTypographyPolicy.followGroupIntroLineHeight.value)
        assertEquals(18f, DynamicTypographyPolicy.followGroupSummaryLineHeight.value)
    }
}
