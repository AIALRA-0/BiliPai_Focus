package com.android.purebilibili.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.LocalAndroidNativeVariant
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.resolveAndroidNativeChromeTokens
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar as MiuixSmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar

fun isNativeMiuixEnabled(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant
): Boolean = uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX

@Composable
fun rememberIsNativeMiuixEnabled(): Boolean {
    return isNativeMiuixEnabled(
        uiPreset = LocalUiPreset.current,
        androidNativeVariant = LocalAndroidNativeVariant.current
    )
}

enum class AdaptiveTopAppBarStyle {
    SMALL,
    CENTERED,
    LARGE
}

data class AdaptiveTopAppBarChromeSpec(
    val containerCornerRadiusDp: Int,
    val scrolledContainerAlpha: Float,
    val scrolledTonalElevationDp: Int,
    val motionScale: Float
)

val LocalGlobalWallpaperBackdropVisible = compositionLocalOf { false }

fun resolveAdaptiveTopAppBarChromeSpec(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
): AdaptiveTopAppBarChromeSpec {
    val chromeTokens = resolveAndroidNativeChromeTokens(uiPreset, androidNativeVariant)
    return when {
        uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX -> AdaptiveTopAppBarChromeSpec(
            containerCornerRadiusDp = chromeTokens.containerCornerRadiusDp,
            scrolledContainerAlpha = 1f,
            scrolledTonalElevationDp = 0,
            motionScale = chromeTokens.motionScale
        )
        uiPreset == UiPreset.MD3 -> AdaptiveTopAppBarChromeSpec(
            containerCornerRadiusDp = chromeTokens.containerCornerRadiusDp,
            scrolledContainerAlpha = 1f,
            scrolledTonalElevationDp = 0,
            motionScale = chromeTokens.motionScale
        )
        else -> AdaptiveTopAppBarChromeSpec(
            containerCornerRadiusDp = chromeTokens.containerCornerRadiusDp,
            scrolledContainerAlpha = 1f,
            scrolledTonalElevationDp = 0,
            motionScale = chromeTokens.motionScale
        )
    }
}

fun resolveAdaptiveScaffoldContainerColor(
    requestedContainerColor: Color,
    defaultBackgroundColor: Color,
    globalWallpaperVisible: Boolean
): Color {
    return if (globalWallpaperVisible && requestedContainerColor == defaultBackgroundColor) {
        Color.Transparent
    } else {
        requestedContainerColor
    }
}

fun resolveGlobalWallpaperChromeColor(
    requestedColor: Color,
    defaultBackgroundColor: Color,
    defaultSurfaceColor: Color,
    globalWallpaperVisible: Boolean
): Color {
    if (!globalWallpaperVisible || requestedColor.alpha == 0f) return requestedColor
    val requestedOpaque = requestedColor.copy(alpha = 1f)
    return if (
        requestedOpaque == defaultBackgroundColor.copy(alpha = 1f) ||
        requestedOpaque == defaultSurfaceColor.copy(alpha = 1f)
    ) {
        Color.Transparent
    } else {
        requestedColor
    }
}

@Composable
fun globalWallpaperAwareChromeColor(color: Color): Color {
    return resolveGlobalWallpaperChromeColor(
        requestedColor = color,
        defaultBackgroundColor = MaterialTheme.colorScheme.background,
        defaultSurfaceColor = MaterialTheme.colorScheme.surface,
        globalWallpaperVisible = LocalGlobalWallpaperBackdropVisible.current
    )
}

@Composable
fun Modifier.globalWallpaperAwareBackground(
    color: Color = MaterialTheme.colorScheme.background
): Modifier {
    return if (LocalGlobalWallpaperBackdropVisible.current) {
        this
    } else {
        background(color)
    }
}

