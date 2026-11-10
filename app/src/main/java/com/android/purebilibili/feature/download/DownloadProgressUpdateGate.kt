package com.android.purebilibili.feature.download

import android.os.SystemClock

/** Keeps parallel chunk callbacks ordered while limiting UI state updates. */
internal class DownloadProgressUpdateGate(
    private val nowMs: () -> Long = SystemClock::elapsedRealtime,
    private val intervalMs: Long = 200L,
) {
    private var lastUpdateMs = Long.MIN_VALUE
    private var lastDownloadedBytes = -1L
    private var lastTotalBytes = -1L

    @Synchronized
    fun emit(downloadedBytes: Long, totalBytes: Long, onProgress: (Long, Long) -> Unit) {
        val downloaded = downloadedBytes.coerceAtLeast(lastDownloadedBytes).coerceAtMost(totalBytes)
        if (downloaded == lastDownloadedBytes && totalBytes == lastTotalBytes) return

        val now = nowMs()
        if (lastUpdateMs != Long.MIN_VALUE &&
            downloaded < totalBytes &&
            now - lastUpdateMs < intervalMs
        ) return

        lastUpdateMs = now
        lastDownloadedBytes = downloaded
        lastTotalBytes = totalBytes
        onProgress(downloaded, totalBytes)
    }
}
