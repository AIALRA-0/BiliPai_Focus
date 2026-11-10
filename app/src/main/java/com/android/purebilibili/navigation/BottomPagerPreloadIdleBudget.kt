package com.android.purebilibili.navigation

import android.os.Looper
import android.os.MessageQueue
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Let doFrame and already queued input finish before granting another background mount. */
internal suspend fun awaitBottomPagerPreloadIdle() = suspendCancellableCoroutine<Unit> { continuation ->
    val queue = Looper.getMainLooper().queue
    val idleHandler = MessageQueue.IdleHandler {
        if (continuation.isActive) continuation.resume(Unit)
        false
    }
    queue.addIdleHandler(idleHandler)
    continuation.invokeOnCancellation { queue.removeIdleHandler(idleHandler) }
}
