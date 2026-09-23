package com.android.purebilibili.feature.video.danmaku

import android.content.Context
import kotlinx.coroutines.CoroutineScope

data class DanmakuSessionIdentity(
    val cid: Long,
    val playbackInstanceId: Int
)

/** Single construction and ownership point for playback-identity scoped sessions. */
object DanmakuSessionFactory {
    private data class Entry(
        val manager: DanmakuManager,
        var owners: Int
    )

    private val sessions = LinkedHashMap<Any, Entry>()

    @Synchronized
    fun acquire(
        context: Context,
        scope: CoroutineScope,
        playbackIdentity: Any
    ): DanmakuManager {
        sessions[playbackIdentity]?.let { entry ->
            entry.owners++
            return entry.manager
        }
        return DanmakuManager.createSession(context, scope).also { manager ->
            manager.bindSessionIdentity(
                DanmakuSessionIdentity(
                    cid = 0L,
                    playbackInstanceId = playbackIdentity.hashCode()
                )
            )
            sessions[playbackIdentity] = Entry(manager, owners = 1)
        }
    }

    @Synchronized
    fun release(playbackIdentity: Any, manager: DanmakuManager) {
        val entry = sessions[playbackIdentity] ?: return
        if (entry.manager !== manager) return
        entry.owners--
        if (entry.owners <= 0) {
            sessions.remove(playbackIdentity)
            entry.manager.release()
        }
    }

    @Synchronized
    fun trimCachesForBackground() {
        sessions.values.forEach { it.manager.trimCachesForBackground() }
    }
}
