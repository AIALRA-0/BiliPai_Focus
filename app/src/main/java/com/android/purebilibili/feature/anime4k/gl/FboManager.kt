package com.android.purebilibili.feature.anime4k.gl

import android.opengl.GLES30

internal data class FboTarget(
    val framebuffer: Int,
    val texture: Int,
    val width: Int,
    val height: Int,
    val precision: FboPrecision
)

internal enum class FboPrecision(
    val internalFormat: Int,
    val pixelFormat: Int,
    val pixelType: Int
) {
    RGBA8(GLES30.GL_RGBA8, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE),
    R16F(GLES30.GL_R16F, GLES30.GL_RED, GLES30.GL_HALF_FLOAT),
    RGBA16F(GLES30.GL_RGBA16F, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT)
}

/**
 * 管理 Anime4K 多 Pass FBO，并复用不再被命名纹理引用的目标。
 * CNN 特征必须保留负值，因此中间层使用半浮点纹理。
 */
internal class FboManager {
    private val targets = mutableListOf<FboTarget>()

    fun obtain(
        width: Int,
        height: Int,
        precision: FboPrecision,
        protectedTextureIds: Set<Int> = emptySet()
    ): FboTarget {
        targets.firstOrNull {
            it.width == width &&
                it.height == height &&
                it.precision == precision &&
                it.texture !in protectedTextureIds
        }?.let { return it }

        return create(width, height, precision).also(targets::add)
    }

    fun release() {
        targets.forEach(::delete)
        targets.clear()
    }

    private fun create(width: Int, height: Int, precision: FboPrecision): FboTarget {
        val textures = IntArray(1)
        val framebuffers = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D,
            0,
            precision.internalFormat,
            width,
            height,
            0,
            precision.pixelFormat,
            precision.pixelType,
            null
        )
        GLES30.glGenFramebuffers(1, framebuffers, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebuffers[0])
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER,
            GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D,
            textures[0],
            0
        )
        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            GLES30.glDeleteFramebuffers(1, framebuffers, 0)
            GLES30.glDeleteTextures(1, textures, 0)
            error("Anime4K ${precision.name} FBO 创建失败：0x${status.toString(16)}")
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        return FboTarget(
            framebuffer = framebuffers[0],
            texture = textures[0],
            width = width,
            height = height,
            precision = precision
        )
    }

    private fun delete(target: FboTarget) {
        GLES30.glDeleteFramebuffers(1, intArrayOf(target.framebuffer), 0)
        GLES30.glDeleteTextures(1, intArrayOf(target.texture), 0)
    }
}
