package com.android.purebilibili.core.store

import android.content.Context
import com.android.purebilibili.core.network.policy.AppHttpProxySettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sync-readable HTTP proxy prefs for OkHttp [ProxySelector].
 * SharedPreferences only — must be available before network clients are first used.
 */
object NetworkProxyStore {
    private const val PREFS = "network_proxy_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_HOST = "host"
    private const val KEY_PORT = "port"

    @Volatile
    private var cached: AppHttpProxySettings = AppHttpProxySettings()

    @Volatile
    private var initialized: Boolean = false

    private val mutableSettings = MutableStateFlow(AppHttpProxySettings())

    val settings: StateFlow<AppHttpProxySettings> = mutableSettings.asStateFlow()

    fun init(context: Context) {
        if (initialized) return
        val loaded = load(context.applicationContext)
        cached = loaded
        mutableSettings.value = loaded
        initialized = true
    }

    fun getSync(): AppHttpProxySettings = cached

    fun getSync(context: Context): AppHttpProxySettings {
        if (!initialized) {
            init(context)
        }
        return cached
    }

    fun save(context: Context, settings: AppHttpProxySettings) {
        val appContext = context.applicationContext
        val normalized = settings.copy(
            host = settings.host.trim(),
            portText = settings.portText.trim(),
        )
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, normalized.enabled)
            .putString(KEY_HOST, normalized.host)
            .putString(KEY_PORT, normalized.portText)
            .apply()
        cached = normalized
        mutableSettings.value = normalized
    }

    private fun load(context: Context): AppHttpProxySettings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return AppHttpProxySettings(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            host = prefs.getString(KEY_HOST, "").orEmpty(),
            portText = prefs.getString(KEY_PORT, "").orEmpty(),
        )
    }
}
