package com.android.purebilibili.feature.settings

import com.android.purebilibili.R
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BottomBarSettingsScreenIconPolicyTest {
    @Test
    fun navigationPreview_usesMaterialSymbolResources() {
        assertEquals(R.drawable.ms_home_24, resolveSettingsNavigationPreviewMaterialSymbolResource("HOME"))
        assertEquals(R.drawable.ms_notifications_none_24, resolveSettingsNavigationPreviewMaterialSymbolResource("DYNAMIC"))
        assertEquals(R.drawable.ms_collections_bookmark_24, resolveSettingsNavigationPreviewMaterialSymbolResource("FAVORITE"))
        assertEquals(R.drawable.ms_watch_later_24, resolveSettingsNavigationPreviewMaterialSymbolResource("WATCHLATER"))
        assertEquals(R.drawable.ms_trending_up_24, resolveSettingsNavigationPreviewMaterialSymbolResource("POPULAR"))
        assertEquals(R.drawable.ms_collections_bookmark_24, resolveSettingsNavigationPreviewMaterialSymbolResource("ANIME"))
    }

    @Test
    fun selectedNavigationPreview_usesFilledMaterialSymbols() {
        assertEquals(R.drawable.ms_home_fill_24, resolveSettingsNavigationPreviewMaterialSymbolResource("HOME", true))
        assertEquals(R.drawable.ms_notifications_fill_24, resolveSettingsNavigationPreviewMaterialSymbolResource("DYNAMIC", true))
        assertNotEquals(
            resolveSettingsNavigationPreviewMaterialSymbolResource("HOME"),
            resolveSettingsNavigationPreviewMaterialSymbolResource("HOME", true),
        )
    }
}
