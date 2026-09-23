package com.android.purebilibili.core.plugin.skin

import kotlinx.serialization.Serializable

enum class UiSkinSurface {
    HOME_BOTTOM_BAR,
    HOME_TOP_CHROME,
    HOME_DRAWER,
    PROFILE,
    DYNAMIC_PUBLISH,
    LOADING_INDICATOR,
    LIKE_EFFECT,
    PLAYER_PROGRESS
}

enum class UiSkinAssetType {
    PNG,
    WEBP,
    JPEG,
    MP4,
    JSON
}

@Serializable
data class UiSkinManifest(
    val formatVersion: Int,
    val skinId: String,
    val displayName: String,
    val version: String,
    val apiVersion: Int,
    val author: String? = null,
    val surfaces: Set<UiSkinSurface>,
    val assets: UiSkinAssets = UiSkinAssets(),
    val colors: UiSkinColorTokens = UiSkinColorTokens(),
    val motion: UiSkinMotionTokens = UiSkinMotionTokens(),
    val styleSourceName: String? = null,
    val styleSourceUrl: String? = null,
    val licenseNote: String? = null,
    val communityShareable: Boolean = false,
    val containsOfficialAssets: Boolean = false
)

@Serializable
data class UiSkinAssets(
    val bottomBarTrim: String? = null,
    val drawerBottomTrim: String? = null,
    val topAtmosphere: String? = null,
    val homeTopTabBackground: String? = null,
    val searchCapsuleBackground: String? = null,
    val homeSideBackground: String? = null,
    val homeProfileBackground: String? = null,
    val homeProfileSquaredBackground: String? = null,
    val homeProfileVideoBackground: String? = null,
    val homeChannelIcon: String? = null,
    val homeChannelSelectedIcon: String? = null,
    val dynamicPublishIcon: String? = null,
    val dynamicPublishSelectedIcon: String? = null,
    val loadingAnimation: String? = null,
    val loadingFrame: String? = null,
    val likeEffectAnimation: String? = null,
    val likeEffectPreview: String? = null,
    val playerProgressIcon: String? = null,
    val playerProgressDraggingIcon: String? = null,
    val playerProgressStaticIcon: String? = null,
    val bottomBarIcons: Map<String, String> = emptyMap()
) {
    fun declaredPaths(): List<String> {
        return buildList {
            bottomBarTrim?.let(::add)
            drawerBottomTrim?.let(::add)
            topAtmosphere?.let(::add)
            homeTopTabBackground?.let(::add)
            searchCapsuleBackground?.let(::add)
            homeSideBackground?.let(::add)
            homeProfileBackground?.let(::add)
            homeProfileSquaredBackground?.let(::add)
            homeProfileVideoBackground?.let(::add)
            homeChannelIcon?.let(::add)
            homeChannelSelectedIcon?.let(::add)
            dynamicPublishIcon?.let(::add)
            dynamicPublishSelectedIcon?.let(::add)
            loadingAnimation?.let(::add)
            loadingFrame?.let(::add)
            likeEffectAnimation?.let(::add)
            likeEffectPreview?.let(::add)
            playerProgressIcon?.let(::add)
            playerProgressDraggingIcon?.let(::add)
            playerProgressStaticIcon?.let(::add)
            addAll(bottomBarIcons.values)
        }
    }
}

@Serializable
data class UiSkinColorTokens(
    val bottomBarTrimTint: String? = null,
    val bottomBarIconTint: String? = null,
    val bottomBarIconDarkTint: String? = null,
    val bottomBarSelectedTint: String? = null,
    val bottomBarSelectedDarkTint: String? = null,
    val topAtmosphereTint: String? = null,
    val searchCapsuleTint: String? = null,
    val sideBackgroundTint: String? = null,
    val dynamicPublishIconTint: String? = null,
    val dynamicPublishShadeTop: String? = null,
    val dynamicPublishShadeBottom: String? = null,
    val playerProgressActiveTint: String? = null,
    val playerProgressBufferedTint: String? = null,
    val playerProgressTrackTint: String? = null,
    val colorMode: String? = null
)

@Serializable
data class UiSkinMotionTokens(
    val bottomBarIconAnimated: Boolean = false,
    val bottomBarIconAnimationMode: String? = null,
    val bottomBarIconMode: String? = null,
    val profileVideoPlayMode: String? = null
)

data class UiSkinAssetEntry(
    val path: String,
    val type: UiSkinAssetType,
    val sizeBytes: Long
)

data class UiSkinPackagePreview(
    val manifest: UiSkinManifest,
    val packageSha256: String,
    val assetEntries: List<UiSkinAssetEntry>
)

@Serializable
data class InstalledUiSkinPackage(
    val manifest: UiSkinManifest,
    val packageSha256: String,
    val packagePath: String,
    val installedAtMillis: Long,
    val enabled: Boolean = false,
    val assetFiles: Map<String, String> = emptyMap(),
    val installId: String = buildUiSkinInstallId(manifest.skinId, packageSha256)
) {
    val skinId: String
        get() = manifest.skinId

    val displayName: String
        get() = manifest.displayName

    fun assetFilePath(assetPath: String?): String? {
        return assetPath?.let(assetFiles::get)
    }
}

data class UiSkinSelection(
    val enabled: Boolean = false,
    val selectedSkinId: String? = null,
    val selectedInstallId: String? = null
)

data class UiSkinState(
    val enabled: Boolean = false,
    val activeSkin: InstalledUiSkinPackage? = null
)

internal fun buildUiSkinInstallId(
    skinId: String,
    packageSha256: String
): String {
    return "${skinId.safeUiSkinFileSegment()}-${packageSha256.take(16)}"
}

internal fun String.safeUiSkinFileSegment(): String {
    return replace(Regex("[^A-Za-z0-9_.-]"), "_")
}
