package com.android.purebilibili.feature.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.android.purebilibili.core.plugin.skin.UiSkinState
import com.android.purebilibili.core.plugin.skin.UiSkinSurface
import java.io.File

data class BottomBarUiSkinDecoration(
    val skinId: String,
    val bottomTrimTint: Color,
    val bottomTrimAccent: Color,
    val bottomTrimImagePath: String? = null,
    val bottomBarIconPaths: Map<BottomNavItem, BottomBarSkinIconPaths> = emptyMap()
) {
    fun iconPathFor(item: BottomNavItem, selected: Boolean = false): String? {
        val paths = bottomBarIconPaths[item] ?: return null
        return if (selected) {
            paths.selected ?: paths.unselected
        } else {
            paths.unselected
        }
    }
}

data class BottomBarSkinIconPaths(
    val unselected: String,
    val selected: String? = null
)

data class TopTabSkinIconPaths(
    val unselected: String,
    val selected: String? = null
) {
    fun pathFor(selected: Boolean): String {
        return if (selected) {
            this.selected ?: unselected
        } else {
            unselected
        }
    }
}

data class HomeUiSkinDecoration(
    val skinId: String,
    val topAtmosphereTint: Color,
    val searchCapsuleTint: Color,
    val topAtmosphereImagePath: String? = null,
    val topTabBackgroundImagePath: String? = null,
    val sideBackgroundImagePath: String? = null,
    val profileBackgroundImagePath: String? = null,
    val profileSquaredBackgroundImagePath: String? = null,
    val topTabSkinIconPaths: Map<String, TopTabSkinIconPaths> = emptyMap(),
    val topTabPartitionSkinIconPaths: TopTabSkinIconPaths? = null
) {
    fun topTabIconPathFor(categoryKey: String, selected: Boolean = false): String? {
        val normalizedKey = categoryKey.trim().uppercase()
        val paths = topTabSkinIconPaths[normalizedKey] ?: return null
        return paths.pathFor(selected)
    }

    fun topTabPartitionIconPath(selected: Boolean = false): String? {
        return topTabPartitionSkinIconPaths?.pathFor(selected)
    }
}

internal fun resolveBottomBarSkinDockIconSize(): Dp = 40.dp

internal fun resolveBottomBarSkinDockHeight(): Dp = 88.dp

internal fun resolveBottomBarSkinDockContentPadding(): PaddingValues = PaddingValues(
    start = 4.dp,
    end = 4.dp,
    top = 0.dp,
    bottom = 0.dp
)

internal fun resolveBottomBarSkinIconLabelGap(): Dp = 2.dp

internal fun resolveBottomBarSkinDockIconTopPadding(): Dp = 10.dp

internal fun resolveBottomBarSkinDockLabelBottomPadding(): Dp = 18.dp

internal fun resolveBottomBarSkinDockLabelFontSize(): TextUnit = 12.sp

internal fun resolveBottomBarSkinDockLabelLineHeight(): TextUnit = 18.sp

internal fun resolveBottomBarMiuixSkinDockIconSize(): Dp = 38.dp

internal fun resolveBottomBarCompactSkinHomeIconSize(): Dp = 40.dp

internal fun resolveMiuixDockedBottomBarItemHeight(hasUiSkinDecoration: Boolean): Dp {
    return if (hasUiSkinDecoration) {
        resolveBottomBarSkinDockHeight()
    } else {
        64.dp
    }
}

@Composable
fun rememberBottomBarUiSkinDecoration(uiSkinState: UiSkinState): BottomBarUiSkinDecoration? {
    return remember(uiSkinState) {
        resolveBottomBarUiSkinDecoration(uiSkinState)
    }
}

@Composable
fun rememberHomeUiSkinDecoration(uiSkinState: UiSkinState): HomeUiSkinDecoration? {
    return remember(uiSkinState) {
        resolveHomeUiSkinDecoration(uiSkinState)
    }
}

fun resolveBottomBarUiSkinDecoration(uiSkinState: UiSkinState): BottomBarUiSkinDecoration? {
    val activeSkin = uiSkinState.activeSkin
    return if (!uiSkinState.enabled || activeSkin == null) {
        null
    } else {
        BottomBarUiSkinDecoration(
            skinId = activeSkin.manifest.skinId,
            bottomTrimTint = parseUiSkinColor(
                value = activeSkin.manifest.colors.bottomBarTrimTint,
                fallback = Color(0xFFEAF8FF)
            ),
            bottomTrimAccent = parseUiSkinColor(
                value = activeSkin.manifest.colors.topAtmosphereTint,
                fallback = Color(0xFFDFF5FF)
            ),
            bottomTrimImagePath = activeSkin.assetFilePath(activeSkin.manifest.assets.bottomBarTrim),
            bottomBarIconPaths = resolveBottomBarSkinIconPaths(activeSkin)
        )
    }
}

