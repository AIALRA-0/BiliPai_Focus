package com.android.purebilibili.feature.settings

internal fun selectPreferredAppUpdateAsset(
    assets: List<AppUpdateAsset>
): AppUpdateAsset? {
    return assets
        .asSequence()
        .filter { it.isApk }
        .sortedWith(
            compareBy<AppUpdateAsset> { asset ->
                val lowercaseName = asset.name.lowercase()
                when {
                    "arm64" in lowercaseName -> 1
                    "x86" in lowercaseName -> 1
                    "universal" in lowercaseName -> 0
                    else -> 0
                }
            }.thenByDescending { it.sizeBytes }
        )
        .firstOrNull()
}

/** Prefer the checksum published with the asset, then fall back to release build metadata. */
internal fun resolveAppUpdateExpectedSha256(
    asset: AppUpdateAsset,
    buildMetadata: AppReleaseBuildMetadata?
): String? {
    return asset.sha256Digest ?: buildMetadata
        ?.artifacts
        ?.firstOrNull { it.name.equals(asset.name, ignoreCase = true) }
        ?.sha256
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}
