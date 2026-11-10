package com.android.purebilibili.feature.profile

import com.android.purebilibili.data.model.AccountLedgerKind
import java.math.BigDecimal
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal val AccountLedgerKind.title: String
    get() = when (this) {
        AccountLedgerKind.COINS -> "硬币明细"
        AccountLedgerKind.EXPERIENCE -> "经验明细"
    }

internal enum class LedgerDeltaDirection { INCREASE, DECREASE, ZERO, UNKNOWN }
internal data class LedgerDelta(val text: String, val direction: LedgerDeltaDirection)

internal fun formatLedgerDelta(delta: JsonPrimitive?): LedgerDelta {
    val amount = delta?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }?.let {
        try { BigDecimal(it) } catch (_: NumberFormatException) { null }
    } ?: return LedgerDelta("未知", LedgerDeltaDirection.UNKNOWN)
    // 限制异常指数，避免服务端脏数据在展开十进制时分配过大字符串。
    if (amount.precision() > 64 || amount.scale() !in -64..64) return LedgerDelta("未知", LedgerDeltaDirection.UNKNOWN)
    val normalized = amount.stripTrailingZeros().toPlainString()
    return when (amount.signum()) {
        1 -> LedgerDelta("+$normalized", LedgerDeltaDirection.INCREASE)
        -1 -> LedgerDelta(normalized, LedgerDeltaDirection.DECREASE)
        else -> LedgerDelta("0", LedgerDeltaDirection.ZERO)
    }
}
