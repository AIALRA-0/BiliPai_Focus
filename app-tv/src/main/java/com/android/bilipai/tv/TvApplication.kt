package com.android.bilipai.tv

import android.app.Application
import com.android.purebilibili.core.network.CoreNetworkConfig
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.WbiKeyManager
import com.android.purebilibili.core.store.TokenManager

class TvApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NetworkModule.init(this, CoreNetworkConfig(isPrivacyModeEnabled = { TvPreferences(it).privacyMode }))
        TokenManager.init(this)
        WbiKeyManager.restoreFromStorage(this)
    }
}
