package com.android.purebilibili.feature.anime4k.gl

import android.graphics.SurfaceTexture
import android.view.Surface

/**
 * 解码器输入与 OES 纹理由 GL 线程创建、消费和释放。
 * GL 线程退出后的兜底清理只归还 Surface/SurfaceTexture，不再操作 OES 纹理。
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
        surfaceTexture.setOnFrameAvailableListener(null)
        surface.release()
        surfaceTexture.release()
    }
}
