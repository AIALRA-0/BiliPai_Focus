package com.android.purebilibili.core.performance

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Base64

private const val TOMBSTONE_BEGIN = "----- BEGIN TOMBSTONE PROTOBUF BASE64 -----"
private const val TOMBSTONE_END = "----- END TOMBSTONE PROTOBUF BASE64 -----"

/** The readable crash summary can be sanitized without touching the encoded protobuf. */
internal fun nativeExitTraceSummary(trace: String): String =
    trace.substringBefore(TOMBSTONE_BEGIN).trimEnd()

internal fun decodeNativeExitTrace(trace: String): ByteArray? {
    val start = trace.indexOf(TOMBSTONE_BEGIN)
    if (start < 0) return null
    val end = trace.indexOf(TOMBSTONE_END, start + TOMBSTONE_BEGIN.length)
    if (end < 0) return null
    val payload = trace.substring(start + TOMBSTONE_BEGIN.length, end).trim()
    return runCatching { Base64.getDecoder().decode(payload) }.getOrNull()
}

private data class NativeBacktraceFrame(
    val relativePc: Long?,
    val functionName: String?,
    val fileName: String?,
    val functionOffset: Long? = null,
    val buildId: String? = null,
)

private data class NativeCrashSummary(
    val signalName: String?,
    val signalCodeName: String?,
    val faultAddress: Long?,
    val threadId: Long?,
    val threadName: String?,
    val frames: List<NativeBacktraceFrame>,
    val buildFingerprint: String?,
    val timestamp: String?,
    val processUptime: Long?,
    val pageSize: Long?,
    val mainThreadId: Long?,
    val threads: Map<Long, Pair<String?, List<NativeBacktraceFrame>>>,
) {
    val isArtJitInliningCrash: Boolean
        get() = threadName == "Jit thread pool" &&
            frames.none { frame ->
                frame.fileName?.let { it.startsWith("/data/app/") && it.endsWith(".so") } == true
            } &&
            frames.any { frame ->
                frame.fileName?.endsWith("/libart.so") == true &&
                    frame.functionName?.let { name ->
                        name.contains("HInliner") ||
                            name.contains("FindDexMethodIndexInOtherDexFile")
                    } == true
            }
}

/** Preserve binary tombstones without UTF-8 replacement or line-based truncation. */
internal fun encodeNativeExitTrace(stream: InputStream, maxBytes: Int = 4 * 1024 * 1024): String? {
    require(maxBytes > 0)
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (output.size() < maxBytes) {
        val count = stream.read(buffer, 0, minOf(buffer.size, maxBytes - output.size()))
        if (count < 0) break
        if (count == 0) continue
        output.write(buffer, 0, count)
    }
    if (output.size() == 0) return null
    val truncated = stream.read() != -1
    val bytes = output.toByteArray()
    val summary = parseNativeCrashSummary(bytes)
    return buildString {
        appendLine("Native tombstone: protobuf/base64; truncated=$truncated; bytes=${output.size()}")
        if (summary != null) {
            appendNativeCrashSummary(summary)
        } else {
            appendLine("Native crash summary: unavailable")
        }
        appendLine(TOMBSTONE_BEGIN)
        appendLine(Base64.getEncoder().encodeToString(bytes))
        append(TOMBSTONE_END)
    }
}

