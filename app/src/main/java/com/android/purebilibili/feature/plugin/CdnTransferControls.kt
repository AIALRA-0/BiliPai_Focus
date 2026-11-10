package com.android.purebilibili.feature.plugin

import com.android.purebilibili.plugin.sdk.Plugin

interface CdnTransferControls : Plugin {
    fun canUseParallelDownload(): Boolean
    suspend fun setParallelDownloadEnabled(enabled: Boolean)
}
