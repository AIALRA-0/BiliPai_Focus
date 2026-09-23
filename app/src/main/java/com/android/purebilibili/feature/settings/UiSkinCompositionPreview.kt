package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.OpticalContrastPalette

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.purebilibili.core.plugin.skin.UiSkinAssets
import com.android.purebilibili.core.plugin.skin.UiSkinColorTokens
import com.android.purebilibili.core.plugin.skin.UiSkinManifest
import java.io.File

/**
 * 真实合成预览的数据模型。从已安装皮肤或临时预览资源构造，不可变。
 *
 * [assetFiles] 的 key 是声明的 assetPath（如 "assets/tail_bg.png"），value 是本地文件绝对路径。
 * 这与 [com.android.purebilibili.core.plugin.skin.InstalledUiSkinPackage.assetFiles] 同构，
 * 因此已安装皮肤与在线未下载主题可复用同一预览组件。
 */
data class UiSkinCompositionPreviewData(
    val displayName: String,
    val manifest: UiSkinManifest,
    val assetFiles: Map<String, String>,
    val darkMode: Boolean
) {
    fun assetPath(assetPath: String?): String? {
        return assetPath?.let(assetFiles::get)
    }
}

/**
 * 纯布局策略：从 manifest 的 assets/colors 与资源文件映射，解析出合成预览所需的分层输入。
 * 抽成顶层函数便于在无 Compose 环境下做单元测试（验证缺图降级、图标映射、尺寸）。
 */
data class UiSkinCompositionLayers(
    val bottomBarTrimImagePath: String?,
    val drawerBottomTrimImagePath: String?,
    val topAtmosphereImagePath: String?,
    val topTabBackgroundImagePath: String?,
    val profileBackgroundImagePath: String?,
    val profileSquaredBackgroundImagePath: String?,
    val profileVideoBackgroundPath: String?,
    val sideBackgroundImagePath: String?,
    val publishIconImagePath: String?,
    val bottomBarIconPaths: Map<String, String>,
    val bottomBarTrimTint: Color,
    val topAtmosphereTint: Color,
    val searchCapsuleTint: Color,
    val hasBottomBarIcons: Boolean,
    val hasTopAtmosphere: Boolean
)

fun resolveUiSkinCompositionLayers(data: UiSkinCompositionPreviewData): UiSkinCompositionLayers {
    val assets = data.manifest.assets
    val colors = data.manifest.colors
    return UiSkinCompositionLayers(
        bottomBarTrimImagePath = data.assetPath(assets.bottomBarTrim),
        drawerBottomTrimImagePath = data.assetPath(assets.drawerBottomTrim),
        topAtmosphereImagePath = data.assetPath(assets.topAtmosphere),
        topTabBackgroundImagePath = data.assetPath(assets.homeTopTabBackground),
        profileBackgroundImagePath = data.assetPath(assets.homeProfileBackground),
        profileSquaredBackgroundImagePath = data.assetPath(assets.homeProfileSquaredBackground),
        profileVideoBackgroundPath = data.assetPath(assets.homeProfileVideoBackground),
        sideBackgroundImagePath = data.assetPath(assets.homeSideBackground),
        publishIconImagePath = data.assetPath(assets.dynamicPublishIcon),
        bottomBarIconPaths = assets.bottomBarIcons.mapValues { (key, path) ->
            data.assetPath(path) ?: path
        }.filterValues { it.isNotBlank() },
        bottomBarTrimTint = parsePreviewColor(colors.bottomBarTrimTint, defaultTrimTint(data.darkMode)),
        topAtmosphereTint = parsePreviewColor(colors.topAtmosphereTint, defaultAtmosphereTint(data.darkMode)),
        searchCapsuleTint = parsePreviewColor(colors.searchCapsuleTint, OpticalContrastPalette.Highlight),
        hasBottomBarIcons = assets.bottomBarIcons.isNotEmpty(),
        hasTopAtmosphere = assets.topAtmosphere != null || assets.homeTopTabBackground != null
    )
}

private fun defaultTrimTint(dark: Boolean): Color =
    if (dark) Color(0xFF1B2A4A) else Color(0xFFEAF8FF)

private fun defaultAtmosphereTint(dark: Boolean): Color =
    if (dark) Color(0xFF13203A) else Color(0xFFDFF5FF)

fun parsePreviewColor(value: String?, fallback: Color): Color {
    val normalized = value?.trim()?.removePrefix("#")?.takeIf { it.length == 6 || it.length == 8 }
        ?: return fallback
    val argb = if (normalized.length == 6) "FF$normalized" else normalized
    return runCatching { Color(argb.toLong(16)) }.getOrDefault(fallback)
}

/**
 * 真实合成预览组件：按真实尺寸渲染迷你底栏 dock + 皮肤图标 + 背景叠加，
 * 让用户在导入前看到「启用后底栏实际长什么样」，而非资源网格。
 *
 * 尺寸复用 [com.android.purebilibili.feature.home.components] 的 token：
 * dock 高 64dp、图标 32dp，与生产底栏一致，确保预览所见即所得。
 */
@Composable
fun UiSkinCompositionPreview(
    data: UiSkinCompositionPreviewData,
    modifier: Modifier = Modifier,
    showLiquidGlass: Boolean = true
) {
    val layers = resolveUiSkinCompositionLayers(data)
    val dockShape: Shape = RoundedCornerShape(28.dp)
    val dockHeight = previewDockHeight()
    val iconSize = previewDockIconSize()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.container(ContainerLevel.Card))
            .background(if (data.darkMode) Color(0xFF0E1422) else Color(0xFFF2F4F8)),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // 顶部氛围区 + 个人页背景预览
        PreviewTopAtmosphere(
            layers = layers,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )
        PreviewExtendedSurfaces(layers = layers)
        // 真实尺寸迷你底栏 dock
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            PreviewBottomBarDock(
                layers = layers,
                dockShape = dockShape,
                dockHeight = dockHeight,
                iconSize = iconSize,
                showLiquidGlass = showLiquidGlass,
                darkMode = data.darkMode
            )
        }
    }
}

