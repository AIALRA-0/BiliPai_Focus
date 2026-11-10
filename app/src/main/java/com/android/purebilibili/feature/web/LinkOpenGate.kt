package com.android.purebilibili.feature.web

/** Uses monotonic time. Pending work remains exclusive even after the click window expires. */
internal class LinkOpenGate(private val windowMillis: Long = 800L) {
    private var lastAcceptedAt: Long? = null
    private var pending = false

    @Synchronized
    fun tryAcquire(now: Long): Boolean {
        if (pending || lastAcceptedAt?.let { now - it < windowMillis } == true) return false
        lastAcceptedAt = now
        pending = true
        return true
    }

    @Synchronized
    fun release() {
        pending = false
    }
}
