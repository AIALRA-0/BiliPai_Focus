package com.android.purebilibili.feature.plugin.dlna

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tv
import com.android.purebilibili.core.plugin.CastPluginApi
import com.android.purebilibili.core.plugin.CastPluginMediaRequest
import com.android.purebilibili.core.plugin.CastPluginRoute
import com.android.purebilibili.core.plugin.PluginCapability
import com.android.purebilibili.core.plugin.PluginCapabilityManifest
import com.android.purebilibili.feature.cast.DlnaManager
import com.android.purebilibili.feature.cast.associateNotNullBy
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
    override val version = "0.1.0"
    override val author = "BiliPai项目组, Leko (lekoOwO)"
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

    private val _ssdpDevices = MutableStateFlow<List<SsdpDiscovery.SsdpDevice>>(emptyList())
    private val _ssdpProfiles = MutableStateFlow<Map<String, SsdpCastClient.SsdpDeviceProfile>>(emptyMap())

    private var discoveryJob: Job? = null
    private var ssdpJob: Job? = null
    private var boundContext: Context? = null

    private var clingCache = emptyMap<String, com.android.purebilibili.feature.cast.CastDeviceInfo>()
    private var ssdpCache = emptyMap<String, SsdpDiscovery.SsdpDevice>()

    override fun startRouteDiscovery(context: Context) {
        if (discoveryJob?.isActive == true) return
        val appContext = context.applicationContext

        DlnaManager.bindService(appContext)
        boundContext = appContext
        DlnaManager.refresh()

        startRouteCollector()
        refreshSsdpDevices(appContext)
    }

    override fun refreshRouteDiscovery(context: Context) {
        val appContext = context.applicationContext
        if (boundContext == null) {
            DlnaManager.bindService(appContext)
            boundContext = appContext
        }
        startRouteCollector()
        DlnaManager.refresh()
        refreshSsdpDevices(appContext)
    }

    private fun startRouteCollector() {
        if (discoveryJob?.isActive == true) return
        discoveryJob = scope.launch {
            combine(
                DlnaManager.devices,
                _ssdpDevices,
                _ssdpProfiles
            ) { clingDevices, ssdpDevices, profiles ->
                val visibleSsdp = resolveVisibleSsdpDevices(clingDevices, ssdpDevices, profiles)
                buildDlnaRouteSnapshot(clingDevices, visibleSsdp)
            }.collect { snapshot ->
                clingCache = snapshot.clingCache
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
                val discovered = SsdpDiscovery.discover(context, 5000)
                val profiles = discovered.associateNotNullBy(
                    keySelector = { it.location },
                    valueSelector = { SsdpCastClient.fetchDeviceProfile(it) }
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

        val ctx = boundContext
        if (ctx != null) {
            DlnaManager.unbindService(ctx)
            boundContext = null
        }

        _routes.value = emptyList()
        _ssdpDevices.value = emptyList()
        _ssdpProfiles.value = emptyMap()
        clingCache = emptyMap()
        ssdpCache = emptyMap()
        _isDiscovering.value = false
    }

    override suspend fun cast(
        context: Context,
        route: CastPluginRoute,
        media: CastPluginMediaRequest
    ): Result<Unit> {
        val selection = resolveDlnaRouteSelection(route.routeId, clingCache, ssdpCache)
        return when (selection) {
            is DlnaRouteSelection.Cling -> {
                DlnaManager.cast(selection.device, media.url, media.title, media.creator)
                Result.success(Unit)
            }
            is DlnaRouteSelection.Ssdp -> {
                SsdpCastClient.cast(selection.device, media.url, media.title, media.creator)
            }
            null -> Result.failure(IllegalArgumentException("未知的 DLNA 设备: ${route.routeId}"))
        }
    }

    override suspend fun onEnable() {
        // No-op; discovery starts on demand from dialog
    }

    override suspend fun onDisable() {
        stopRouteDiscovery()
    }
}
