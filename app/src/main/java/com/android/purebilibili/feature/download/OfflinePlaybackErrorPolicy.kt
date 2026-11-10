package com.android.purebilibili.feature.download

import androidx.media3.common.PlaybackException

internal data class OfflinePlaybackFailure(val message: String, val canRetry: Boolean)

internal fun resolveOfflinePlaybackFailure(errorCode: Int): OfflinePlaybackFailure = when (errorCode) {
    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> OfflinePlaybackFailure(
        "本地视频文件已丢失，请返回下载列表重新下载。", false
    )
    PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> OfflinePlaybackFailure(
        "无法读取本地视频文件，请检查文件访问权限，或返回下载列表重新下载。", false
    )
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> OfflinePlaybackFailure(
        "视频文件损坏或格式不受支持，请返回下载列表重新下载。", false
    )
    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
    PlaybackException.ERROR_CODE_DECODING_FAILED -> OfflinePlaybackFailure(
        "视频解码失败，可以重新播放；若仍失败，请返回下载列表重新下载。", true
    )
    else -> OfflinePlaybackFailure(
        "本地视频播放失败，可以重新播放；若仍失败，请检查文件或重新下载。", true
    )
}
