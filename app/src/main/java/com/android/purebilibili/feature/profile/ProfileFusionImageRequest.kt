package com.android.purebilibili.feature.profile

import coil3.request.ImageRequest
import coil3.request.allowHardware

/**
 * Images drawn by [ProfileAvatarFusionView] also enter its CPU bitmap canvas on API 30
 * and below, or whenever its GPU backend is unavailable. Keep this invariant for the
 * entire request lifetime: scroll and the runtime visual guard can switch drawing paths
 * after decoding. Coil also rejects incompatible HARDWARE memory-cache hits for these
 * requests, without changing their source, cache keys, headers or cache policies.
 */
internal fun ImageRequest.Builder.buildForProfileFusion(): ImageRequest =
    allowHardware(false).build()
