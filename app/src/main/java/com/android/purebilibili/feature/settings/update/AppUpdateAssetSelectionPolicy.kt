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
