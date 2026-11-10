package com.android.purebilibili.core.plugin

import androidx.annotation.MainThread
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.plugin.sdk.PlayerPluginApi
import com.android.purebilibili.plugin.sdk.PlayerPluginSession
import java.util.Collections
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal data class PluginVideoKey(val bvid: String, val cid: Long)

internal class PlayerPluginSessionEntry(
    val definition: PlayerPluginApi,
    val session: PlayerPluginSession,
    val videoKey: PluginVideoKey,
)

/**
 * Owns one player's sessions, not its coroutine scope. All access, including the supplied scope's
 * execution and callbacks, must stay on Main. Definitions arrive in registration order and contain
 * only enabled plugins. Removed sessions end before replacement factories run. Loading failures
 * remain not-ready until explicitly retried or replaced.
 */
@MainThread
internal class PlayerPluginSessionHost(
    private val scope: CoroutineScope,
    private val onLoaded: (PlayerPluginSessionEntry) -> Unit,
    private val onLoadFailed: (PlayerPluginSessionEntry, Exception) -> Unit,
    private val onEnded: (PlayerPluginSessionEntry) -> Unit,
) {
    private class SessionState(val entry: PlayerPluginSessionEntry) {
        var job: Job? = null
        var generation = 0L
        var ready = false
        var ended = false
    }

    private var states = linkedMapOf<String, SessionState>()
    private var videoKey: PluginVideoKey? = null
    private var currentSnapshot: List<PlayerPluginSessionEntry> = emptyList()
    private var readySnapshot: List<PlayerPluginSessionEntry> = emptyList()
    private var closed = false
    private var revision = 0L

    fun update(definitions: List<PlayerPluginApi>, videoKey: PluginVideoKey?) {
        if (closed || matches(definitions, videoKey)) return

        val revision = ++this.revision
        val retained = LinkedHashMap<String, SessionState>(definitions.size)
        if (videoKey != null) {
            for (definition in definitions) {
                val previous = states[definition.id]
                if (
                    previous != null && previous.entry.definition === definition &&
                    previous.entry.videoKey == videoKey
                ) {
                    retained[definition.id] = previous
                }
            }
        }
        val removed = states.values.filter { retained[it.entry.definition.id] !== it }

        // Publish invalidation before cancellation, ending callbacks, or replacement factories.
        states = retained
        this.videoKey = videoKey
        publishSnapshots()
        for (state in removed) end(state)
        if (closed || this.revision != revision) return

        val next = LinkedHashMap<String, SessionState>(definitions.size)
        if (videoKey != null) {
            for (definition in definitions) {
                val retainedState = retained[definition.id]
                if (retainedState != null) {
                    next[definition.id] = retainedState
                    continue
                }
                val session = try {
                    definition.createSession()
                } catch (error: CancellationException) {
                    // 尚未发布的新会话也必须清理；保留调用方的取消语义。
                    for (state in next.values) {
                        if (retained[state.entry.definition.id] !== state) end(state)
                    }
                    throw error
                } catch (error: Exception) {
                    // 单个工厂失败不能终止宿主的状态收集；后续更新可重新尝试。
                    Logger.e(TAG, "Player plugin session creation failed: ${definition.id}", error)
                    continue
                }
                next[definition.id] = SessionState(
                    PlayerPluginSessionEntry(definition, session, videoKey)
                )
            }
        }
        states = next
        publishSnapshots()
        for (state in next.values) {
            if (state.generation == 0L && isCurrent(state.entry)) startLoad(state)
        }
    }

    fun retryLoad(pluginId: String) {
        val state = states[pluginId] ?: return
        if (!state.ready && state.job?.isActive != true) startLoad(state)
    }

    /** Cached, immutable snapshots keep the playback-position polling path allocation-free. */
    fun readySessions(videoKey: PluginVideoKey): List<PlayerPluginSessionEntry> =
        if (this.videoKey == videoKey) readySnapshot else emptyList()

    fun currentSessions(): List<PlayerPluginSessionEntry> = currentSnapshot

    fun isCurrent(entry: PlayerPluginSessionEntry): Boolean =
        !closed && states[entry.definition.id]?.entry === entry

    fun close() {
        if (closed) return
        closed = true
        revision++
        val removed = states.values
        states = linkedMapOf()
        videoKey = null
        currentSnapshot = emptyList()
        readySnapshot = emptyList()
        for (state in removed) end(state)
    }

    private fun matches(definitions: List<PlayerPluginApi>, videoKey: PluginVideoKey?): Boolean {
        if (this.videoKey != videoKey) return false
        if (videoKey == null) return states.isEmpty()
        if (states.size != definitions.size) return false
        val entries = states.values.iterator()
        for (definition in definitions) {
            if (entries.next().entry.definition !== definition) return false
        }
        return true
    }

    private fun startLoad(state: SessionState) {
        if (!isCurrent(state.entry) || !scope.isActive) return
        val generation = ++state.generation
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                try {
                    val key = state.entry.videoKey
                    state.entry.session.onVideoLoad(key.bvid, key.cid)
                    currentCoroutineContext().ensureActive()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    currentCoroutineContext().ensureActive()
                    if (isCurrentLoad(state, generation)) {
                        state.job = null
                        Logger.e(TAG, "Player plugin load failed: ${state.entry.definition.id}", error)
                        notify(state, "load failure") { onLoadFailed(state.entry, error) }
                    }
                    return@launch
                }
                if (isCurrentLoad(state, generation)) {
                    state.job = null
                    state.ready = true
                    publishReadySnapshot()
                    notify(state, "loaded") { onLoaded(state.entry) }
                }
            } finally {
                if (state.generation == generation) state.job = null
            }
        }
        // LAZY ensures an immediate dispatcher cannot finish or retry before this assignment.
        state.job = job
        job.start()
    }

    private fun isCurrentLoad(state: SessionState, generation: Long): Boolean =
        state.generation == generation && isCurrent(state.entry)

    private fun end(state: SessionState) {
        if (state.ended) return
        state.ended = true
        state.ready = false
        state.generation++
        val job = state.job
        state.job = null
        job?.cancel()
        notify(state, "video end") { state.entry.session.onVideoEnd() }
        notify(state, "ended") { onEnded(state.entry) }
    }

    private inline fun notify(state: SessionState, event: String, callback: () -> Unit) {
        try {
            callback()
        } catch (error: Exception) {
            Logger.e(TAG, "Player plugin $event callback failed: ${state.entry.definition.id}", error)
        }
    }

    private fun publishSnapshots() {
        if (states.isEmpty()) {
            currentSnapshot = emptyList()
            readySnapshot = emptyList()
            return
        }
        val current = ArrayList<PlayerPluginSessionEntry>(states.size)
        for (state in states.values) current.add(state.entry)
        currentSnapshot = Collections.unmodifiableList(current)
        publishReadySnapshot()
    }

    private fun publishReadySnapshot() {
        val ready = ArrayList<PlayerPluginSessionEntry>(states.size)
        for (state in states.values) {
            if (state.ready) ready.add(state.entry)
        }
        readySnapshot = if (ready.isEmpty()) emptyList() else Collections.unmodifiableList(ready)
    }

    private companion object {
        const val TAG = "PlayerPluginSessionHost"
    }
}
