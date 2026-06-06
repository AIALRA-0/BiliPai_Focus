package com.android.purebilibili.feature.download

import android.content.pm.ServiceInfo
import android.os.Build
import java.util.concurrent.TimeUnit

private val ANDROID_15_DATA_SYNC_FOREGROUND_SERVICE_LIMIT_MS = TimeUnit.HOURS.toMillis(6)
private val DOWNLOAD_FOREGROUND_SERVICE_TIMEOUT_SAFETY_BUFFER_MS = TimeUnit.MINUTES.toMillis(15)

internal fun resolveDownloadForegroundServiceType(sdkInt: Int): Int? {
    return if (sdkInt >= Build.VERSION_CODES.Q) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    } else {
        null
    }
}

internal fun resolveDownloadForegroundWorkTimeoutMs(sdkInt: Int): Long? {
    return if (sdkInt >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        ANDROID_15_DATA_SYNC_FOREGROUND_SERVICE_LIMIT_MS -
            DOWNLOAD_FOREGROUND_SERVICE_TIMEOUT_SAFETY_BUFFER_MS
    } else {
        null
    }
}
