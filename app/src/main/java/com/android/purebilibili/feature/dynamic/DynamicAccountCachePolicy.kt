package com.android.purebilibili.feature.dynamic

internal fun dynamicAccountStorageName(baseName: String, accountMid: Long?): String {
    val account = accountMid?.takeIf { it > 0L }?.toString() ?: "guest"
    return "${baseName}_$account"
}
