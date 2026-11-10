package com.android.purebilibili.feature.profile

import android.content.Context
import android.graphics.Matrix
import android.view.TextureView
import android.view.View
import androidx.media3.common.Player
import androidx.media3.common.VideoSize

/** Keep the output surface inside the header, including when chrome records its RenderNode. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class ProfileSkinVideoView(context: Context) : TextureView(context), Player.Listener {
    private val cropMatrix = Matrix()

    var player: Player? = null
        set(value) {
            if (field === value) return
            field?.removeListener(this)
            field?.clearVideoTextureView(this)
            field = value
            value?.addListener(this)
            value?.setVideoTextureView(this)
            updateCrop()
        }

    init {
        isOpaque = false
        isFocusable = false
        isClickable = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        updateCrop()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateCrop()
    }

    private fun updateCrop() {
        val video = player?.videoSize ?: VideoSize.UNKNOWN
        cropMatrix.reset()
        if (width > 0 && height > 0 && video.width > 0 && video.height > 0) {
            val videoAspect = video.width * video.pixelWidthHeightRatio / video.height
            val viewportAspect = width.toFloat() / height
            // PlayerView ZOOM enlarges the TextureView itself beyond the viewport. Crop
            // texels instead, so offscreen backdrop recording never sees an oversized surface.
            cropMatrix.setScale(
                maxOf(1f, videoAspect / viewportAspect),
                maxOf(1f, viewportAspect / videoAspect),
                width / 2f,
                height / 2f,
            )
        }
        setTransform(cropMatrix)
    }
}
