package com.android.purebilibili.feature.live

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** 高频列表在自己的内容边界订阅，不使播放器和导航外壳读取列表值。 */
@Composable
internal fun <T> LiveRoomUpdates(
    updates: StateFlow<T>,
    content: @Composable (T) -> Unit,
) {
    key(updates) {
        val value by updates.collectAsStateWithLifecycle()
        content(value)
    }
}

/** 标签与切换按钮只订阅数量；同数量的内容更新留在 SC 列表内部。 */
@Composable
internal fun <T> LiveRoomCountUpdates(
    updates: StateFlow<List<T>>,
    content: @Composable (Int) -> Unit,
) {
    key(updates) {
        val counts = remember(updates) { updates.map { it.size }.distinctUntilChanged() }
        val count by counts.collectAsStateWithLifecycle(initialValue = updates.value.size)
        content(count)
    }
}
