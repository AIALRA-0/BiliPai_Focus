package com.android.purebilibili.feature.nearby

import java.io.Closeable
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** 阻塞式 TLS 读写不会响应协程取消，必须同步关闭套接字解除阻塞。 */
internal suspend fun <T> withNearbySocket(resource: Closeable, block: suspend () -> T): T = coroutineScope {
    val cancellationCloser = launch(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
        try { awaitCancellation() }
        finally { runCatching { resource.close() } }
    }
    try { block() }
    finally {
        runCatching { resource.close() }
        cancellationCloser.cancel()
    }
}
