package com.android.purebilibili.feature.plugin.dlna

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tv
import com.android.purebilibili.core.plugin.CastPluginApi
import com.android.purebilibili.core.plugin.CastDiscoveryRequirement
import com.android.purebilibili.core.plugin.CastPluginMediaRequest
import com.android.purebilibili.core.plugin.CastPluginPlaybackState
import com.android.purebilibili.core.plugin.CastPluginRoute
import com.android.purebilibili.core.plugin.PluginCapability
import com.android.purebilibili.core.plugin.PluginCapabilityManifest
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.feature.cast.associateNotNullBy
import com.android.purebilibili.feature.cast.hasRawLocalNetworkAccess
import com.android.purebilibili.feature.cast.LocalProxyServer
import com.android.purebilibili.feature.cast.SsdpCastClient
import com.android.purebilibili.feature.cast.SsdpDiscovery
import com.android.purebilibili.feature.cast.resolveVisibleSsdpDevices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

const val DLNA_CAST_PLUGIN_ID = "dlna_cast"

class DlnaCastPlugin : CastPluginApi {

    override val id = DLNA_CAST_PLUGIN_ID
    override val name = "DLNA"
    override val description = "通过 DLNA 协议将视频投屏到智能电视等设备"
    override val version = "0.1.1"
    override val author = "BiliPai项目组, Leko (lekoOwO)"
    override val discoveryRequirement = CastDiscoveryRequirement.RAW_LOCAL_NETWORK
    override val icon = Icons.Rounded.Tv
    override val capabilityManifest = PluginCapabilityManifest(
        pluginId = id,
        displayName = name,
        version = version,
        apiVersion = 1,
        entryClassName = "com.android.purebilibili.feature.plugin.dlna.DlnaCastPlugin",
        capabilities = setOf(
            PluginCapability.PLAYER_STATE,
            PluginCapability.PLAYER_CONTROL,
            PluginCapability.NETWORK,
            PluginCapability.PLUGIN_STORAGE
        )
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _routes = MutableStateFlow<List<CastPluginRoute>>(emptyList())
    override val routes: StateFlow<List<CastPluginRoute>> = _routes.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    override val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()
    override val playbackState: StateFlow<CastPluginPlaybackState> = SsdpCastClient.playbackState

    private val _ssdpDevices = MutableStateFlow<List<SsdpDiscovery.SsdpDevice>>(emptyList())
    private val _ssdpProfiles = MutableStateFlow<Map<String, SsdpCastClient.SsdpDeviceProfile>>(emptyMap())

    private var discoveryJob: Job? = null
    private var ssdpJob: Job? = null
    private var ssdpCache = emptyMap<String, SsdpDiscovery.SsdpDevice>()

    override fun startRouteDiscovery(context: Context) {
        val appContext = context.applicationContext
        if (!hasRawLocalNetworkAccess(appContext)) {
            onDiscoveryAccessRevoked()
            return
        }

        startRouteCollector()
        // DeviceListDialog is recreated whenever the user opens another video.
        // Keep the last valid routes available and only scan on an explicit refresh
        // or when there is no cached route yet.
        if (_routes.value.isNotEmpty() || ssdpJob?.isActive == true) return
        refreshSsdpDevices(appContext)
    }

    override fun refreshRouteDiscovery(context: Context) {
        val appContext = context.applicationContext
        if (!hasRawLocalNetworkAccess(appContext)) {
            onDiscoveryAccessRevoked()
            return
        }
        startRouteCollector()
        refreshSsdpDevices(appContext)
    }

    private fun startRouteCollector() {
        if (discoveryJob?.isActive == true) return
        discoveryJob = scope.launch {
            combine(_ssdpDevices, _ssdpProfiles) { ssdpDevices, profiles ->
                buildDlnaRouteSnapshot(resolveVisibleSsdpDevices(ssdpDevices, profiles))
            }.collect { snapshot ->
                ssdpCache = snapshot.ssdpCache
                _routes.value = snapshot.routes
            }
        }
    }

    private fun refreshSsdpDevices(context: Context) {
        ssdpJob?.cancel()
        ssdpJob = scope.launch {
            _isDiscovering.value = true
            try {
                val discovered = SsdpDiscovery.discover(context, timeoutMs = 8_000)
                val profiles = discovered.associateNotNullBy(
                    keySelector = { it.location },
                    valueSelector = { SsdpCastClient.fetchDeviceProfile(it) }
                )
                val visible = resolveVisibleSsdpDevices(discovered, profiles)
                Logger.i(
                    "DlnaCastPlugin",
                    "📺 [DLNA] Discovery summary: ssdp=${discovered.size}, profiles=${profiles.size}, castable=${visible.size}"
                )
                _ssdpDevices.value = discovered
                _ssdpProfiles.value = profiles
            } catch (_: Exception) {
                // Leave current values on failure
            } finally {
                _isDiscovering.value = false
            }
        }
    }

    override fun stopRouteDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        ssdpJob?.cancel()
        ssdpJob = null
        // Keep the last successful discovery result. The next cast dialog can
        // render it immediately; the refresh button remains the explicit way
        // to invalidate it.
        _isDiscovering.value = false
    }

    override fun onDiscoveryAccessRevoked() {
        stopRouteDiscovery()
        _routes.value = emptyList()
        _ssdpDevices.value = emptyList()
        _ssdpProfiles.value = emptyMap()
        ssdpCache = emptyMap()
        SsdpCastClient.clearPlaybackSession()
        LocalProxyServer.stopAndClear()
    }

    override suspend fun cast(
        context: Context,
        route: CastPluginRoute,
        media: CastPluginMediaRequest
    ): Result<Unit> {
        if (!hasRawLocalNetworkAccess(context)) {
            onDiscoveryAccessRevoked()
            return Result.failure(SecurityException("DLNA 需要本地网络权限"))
        }
        val selection = resolveDlnaRouteSelection(route.routeId, ssdpCache)
        return when (selection) {
            is DlnaRouteSelection.Ssdp -> {
                SsdpCastClient.cast(
                    device = selection.device,
                    mediaUrl = media.url,
                    title = media.title,
                    creator = media.creator,
                    startPositionMs = media.startPositionMs,
                    autoplay = media.autoplay
                )
            }
            null -> Result.failure(IllegalArgumentException("未知的 DLNA 设备: ${route.routeId}"))
        }
    }

    override suspend fun play(): Result<Unit> = SsdpCastClient.play()

    override suspend fun pause(): Result<Unit> = SsdpCastClient.pause()

    override suspend fun seek(positionMs: Long): Result<Unit> = SsdpCastClient.seek(positionMs)

    override suspend fun onEnable() {
        // No-op; discovery starts on demand from dialog
    }

    override suspend fun onDisable() {
        stopRouteDiscovery()
        _routes.value = emptyList()
        _ssdpDevices.value = emptyList()
        _ssdpProfiles.value = emptyMap()
        ssdpCache = emptyMap()
        SsdpCastClient.clearPlaybackSession()
        LocalProxyServer.stopAndClear()
    }
}
