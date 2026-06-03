package com.android.purebilibili.feature.plugin.googlecast

import androidx.mediarouter.media.MediaRouter
import com.android.purebilibili.core.plugin.CastPluginRoute

internal fun toCastPluginRoute(
    routeId: String,
    name: String,
    description: String?,
    deviceType: Int,
    isDefaultOrBluetooth: Boolean,
    supportsCastCategory: Boolean
): CastPluginRoute? {
    if (isDefaultOrBluetooth) return null
    if (!supportsCastCategory) return null
    return CastPluginRoute(
        routeId = routeId,
        name = name.ifBlank { "Unknown Device" },
        description = description
    )
}
