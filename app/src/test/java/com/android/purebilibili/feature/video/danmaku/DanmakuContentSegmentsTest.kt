package com.android.purebilibili.feature.video.danmaku

import kotlin.test.Test
import kotlin.test.assertEquals

class DanmakuContentSegmentsTest {
    @Test
    fun wholeImageMessageDoesNotRequireBrackets() {
        assertEquals(listOf(DanmakuContentSegment("害怕", true)),
            resolveDanmakuContentSegments("害怕", true, mapOf("害怕" to "image"), emptyMap()))
    }

    @Test
    fun wholeImageWinsOverBracketMarkersInsideItsLabel() {
        assertEquals(listOf(DanmakuContentSegment("我[害怕]", true)),
            resolveDanmakuContentSegments("我[害怕]", true, mapOf("我[害怕]" to "image"), mapOf("害怕" to "other")))
    }

    @Test
    fun inlineMessageKeysRetainTheirOriginalSpelling() {
        assertEquals(listOf(DanmakuContentSegment("我：", false), DanmakuContentSegment("害怕", true)),
            resolveDanmakuContentSegments("我：害怕", true, mapOf("害怕" to "image"), emptyMap()))
    }

    @Test
    fun roomCacheOnlyReplacesBracketedTokensAndUnknownTextSurvives() {
        assertEquals(listOf(DanmakuContentSegment("害怕", false), DanmakuContentSegment("害怕", true),
            DanmakuContentSegment("[未知]", false)),
            resolveDanmakuContentSegments("害怕[害怕][未知]", true, emptyMap(), mapOf("害怕" to "image")))
    }

    @Test
    fun disabledImagesAndBlankResourcesKeepText() {
        assertEquals(listOf(DanmakuContentSegment("害怕", false)),
            resolveDanmakuContentSegments("害怕", false, mapOf("害怕" to "image"), emptyMap()))
        assertEquals(listOf(DanmakuContentSegment("害怕", false)),
            resolveDanmakuContentSegments("害怕", true, mapOf("害怕" to " "), emptyMap()))
    }
}