fun resolveHomeUiSkinDecoration(uiSkinState: UiSkinState): HomeUiSkinDecoration? {
    val activeSkin = uiSkinState.activeSkin
    return if (!uiSkinState.enabled || activeSkin == null) {
        null
    } else {
        val manifest = activeSkin.manifest
        val hasTopDecoration = UiSkinSurface.HOME_TOP_CHROME in manifest.surfaces &&
            (
                manifest.assets.topAtmosphere != null ||
                    manifest.assets.homeTopTabBackground != null ||
                    manifest.assets.homeSideBackground != null ||
                    manifest.assets.homeProfileBackground != null ||
                    manifest.assets.homeProfileSquaredBackground != null ||
                    manifest.assets.homeChannelIcon != null ||
                    manifest.assets.bottomBarIcons.isNotEmpty() ||
                    manifest.colors.topAtmosphereTint != null ||
                    manifest.colors.searchCapsuleTint != null
                )
        if (!hasTopDecoration) return null
        val topTabIconPaths = resolveTopTabSkinIconPaths(activeSkin)
        val partitionIconPaths = resolveTopTabPartitionSkinIconPaths(activeSkin, topTabIconPaths)
        HomeUiSkinDecoration(
            skinId = manifest.skinId,
            topAtmosphereTint = parseUiSkinColor(
                value = manifest.colors.topAtmosphereTint,
                fallback = Color(0xFFDFF5FF)
            ),
            searchCapsuleTint = parseUiSkinColor(
                value = manifest.colors.searchCapsuleTint,
                fallback = Color.White
            ),
            topAtmosphereImagePath = activeSkin.assetFilePath(manifest.assets.topAtmosphere),
            topTabBackgroundImagePath = activeSkin.assetFilePath(manifest.assets.homeTopTabBackground),
            sideBackgroundImagePath = activeSkin.assetFilePath(manifest.assets.homeSideBackground),
            profileBackgroundImagePath = activeSkin.assetFilePath(manifest.assets.homeProfileBackground),
            profileSquaredBackgroundImagePath = activeSkin.assetFilePath(
                manifest.assets.homeProfileSquaredBackground
            ),
            topTabSkinIconPaths = topTabIconPaths,
            topTabPartitionSkinIconPaths = partitionIconPaths
        )
    }
}

@Composable
internal fun BottomBarSkinIcon(
    iconPath: String,
    contentDescription: String?,
    size: Dp = resolveBottomBarSkinDockIconSize(),
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(size)) {
        AsyncImage(
            model = File(iconPath),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
internal fun BottomBarSkinDecorativeTrim(
    decoration: BottomBarUiSkinDecoration?,
    modifier: Modifier = Modifier,
    clipShape: androidx.compose.ui.graphics.Shape? = null
) {
    if (decoration == null) return
    Box(
        modifier = modifier
            .then(clipShape?.let { Modifier.clip(it) } ?: Modifier)
            .clearAndSetSemantics {}
            .drawBehind {
                val trimHeight = size.height * 0.36f
                val top = size.height - trimHeight
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            decoration.bottomTrimTint.copy(alpha = 0.08f),
                            decoration.bottomTrimTint.copy(alpha = 0.28f)
                        ),
                        startY = top,
                        endY = size.height
                    ),
                    topLeft = Offset(0f, top),
                    size = Size(size.width, trimHeight),
                    cornerRadius = CornerRadius(trimHeight, trimHeight)
                )

                val cloudRadius = trimHeight * 0.38f
                val centers = listOf(0.12f, 0.26f, 0.44f, 0.62f, 0.78f, 0.91f)
                centers.forEachIndexed { index, fraction ->
                    val y = top + trimHeight * if (index % 2 == 0) 0.46f else 0.58f
                    drawCircle(
                        color = decoration.bottomTrimAccent.copy(alpha = 0.18f),
                        radius = cloudRadius * if (index % 2 == 0) 1.0f else 0.78f,
                        center = Offset(size.width * fraction, y)
                    )
                }
            }
    ) {
        val imagePath = decoration.bottomTrimImagePath
        if (!imagePath.isNullOrBlank()) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.82f)
                    .clearAndSetSemantics {}
            )
        }
    }
}

private fun parseUiSkinColor(
    value: String?,
    fallback: Color
): Color {
    val normalized = value
        ?.trim()
        ?.removePrefix("#")
        ?.takeIf { it.length == 6 || it.length == 8 }
        ?: return fallback
    val argb = if (normalized.length == 6) "FF$normalized" else normalized
    return runCatching { Color(argb.toLong(16)) }.getOrDefault(fallback)
}

