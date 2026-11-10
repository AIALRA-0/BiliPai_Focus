package com.android.purebilibili.core.paging

import androidx.paging.LoadState
import androidx.paging.PagingConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForwardPagingSessionTest {
    @Test fun appendFailureKeepsPresentedRowsAndRetryReusesFailedKey() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var items = emptyList<Int>()
            var appendError = false
            var shouldFail = true
            val requested = mutableListOf<Int>()
            val session = ForwardPagingSession(
                config = PagingConfig(pageSize = 1, initialLoadSize = 1, prefetchDistance = 1, enablePlaceholders = false),
                sourceFactory = {
                    ForwardPagingSource<Int, Int>(1) { key ->
                        requested += key
                        if (key == 2 && shouldFail) { shouldFail = false; error("暂时失败") }
                        ForwardPage(listOf(key), (key + 1).takeIf { key < 2 })
                    }
                },
                onChanged = { rows, states -> items = rows; appendError = states?.append is LoadState.Error }
            )
            val collection = backgroundScope.launch { session.run() }
            runCurrent()
            session.awaitInitialLoad()
            assertEquals(listOf(1), items)
            session.append()
            runCurrent()
            assertTrue(appendError)
            assertEquals(listOf(1), items)
            session.retry()
            runCurrent()
            assertFalse(appendError)
            assertEquals(listOf(1, 2), items)
            assertEquals(listOf(1, 2, 2), requested)
            collection.cancelAndJoin()
        } finally { Dispatchers.resetMain() }
    }

    @Test fun initialFailureFinishesRefreshWaitAndCanRetryWithoutANewSession() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var items = emptyList<Int>()
            var shouldFail = true
            val session = ForwardPagingSession(
                config = PagingConfig(pageSize = 1, initialLoadSize = 1, enablePlaceholders = false),
                sourceFactory = {
                    ForwardPagingSource<Int, Int>(1) {
                        if (shouldFail) { shouldFail = false; error("暂时失败") }
                        ForwardPage(listOf(1), nextKey = null)
                    }
                },
                onChanged = { rows, _ -> items = rows }
            )
            val collection = backgroundScope.launch { session.run() }
            runCurrent()
            session.awaitInitialLoad()
            assertTrue(items.isEmpty())
            session.retry()
            runCurrent()
            assertEquals(listOf(1), items)
            collection.cancelAndJoin()
        } finally { Dispatchers.resetMain() }
    }
}
