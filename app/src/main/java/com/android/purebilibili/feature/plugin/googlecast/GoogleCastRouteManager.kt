package com.android.purebilibili.feature.plugin.googlecast

import android.content.Context
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.android.purebilibili.core.plugin.CastPluginRoute
import com.google.android.gms.cast.CastMediaControlIntent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object GoogleCastRouteManager {

    private val _routes = MutableStateFlow<List<CastPluginRoute>>(emptyList())
    val routes: StateFlow<List<CastPluginRoute>> = _routes.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private var mediaRouter: MediaRouter? = null
    private var selector: MediaRouteSelector? = null
    private var callback: MediaRouter.Callback? = null

    private val castControlCategory: String by lazy {
        CastMediaControlIntent.categoryForCast(
            CastReceiverPolicy.resolveReceiverApplicationId()
        )
    }

    private val routeCache = mutableMapOf<String, MediaRouter.RouteInfo>()

    fun startDiscovery(context: Context) {
        if (mediaRouter != null) return
        val router = MediaRouter.getInstance(context)
        val sel = MediaRouteSelector.Builder()
            .addControlCategory(castControlCategory)
            .build()
        val cb = object : MediaRouter.Callback() {
            override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) {
                updateRoutes(router)
            }

            override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) {
                updateRoutes(router)
            }

            override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
                updateRoutes(router)
            }
        }
        router.addCallback(sel, cb, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
        mediaRouter = router
        selector = sel
        callback = cb
        _isDiscovering.value = true
        updateRoutes(router)
        _isDiscovering.value = false
    }

    fun stopDiscovery() {
        val router = mediaRouter ?: return
        val cb = callback ?: return
        router.removeCallback(cb)
        mediaRouter = null
        selector = null
        callback = null
        _isDiscovering.value = false
        _routes.value = emptyList()
    }

    fun getCachedRoute(routeId: String): MediaRouter.RouteInfo? {
        return routeCache[routeId]
    }

    private fun updateRoutes(router: MediaRouter) {
        val castRoutes = router.routes.mapNotNull { route ->
            routeCache[route.id] = route
            toCastPluginRoute(
                routeId = route.id,
                name = route.name,
                description = route.description,
                deviceType = route.deviceType,
                isDefaultOrBluetooth = route.isDefaultOrBluetooth,
                supportsCastCategory = route.supportsControlCategory(castControlCategory)
            )
        }.distinctBy { it.routeId }
        _routes.value = castRoutes
    }
}
