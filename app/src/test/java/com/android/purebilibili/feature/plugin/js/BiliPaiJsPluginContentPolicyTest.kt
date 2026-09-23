package com.android.purebilibili.feature.plugin.js

import com.android.purebilibili.core.plugin.js.BiliPaiJsParam
import com.android.purebilibili.core.plugin.js.BiliPaiJsMediaItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BiliPaiJsPluginContentPolicyTest {

    @Test
    fun initialParamsPreferSavedValuesOverManifestDefaults() {
        val params = listOf(
            BiliPaiJsParam(name = "dataSource", title = "数据源", defaultValue = ""),
            BiliPaiJsParam(name = "category", title = "分类", defaultValue = "all")
        )

        val values = resolveBiliPaiJsInitialParamValues(
            params = params,
            savedValues = mapOf("dataSource" to "https://example.com/live.m3u")
        )

        assertEquals("https://example.com/live.m3u", values["dataSource"])
        assertEquals("all", values["category"])
    }

    @Test
    fun paramPreferenceKeyIncludesPluginModuleAndParamName() {
        assertEquals(
            "js_param_tv.live_channels_dataSource",
            buildBiliPaiJsParamPreferenceKey("tv.live", "channels", "dataSource")
        )
    }

    @Test
    fun mediaItemLazyKeyKeepsDuplicateImportedUrlsUnique() {
        val first = BiliPaiJsMediaItem(
            id = "https://epg.yang-1989.eu.org/v/302.mp4",
            title = "频道 A",
            videoUrl = "https://epg.yang-1989.eu.org/v/302.mp4"
        )
        val second = first.copy(title = "频道 B")

        assertNotEquals(
            resolveBiliPaiJsMediaItemLazyKey(index = 0, item = first),
            resolveBiliPaiJsMediaItemLazyKey(index = 1, item = second)
        )
    }
}
