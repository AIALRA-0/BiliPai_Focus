package com.android.purebilibili.core.performance

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NativeExitTraceTest {
    @Test
    fun binaryTraceSurvivesInvalidUtf8AndEmbeddedNewlines() {
        val bytes = ByteArray(4096) { it.toByte() }
        val trace = requireNotNull(encodeNativeExitTrace(bytes.inputStream()))
        assertTrue(trace.contains("truncated=false"))
        assertContentEquals(bytes, decodeRawTombstone(trace))
        assertContentEquals(bytes, decodeNativeExitTrace(trace))
        assertTrue(nativeExitTraceSummary(trace).contains("Native tombstone: protobuf/base64"))
        assertTrue(!nativeExitTraceSummary(trace).contains("BEGIN TOMBSTONE"))
    }

    @Test
    fun oversizedTraceIsExplicitlyMarkedAndBounded() {
        val trace = requireNotNull(encodeNativeExitTrace(ByteArray(32).inputStream(), maxBytes = 16))
        assertTrue(trace.contains("truncated=true; bytes=16"))
        assertTrue(decodeRawTombstone(trace).size == 16)
        assertNull(encodeNativeExitTrace(byteArrayOf().inputStream()))
    }

    @Test
    fun nativeArtJitCrashIsSummarizedBeforeRawPayload() {
        val artFrame = message(
            varintField(1, 0x62008c),
            bytesField(4, text("art::ArtMethod::FindDexMethodIndexInOtherDexFile")),
            bytesField(6, text("/apex/com.android.art/lib64/libart.so"))
        )
        val thread = message(
            varintField(1, 30046),
            bytesField(2, text("Jit thread pool")),
            bytesField(4, artFrame)
        )
        val threadEntry = message(varintField(1, 30046), bytesField(2, thread))
        val signal = message(
            bytesField(2, text("SIGSEGV")),
            bytesField(4, text("SEGV_ACCERR")),
            varintField(9, 0x1234)
        )
        val tombstone = message(
            varintField(6, 30046),
            bytesField(10, signal),
            bytesField(16, threadEntry)
        )

        val trace = requireNotNull(encodeNativeExitTrace(tombstone.inputStream()))

        assertTrue(trace.contains("signal=SIGSEGV (SEGV_ACCERR), faultAddress=0x1234"))
        assertTrue(trace.contains("Crashing thread: Jit thread pool (tid=30046)"))
        assertTrue(trace.contains("Crash-site classification: Android Runtime (ART) JIT compiler"))
        assertTrue(trace.contains("FindDexMethodIndexInOtherDexFile"))
        assertContentEquals(tombstone, decodeRawTombstone(trace))
    }

    @Test
    fun renderCrashKeepsDeepFramesAndReportsRecursionAsAHint() {
        val frames = (0 until 80).map { index ->
            bytesField(4, message(
                varintField(1, index),
                bytesField(4, text(if (index < 79) "RenderNode::prepareTreeImpl" else "deep_origin")),
                bytesField(6, text("/system/lib64/libhwui.so"))
            ))
        }
        val thread = message(bytesField(2, text("RenderThread")), *frames.toTypedArray())
        val tombstone = message(
            varintField(6, 42),
            bytesField(16, message(varintField(1, 42), bytesField(2, thread)))
        )
        val trace = requireNotNull(encodeNativeExitTrace(tombstone.inputStream()))
        val summary = nativeExitTraceSummary(trace)
        assertTrue(summary.contains("80 frames (all decoded frames)"))
        assertTrue(summary.contains("#79 pc 0x4f deep_origin"))
        assertTrue(summary.contains("RenderNode::prepareTreeImpl ×79"))
        assertTrue(summary.contains("重复帧不等同于已确认根因"))
        assertContentEquals(tombstone, decodeNativeExitTrace(trace))
    }

    @Test
    fun summaryIncludesSystemIdentityAndConcurrentMainThreadWithoutClaimingCausality() {
        val frame = message(
            varintField(1, 0x62028c), bytesField(4, text("compiled_method")),
            varintField(5, 128), bytesField(6, text("/apex/com.android.art/lib64/libart.so")),
            bytesField(8, text("art-build-id")),
        )
        val tombstone = message(
            bytesField(2, text("OnePlus/test:17/build")),
            bytesField(4, text("2026-10-23 21:01:57+0800")),
            varintField(5, 100), varintField(6, 101), varintField(20, 48), varintField(22, 4096),
            bytesField(16, message(varintField(1, 101), bytesField(2, message(
                bytesField(2, text("Jit thread pool")), bytesField(4, frame))))),
            bytesField(16, message(varintField(1, 100), bytesField(2, message(
                bytesField(2, text("main")), bytesField(4, frame))))),
        )
        val trace = requireNotNull(encodeNativeExitTrace(tombstone.inputStream()))
        assertTrue(trace.contains("System build fingerprint: OnePlus/test:17/build"))
        assertTrue(trace.contains("Process uptime: 48s"))
        assertTrue(trace.contains("Memory page size: 4096 bytes"))
        assertTrue(trace.contains("Captured threads: 2"))
        assertTrue(trace.contains("compiled_method +128"))
        assertTrue(trace.contains("[BuildId: art-build-id]"))
        assertTrue(trace.contains("Other thread snapshot (causality unknown): main (tid=100)"))
        assertContentEquals(tombstone, decodeNativeExitTrace(trace))
    }

    private fun decodeRawTombstone(trace: String): ByteArray {
        val payload = trace.substringAfter("----- BEGIN TOMBSTONE PROTOBUF BASE64 -----\n")
            .substringBefore("\n----- END TOMBSTONE PROTOBUF BASE64 -----")
        return Base64.getDecoder().decode(payload)
    }

    private fun varintField(number: Int, value: Int): ByteArray =
        varint(number shl 3) + varint(value)

    private fun bytesField(number: Int, payload: ByteArray): ByteArray =
        varint((number shl 3) or 2) + varint(payload.size) + payload

    private fun text(value: String): ByteArray = value.toByteArray()

    private fun message(vararg fields: ByteArray): ByteArray =
        fields.fold(ByteArray(0)) { result, field -> result + field }

    private fun varint(value: Int): ByteArray {
        var remaining = value.toLong()
        val bytes = mutableListOf<Byte>()
        do {
            var next = (remaining and 0x7f).toInt()
            remaining = remaining ushr 7
            if (remaining != 0L) next = next or 0x80
            bytes += next.toByte()
        } while (remaining != 0L)
        return bytes.toByteArray()
    }
}