@Composable
fun AdaptiveScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit
) {
    val effectiveContainerColor = resolveAdaptiveScaffoldContainerColor(
        requestedContainerColor = containerColor,
        defaultBackgroundColor = MaterialTheme.colorScheme.background,
        globalWallpaperVisible = LocalGlobalWallpaperBackdropVisible.current
    )
    if (rememberIsNativeMiuixEnabled()) {
        MiuixScaffold(
            modifier = modifier,
            topBar = topBar,
            bottomBar = bottomBar,
            floatingActionButton = floatingActionButton,
            snackbarHost = snackbarHost,
            containerColor = effectiveContainerColor,
            contentWindowInsets = contentWindowInsets,
            content = content
        )
    } else {
        Scaffold(
            modifier = modifier,
            topBar = topBar,
            bottomBar = bottomBar,
            floatingActionButton = floatingActionButton,
            snackbarHost = snackbarHost,
            containerColor = effectiveContainerColor,
            contentWindowInsets = contentWindowInsets,
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    largeTitle: String = title,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    style: AdaptiveTopAppBarStyle = AdaptiveTopAppBarStyle.SMALL,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val uiPreset = LocalUiPreset.current
    val androidNativeVariant = LocalAndroidNativeVariant.current
    val globalWallpaperVisible = LocalGlobalWallpaperBackdropVisible.current
    val chromeSpec = resolveAdaptiveTopAppBarChromeSpec(uiPreset, androidNativeVariant)
    val effectiveColors = if (globalWallpaperVisible) {
        colors.copy(
            containerColor = resolveGlobalWallpaperChromeColor(
                requestedColor = colors.containerColor,
                defaultBackgroundColor = MaterialTheme.colorScheme.background,
                defaultSurfaceColor = MaterialTheme.colorScheme.surface,
                globalWallpaperVisible = true
            ),
            scrolledContainerColor = resolveGlobalWallpaperChromeColor(
                requestedColor = colors.scrolledContainerColor,
                defaultBackgroundColor = MaterialTheme.colorScheme.background,
                defaultSurfaceColor = MaterialTheme.colorScheme.surface,
                globalWallpaperVisible = true
            )
        )
    } else {
        colors
    }
    val topAppBarColors = effectiveColors

    if (rememberIsNativeMiuixEnabled()) {
        val navigationContent =
            @Composable {
                CompositionLocalProvider(
                    LocalContentColor provides topAppBarColors.navigationIconContentColor
                ) {
                    navigationIcon()
                }
            }
        val actionsContent: @Composable RowScope.() -> Unit = {
            CompositionLocalProvider(
                LocalContentColor provides topAppBarColors.actionIconContentColor
            ) {
                actions()
            }
        }
        when (style) {
            AdaptiveTopAppBarStyle.LARGE -> {
                MiuixTopAppBar(
                    title = title,
                    largeTitle = largeTitle,
                    modifier = modifier,
                    color = topAppBarColors.containerColor,
                    navigationIcon = navigationContent,
                    actions = actionsContent
                )
            }

            AdaptiveTopAppBarStyle.SMALL,
            AdaptiveTopAppBarStyle.CENTERED -> {
                MiuixSmallTopAppBar(
                    title = title,
                    modifier = modifier,
                    color = topAppBarColors.containerColor,
                    navigationIcon = navigationContent,
                    actions = actionsContent
                )
            }
        }
        return
    }

    when (style) {
        AdaptiveTopAppBarStyle.SMALL -> {
            TopAppBar(
                modifier = modifier,
                title = { Text(title) },
                navigationIcon = navigationIcon,
                actions = actions,
                colors = topAppBarColors,
                scrollBehavior = scrollBehavior
            )
        }

        AdaptiveTopAppBarStyle.CENTERED -> {
            CenterAlignedTopAppBar(
                modifier = modifier,
                title = { Text(title) },
                navigationIcon = navigationIcon,
                actions = actions,
                colors = topAppBarColors,
                scrollBehavior = scrollBehavior
            )
        }

        AdaptiveTopAppBarStyle.LARGE -> {
            TopAppBar(
                modifier = modifier,
                title = { Text(largeTitle, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = navigationIcon,
                actions = actions,
                colors = topAppBarColors,
                scrollBehavior = scrollBehavior
            )
        }
    }
}
