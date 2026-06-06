package com.android.purebilibili.feature.download

import android.content.pm.ServiceInfo
import java.util.concurrent.TimeUnit
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

    @Test
    fun resolveDownloadForegroundWorkTimeoutMs_capsAndroid15BeforeSystemLimit() {
        assertEquals(
            TimeUnit.HOURS.toMillis(5) + TimeUnit.MINUTES.toMillis(45),
            resolveDownloadForegroundWorkTimeoutMs(sdkInt = 35)
        )
    }

    @Test
    fun resolveDownloadForegroundWorkTimeoutMs_returnsNullBelowAndroid15() {
        assertNull(resolveDownloadForegroundWorkTimeoutMs(sdkInt = 34))
    }
}
