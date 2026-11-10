package com.android.purebilibili.feature.download

import androidx.media3.common.PlaybackException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OfflinePlaybackErrorPolicyTest {
    @Test
    fun missingAndCorruptFilesSuggestDownloadingAgain() {
        listOf(
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        ).forEach { code ->
            val failure = resolveOfflinePlaybackFailure(code)
            assertFalse(failure.canRetry)
            assertTrue(failure.message.contains("重新下载"))
        }
    }

    @Test
    fun decoderFailureOffersPlaybackRetry() {
        assertTrue(resolveOfflinePlaybackFailure(PlaybackException.ERROR_CODE_DECODER_INIT_FAILED).canRetry)
        assertTrue(resolveOfflinePlaybackFailure(PlaybackException.ERROR_CODE_DECODING_FAILED).canRetry)
    }
}
