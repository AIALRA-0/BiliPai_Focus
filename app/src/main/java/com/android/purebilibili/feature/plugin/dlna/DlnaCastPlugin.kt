package com.android.purebilibili.feature.plugin.dlna

import com.android.purebilibili.core.plugin.BuiltinPluginIds

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tv
import com.android.purebilibili.plugin.sdk.CastPluginApi
import com.android.purebilibili.core.plugin.BuiltinPlugin
import com.android.purebilibili.plugin.sdk.CastDiscoveryRequirement
import com.android.purebilibili.plugin.sdk.CastPluginMediaRequest
import com.android.purebilibili.plugin.sdk.CastPluginPlaybackState
import com.android.purebilibili.plugin.sdk.CastPluginRoute
import com.android.purebilibili.plugin.sdk.PluginCapability
import com.android.purebilibili.plugin.sdk.PluginCapabilityManifest
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.feature.cast.preferSsdpDeviceAdvertisement
import com.android.purebilibili.feature.cast.hasRawLocalNetworkAccess
import com.android.purebilibili.feature.cast.LocalProxyServer
import com.android.purebilibili.feature.cast.SsdpCastClient
import com.android.purebilibili.feature.cast.SsdpDiscovery
import com.android.purebilibili.feature.cast.resolveVisibleSsdpDevices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit


class DlnaCastPlugin : BuiltinPlugin, CastPluginApi {

    override val id = BuiltinPluginIds.DLNA_CAST
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

    private val scope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }

    private val _routes = MutableStateFlow<List<CastPluginRoute>>(emptyList())
    override val routes: StateFlow<List<CastPluginRoute>> = _routes.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    override val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()
    override val playbackState: StateFlow<CastPluginPlaybackState> = SsdpCastClient.playbackState

    private val _ssdpDevices = MutableStateFlow<List<SsdpDiscovery.SsdpDevice>>(emptyList())
    private val _ssdpProfiles = MutableStateFlow<Map<String, SsdpCastClient.SsdpDeviceProfile>>(emptyMap())

    private var discoveryJob: Job? = null
    private var ssdpJob: Job? = null
    private var discoveryGeneration = 0L
    private var ssdpCache = emptyMap<String, SsdpDiscovery.SsdpDevice>()

    override fun startRouteDiscovery(context: Context) {
        val appContext = context.applicationContext
        if (!hasRawLocalNetworkAccess(appContext)) {
            onDiscoveryAccessRevoked()
            return
        }

        startRouteCollector()
        // 先展示缓存，同时重新搜索，避免换网络或设备上下线后只显示旧列表。
        if (ssdpJob?.isActive == true) return
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
        val generation = ++discoveryGeneration
        ssdpJob?.cancel()
        ssdpJob = scope.launch {
            _isDiscovering.value = true
            try {
                coroutineScope {
                    val events = Channel<SsdpDiscovery.SsdpDevice>(Channel.UNLIMITED)
                    val devices = linkedMapOf<String, SsdpDiscovery.SsdpDevice>()
                    val profiles = mutableMapOf<String, SsdpCastClient.SsdpDeviceProfile>()
                    val profileJobs = mutableListOf<Job>()
                    val permits = Semaphore(4)
                    val scan = launch {
                        try {
                            SsdpDiscovery.discover(context, timeoutMs = 8_000) { events.trySend(it) }
                        } finally {
                            events.close()
                        }
                    }
                    for (device in events) {
                        val previous = devices[device.location]
                        val preferred = preferSsdpDeviceAdvertisement(previous, device)
                        if (previous == preferred) continue
                        devices[device.location] = preferred
                        if (previous == null) {
                            if (devices.size == 1) _ssdpProfiles.value = emptyMap()
                            profileJobs += launch {
                                val profile = permits.withPermit { SsdpCastClient.fetchDeviceProfile(device) }
                                if (profile != null) {
                                    profiles[device.location] = profile
                                    _ssdpProfiles.value = profiles.toMap()
                                }
                            }
                        }
                        _ssdpDevices.value = devices.values.toList()
                    }
                    scan.join()
                    if (devices.isEmpty()) {
                        _ssdpDevices.value = emptyList()
                        _ssdpProfiles.value = emptyMap()
                    }
                    profileJobs.joinAll()
                    Logger.i("DlnaCastPlugin", "📺 [DLNA] Discovery summary: ssdp=${devices.size}, profiles=${profiles.size}")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Leave current values on failure
            } finally {
                if (generation == discoveryGeneration) _isDiscovering.value = false
            }
        }
    }

    override fun stopRouteDiscovery() {
        discoveryGeneration++
        discoveryJob?.cancel()
        discoveryJob = null
        ssdpJob?.cancel()
        ssdpJob = null
        // 保留上次结果供下次打开时立即显示；重新打开会启动新的搜索。
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
                    autoplay = media.autoplay,
                    contentType = media.contentType,
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
