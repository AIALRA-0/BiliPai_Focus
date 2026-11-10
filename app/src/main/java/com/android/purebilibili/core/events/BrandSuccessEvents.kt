package com.android.purebilibili.core.events

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class BrandSuccessKind { FAVORITE, DOWNLOAD, FOLLOW, UNFOLLOW }

data class BrandSuccessFeedback(
    val id: Int,
    val kind: BrandSuccessKind,
    val detail: String? = null
)

/** Ephemeral, confirmed successes. No replay means background work cannot surprise a returning user. */
object BrandSuccessEvents {
    private val sequence = AtomicInteger()
    private val completedDownloads = linkedSetOf<String>()
    private val mutableEvents = MutableSharedFlow<BrandSuccessFeedback>(
        replay = 0,
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = mutableEvents.asSharedFlow()

    fun favoriteSaved() {
        mutableEvents.tryEmit(BrandSuccessFeedback(sequence.incrementAndGet(), BrandSuccessKind.FAVORITE))
    }

    fun followChanged(following: Boolean, detail: String? = null) {
        mutableEvents.tryEmit(BrandSuccessFeedback(
            sequence.incrementAndGet(),
            if (following) BrandSuccessKind.FOLLOW else BrandSuccessKind.UNFOLLOW,
            detail
        ))
    }

    fun downloadCompleted(taskId: String, createdAt: Long, title: String) {
        // Repeated worker completion must not celebrate the same download twice.
        val firstCompletion = synchronized(completedDownloads) {
            val added = completedDownloads.add("$taskId:$createdAt")
            if (completedDownloads.size > 256) completedDownloads.remove(completedDownloads.first())
            added
        }
        if (firstCompletion) {
            mutableEvents.tryEmit(
                BrandSuccessFeedback(sequence.incrementAndGet(), BrandSuccessKind.DOWNLOAD, title)
            )
        }
    }
}
