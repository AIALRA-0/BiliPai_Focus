package com.android.purebilibili.feature.video.danmaku

import android.view.View
import com.android.purebilibili.danmaku.engine.DanmakuRenderView

internal fun DanmakuRenderView.configureAsPassiveDanmakuOverlay() {
    // 原生视图不抢触摸；Compose 确认单击后通过 DanmakuManager.captureTap 做命中检测。
    isClickable = false
    isLongClickable = false
    isFocusable = false
    isFocusableInTouchMode = false
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    setRendererTouchable(false)
    setOnTouchListener { _, _ -> false }
}
