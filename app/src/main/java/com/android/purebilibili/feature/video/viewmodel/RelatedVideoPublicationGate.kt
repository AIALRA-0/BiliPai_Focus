package com.android.purebilibili.feature.video.viewmodel

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/** Requests may finish during entry, but an obsolete result must never be published. */
internal suspend fun awaitRelatedVideoPublication(
    publicationAllowed: StateFlow<Boolean>,
    requestIsCurrent: () -> Boolean,
): Boolean {
    publicationAllowed.first { it }
    return requestIsCurrent()
}
