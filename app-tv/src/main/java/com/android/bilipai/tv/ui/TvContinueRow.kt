package com.android.bilipai.tv.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import androidx.tv.material3.MaterialTheme
import com.android.bilipai.tv.tvId
import com.android.bilipai.tv.ui.components.TvVideoCard
import com.android.bilipai.tv.ui.components.TvCardWatchProgress
import com.android.purebilibili.data.model.resolveVideoDisplayProgressState
import com.android.purebilibili.data.model.response.VideoItem

@Composable
internal fun TvContinueRow(items: List<VideoItem>, entryId: String?, requester: FocusRequester,
    navigationFocus: FocusRequester, onOpen: (VideoItem) -> Unit, onFocused: (String) -> Unit) {
    if (items.isEmpty()) return
    val entry = items.indexOfFirst { "continue:${it.tvId()}" == entryId }.coerceAtLeast(0)
    val rowState = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = entry)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("继续观看", style = MaterialTheme.typography.titleLarge)
        LazyRow(state = rowState, contentPadding = PaddingValues(8.dp), horizontalArrangement = Arrangement.spacedBy(TvUiTokens.cardGap)) {
            itemsIndexed(items, key = { _, item -> item.tvId() }) { index, item ->
                TvVideoCard(item, { onOpen(item) }, Modifier.width(220.dp)
                    .then(if (index == entry) Modifier.focusRequester(requester) else Modifier)
                    .focusProperties { if (index == 0) left = navigationFocus }
                    .onFocusChanged { if (it.isFocused) onFocused("continue:${item.tvId()}") },
                    supportingContent = {
                        TvCardWatchProgress(resolveVideoDisplayProgressState(item.progress, item.duration, viewAt = item.view_at),
                            item.duration, Modifier.fillMaxWidth().padding(12.dp))
                    })
            }
        }
    }
}
