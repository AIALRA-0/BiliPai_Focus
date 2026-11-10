package com.android.purebilibili.core.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class VideoDecoderPreferenceTest {
    private data class Decoder(val name: String, val hardware: Boolean = false, val software: Boolean = false)
    private val hardware1 = Decoder("vendor-1", hardware = true)
    private val hardware2 = Decoder("vendor-2", hardware = true)
    private val software1 = Decoder("system-1", software = true)
    private val software2 = Decoder("system-2", software = true)
    private val unknown = Decoder("unknown")

    private fun order(decoders: List<Decoder>, hardware: Boolean) =
        orderVideoDecoders("video/avc", decoders, hardware, { it.hardware }, { it.software })

    @Test fun `software preference retains all hardware fallback candidates and platform order within each type`() {
        assertEquals(
            listOf(software1, software2, unknown, hardware1, hardware2),
            order(listOf(hardware1, software1, unknown, hardware2, software2), hardware = false),
        )
    }

    @Test fun `hardware preference promotes hardware while retaining unknown and software candidates`() {
        assertEquals(
            listOf(hardware2, hardware1, unknown, software2, software1),
            order(listOf(software2, hardware2, unknown, software1, hardware1), hardware = true),
        )
    }

    @Test fun `missing preferred type does not remove available decoders`() {
        assertEquals(listOf(hardware1, hardware2), order(listOf(hardware1, hardware2), hardware = false))
        assertEquals(listOf(software1), order(listOf(software1), hardware = true))
        assertEquals(emptyList<Decoder>(), order(emptyList(), hardware = false))
    }

    @Test fun `audio selection returns the platform list without classifying or reordering`() {
        val audio = listOf(software1, hardware1)
        val result = orderVideoDecoders("audio/eac3", audio, true,
            { error("Video classification must not run for audio") },
            { error("Video classification must not run for audio") })
        assertSame(audio, result)
    }
}
