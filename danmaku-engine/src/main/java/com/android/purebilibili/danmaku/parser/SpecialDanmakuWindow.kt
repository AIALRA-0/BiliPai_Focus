package com.android.purebilibili.danmaku.parser

import com.android.purebilibili.danmaku.engine.DanmakuItem
import com.android.purebilibili.danmaku.parser.bas.BasDanmaku
import com.android.purebilibili.danmaku.parser.bas.BasParseException
import com.android.purebilibili.danmaku.parser.bas.BasScriptParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/** A seekable remote range or an existing local file; never a whole-file byte cache. */
interface SpecialDanmakuSource {
    val byteLength: Long
    suspend fun readRange(offset: Long, byteCount: Int): ByteArray
}

data class IndexedSpecialDanmakuSource(
    val source: SpecialDanmakuSource,
    val entries: List<SpecialDanmakuEntry>
)

data class SpecialDanmakuWindowResult(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val parsed: ParsedDanmaku
)

/**
 * Retains compact offsets/lifetimes for the session, but compiled scenes only for the playback
 * window. Calls must be serialized by the owner. A cold seek has to inspect unknown earlier BAS
 * lifetimes: protobuf supplies a start time, not an end time. That pass discards visual values.
 */
class SpecialDanmakuWindow(sources: List<IndexedSpecialDanmakuSource> = emptyList()) {
    private class Record(val source: SpecialDanmakuSource, val entry: SpecialDanmakuEntry) {
        var durationMs: Long? = null
        var parsed: ParsedDanmaku? = null
    }

    private val records = ArrayList<Record>()

    init {
        for (source in sources) addSource(source)
    }

    /** The owner serializes index additions with [load], so ready files need not wait for others. */
    fun addSource(indexed: IndexedSpecialDanmakuSource) {
        for (entry in indexed.entries) {
            if (entry.mode < 8 || entry.mode == 9) records.add(Record(indexed.source, entry))
        }
        records.sortBy { it.entry.startTimeMs }
    }

    fun clearPrograms() {
        for (record in records) record.parsed = null
    }

    suspend fun load(
        positionMs: Long,
        lookAheadMs: Long = 3_000L,
        standardDurationMs: Long = 10_000L
    ): SpecialDanmakuWindowResult {
        require(lookAheadMs > 0L)
        val start = positionMs.coerceAtLeast(0L)
        val end = saturatedAdd(start, lookAheadMs)
        for (record in records) {
            if (record.entry.mode != 7 && record.entry.mode != 9) {
                record.durationMs = standardDurationMs.coerceAtLeast(0L)
            }
            if (record.entry.startTimeMs >= end || !overlaps(record, start)) record.parsed = null
        }

        var index = 0
        while (index < records.size && records[index].entry.startTimeMs < end) {
            currentCoroutineContext().ensureActive()
            coroutineScope {
                var pending = 0
                while (index < records.size && records[index].entry.startTimeMs < end && pending < 4) {
                    val record = records[index++]
                    if (record.parsed != null || !overlaps(record, start)) continue
                    pending++
                    launch(Dispatchers.Default) { loadRecord(record, start, standardDurationMs) }
                }
            }
        }
        val standard = mutableListOf<DanmakuItem>()
        val advanced = mutableListOf<AdvancedDanmakuData>()
        val bas = mutableListOf<BasDanmaku>()
        for (record in records) {
            if (record.entry.startTimeMs >= end) break
            val parsed = record.parsed ?: continue
            standard.addAll(parsed.standardList)
            advanced.addAll(parsed.advancedList)
            bas.addAll(parsed.basList)
        }
        return SpecialDanmakuWindowResult(start, end, ParsedDanmaku(standard, advanced, basList = bas))
    }

    private suspend fun loadRecord(record: Record, start: Long, standardDurationMs: Long) {
        val entry = record.entry
        val bytes = record.source.readRange(entry.offset, entry.byteLength)
        val elem = DanmakuProto.parseReply(bytes).elems.singleOrNull()
        if (elem == null) {
            record.durationMs = 0L
            return
        }
        if (entry.mode == 9 && entry.startTimeMs < start && record.durationMs == null) {
            record.durationMs = try {
                BasScriptParser.parseDurationMs(elem.content)
            } catch (_: BasParseException) {
                0L
            }
            if (!overlaps(record, start)) return
        }
        val parsed = DanmakuParser.parseElement(elem)
        currentCoroutineContext().ensureActive()
        record.durationMs = when {
            parsed.basList.isNotEmpty() -> parsed.basList.single().durationMs
            parsed.advancedList.isNotEmpty() -> parsed.advancedList.single().durationMs
            parsed.standardList.isNotEmpty() -> standardDurationMs.coerceAtLeast(0L)
            else -> 0L
        }
        if (overlaps(record, start)) record.parsed = parsed
    }

    private fun overlaps(record: Record, positionMs: Long): Boolean {
        val duration = record.durationMs ?: return true
        return duration > 0L && saturatedAdd(record.entry.startTimeMs, duration) > positionMs
    }

    private fun saturatedAdd(start: Long, duration: Long): Long =
        if (start > Long.MAX_VALUE - duration) Long.MAX_VALUE else start + duration
}
