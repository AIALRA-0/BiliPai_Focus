package com.android.purebilibili.core.plugin.skin

import com.android.purebilibili.core.store.HomeSettings

fun resolveUiSkinState(
    selection: UiSkinSelection,
    installedSkins: List<InstalledUiSkinPackage>
): UiSkinState {
    if (!selection.enabled) {
        return UiSkinState()
    }
    val selectedInstallId = selection.selectedInstallId
    val activeSkin = if (!selectedInstallId.isNullOrBlank()) {
        installedSkins.firstOrNull { it.installId == selectedInstallId }
    } else {
        val selectedSkinId = selection.selectedSkinId
        if (selectedSkinId.isNullOrBlank()) {
            null
        } else {
            installedSkins.firstOrNull { it.manifest.skinId == selectedSkinId }
        }
    }
        ?: return UiSkinState()
    return UiSkinState(
        enabled = true,
        activeSkin = activeSkin
    )
}

fun resolveUiSkinHomeSettings(
    homeSettings: HomeSettings,
    uiSkinState: UiSkinState
): HomeSettings {
    return homeSettings
}
