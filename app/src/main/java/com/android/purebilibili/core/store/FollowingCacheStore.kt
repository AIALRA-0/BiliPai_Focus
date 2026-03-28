package com.android.purebilibili.core.store

import android.content.Context
import com.android.purebilibili.data.model.response.FollowingUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class FollowingCacheSnapshot(
    val mid: Long,
    val total: Int,
    val users: List<FollowingUser>,
    val cachedAtMs: Long,
    val revision: Long
)

@Serializable
private data class FollowingCachePayload(
    val mid: Long = 0L,
    val total: Int = 0,
    val users: List<FollowingUser> = emptyList(),
    val cachedAtMs: Long = 0L,
    val revision: Long = 0L
)

object FollowingCacheStore {
    private const val PREFS_NAME = "following_cache"
    private const val KEY_PAYLOAD = "following_payload_v1"
    private const val MAX_CACHE_USERS = 2000

    private val lock = Any()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val snapshotState = MutableStateFlow<FollowingCacheSnapshot?>(null)

    fun observeSnapshot(context: Context, mid: Long): Flow<FollowingCacheSnapshot?> {
        if (mid <= 0L) return flowOf(null)
        return snapshotState
            .onStart {
                emit(getSnapshot(context, mid))
            }
            .map { activeSnapshot ->
                when {
                    activeSnapshot == null -> getSnapshot(context, mid)
                    activeSnapshot.mid == mid -> activeSnapshot
                    else -> getSnapshot(context, mid)
                }
            }
            .distinctUntilChanged()
    }

    fun getSnapshot(context: Context, mid: Long): FollowingCacheSnapshot? {
        if (mid <= 0L) return null
        return synchronized(lock) {
            readPersistedSnapshotLocked(context, mid)
        }
    }

    fun saveSnapshot(
        context: Context,
        mid: Long,
        total: Int,
        users: List<FollowingUser>,
        cachedAtMs: Long = System.currentTimeMillis()
    ) {
        if (mid <= 0L) return
        synchronized(lock) {
            val normalizedUsers = users
                .asSequence()
                .filter { it.mid > 0L }
                .distinctBy { it.mid }
                .take(MAX_CACHE_USERS)
                .toList()
            val nextRevision = resolveNextRevisionLocked(
                context = context,
                requestedRevision = cachedAtMs
            )

            val payload = FollowingCachePayload(
                mid = mid,
                total = total.coerceAtLeast(normalizedUsers.size),
                users = normalizedUsers,
                cachedAtMs = cachedAtMs,
                revision = nextRevision
            )
            val raw = json.encodeToString(payload)
            context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PAYLOAD, raw)
                .apply()
            snapshotState.value = FollowingCacheSnapshot(
                mid = payload.mid,
                total = payload.total,
                users = normalizedUsers,
                cachedAtMs = payload.cachedAtMs,
                revision = payload.revision
            )
        }
    }

    fun clear(context: Context) {
        synchronized(lock) {
            context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_PAYLOAD)
                .apply()
            snapshotState.value = null
        }
    }

    private fun readPersistedSnapshotLocked(
        context: Context,
        mid: Long
    ): FollowingCacheSnapshot? {
        val raw = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PAYLOAD, null)
            .orEmpty()
        if (raw.isBlank()) return null

        val payload = runCatching {
            json.decodeFromString<FollowingCachePayload>(raw)
        }.getOrNull() ?: return null

        if (payload.mid != mid) return null
        val normalizedUsers = payload.users
            .asSequence()
            .filter { it.mid > 0L }
            .distinctBy { it.mid }
            .take(MAX_CACHE_USERS)
            .toList()

        return FollowingCacheSnapshot(
            mid = payload.mid,
            total = payload.total.coerceAtLeast(normalizedUsers.size),
            users = normalizedUsers,
            cachedAtMs = payload.cachedAtMs,
            revision = payload.revision
        )
    }

    private fun resolveNextRevisionLocked(
        context: Context,
        requestedRevision: Long
    ): Long {
        val raw = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PAYLOAD, null)
            .orEmpty()
        val currentRevision = runCatching {
            json.decodeFromString<FollowingCachePayload>(raw).revision
        }.getOrDefault(0L)
        return requestedRevision.coerceAtLeast(currentRevision + 1L).coerceAtLeast(1L)
    }
}
