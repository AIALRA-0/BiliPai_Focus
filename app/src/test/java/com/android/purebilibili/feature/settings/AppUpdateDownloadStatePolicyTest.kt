package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class AppUpdateDownloadStatePolicyTest {

    @Test
    fun startDownload_initializesDownloadingState() {
        val state = startAppUpdateDownload(totalBytes = 200L)

        assertEquals(AppUpdateDownloadStatus.DOWNLOADING, state.status)
        assertEquals(0L, state.downloadedBytes)
        assertEquals(200L, state.totalBytes)
        assertEquals(0f, state.progress)
    }

    @Test
    fun updateProgress_tracksClampedFraction() {
        val state = updateAppUpdateDownloadProgress(
            current = startAppUpdateDownload(totalBytes = 200L),
            downloadedBytes = 50L
        )

        assertEquals(AppUpdateDownloadStatus.DOWNLOADING, state.status)
        assertEquals(50L, state.downloadedBytes)
        assertEquals(0.25f, state.progress)
    }

    @Test
    fun completeDownload_marksStateComplete_andStoresFilePath() {
        val state = completeAppUpdateDownload(
            current = startAppUpdateDownload(totalBytes = 200L),
            filePath = "/tmp/BiliPai.apk"
        )

        assertEquals(AppUpdateDownloadStatus.COMPLETED, state.status)
        assertEquals(1f, state.progress)
        assertEquals("/tmp/BiliPai.apk", state.filePath)
    }

    @Test
    fun failDownload_marksStateFailed_andPreservesMessage() {
        val state = failAppUpdateDownload(
            current = startAppUpdateDownload(totalBytes = 200L),
            errorMessage = "network"
        )

        assertEquals(AppUpdateDownloadStatus.FAILED, state.status)
        assertEquals("network", state.errorMessage)
    }

    @Test
    fun queuedDownload_isAnExplicitRecoverableState() {
        val state = AppUpdateDownloadState(status = AppUpdateDownloadStatus.QUEUED)

        assertEquals(AppUpdateDownloadStatus.QUEUED, state.status)
        assertEquals(0f, state.progress)
    }
}