@Composable
private fun PreviewExtendedSurfaces(
    layers: UiSkinCompositionLayers,
) {
    val hasDrawer = !layers.sideBackgroundImagePath.isNullOrBlank() ||
        !layers.drawerBottomTrimImagePath.isNullOrBlank()
    val profilePath = layers.profileBackgroundImagePath ?: layers.profileSquaredBackgroundImagePath
    val hasProfile = !profilePath.isNullOrBlank() || !layers.profileVideoBackgroundPath.isNullOrBlank()
    val hasPublish = !layers.publishIconImagePath.isNullOrBlank()
    if (!hasDrawer && !hasProfile && !hasPublish) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasDrawer) {
            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 48.dp)
                    .clip(AppShapes.container(ContainerLevel.Chip))
                    .background(layers.topAtmosphereTint.copy(alpha = 0.28f))
            ) {
                layers.sideBackgroundImagePath?.let { path ->
                    AsyncImage(
                        model = File(path),
                        contentDescription = "侧栏背景",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                layers.drawerBottomTrimImagePath?.let { path ->
                    AsyncImage(
                        model = File(path),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        alignment = Alignment.BottomCenter,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        if (hasProfile) {
            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 48.dp)
                    .clip(AppShapes.container(ContainerLevel.Chip))
                    .background(layers.topAtmosphereTint.copy(alpha = 0.42f))
            ) {
                profilePath?.let { path ->
                    AsyncImage(
                        model = File(path),
                        contentDescription = "个人页背景",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        layers.publishIconImagePath?.let { path ->
            AsyncImage(
                model = File(path),
                contentDescription = "发布图标",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

/** 迷你 dock 高度，与生产 resolveBottomBarSkinDockHeight() 对齐（48+16=64dp）。 */
fun previewDockHeight(): Dp = AppSpacingTokens.TripleExtraLarge + AppSpacingTokens.Large

/** 迷你 dock 图标尺寸，与生产 resolveBottomBarSkinDockIconSize() 对齐（32dp）。 */
fun previewDockIconSize(): Dp = AppSpacingTokens.DoubleExtraLarge

@Composable
private fun PreviewTopAtmosphere(
    layers: UiSkinCompositionLayers,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // 顶部氛围背景图
        val atmospherePath = layers.topAtmosphereImagePath ?: layers.topTabBackgroundImagePath
        if (!atmospherePath.isNullOrBlank()) {
            AsyncImage(
                model = File(atmospherePath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // 无图时用 tint 渐变兜底
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                layers.topAtmosphereTint,
                                layers.topAtmosphereTint.copy(alpha = 0.4f)
                            )
                        )
                    )
            )
        }
        // 右下角个人页背景小图预览（若有）
        val profilePath = layers.profileBackgroundImagePath
            ?: layers.profileSquaredBackgroundImagePath
        if (!profilePath.isNullOrBlank()) {
            AsyncImage(
                model = File(profilePath),
                contentDescription = "个人页背景",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(width = 72.dp, height = 48.dp)
                    .clip(AppShapes.container(ContainerLevel.Chip))
            )
        }
    }
}

@Composable
private fun PreviewBottomBarDock(
    layers: UiSkinCompositionLayers,
    dockShape: Shape,
    dockHeight: Dp,
    iconSize: Dp,
    showLiquidGlass: Boolean,
    darkMode: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(dockHeight)
            .clip(dockShape)
            .background(if (darkMode) Color(0xFF11192B) else Color(0xCCFFFFFF))
    ) {
        // 皮肤饰面 trim（在玻璃层背后，复刻生产架构）
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    val trimHeight = size.height * 0.36f
                    val top = size.height - trimHeight
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                layers.bottomBarTrimTint.copy(alpha = 0.08f),
                                layers.bottomBarTrimTint.copy(alpha = 0.28f)
                            ),
                            startY = top,
                            endY = size.height
                        ),
                        topLeft = Offset(0f, top),
                        size = Size(size.width, trimHeight)
                    )
                }
        ) {
            val trimPath = layers.bottomBarTrimImagePath
            if (!trimPath.isNullOrBlank()) {
                AsyncImage(
                    model = File(trimPath),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0.82f)
                )
            }
        }
        // 液态玻璃半透明层（开启时叠在 trim 之上、图标之下）
        if (showLiquidGlass) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        if (darkMode) Color(0x22FFFFFF) else Color(0x33FFFFFF)
                    )
                    .alpha(0.5f)
            )
        }
        // 底栏图标行（真实尺寸 Fit，复刻 BottomBarSkinIcon 行为）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconKeys = listOf("home", "following", "member", "channel", "profile")
            iconKeys.forEach { key ->
                val path = layers.bottomBarIconPaths[key]
                Box(modifier = Modifier.size(iconSize)) {
                    if (!path.isNullOrBlank()) {
                        AsyncImage(
                            model = File(path),
                            contentDescription = key,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // 缺图占位：用 tint 色块示意
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(AppShapes.container(ContainerLevel.Chip))
                                .background(layers.bottomBarTrimTint.copy(alpha = 0.3f))
                        )
                    }
                }
            }
        }
    }
}