private fun StringBuilder.appendNativeCrashSummary(summary: NativeCrashSummary) {
    summary.buildFingerprint?.let { appendLine("System build fingerprint: $it") }
    summary.timestamp?.let { appendLine("Native crash timestamp: $it") }
    summary.processUptime?.let { appendLine("Process uptime: ${it}s") }
    summary.pageSize?.let { appendLine("Memory page size: $it bytes") }
    appendLine("Captured threads: ${summary.threads.size}")
    append("Native crash summary: signal=")
    append(summary.signalName ?: "unknown")
    summary.signalCodeName?.let { append(" (").append(it).append(')') }
    summary.faultAddress?.let { append(", faultAddress=0x").append(it.toString(16)) }
    appendLine()
    append("Crashing thread: ")
    append(summary.threadName ?: "unknown")
    summary.threadId?.let { append(" (tid=").append(it).append(')') }
    appendLine()
    if (summary.isArtJitInliningCrash) {
        appendLine(
            "Crash-site classification: Android Runtime (ART) JIT compiler crashed while " +
                "inlining Dex code; no app native library appears in the crashing stack."
        )
        appendLine("提示: 此分类仅定位崩溃位置，不能确定触发的业务方法或系统缺陷；JIT 编译目标可能未包含在回溯中。")
    }
    if (summary.frames.isNotEmpty()) {
        if (summary.threadName == "RenderThread") {
            appendLine("排查方向: 界面原生渲染崩溃；仅凭渲染栈不能确定具体组件或根因。")
            val repeatedFrames = summary.frames.mapNotNull { it.functionName }
                .groupingBy { it }.eachCount().filterValues { it >= 8 }
            if (repeatedFrames.isNotEmpty()) {
                appendLine("重复栈帧: ${repeatedFrames.entries.joinToString { "${it.key} ×${it.value}" }}")
                appendLine("提示: 检查渲染层级递归或背景自采样；重复帧不等同于已确认根因。")
            }
        }
        appendLine("Crashing thread backtrace: ${summary.frames.size} frames (all decoded frames)")
        summary.frames.forEachIndexed { index, frame ->
            append("  #")
            append(index.toString().padStart(2, '0'))
            frame.relativePc?.let { append(" pc 0x").append(it.toString(16)) }
            append(' ')
            append(frame.functionName ?: "<unknown>")
            frame.functionOffset?.let { append(" +").append(it) }
            frame.fileName?.let { append(" (").append(it).append(')') }
            frame.buildId?.let { append(" [BuildId: ").append(it).append(']') }
            appendLine()
        }
    }
    // These are concurrent snapshots, not the caller chain of the crashing JIT worker.
    summary.threads.filter { (id, thread) ->
        id != summary.threadId && (id == summary.mainThreadId || thread.first == "RenderThread")
    }.forEach { (id, thread) ->
        appendLine("Other thread snapshot (causality unknown): ${thread.first ?: "unknown"} (tid=$id)")
        thread.second.take(120).forEachIndexed { index, frame ->
            append("  #${index.toString().padStart(2, '0')} ")
            frame.relativePc?.let { append("pc 0x${it.toString(16)} ") }
            append(frame.functionName ?: "<unknown>")
            frame.functionOffset?.let { append(" +$it") }
            frame.fileName?.let { append(" ($it)") }
            frame.buildId?.let { append(" [BuildId: $it]") }
            appendLine()
        }
        if (thread.second.size > 120) appendLine("  [text limited to 120 frames; full frames in .pb]")
    }
}

private fun parseNativeCrashSummary(bytes: ByteArray): NativeCrashSummary? = runCatching {
    var signalName: String? = null
    var signalCodeName: String? = null
    var faultAddress: Long? = null
    var crashingThreadId: Long? = null
    var buildFingerprint: String? = null
    var timestamp: String? = null
    var processUptime: Long? = null
    var pageSize: Long? = null
    var processId: Long? = null
    val threads = mutableMapOf<Long, Pair<String?, List<NativeBacktraceFrame>>>()

    ProtoReader(bytes).forEachField { number, wireType, value ->
        when (number) {
            2 -> if (wireType == WIRE_LENGTH_DELIMITED) buildFingerprint = value.asUtf8()
            4 -> if (wireType == WIRE_LENGTH_DELIMITED) timestamp = value.asUtf8()
            5 -> if (wireType == WIRE_VARINT) processId = value.asLong()
            6 -> if (wireType == WIRE_VARINT) crashingThreadId = value.asLong()
            20 -> if (wireType == WIRE_VARINT) processUptime = value.asLong()
            22 -> if (wireType == WIRE_VARINT) pageSize = value.asLong()
            10 -> if (wireType == WIRE_LENGTH_DELIMITED) {
                ProtoReader(value.asBytes()).forEachField { signalField, signalWireType, signalValue ->
                    when (signalField) {
                        2 -> if (signalWireType == WIRE_LENGTH_DELIMITED) {
                            signalName = signalValue.asUtf8()
                        }
                        4 -> if (signalWireType == WIRE_LENGTH_DELIMITED) {
                            signalCodeName = signalValue.asUtf8()
                        }
                        9 -> if (signalWireType == WIRE_VARINT) faultAddress = signalValue.asLong()
                    }
                }
            }
            16 -> if (wireType == WIRE_LENGTH_DELIMITED) {
                parseThreadMapEntry(value.asBytes())?.let { (id, thread) -> threads[id] = thread }
            }
        }
    }

    val crashingThread = crashingThreadId?.let(threads::get)
    NativeCrashSummary(
        signalName = signalName,
        signalCodeName = signalCodeName,
        faultAddress = faultAddress,
        threadId = crashingThreadId,
        threadName = crashingThread?.first,
        frames = crashingThread?.second.orEmpty(),
        buildFingerprint = buildFingerprint,
        timestamp = timestamp,
        processUptime = processUptime,
        pageSize = pageSize,
        mainThreadId = processId,
        threads = threads,
    )
}.getOrNull()

