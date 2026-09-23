package com.android.purebilibili.core.player

import androidx.media3.common.MimeTypes
import kotlin.test.Test
import kotlin.test.assertEquals

class HiResCompatibleRenderersFactoryTest {

    @Test
    fun `FLAC codec input buffer is raised above undersized platform default`() {
        assertEquals(
            HI_RES_FLAC_MIN_CODEC_INPUT_SIZE_BYTES,
            resolveHiResCodecMaxInputSize(
                defaultMaxInputSize = 32 * 1024,
                sampleMimeTypes = listOf(MimeTypes.AUDIO_FLAC)
            )
        )
    }

    @Test
    fun `larger FLAC codec input buffer is preserved`() {
        val existingSize = HI_RES_FLAC_MIN_CODEC_INPUT_SIZE_BYTES * 2

        assertEquals(
            existingSize,
            resolveHiResCodecMaxInputSize(
                defaultMaxInputSize = existingSize,
                sampleMimeTypes = listOf(MimeTypes.AUDIO_FLAC)
            )
        )
    }

    @Test
    fun `AAC codec input buffer remains unchanged`() {
        assertEquals(
            32 * 1024,
            resolveHiResCodecMaxInputSize(
                defaultMaxInputSize = 32 * 1024,
                sampleMimeTypes = listOf(MimeTypes.AUDIO_AAC)
            )
        )
    }
}
