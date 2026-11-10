package com.android.purebilibili.core.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException

/** App-specific diagnostics are injected; data code never depends on an application module. */
data class CoreNetworkConfig(
    val debug: Boolean = false,
    val allowHardcodedDnsFallback: Boolean = false,
    val log: (String, String, String, Throwable?) -> Unit = { level, tag, message, error ->
        when (level) {
            "E" -> Log.e(tag, message, error)
            "W" -> Log.w(tag, message, error)
        }
    },
    val reportApiError: (String, Int, String) -> Unit = { _, _, _ -> },
    val isPrivacyModeEnabled: (Context) -> Boolean = { false },
)

object CoreNetworkRuntime {
    @Volatile
    var config = CoreNetworkConfig()
        internal set
}

object CoreDataLog {
    fun d(tag: String, message: String) = CoreNetworkRuntime.config.log("D", tag, message, null)
    fun w(tag: String, message: String, error: Throwable? = null) =
        CoreNetworkRuntime.config.log("W", tag, message, error)
    fun e(tag: String, message: String, error: Throwable? = null) =
        CoreNetworkRuntime.config.log("E", tag, message, error)

    fun reportApiError(endpoint: String, httpCode: Int, errorMessage: String) =
        CoreNetworkRuntime.config.reportApiError(endpoint, httpCode, errorMessage)
}

internal fun shouldReportNetworkFailure(callCanceled: Boolean, throwable: Throwable): Boolean {
    if (callCanceled) return false
    var cause: Throwable? = throwable
    val visited = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Throwable, Boolean>())
    while (cause != null && visited.add(cause)) {
        if (cause is CancellationException) return false
        if (cause.message?.trim()?.lowercase() in setOf("canceled", "cancelled")) return false
        cause = cause.cause
    }
    return true
}
