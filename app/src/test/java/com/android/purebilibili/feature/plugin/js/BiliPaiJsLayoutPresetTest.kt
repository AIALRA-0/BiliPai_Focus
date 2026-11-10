package com.android.purebilibili.feature.plugin.js

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BiliPaiJsLayoutPresetTest {

    @Test
    fun parsesValidPresetAndRoundTripsThroughEncoding() {
        val text = """
            {
              "formatVersion": 1,
              "name": "番剧索引 · 排名网格",
              "pluginId": "bilipai.official.bangumi",
              "moduleId": "browse",
              "layout": "grid",
              "params": { "subjectType": "2", "sort": "rank" }
            }
        """.trimIndent()

        val preset = BiliPaiJsLayoutPresetStore.parsePreset(text).getOrThrow()

        assertEquals("bilipai.official.bangumi", preset.pluginId)
        assertEquals("browse", preset.moduleId)
        assertEquals("grid", preset.layout)
        assertEquals("rank", preset.params["sort"])

        val reparsed = BiliPaiJsLayoutPresetStore.parsePreset(
            BiliPaiJsLayoutPresetStore.encodePreset(preset)
        ).getOrThrow()
        assertEquals(preset, reparsed)
    }

    @Test
    fun parseRejectsUnknownVersionAndBlankIdentifiersAndBadLayout() {
        val badVersion = BiliPaiJsLayoutPresetStore.parsePreset(
            """{"formatVersion": 99, "pluginId": "a.b", "moduleId": "m"}"""
        )
        assertTrue(badVersion.isFailure)

        val blankPlugin = BiliPaiJsLayoutPresetStore.parsePreset(
            """{"formatVersion": 1, "pluginId": "", "moduleId": "m"}"""
        )
        assertTrue(blankPlugin.isFailure)

        val blankModule = BiliPaiJsLayoutPresetStore.parsePreset(
            """{"formatVersion": 1, "pluginId": "a.b", "moduleId": ""}"""
        )
        assertTrue(blankModule.isFailure)

        val badLayout = BiliPaiJsLayoutPresetStore.parsePreset(
            """{"formatVersion": 1, "pluginId": "a.b", "moduleId": "m", "layout": "waterfall"}"""
        )
        assertTrue(badLayout.isFailure)
    }
}
