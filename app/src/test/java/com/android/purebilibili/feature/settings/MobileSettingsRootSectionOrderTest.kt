package com.android.purebilibili.feature.settings

import com.android.purebilibili.R
import kotlin.test.Test
import kotlin.test.assertEquals

class MobileSettingsRootSectionOrderTest {

    @Test
    fun shouldUseFlatGroupedOrderForSettingsHome() {
        assertEquals(
            listOf(
                MobileSettingsRootSection.FOLLOW_AUTHOR,
                MobileSettingsRootSection.GENERAL,
                MobileSettingsRootSection.PRIVACY,
                MobileSettingsRootSection.STORAGE,
                MobileSettingsRootSection.DEVELOPER,
                MobileSettingsRootSection.FEED,
                MobileSettingsRootSection.ABOUT,
                MobileSettingsRootSection.SUPPORT
            ),
            resolveMobileSettingsRootSectionOrder()
        )
    }

    @Test
    fun rootSections_shouldMapToLocalizedStringResources() {
        assertEquals(R.string.settings_section_follow_author, resolveMobileSettingsRootSectionTitleRes(MobileSettingsRootSection.FOLLOW_AUTHOR))
        assertEquals(R.string.settings_section_general, resolveMobileSettingsRootSectionTitleRes(MobileSettingsRootSection.GENERAL))
        assertEquals(R.string.settings_section_privacy, resolveMobileSettingsRootSectionTitleRes(MobileSettingsRootSection.PRIVACY))
        assertEquals(R.string.settings_section_storage, resolveMobileSettingsRootSectionTitleRes(MobileSettingsRootSection.STORAGE))
        assertEquals(R.string.settings_section_developer, resolveMobileSettingsRootSectionTitleRes(MobileSettingsRootSection.DEVELOPER))
        assertEquals(R.string.settings_section_feed, resolveMobileSettingsRootSectionTitleRes(MobileSettingsRootSection.FEED))
        assertEquals(R.string.settings_section_about, resolveMobileSettingsRootSectionTitleRes(MobileSettingsRootSection.ABOUT))
        assertEquals(R.string.settings_section_support, resolveMobileSettingsRootSectionTitleRes(MobileSettingsRootSection.SUPPORT))
    }
}
