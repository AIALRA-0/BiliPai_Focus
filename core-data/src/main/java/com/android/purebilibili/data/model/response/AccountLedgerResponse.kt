package com.android.purebilibili.data.model.response

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class AccountLedgerResponse(
    val code: Int = -1,
    val message: String = "",
    val data: AccountLedgerData? = null,
)

@Serializable
data class AccountLedgerData(val list: List<AccountLedgerEntry>? = null)

@Serializable
data class AccountLedgerEntry(
    val time: String? = null,
    val delta: JsonPrimitive? = null,
    val reason: String? = null,
)
