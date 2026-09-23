package com.android.purebilibili.feature.anime4k.gl

enum class Anime4KDisplayScaleMode {
    FIT,
    CROP,
    STRETCH
}

data class Anime4KDisplayTransform(
    val scaleX: Float,
    val scaleY: Float
)

fun resolveAnime4KDisplayTransform(
    outputWidth: Int,
    outputHeight: Int,
    sourceWidth: Int,
    sourceHeight: Int,
    scaleMode: Anime4KDisplayScaleMode
): Anime4KDisplayTransform {
    if (
        scaleMode == Anime4KDisplayScaleMode.STRETCH ||
        outputWidth <= 0 ||
        outputHeight <= 0 ||
        sourceWidth <= 0 ||
        sourceHeight <= 0
    ) {
        return Anime4KDisplayTransform(scaleX = 1f, scaleY = 1f)
    }

    val outputAspectRatio = outputWidth.toFloat() / outputHeight.toFloat()
    val sourceAspectRatio = sourceWidth.toFloat() / sourceHeight.toFloat()
    if (
        !outputAspectRatio.isFinite() ||
        outputAspectRatio <= 0f ||
        !sourceAspectRatio.isFinite() ||
        sourceAspectRatio <= 0f
    ) {
        return Anime4KDisplayTransform(scaleX = 1f, scaleY = 1f)
    }

    val outputIsWider = outputAspectRatio > sourceAspectRatio
    return when (scaleMode) {
        Anime4KDisplayScaleMode.FIT -> {
            if (outputIsWider) {
                Anime4KDisplayTransform(
                    scaleX = sourceAspectRatio / outputAspectRatio,
                    scaleY = 1f
                )
            } else {
                Anime4KDisplayTransform(
                    scaleX = 1f,
                    scaleY = outputAspectRatio / sourceAspectRatio
                )
            }
        }
        Anime4KDisplayScaleMode.CROP -> {
            if (outputIsWider) {
                Anime4KDisplayTransform(
                    scaleX = 1f,
                    scaleY = outputAspectRatio / sourceAspectRatio
                )
            } else {
                Anime4KDisplayTransform(
                    scaleX = sourceAspectRatio / outputAspectRatio,
                    scaleY = 1f
                )
            }
        }
        Anime4KDisplayScaleMode.STRETCH -> {
            Anime4KDisplayTransform(scaleX = 1f, scaleY = 1f)
        }
    }
}
