package com.android.purebilibili.feature.download

import android.content.pm.ServiceInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DownloadForegroundServicePolicyTest {

    @Test
    fun resolveDownloadForegroundServiceType_returnsDataSyncOnAndroidQAndAbove() {
        assertEquals(
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            resolveDownloadForegroundServiceType(sdkInt = 29)
        )
    }

    @Test
    fun resolveDownloadForegroundServiceType_returnsNullBelowAndroidQ() {
        assertNull(resolveDownloadForegroundServiceType(sdkInt = 28))
    }
}
