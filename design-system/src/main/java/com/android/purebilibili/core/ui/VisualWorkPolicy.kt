package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

/** Visual work only; never gates audio, downloads, casting or persistence. */
val LocalVisualWorkAllowed = compositionLocalOf { true }

fun shouldRunVisualWork(pageActive: Boolean, resumed: Boolean, focused: Boolean): Boolean =
    pageActive && resumed && focused

@Composable
fun rememberVisualWorkActive(): Boolean {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    return shouldRunVisualWork(
        pageActive = LocalVisualWorkAllowed.current,
        resumed = lifecycle.isAtLeast(Lifecycle.State.RESUMED),
        focused = LocalWindowInfo.current.isWindowFocused,
    )
}
