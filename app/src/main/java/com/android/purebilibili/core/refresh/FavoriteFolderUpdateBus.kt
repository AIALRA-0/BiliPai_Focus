package com.android.purebilibili.core.refresh

import com.android.purebilibili.data.model.response.FavoriteInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Metadata-only edits; consumers patch matching owned cards without reloading resources. */
internal object FavoriteFolderUpdateBus {
    data class Update(val mediaId: Long, val ownerMid: Long, val info: FavoriteInfo)

    private val _changes = MutableSharedFlow<Update>(extraBufferCapacity = 8)
    val changes = _changes.asSharedFlow()

    suspend fun notifyChanged(mediaId: Long, ownerMid: Long, info: FavoriteInfo) {
        _changes.emit(Update(mediaId, ownerMid, info))
    }
}
