package com.android.purebilibili.feature.video.ui.components

internal data class CoinTossFrame(
    val characterLift: Float,
    val boxLift: Float,
    val coinLift: Float,
    val coinAlpha: Float,
    val thunderIndex: Int,
)

/** 角色先起落，随后箱体起落、硬币上抛；两枚投币额外显示三帧闪电。 */
internal fun resolveCoinTossFrame(elapsedMs: Float, count: Int): CoinTossFrame {
    val time = elapsedMs.coerceIn(0f, 400f)
    fun bounce(start: Float): Float = when {
        time < start -> 0f
        time < start + 50f -> (time - start) / 50f
        time < start + 100f -> (start + 100f - time) / 50f
        else -> 0f
    }
    return CoinTossFrame(
        characterLift = bounce(0f) * 0.2f,
        boxLift = bounce(100f) * 0.2f,
        coinLift = ((time - 100f) / 200f).coerceIn(0f, 1f) * 2f,
        coinAlpha = 1f - ((time - 300f) / 100f).coerceIn(0f, 1f),
        thunderIndex = if (count == 2 && time >= 100f && time < 148f) {
            ((time - 100f) / 16f).toInt()
        } else -1,
    )
}
