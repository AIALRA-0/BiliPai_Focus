package com.android.purebilibili.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountLoginDevicesResponse(
    val code: Int = -1,
    val message: String = "",
    val data: AccountLoginDevicesData? = null,
)

@Serializable
data class AccountLoginDevicesData(val devices: List<AccountLoginDevice>? = null)

@Serializable
data class AccountLoginDevice(
    @SerialName("device_name") val deviceName: String? = null,
    @SerialName("is_current_device") val isCurrentDevice: Boolean? = null,
    @SerialName("latest_login_at") val latestLoginAt: String? = null,
    val source: String? = null,
)

@Serializable
data class AccountLoginRecordsResponse(
    val code: Int = -1,
    val message: String = "",
    val data: AccountLoginRecordsData? = null,
)

@Serializable
data class AccountLoginRecordsData(val list: List<AccountLoginRecord>? = null)

@Serializable
data class AccountLoginRecord(
    val ip: String? = null,
    @SerialName("time_at") val timeAt: String? = null,
    val geo: String? = null,
)
