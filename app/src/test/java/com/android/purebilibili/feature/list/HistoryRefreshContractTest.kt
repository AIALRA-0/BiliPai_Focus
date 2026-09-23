package com.android.purebilibili.feature.list

import com.android.purebilibili.core.refresh.HistoryRefreshBus
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class HistoryRefreshContractTest {
    @Test
    fun historyRefreshSignal_emitsForActiveCollectors() = runTest {
        val events = Channel<Unit>(capacity = 1)
        val collector = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            HistoryRefreshBus.changes.collect {
                events.send(Unit)
                cancel()
            }
        }

        HistoryRefreshBus.notifyChanged()

        events.receive()
        collector.cancel()
    }

    @Test
    fun videoRepository_notifiesHistoryRefreshAfterSuccessfulHeartbeat() {
        val source = sourceText("src/main/java/com/android/purebilibili/data/repository/VideoRepository.kt")

        assertTrue(
            source.contains("HistoryRefreshBus.notifyChanged()"),
            "播放心跳上报成功后必须通知历史记录列表刷新"
        )
    }

    @Test
    fun historyViewModel_collectsRefreshSignalAndReloadsList() {
        val source = sourceText("src/main/java/com/android/purebilibili/feature/list/ListViewModel.kt")
        val historySection = source.substringAfter("class HistoryViewModel")

        assertTrue(
            historySection.contains("HistoryRefreshBus.changes.collect"),
            "历史记录列表必须订阅刷新信号"
        )
        assertTrue(
            historySection.contains("loadData("),
            "收到刷新信号后必须重新加载列表"
        )
    }

    @Test
    fun bottomHistoryTab_refreshesWhenPagerPageBecomesActive() {
        val source = sourceText("src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")
        val historySection = source.substringAfter("BiliPaiNavEntryContentRole.HISTORY")

        assertTrue(
            historySection.contains("isBottomPagerPageActive"),
            "历史记录底栏页必须感知 pager 激活状态"
        )
        assertTrue(
            historySection.contains("historyViewModel.loadData("),
            "历史记录底栏页激活时必须重新拉取列表"
        )
    }

    private fun sourceText(path: String): String {
        val sourceFile = listOf(
            File(path),
            File("app/$path")
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}