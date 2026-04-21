package com.android.purebilibili.feature.download

import android.content.pm.ServiceInfo
import android.os.Build

internal fun resolveDownloadForegroundServiceType(sdkInt: Int): Int? {
    return if (sdkInt >= Build.VERSION_CODES.Q) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    } else {
        null
    }
}
