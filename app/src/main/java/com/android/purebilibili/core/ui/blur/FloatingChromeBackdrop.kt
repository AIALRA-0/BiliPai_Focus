package com.android.purebilibili.core.ui.blur

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.staticCompositionLocalOf
import top.yukonga.miuix.kmp.blur.Backdrop

/**
 * Shared app-shell backdrop for chrome drawn outside the navigation capture subtree.
 * Page-local controls must use a separate sibling source: this recording includes them,
 * so sampling it from inside a page can create a RenderNode cycle.
 */
val LocalFloatingChromeBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/** 页面内的悬浮内容可在底栏隐藏时请求保留共享背景采样。 */
class FloatingChromeBackdropDemand {
    private val consumers = mutableStateListOf<Any>()
    val isRequired: Boolean get() = consumers.isNotEmpty()

    fun acquire(token: Any) { if (token !in consumers) consumers.add(token) }
    fun release(token: Any) { consumers.remove(token) }
}

val LocalFloatingChromeBackdropDemand = staticCompositionLocalOf<FloatingChromeBackdropDemand?> { null }
