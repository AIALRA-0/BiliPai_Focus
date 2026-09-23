package com.android.purebilibili.feature.login

import android.content.Context

/**
 * Mirrors PiliPlus' identity lifetime:
 * - `buvid` is generated once and persisted in local preferences.
 * - `deviceId` is generated once for the current app process.
 */
internal object PiliPlusLoginIdentityStore {
    private const val PREFERENCES_NAME = "piliplus_login_identity"
    private const val KEY_BUVID = "buvid"
    private val buvidPattern = Regex("XY[0-9a-f]{35}")

    @Volatile
    private var processDeviceId: String? = null

    fun get(context: Context): PiliPlusLoginIdentity {
        val appContext = context.applicationContext
        val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val storedBuvid = preferences.getString(KEY_BUVID, null)
            ?.takeIf { it.matches(buvidPattern) }
        val generated = createPiliPlusLoginIdentity()
        val buvid = storedBuvid ?: generated.buvid.also { value ->
            preferences.edit().putString(KEY_BUVID, value).apply()
        }
        val deviceId = processDeviceId ?: synchronized(this) {
            processDeviceId ?: generated.deviceId.also { value ->
                processDeviceId = value
            }
        }
        return PiliPlusLoginIdentity(buvid = buvid, deviceId = deviceId)
    }
}
