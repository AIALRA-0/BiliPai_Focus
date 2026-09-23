package com.android.purebilibili.core.refresh

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HistoryRefreshSuppressionTest {

    @Test
    fun `notify while suppressed defers emission until resume`() = runBlocking {
        try {
            HistoryRefreshSuppression.suppress()
            HistoryRefreshBus.notifyChanged()

            val deferred = async { HistoryRefreshBus.changes.first() }
            delay(100)
            assertTrue(deferred.isActive, "抑制期间刷新信号不应被发射")

            HistoryRefreshSuppression.resume()
            assertNotNull(withTimeout(1000) { deferred.await() }, "恢复后应补发一次刷新")
        } finally {
            while (HistoryRefreshSuppression.isSuppressed) {
                HistoryRefreshSuppression.resume()
            }
        }
    }

    @Test
    fun `unsuppressed notify emits immediately`() = runBlocking {
        val deferred = async { HistoryRefreshBus.changes.first() }
        delay(50)
        HistoryRefreshBus.notifyChanged()
        assertNotNull(withTimeout(1000) { deferred.await() })
    }

    @Test
    fun `nested suppression requires matching resumes`() {
        try {
            HistoryRefreshSuppression.suppress()
            HistoryRefreshSuppression.suppress()
            assertTrue(HistoryRefreshSuppression.isSuppressed)

            HistoryRefreshSuppression.resume()
            assertTrue(HistoryRefreshSuppression.isSuppressed, "第一层恢复后仍处于抑制")

            HistoryRefreshSuppression.resume()
            assertFalse(HistoryRefreshSuppression.isSuppressed, "全部恢复后解除抑制")
        } finally {
            while (HistoryRefreshSuppression.isSuppressed) {
                HistoryRefreshSuppression.resume()
            }
        }
    }

    @Test
    fun `resume without suppression does not crash`() {
        HistoryRefreshSuppression.resume()
        assertFalse(HistoryRefreshSuppression.isSuppressed)
    }
}
