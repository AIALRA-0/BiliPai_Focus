package com.android.purebilibili.feature.settings.share

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

const val SETTINGS_SHARE_SCHEMA_VERSION = 1

/**
 * 导出包内的设备/显示调试信息（供开发者排查 UI），不参与导入回写。
 */
@Serializable
data class SettingsShareDeviceDebugInfo(
    val androidSdkInt: Int,
    val androidRelease: String,
    val securityPatch: String = "",
    val manufacturer: String,
    val brand: String,
    val model: String,
    val device: String,
    val product: String,
    val hardware: String,
    val displayId: String,
    val widthPixels: Int,
    val heightPixels: Int,
    val density: Float,
    val densityDpi: Int,
    val scaledDensity: Float,
    val xdpi: Float,
    val ydpi: Float,
    val widthDp: Float,
    val heightDp: Float,
    val smallestWidthDp: Int,
    val fontScale: Float,
    val uiModeNight: Boolean,
    val uiPresetValue: Int,
    val uiPresetName: String,
    val androidNativeVariantValue: Int,
    val androidNativeVariantName: String,
    val appVersionName: String,
    val appVersionCode: Long,
)

@Serializable
data class SettingsShareProfile(
    val schemaVersion: Int = SETTINGS_SHARE_SCHEMA_VERSION,
    val app: String = "BiliPai",
    val appVersion: String,
    val exportedAtIso: String,
    val profileName: String,
    val sections: SettingsShareSections = SettingsShareSections(),
    /** 设备调试快照；默认导出包含，导入时忽略。 */
    val deviceDebug: SettingsShareDeviceDebugInfo? = null,
)

@Serializable
data class SettingsShareSections(
    val appearance: Map<String, JsonElement> = emptyMap(),
    val playback: Map<String, JsonElement> = emptyMap(),
    val gesture: Map<String, JsonElement> = emptyMap(),
    val danmaku: Map<String, JsonElement> = emptyMap(),
    val navigation: Map<String, JsonElement> = emptyMap()
)

enum class SettingsShareSection(val wireKey: String, val label: String) {
    APPEARANCE("appearance", "外观"),
    PLAYBACK("playback", "播放"),
    GESTURE("gesture", "手势"),
    DANMAKU("danmaku", "弹幕"),
    NAVIGATION("navigation", "导航");
}

data class SettingsShareEntryDefinition(
    val storageKey: String,
    val section: SettingsShareSection
)

data class SettingsShareImportPreview(
    val profileName: String,
    val importableSections: List<SettingsShareSection>,
    val skippedKeys: List<String>
)

data class SettingsShareApplyResult(
    val appliedKeys: List<String>,
    val skippedKeys: List<String>
)

data class SettingsShareExportArtifact(
    val fileName: String,
    val json: String,
    val profile: SettingsShareProfile
)

data class SettingsShareImportSession(
    val profile: SettingsShareProfile,
    val preview: SettingsShareImportPreview,
    val rawJson: String
)

data class SavedSettingsProfile(
    val name: String,
    val fileName: String,
    val exportedAtIso: String,
)
