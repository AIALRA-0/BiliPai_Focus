package com.android.purebilibili.core.ui.blur

import androidx.compose.runtime.staticCompositionLocalOf
import top.yukonga.miuix.kmp.blur.Backdrop

/** Shared app-shell backdrop for floating chrome drawn above navigation content. */
val LocalFloatingChromeBackdrop = staticCompositionLocalOf<Backdrop?> { null }
