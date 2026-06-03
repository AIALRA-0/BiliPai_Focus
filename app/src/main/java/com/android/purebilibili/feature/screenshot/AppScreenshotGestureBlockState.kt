package com.android.purebilibili.feature.screenshot

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AppScreenshotGestureBlockState {
    var fullscreenPlayerLocked by mutableStateOf(false)
}
