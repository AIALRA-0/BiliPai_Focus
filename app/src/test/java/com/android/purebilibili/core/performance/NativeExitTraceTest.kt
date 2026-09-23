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
        assertTrue(trace.contains("Root-cause classification: Android Runtime (ART) JIT compiler"))
        assertTrue(trace.contains("FindDexMethodIndexInOtherDexFile"))
        assertContentEquals(tombstone, decodeRawTombstone(trace))
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
