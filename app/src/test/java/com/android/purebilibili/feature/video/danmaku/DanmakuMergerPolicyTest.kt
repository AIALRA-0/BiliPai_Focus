package com.android.purebilibili.feature.video.danmaku

import com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_SCROLL
import com.android.purebilibili.danmaku.engine.DanmakuItem
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DanmakuMergerPolicyTest {

    @Test
    fun merge_repeatedDanmakuStaysStandardAndDoesNotCreateYellowAdvancedDanmaku() {
        val items = List(5) { index ->
            DanmakuItem().apply {
                text = "重复弹幕"
                showAtTime = index * 100L
                textColor = 0xFFFFFFFF.toInt()
                layerType = DANMAKU_LAYER_SCROLL
            }
        }

        val (standard, advanced) = DanmakuMerger.merge(items, intervalMs = 500L)

        assertEquals(1, standard.size)
        assertTrue(advanced.isEmpty())
        assertEquals("重复弹幕 x5", standard.first().text)
    }

    @Test
    fun merge_onlyAppendsCountWhenDuplicateThresholdIsReached() {
        val items = List(2) { index ->
            DanmakuItem().apply {
                text = "重复弹幕"
                showAtTime = index * 100L
                textColor = 0xFFFFFFFF.toInt()
                layerType = DANMAKU_LAYER_SCROLL
            }
        }

        val (standard, advanced) = DanmakuMerger.merge(
            list = items,
            intervalMs = 500L,
            countThreshold = 3
        )

        assertEquals(1, standard.size)
        assertTrue(advanced.isEmpty())
        assertEquals("重复弹幕", standard.first().text)
    }

    @Test
    fun merge_respectsCustomMergeWindow() {
        val items = List(2) { index ->
            DanmakuItem().apply {
                text = "重复弹幕"
                showAtTime = index * 600L
                textColor = 0xFFFFFFFF.toInt()
                layerType = DANMAKU_LAYER_SCROLL
            }
        }

        val (standard, _) = DanmakuMerger.merge(
            list = items,
            intervalMs = 500L,
            countThreshold = 2
        )

        assertEquals(2, standard.size)
    }

    @Test
    fun merge_anchorsTheInclusiveWindowToTheFirstItem() {
        val items = listOf(0L, 15_000L, 15_001L).map { time ->
            DanmakuItem().apply {
                text = "重复弹幕"
                showAtTime = time
            }
        }

        val (standard, _) = DanmakuMerger.merge(items, intervalMs = 15_000L)

        assertEquals(listOf(0L, 15_001L), standard.map { it.showAtTime })
        assertEquals(listOf("重复弹幕 x2", "重复弹幕"), standard.map { it.text })
    }

    @Test
    fun merge_sumsLikesWithinEachWindowWithoutMutatingTheSource() {
        val items = listOf(
            0L to 3L,
            10_000L to 10L,
            20_000L to 7L,
            30_000L to 0L
        ).map { (time, likes) ->
            DanmakuItem().apply {
                text = "相同内容"
                showAtTime = time
                likeCount = likes
            }
        }

        val (standard, _) = DanmakuMerger.merge(items, intervalMs = 15_000L)

        assertEquals(listOf(0L, 20_000L), standard.map { it.showAtTime })
        assertEquals(listOf(13L, 7L), standard.map { it.likeCount })
        assertEquals(listOf(3L, 10L, 7L, 0L), items.map { it.likeCount })
    }

    @Test
    fun merge_keepsOriginalContentForHotSuppressionWithoutMutatingTheHotSource() {
        val items = List(3) { index ->
            DanmakuItem().apply {
                danmakuId = 10L + index
                text = "相同内容"
                showAtTime = index * 100L
            }
        }
        val unrelated = DanmakuItem().apply {
            danmakuId = 99L
            text = "其他内容"
            showAtTime = 150L
        }

        val (standard, _) = DanmakuMerger.merge(
            (items + unrelated).sortedBy { it.showAtTime },
            countThreshold = 2
        )
        val merged = standard.first()
        assertEquals("相同内容", merged.mergedOriginalText)
        assertEquals("相同内容", items.first().text)
        assertNull(standard.last().mergedOriginalText)
    }
}
