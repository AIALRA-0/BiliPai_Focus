package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.core.store.FullscreenAspectRatio

internal fun FullscreenAspectRatio.toVideoAspectRatio(): VideoAspectRatio {
    return when (this) {
        FullscreenAspectRatio.FIT -> VideoAspectRatio.FIT
        FullscreenAspectRatio.FILL -> VideoAspectRatio.FILL
        FullscreenAspectRatio.RATIO_16_9 -> VideoAspectRatio.RATIO_16_9
        FullscreenAspectRatio.RATIO_4_3 -> VideoAspectRatio.RATIO_4_3
        FullscreenAspectRatio.STRETCH -> VideoAspectRatio.STRETCH
        FullscreenAspectRatio.FIT_WIDTH -> VideoAspectRatio.FIT_WIDTH
        FullscreenAspectRatio.FIT_HEIGHT -> VideoAspectRatio.FIT_HEIGHT
        FullscreenAspectRatio.ORIGINAL -> VideoAspectRatio.ORIGINAL
        FullscreenAspectRatio.SCALE_DOWN -> VideoAspectRatio.SCALE_DOWN
    }
}

internal fun VideoAspectRatio.toFullscreenAspectRatio(): FullscreenAspectRatio {
    return when (this) {
        VideoAspectRatio.FIT -> FullscreenAspectRatio.FIT
        VideoAspectRatio.FILL -> FullscreenAspectRatio.FILL
        VideoAspectRatio.RATIO_16_9 -> FullscreenAspectRatio.RATIO_16_9
        VideoAspectRatio.RATIO_4_3 -> FullscreenAspectRatio.RATIO_4_3
        VideoAspectRatio.STRETCH -> FullscreenAspectRatio.STRETCH
        VideoAspectRatio.FIT_WIDTH -> FullscreenAspectRatio.FIT_WIDTH
        VideoAspectRatio.FIT_HEIGHT -> FullscreenAspectRatio.FIT_HEIGHT
        VideoAspectRatio.ORIGINAL -> FullscreenAspectRatio.ORIGINAL
        VideoAspectRatio.SCALE_DOWN -> FullscreenAspectRatio.SCALE_DOWN
    }
}
