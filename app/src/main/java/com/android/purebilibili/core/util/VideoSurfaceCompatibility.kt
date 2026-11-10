package com.android.purebilibili.core.util

import android.os.Build

/**
 * vivo and Xiaomi Android 13 tombstones show hwuiTask0 crashing in SurfaceControl
 * transactions while SurfaceView reports its position. Keep SDR off that native path.
 * HDR callers retain SurfaceView because TextureView cannot preserve HDR display output.
 */
internal fun prefersTextureVideoSurface(
    sdkInt: Int = Build.VERSION.SDK_INT,
    manufacturer: String? = Build.MANUFACTURER,
    brand: String? = Build.BRAND,
): Boolean = sdkInt == 33 &&
    (manufacturer.equals("vivo", ignoreCase = true) ||
        brand.equals("vivo", ignoreCase = true) || brand.equals("iQOO", ignoreCase = true) ||
        manufacturer.equals("Xiaomi", ignoreCase = true) ||
        brand.equals("Xiaomi", ignoreCase = true) || brand.equals("Redmi", ignoreCase = true) ||
        brand.equals("POCO", ignoreCase = true))
