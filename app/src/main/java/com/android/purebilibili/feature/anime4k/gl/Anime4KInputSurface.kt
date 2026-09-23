package com.android.purebilibili.feature.anime4k.gl

import android.graphics.SurfaceTexture
import android.view.Surface

/**
 * 解码器输入 surface 必须和其 OES 纹理在同一个 GL 线程创建、消费和释放。
 */
internal class Anime4KInputSurface(
    externalTextureId: Int,
    onFrameAvailable: () -> Unit
) {
    val surfaceTexture = SurfaceTexture(externalTextureId).apply {
        setOnFrameAvailableListener { onFrameAvailable() }
    }
    val surface = Surface(surfaceTexture)

    fun setDefaultBufferSize(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            surfaceTexture.setDefaultBufferSize(width, height)
        }
    }

    fun release() {
        surface.release()
        surfaceTexture.release()
    }
}
