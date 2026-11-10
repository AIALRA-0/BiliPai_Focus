package com.android.purebilibili.feature.video.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.os.SystemClock

internal class VideoNetworkPressureObserver(context: Context) {
    private val connectivityManager = context.applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private var observedNetworkHandle: Long? = null
    private var networkObservationStartedAtMs = 0L

    private fun observeNetwork(networkHandle: Long?) {
        if (observedNetworkHandle != networkHandle) {
            observedNetworkHandle = networkHandle
            networkObservationStartedAtMs = SystemClock.elapsedRealtime()
        }
    }

    fun isConstrained(): Boolean = runCatching {
        val network = connectivityManager?.activeNetwork
        observeNetwork(network?.networkHandle)
        val capabilities = network?.let { connectivityManager?.getNetworkCapabilities(it) }
        isConstrainedVideoNetwork(
            networkHandle = observedNetworkHandle,
            downstreamKbps = capabilities?.linkDownstreamBandwidthKbps ?: 0,
            sample = latestSample,
            nowMs = SystemClock.elapsedRealtime()
        )
    }.getOrDefault(false)

    fun recordBandwidthSample(totalLoadTimeMs: Int, totalBytesLoaded: Long, bitrateEstimate: Long) {
        if (totalLoadTimeMs < 100 || totalBytesLoaded < 32_768L || bitrateEstimate <= 0L) return
        val networkHandle = runCatching { connectivityManager?.activeNetwork?.networkHandle }
            .getOrNull() ?: return
        if (observedNetworkHandle != networkHandle) {
            observeNetwork(networkHandle)
            return
        }
        val nowMs = SystemClock.elapsedRealtime()
        if (nowMs - networkObservationStartedAtMs < totalLoadTimeMs) return
        latestSample = VideoBandwidthSample(networkHandle, bitrateEstimate, nowMs)
    }

    companion object {
        @Volatile
        private var latestSample: VideoBandwidthSample? = null
    }
}