private fun resolveBottomBarSkinIconPaths(
    activeSkin: com.android.purebilibili.core.plugin.skin.InstalledUiSkinPackage
): Map<BottomNavItem, BottomBarSkinIconPaths> {
    val manifestIcons = activeSkin.manifest.assets.bottomBarIcons
    return buildMap {
        mapOf(
            "home" to ("home_selected" to BottomNavItem.HOME),
            "following" to ("following_selected" to BottomNavItem.DYNAMIC),
            "member" to ("member_selected" to BottomNavItem.HISTORY),
            "profile" to ("profile_selected" to BottomNavItem.PROFILE)
        ).forEach { (unselectedKey, selectedKeyAndItem) ->
            val (selectedKey, item) = selectedKeyAndItem
            activeSkin.assetFilePath(manifestIcons[unselectedKey])?.let { unselectedPath ->
                put(
                    item,
                    BottomBarSkinIconPaths(
                        unselected = unselectedPath,
                        selected = activeSkin.assetFilePath(manifestIcons[selectedKey])
                    )
                )
            }
        }
        val settingsIcon = resolveBottomBarSettingsSkinIconPaths(activeSkin, manifestIcons)
        if (settingsIcon != null) {
            put(BottomNavItem.SETTINGS, settingsIcon)
        }
    }
}

private fun resolveBottomBarSettingsSkinIconPaths(
    activeSkin: com.android.purebilibili.core.plugin.skin.InstalledUiSkinPackage,
    manifestIcons: Map<String, String>
): BottomBarSkinIconPaths? {
    val unselectedAsset = activeSkin.manifest.assets.homeChannelIcon
        ?: manifestIcons["profile"]
        ?: manifestIcons["member"]
        ?: manifestIcons["home"]
    val selectedAsset = activeSkin.manifest.assets.homeChannelSelectedIcon
        ?: manifestIcons["profile_selected"]
        ?: manifestIcons["member_selected"]
        ?: manifestIcons["home_selected"]
    val unselectedPath = activeSkin.assetFilePath(unselectedAsset) ?: return null
    return BottomBarSkinIconPaths(
        unselected = unselectedPath,
        selected = activeSkin.assetFilePath(selectedAsset)
    )
}

private fun resolveTopTabSkinIconPaths(
    activeSkin: com.android.purebilibili.core.plugin.skin.InstalledUiSkinPackage
): Map<String, TopTabSkinIconPaths> {
    val manifestIcons = activeSkin.manifest.assets.bottomBarIcons
    val homeIcon = resolveTopTabSkinIconPaths(activeSkin, manifestIcons["home"], manifestIcons["home_selected"])
    val followingIcon = resolveTopTabSkinIconPaths(
        activeSkin,
        manifestIcons["following"],
        manifestIcons["following_selected"]
    )
    val memberIcon = resolveTopTabSkinIconPaths(activeSkin, manifestIcons["member"], manifestIcons["member_selected"])
    val profileIcon = resolveTopTabSkinIconPaths(activeSkin, manifestIcons["profile"], manifestIcons["profile_selected"])
    val channelIcon = resolveTopTabSkinIconPaths(
        activeSkin,
        activeSkin.manifest.assets.homeChannelIcon,
        activeSkin.manifest.assets.homeChannelSelectedIcon
    )
    val styleFallbackIcon = channelIcon ?: memberIcon ?: profileIcon ?: homeIcon ?: followingIcon

    return buildMap {
        homeIcon?.let { put("RECOMMEND", it) }
        followingIcon?.let { put("FOLLOW", it) }
        styleFallbackIcon?.let {
            put("POPULAR", channelIcon ?: it)
            put("LIVE", memberIcon ?: it)
            put("ANIME", profileIcon ?: it)
            put("GAME", memberIcon ?: it)
            put("KNOWLEDGE", followingIcon ?: it)
            put("TECH", channelIcon ?: it)
        }
    }
}

private fun resolveTopTabPartitionSkinIconPaths(
    activeSkin: com.android.purebilibili.core.plugin.skin.InstalledUiSkinPackage,
    topTabIconPaths: Map<String, TopTabSkinIconPaths>
): TopTabSkinIconPaths? {
    return resolveTopTabSkinIconPaths(
        activeSkin,
        activeSkin.manifest.assets.homeChannelIcon,
        activeSkin.manifest.assets.homeChannelSelectedIcon
    ) ?: topTabIconPaths["POPULAR"] ?: topTabIconPaths["RECOMMEND"]
}

private fun resolveTopTabSkinIconPaths(
    activeSkin: com.android.purebilibili.core.plugin.skin.InstalledUiSkinPackage,
    unselectedAssetPath: String?,
    selectedAssetPath: String?
): TopTabSkinIconPaths? {
    val unselectedPath = activeSkin.assetFilePath(unselectedAssetPath) ?: return null
    return TopTabSkinIconPaths(
        unselected = unselectedPath,
        selected = activeSkin.assetFilePath(selectedAssetPath)
    )
}
