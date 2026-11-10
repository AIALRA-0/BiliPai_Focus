package com.android.purebilibili.danmaku.parser

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpecialDanmakuWindowTest {
    @Test
    fun futurePayloadIsNotFetchedAndColdSeekKeepsLongLivedScenes() = runSuspend {
        val source = Source(
            element(50_000, 9, "def text far {content=\"far\"}"),
            element(0, 9, "def text long {content=\"long\" duration=1m}"),
            element(2_000, 9, "def text short {content=\"short\" duration=1s}"),
            element(21_000, 9, "def text next {content=\"next\" duration=1s}")
        )
        val window = source.window()
        val initial = window.load(0, 3_000)
        assertEquals(listOf("long", "short"), initial.parsed.basList.map { it.content })
        assertEquals(setOf(0L, 2_000L), source.requestedStarts())

        source.requests.clear()
        val sought = window.load(20_000, 3_000)
        assertEquals(listOf("long", "next"), sought.parsed.basList.map { it.content })
        assertEquals(setOf(21_000L), source.requestedStarts())
        assertEquals(20_000L, sought.startTimeMs)
        assertEquals(23_000L, sought.endTimeMs)
    }

    @Test
    fun unseenEarlierLifetimesAreInspectedWithoutKeepingExpiredPrograms() = runSuspend {
        val source = Source(
            element(0, 9, "def text long {content=\"active\"} set long {} 1h"),
            element(1_000, 9, "def text old {content=\"expired\" duration=1s}"),
            element(200_000, 9, "def text future {content=\"not yet\"}")
        )
        val window = source.window()
        assertEquals(listOf("active"), window.load(100_000).parsed.basList.map { it.content })
        assertEquals(setOf(0L, 1_000L), source.requestedStarts())
        source.requests.clear()
        assertEquals(listOf("active"), window.load(110_000).parsed.basList.map { it.content })
        assertTrue(source.requests.isEmpty(), "Known expired source must not be fetched again")
    }

    @Test
    fun backwardSeekReloadsEvictedPayloadAndRestoresOriginalTimeline() = runSuspend {
        val source = Source(
            element(0, 9, "def text first {content=\"first\" duration=2s}"),
            element(10_000, 9, "def text later {content=\"later\" duration=2s}")
        )
        val window = source.window()
        assertEquals(listOf("first"), window.load(0).parsed.basList.map { it.content })
        assertEquals(listOf("later"), window.load(10_000).parsed.basList.map { it.content })
        source.requests.clear()
        assertEquals(listOf("first"), window.load(500).parsed.basList.map { it.content })
        assertEquals(setOf(0L), source.requestedStarts(), "Expired programs must be evicted, not cached for the video")
    }

    @Test
    fun expiryAndLookaheadAreHalfOpenAndZeroDurationDoesNotStayAlive() = runSuspend {
        val source = Source(
            element(0, 9, "def text old {content=\"old\" duration=1s}"),
            element(1_000, 9, "def text zero {content=\"zero\" duration=0ms}"),
            element(4_000, 9, "def text boundary {content=\"boundary\"}")
        )
        val window = source.window()
        assertTrue(window.load(1_000, 3_000).parsed.basList.isEmpty())
        assertEquals(setOf(0L, 1_000L), source.requestedStarts())
        assertEquals(listOf("boundary"), window.load(4_000).parsed.basList.map { it.content })
    }

    @Test
    fun legacySpecialTextAndOrdinaryRowsUseTheirOwnLifetimeAndMode8IsNeverExecuted() = runSuspend {
        val source = Source(
            element(0, 7, "[0,0,\"1-1\",12,\"legacy\",0,0]"),
            element(0, 1, "ordinary"),
            element(0, 8, "throw new Error('not BAS')")
        )
        val window = source.window()
        val first = window.load(5_000, standardDurationMs = 7_000)
        assertEquals(listOf("legacy"), first.parsed.advancedList.map { it.content })
        assertEquals(listOf("ordinary"), first.parsed.standardList.map { it.text })
        assertEquals(setOf(0L), source.requestedStarts())
        assertEquals(2, source.requests.size)
        assertTrue(window.load(13_000).parsed.advancedList.isEmpty())
        assertTrue(window.load(13_000).parsed.standardList.isEmpty())
    }

    @Test
    fun invalidBASDoesNotPreventLaterValidScenesAndEmptyWindowsClampTheirBounds() = runSuspend {
        val source = Source(
            element(0, 9, "set unknown {} 1s"),
            element(1_000, 9, "def text valid {content=\"valid\"}")
        )
        val window = source.window()
        val result = window.load(-50)
        assertEquals(0L, result.startTimeMs)
        assertEquals(listOf("valid"), result.parsed.basList.map { it.content })
        val end = window.load(Long.MAX_VALUE - 1)
        assertEquals(Long.MAX_VALUE, end.endTimeMs)
        assertTrue(end.parsed.basList.isEmpty())
        window.clearPrograms()
        assertEquals(listOf("valid"), window.load(1_000).parsed.basList.map { it.content })
    }

    @Test
    fun readySourcesCanBeUsedBeforeLaterIndexesAndLateEarlierSourcesPreserveOrder() = runSuspend {
        val middle = Source(element(5_000, 9, "def text middle {content=\"middle\"}"))
        val earlier = Source(element(0, 9, "def text long {content=\"long\" duration=20s}"))
        val window = SpecialDanmakuWindow(listOf(middle.index()))
        assertTrue(window.load(1_000).parsed.basList.isEmpty())
        assertEquals(listOf("middle"), window.load(5_000).parsed.basList.map { it.content })
        window.addSource(earlier.index())
        assertEquals(listOf("long", "middle"), window.load(6_000).parsed.basList.map { it.content })
        assertEquals(listOf("long"), window.load(1_000).parsed.basList.map { it.content })
    }

    private class Source(vararg envelopes: ByteArray) : SpecialDanmakuSource {
        private val bytes = envelopes.fold(byteArrayOf()) { all, envelope -> all + envelope }
        override val byteLength = bytes.size.toLong()
        val requests = mutableListOf<Pair<Long, Int>>()
        private var entries: List<SpecialDanmakuEntry> = emptyList()

        override suspend fun readRange(offset: Long, byteCount: Int): ByteArray {
            synchronized(requests) { requests.add(offset to byteCount) }
            return bytes.copyOfRange(offset.toInt(), offset.toInt() + byteCount)
        }

        suspend fun window(): SpecialDanmakuWindow = SpecialDanmakuWindow(listOf(index()))

        suspend fun index(): IndexedSpecialDanmakuSource {
            val reader = SpecialDanmakuIndexReader(byteLength, this::readRange)
            entries = buildList { while (true) add(reader.next() ?: break) }
            requests.clear()
            return IndexedSpecialDanmakuSource(this, entries)
        }

        fun requestedStarts(): Set<Long> = requests.map { (offset, _) ->
            entries.single { it.offset == offset }.startTimeMs
        }.toSet()
    }

    private fun element(start: Long, mode: Int, content: String): ByteArray =
        field(1, scalar(1, start + 1) + scalar(2, start) + scalar(3, mode.toLong()) + field(7, content.encodeToByteArray()))

    private fun scalar(number: Int, value: Long): ByteArray = varint((number shl 3).toLong()) + varint(value)
    private fun field(number: Int, value: ByteArray): ByteArray =
        varint((number shl 3 or 2).toLong()) + varint(value.size.toLong()) + value

    private fun varint(value: Long): ByteArray {
        var remaining = value
        val bytes = mutableListOf<Byte>()
        do {
            var next = (remaining and 127).toInt()
            remaining = remaining ushr 7
            if (remaining != 0L) next = next or 128
            bytes.add(next.toByte())
        } while (remaining != 0L)
        return bytes.toByteArray()
    }

    private fun <T> runSuspend(block: suspend () -> T): T = runBlocking { block() }
}
