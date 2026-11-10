package com.android.purebilibili.feature.dynamic.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.purebilibili.data.model.response.DynamicAdditionalText
import com.android.purebilibili.data.model.response.OpusLinkCard
import com.android.purebilibili.data.model.response.resolveDynamicCardButtonText

/** 单张卡片的预约请求与回写状态；退出卡片或切换账号后忽略旧回调。 */
internal class DynamicReserveCardSession(private val seed: OpusLinkCard) {
    private var active = true
    var submitting by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    private var result by mutableStateOf<DynamicReserveResult?>(null)
    val shownCard: OpusLinkCard get() = applyDynamicReserveResult(seed, result)

    fun begin(): Boolean {
        if (!active || submitting) return false
        submitting = true
        errorMessage = null
        return true
    }

    fun complete(outcome: Result<DynamicReserveResult>): Boolean {
        if (!active || !submitting) return false
        submitting = false
        outcome.fold(
            onSuccess = { result = it; errorMessage = null },
            onFailure = { errorMessage = it.message?.takeIf(String::isNotBlank) ?: "预约操作失败" },
        )
        return true
    }

    fun close() {
        active = false
    }
}

internal fun applyDynamicReserveResult(card: OpusLinkCard, result: DynamicReserveResult?): OpusLinkCard {
    if (result == null) return card
    val reserve = card.reserve ?: return card
    val updatedReserve = reserve.copy(
        reserve_total = result.reserveTotal,
        button = reserve.button?.copy(status = result.buttonStatus),
        desc2 = if (result.description.isNotBlank()) {
            (reserve.desc2 ?: DynamicAdditionalText()).copy(text = result.description)
        } else reserve.desc2,
    )
    return card.copy(
        reserve = updatedReserve,
        description = reserveDescription(updatedReserve).ifBlank { card.description },
        badgeText = resolveDynamicCardButtonText(updatedReserve.button).ifBlank { card.badgeText },
    )
}
