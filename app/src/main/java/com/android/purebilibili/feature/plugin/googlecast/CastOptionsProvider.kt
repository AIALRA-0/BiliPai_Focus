package com.android.purebilibili.feature.plugin.googlecast

import android.content.Context
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider

object CastReceiverPolicy {
    const val DEFAULT_RECEIVER_APP_ID: String =
        CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
    const val OPTIONS_PROVIDER_CLASS_NAME: String =
        "com.android.purebilibili.feature.plugin.googlecast.CastOptionsProvider"

    fun resolveReceiverApplicationId(): String = DEFAULT_RECEIVER_APP_ID

    fun resolveAdditionalSessionProviders(): List<SessionProvider>? = null
}

class CastOptionsProvider : OptionsProvider {

    override fun getCastOptions(context: Context): CastOptions {
        return CastOptions.Builder()
            .setReceiverApplicationId(CastReceiverPolicy.resolveReceiverApplicationId())
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return CastReceiverPolicy.resolveAdditionalSessionProviders()
    }
}
