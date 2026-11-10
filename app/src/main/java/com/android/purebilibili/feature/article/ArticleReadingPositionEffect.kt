package com.android.purebilibili.feature.article

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

private class ArticleReadingPositionSession(var lastSaved: ArticleReadingPosition)

@Composable
internal fun rememberArticleReadingListState(
    articleId: Long,
    headerCount: Int,
    blockCount: Int,
    commentEntry: Boolean,
): LazyListState = key(articleId) {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val saved = remember(context, articleId) { ArticleReadingStore.readPosition(context, articleId) }
    val initial = resolveArticleReadingListPosition(
        if (commentEntry) ArticleReadingPosition() else saved, headerCount, blockCount,
    )
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initial.first, initialFirstVisibleItemScrollOffset = initial.second)
    val session = remember(articleId) { ArticleReadingPositionSession(saved) }
    fun savePosition() {
        if (commentEntry || state.layoutInfo.totalItemsCount == 0) return
        val position = captureArticleReadingPosition(
            state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset, headerCount, blockCount,
        ) ?: return
        if (position != session.lastSaved) {
            ArticleReadingStore.savePosition(context, articleId, position)
            session.lastSaved = position
        }
    }
    // 仅在滚动停止、后台或离开时落盘，不将逐帧滚动位置引入组合，也不记录评论定位入口。
    LaunchedEffect(state, articleId, headerCount, blockCount, commentEntry) {
        if (!commentEntry) {
            snapshotFlow {
                if (state.isScrollInProgress || state.layoutInfo.totalItemsCount == 0) null
                else state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset
            }.distinctUntilChanged().filterNotNull().collect { savePosition() }
        }
    }
    DisposableEffect(state, lifecycle, articleId, headerCount, blockCount, commentEntry) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) savePosition() }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            savePosition()
        }
    }
    state
}
