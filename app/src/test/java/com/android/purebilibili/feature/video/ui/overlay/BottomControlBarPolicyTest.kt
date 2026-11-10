package com.android.purebilibili.feature.video.ui.overlay

import com.android.purebilibili.feature.video.subtitle.SubtitleTrackOption
import kotlin.test.Test
import kotlin.test.assertEquals

class BottomControlBarPolicyTest {


    @Test
    fun subtitlePanelTrackOptions_dropBlankAndDeduplicateByTrackKey() {
        val options = resolveSubtitlePanelTrackOptions(
            listOf(
                SubtitleTrackOption(
                    trackKey = "zh",
                    languageCode = "zh-Hans",
                    label = "中文（简体）",
                    selected = true,
                    likelyAi = false
                ),
                SubtitleTrackOption(
                    trackKey = "zh",
                    languageCode = "zh-Hans",
                    label = "重复中文",
                    selected = false,
                    likelyAi = false
                ),
                SubtitleTrackOption(
                    trackKey = "",
                    languageCode = "en-US",
                    label = "英语",
                    selected = false,
                    likelyAi = false
                )
            )
        )

        assertEquals(1, options.size)
        assertEquals("中文（简体）", options.single().label)
    }

}
