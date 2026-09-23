package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalView
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.findViewTreeNavigationEventDispatcherOwner

/** Bind comment handlers to the dialog receiving the gesture, not the underlying route. */
@Composable
internal fun CommentWindowNavigation(content: @Composable () -> Unit) {
    val owner = checkNotNull(LocalView.current.findViewTreeNavigationEventDispatcherOwner()) {
        "Comment dialog must provide a window navigation dispatcher"
    }
    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner, content = content)
}
