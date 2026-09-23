package com.android.purebilibili.feature.video.danmaku

import android.view.View
import com.android.purebilibili.danmaku.engine.DanmakuRenderView

internal fun DanmakuRenderView.configureAsPassiveDanmakuOverlay() {
    // 弹幕视图只是视觉覆盖层，触摸必须继续交给 Compose 播放器手势层。
    isClickable = false
    isLongClickable = false
    isFocusable = false
    isFocusableInTouchMode = false
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    setRendererTouchable(false)
    setOnTouchListener { _, _ -> false }
}
