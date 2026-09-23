package com.android.purebilibili.core.ui.transition

import androidx.compose.runtime.compositionLocalOf

internal val LocalVideoCardSharedElementSourceRoute = compositionLocalOf<String?> { null }
internal val LocalVideoSharedTransitionSpeedSettings = compositionLocalOf {
    VideoSharedTransitionSpeedSettings()
}
internal val LocalClickToPlayEnabled = compositionLocalOf { false }
internal val LocalDynamicImagePreviewTextVisible = compositionLocalOf { true }

