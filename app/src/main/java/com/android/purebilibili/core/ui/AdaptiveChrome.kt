package com.android.purebilibili.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.LocalAndroidNativeVariant
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset
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
    if (rememberIsNativeMiuixEnabled()) {
        MiuixScaffold(
            modifier = modifier,
            topBar = topBar,
            bottomBar = bottomBar,
            floatingActionButton = floatingActionButton,
            snackbarHost = snackbarHost,
            containerColor = containerColor,
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
            containerColor = containerColor,
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
    if (rememberIsNativeMiuixEnabled()) {
        val navigationContent =
            @Composable {
                CompositionLocalProvider(
                    LocalContentColor provides colors.navigationIconContentColor
                ) {
                    navigationIcon()
                }
            }
        val actionsContent: @Composable RowScope.() -> Unit = {
            CompositionLocalProvider(
                LocalContentColor provides colors.actionIconContentColor
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
                    color = colors.containerColor,
                    navigationIcon = navigationContent,
                    actions = actionsContent
                )
            }

            AdaptiveTopAppBarStyle.SMALL,
            AdaptiveTopAppBarStyle.CENTERED -> {
                MiuixSmallTopAppBar(
                    title = title,
                    modifier = modifier,
                    color = colors.containerColor,
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
                colors = colors,
                scrollBehavior = scrollBehavior
            )
        }

        AdaptiveTopAppBarStyle.CENTERED -> {
            CenterAlignedTopAppBar(
                modifier = modifier,
                title = { Text(title) },
                navigationIcon = navigationIcon,
                actions = actions,
                colors = colors,
                scrollBehavior = scrollBehavior
            )
        }

        AdaptiveTopAppBarStyle.LARGE -> {
            LargeTopAppBar(
                modifier = modifier,
                title = { Text(largeTitle) },
                navigationIcon = navigationIcon,
                actions = actions,
                colors = colors,
                scrollBehavior = scrollBehavior
            )
        }
    }
}
