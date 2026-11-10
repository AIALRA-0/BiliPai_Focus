package com.android.purebilibili.feature.video.viewmodel

enum class PlayerToastPresentation {
    Standard,
    CenteredHighlight,
    PlaybackResume
}

data class PlayerToastMessage(
    val message: String,
    val presentation: PlayerToastPresentation = PlayerToastPresentation.Standard,
    val mediaKey: String? = null,
)

internal fun buildPlayerToastMessage(message: String): PlayerToastMessage {
    return PlayerToastMessage(
        message = message,
        presentation = PlayerToastPresentation.Standard
    )
}

internal fun buildQualityToastMessage(message: String): PlayerToastMessage {
    return PlayerToastMessage(
        message = message,
        presentation = PlayerToastPresentation.CenteredHighlight
    )
}
