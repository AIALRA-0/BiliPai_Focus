package com.android.purebilibili.core.ui.transition

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.graphics.RenderEffect as ComposeRenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect

/**
 * 图层回调在 UI 线程使用。保留当前半径的单个效果，避免量化半径不变时
 * 每帧创建 Android/Compose 效果对象；不存动画进度，也不写入 Snapshot 状态。
 */
internal class BlurRenderEffectCache {
    private var radiusPx = Float.NaN
    private var effect: ComposeRenderEffect? = null

    fun resolve(blurRadiusPx: Float): ComposeRenderEffect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            !blurRadiusPx.isFinite() || blurRadiusPx <= 0.01f
        ) {
            return null
        }
        if (blurRadiusPx != radiusPx) {
            effect = RenderEffect.createBlurEffect(
                blurRadiusPx,
                blurRadiusPx,
                Shader.TileMode.CLAMP,
            ).asComposeRenderEffect()
            radiusPx = blurRadiusPx
        }
        return effect
    }
}
