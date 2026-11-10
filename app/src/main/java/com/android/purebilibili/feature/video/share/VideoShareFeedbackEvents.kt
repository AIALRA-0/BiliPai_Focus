package com.android.purebilibili.feature.video.share

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Only prepared handoffs, copied links or confirmed in-app sends; no replay. */
internal object VideoShareFeedbackEvents {
    private val mutableEvents = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = mutableEvents.asSharedFlow()
    fun prepared(bvid: String) { mutableEvents.tryEmit(bvid) }
}
