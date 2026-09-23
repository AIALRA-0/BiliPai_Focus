package com.android.purebilibili.feature.cast

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

internal enum class LocalNetworkPermissionRequirement {
    NONE,
    LOCAL_NETWORK,
    NEARBY_WIFI_DEVICES,
    LOCATION
}

internal fun resolveLocalNetworkPermissionRequirement(
    sdkInt: Int = Build.VERSION.SDK_INT
): LocalNetworkPermissionRequirement = when {
    sdkInt >= 37 -> LocalNetworkPermissionRequirement.LOCAL_NETWORK
    sdkInt >= Build.VERSION_CODES.TIRAMISU -> LocalNetworkPermissionRequirement.NEARBY_WIFI_DEVICES
    sdkInt >= Build.VERSION_CODES.S -> LocalNetworkPermissionRequirement.LOCATION
    else -> LocalNetworkPermissionRequirement.NONE
}

internal fun localNetworkRuntimePermissions(
    sdkInt: Int = Build.VERSION.SDK_INT
): Array<String> = when (resolveLocalNetworkPermissionRequirement(sdkInt)) {
    LocalNetworkPermissionRequirement.LOCAL_NETWORK -> arrayOf(Manifest.permission.ACCESS_LOCAL_NETWORK)
    LocalNetworkPermissionRequirement.NEARBY_WIFI_DEVICES -> arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
    LocalNetworkPermissionRequirement.LOCATION -> arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    LocalNetworkPermissionRequirement.NONE -> emptyArray()
}

internal fun hasRawLocalNetworkAccess(
    context: Context,
    sdkInt: Int = Build.VERSION.SDK_INT
): Boolean = localNetworkRuntimePermissions(sdkInt).all { permission ->
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