private fun parseThreadMapEntry(
    bytes: ByteArray
): Pair<Long, Pair<String?, List<NativeBacktraceFrame>>>? {
    var id: Long? = null
    var threadBytes: ByteArray? = null
    ProtoReader(bytes).forEachField { number, wireType, value ->
        when (number) {
            1 -> if (wireType == WIRE_VARINT) id = value.asLong()
            2 -> if (wireType == WIRE_LENGTH_DELIMITED) threadBytes = value.asBytes()
        }
    }
    val resolvedId = id ?: return null
    val payload = threadBytes ?: return null
    var name: String? = null
    val frames = mutableListOf<NativeBacktraceFrame>()
    ProtoReader(payload).forEachField { number, wireType, value ->
        when (number) {
            2 -> if (wireType == WIRE_LENGTH_DELIMITED) name = value.asUtf8()
            4 -> if (wireType == WIRE_LENGTH_DELIMITED) {
                parseBacktraceFrame(value.asBytes())?.let(frames::add)
            }
        }
    }
    return resolvedId to (name to frames)
}

private fun parseBacktraceFrame(bytes: ByteArray): NativeBacktraceFrame? {
    var relativePc: Long? = null
    var functionName: String? = null
    var fileName: String? = null
    var functionOffset: Long? = null
    var buildId: String? = null
    ProtoReader(bytes).forEachField { number, wireType, value ->
        when (number) {
            1 -> if (wireType == WIRE_VARINT) relativePc = value.asLong()
            4 -> if (wireType == WIRE_LENGTH_DELIMITED) functionName = value.asUtf8()
            5 -> if (wireType == WIRE_VARINT) functionOffset = value.asLong()
            6 -> if (wireType == WIRE_LENGTH_DELIMITED) fileName = value.asUtf8()
            8 -> if (wireType == WIRE_LENGTH_DELIMITED) buildId = value.asUtf8()
        }
    }
    if (relativePc == null && functionName == null && fileName == null) return null
    return NativeBacktraceFrame(relativePc, functionName, fileName, functionOffset, buildId)
}

private const val WIRE_VARINT = 0
private const val WIRE_FIXED_64 = 1
private const val WIRE_LENGTH_DELIMITED = 2
private const val WIRE_FIXED_32 = 5

private sealed class ProtoValue {
    data class Varint(val value: Long) : ProtoValue()
    data class Bytes(val value: ByteArray) : ProtoValue()

    fun asLong(): Long = (this as Varint).value
    fun asBytes(): ByteArray = (this as Bytes).value
    fun asUtf8(): String = asBytes().toString(Charsets.UTF_8)
}

private class ProtoReader(private val bytes: ByteArray) {
    private var position = 0

    fun forEachField(block: (number: Int, wireType: Int, value: ProtoValue) -> Unit) {
        while (position < bytes.size) {
            val key = readVarint()
            val fieldNumber = (key ushr 3).toInt()
            val wireType = (key and 0x7).toInt()
            require(fieldNumber > 0) { "Invalid protobuf field number" }
            when (wireType) {
                WIRE_VARINT -> block(fieldNumber, wireType, ProtoValue.Varint(readVarint()))
                WIRE_FIXED_64 -> skip(8)
                WIRE_LENGTH_DELIMITED -> {
                    val length = readVarint()
                    require(length in 0..Int.MAX_VALUE.toLong()) { "Invalid protobuf length" }
                    block(fieldNumber, wireType, ProtoValue.Bytes(readBytes(length.toInt())))
                }
                WIRE_FIXED_32 -> skip(4)
                else -> error("Unsupported protobuf wire type: $wireType")
            }
        }
    }

    private fun readVarint(): Long {
        var result = 0L
        var shift = 0
        while (shift < 64) {
            require(position < bytes.size) { "Truncated protobuf varint" }
            val next = bytes[position++].toInt() and 0xff
            result = result or ((next and 0x7f).toLong() shl shift)
            if (next and 0x80 == 0) return result
            shift += 7
        }
        error("Invalid protobuf varint")
    }

    private fun readBytes(length: Int): ByteArray {
        require(length <= bytes.size - position) { "Truncated protobuf field" }
        return bytes.copyOfRange(position, position + length).also { position += length }
    }

    private fun skip(length: Int) {
        require(length <= bytes.size - position) { "Truncated protobuf field" }
        position += length
    }
}
