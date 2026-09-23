package com.android.purebilibili.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import com.android.purebilibili.core.util.LocalWindowSizeClass
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import com.android.purebilibili.core.ui.components.AppIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.feature.home.components.BiliPaiImmersiveTopBar
import com.android.purebilibili.feature.home.components.shouldUseBiliPaiProgressiveTopBlur
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.core.ui.blur.BlurSurfaceType
import com.android.purebilibili.core.ui.blur.hazeSourceCompat
import com.android.purebilibili.core.ui.blur.rememberRecoverableHazeState
import com.android.purebilibili.core.ui.blur.recoverableBlurEnabled
import com.android.purebilibili.core.ui.blur.shouldAllowRenderEffectBackedHazeEffect
import com.android.purebilibili.core.ui.blur.unifiedBlur
import com.android.purebilibili.core.ui.performance.isLowBlurBudgetForced
import com.android.purebilibili.core.ui.AppScaffold
import com.android.purebilibili.core.ui.AppTopBar
import com.android.purebilibili.core.ui.AppTopBarStyle
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.LocalBottomBarContentPadding
import com.android.purebilibili.core.ui.LocalSetBottomBarVisible
import com.android.purebilibili.core.ui.appTopBarNestedScroll
import com.android.purebilibili.core.ui.isMiuixNonGlassEnabled
import com.android.purebilibili.core.ui.rememberAppTopBarCollapseBehavior
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppPreferenceIconTreatment
import com.android.purebilibili.core.ui.components.AppPreferenceGroupPresentation
import com.android.purebilibili.core.ui.components.LocalAppPreferenceIconTreatment
import com.android.purebilibili.core.ui.components.LocalAppPreferenceGroupPresentation
import com.android.purebilibili.feature.settings.SettingsBottomBarScrollState
import com.android.purebilibili.feature.settings.SettingsBottomBarScrollTracker
import com.android.purebilibili.feature.settings.SettingsPageScrollHost
import com.android.purebilibili.feature.settings.reduceSettingsBottomBarScroll
import kotlinx.coroutines.flow.distinctUntilChanged

internal val LocalSettingsTopContentPadding = staticCompositionLocalOf { 0.dp }

@Composable
internal fun settingsScrollContentPadding(
    extraTop: androidx.compose.ui.unit.Dp = 0.dp,
    extraBottom: androidx.compose.ui.unit.Dp = 0.dp,
    extraHorizontal: androidx.compose.ui.unit.Dp = 0.dp,
    extraVertical: androidx.compose.ui.unit.Dp = 0.dp,
): PaddingValues = PaddingValues(
    start = extraHorizontal,
    end = extraHorizontal,
    top = LocalSettingsTopContentPadding.current + extraTop + extraVertical,
    bottom = extraBottom + extraVertical,
)

@Composable
internal fun SettingsBottomBarScrollEffect(listState: LazyListState) {
    val setBottomBarVisible = LocalSetBottomBarVisible.current
    val density = LocalDensity.current
    val topRevealThresholdPx = with(density) { 24.dp.roundToPx() }
    val directionThresholdPx = with(density) { 32.dp.roundToPx() }

    LaunchedEffect(
        listState,
        setBottomBarVisible,
        topRevealThresholdPx,
        directionThresholdPx,
    ) {
        var tracker = SettingsBottomBarScrollTracker(
            previousState = SettingsBottomBarScrollState(
                firstVisibleItemIndex = listState.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
            ),
        )
        snapshotFlow {
            SettingsBottomBarScrollState(
                firstVisibleItemIndex = listState.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
            )
        }
            .distinctUntilChanged()
            .collect { currentState ->
                val update = reduceSettingsBottomBarScroll(
                    tracker = tracker,
                    currentState = currentState,
                    topRevealThresholdPx = topRevealThresholdPx,
                    directionThresholdPx = directionThresholdPx,
                )
                update.bottomBarVisible?.let(setBottomBarVisible)
                tracker = update.tracker
            }
    }

    DisposableEffect(setBottomBarVisible) {
        onDispose { setBottomBarVisible(true) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsPageScaffold(
    title: String,
    onBack: () -> Unit,
    backContentDescription: String,
    bottomContentPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    scrollHost: SettingsPageScrollHost = SettingsPageScrollHost.LazyColumn,
    topBarBlurEnabled: Boolean? = null,
    externalContentHandlesTopPadding: Boolean = false,
    topBarStyle: AppTopBarStyle = AppTopBarStyle.CENTERED,
    actions: @Composable RowScope.() -> Unit = {},
    header: (@Composable () -> Unit)? = null,
    lazyListContent: (LazyListScope.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    if (scrollHost == SettingsPageScrollHost.LazyColumn) {
        SettingsBottomBarScrollEffect(listState)
    }
    val resolvedBottomContentPadding = maxOf(
        bottomContentPadding,
        LocalBottomBarContentPadding.current,
    )
    val appThemeConfig = LocalAppThemeConfig.current
    val headerBlurEnabled = topBarBlurEnabled ?: appThemeConfig.headerBlurEnabled
    val lowBlurBudget = isLowBlurBudgetForced()
    val nonGlassMiuix = isMiuixNonGlassEnabled()
    val collapseBehavior = if (
        nonGlassMiuix &&
        topBarStyle == AppTopBarStyle.LARGE &&
        scrollHost == SettingsPageScrollHost.LazyColumn
    ) {
        rememberAppTopBarCollapseBehavior()
    } else {
        null
    }
    val progressiveBlurEnabled = shouldUseBiliPaiProgressiveTopBlur(
        enabled = appThemeConfig.progressiveTopBlurEnabled && !headerBlurEnabled,
        hasBackdrop = true,
    ) && !lowBlurBudget
    val fadeActive = appThemeConfig.progressiveTopFadeEnabled && !headerBlurEnabled
    val backdrop = if (progressiveBlurEnabled) rememberLayerBackdrop() else null
    val hazeState = if (
        headerBlurEnabled && !lowBlurBudget &&
        shouldAllowRenderEffectBackedHazeEffect(android.os.Build.VERSION.SDK_INT)
    ) rememberRecoverableHazeState().takeIf { recoverableBlurEnabled(it) } else null
    val hazeReady = hazeState != null
    val topBarBlurActive = progressiveBlurEnabled || hazeReady || fadeActive
    val pageContainerColor = when (LocalAppUiStyle.current) {
        // Miuix page canvas is `background` / chromeBackground so the top bar, split
        // pane, and list share one tone. Cards stay on surfaceContainer.
        AppUiStyle.MIUIX -> AppSurfaceTokens.chromeBackground()
        AppUiStyle.MATERIAL3 -> AppSurfaceTokens.groupedListContainer()
    }

    CompositionLocalProvider(
        LocalAppPreferenceIconTreatment provides AppPreferenceIconTreatment.FILLED,
        LocalAppPreferenceGroupPresentation provides if (nonGlassMiuix) {
            AppPreferenceGroupPresentation.CARD
        } else {
            AppPreferenceGroupPresentation.FLAT
        },
    ) {
        AppScaffold(
            modifier = modifier.appTopBarNestedScroll(collapseBehavior),
            topBar = {
                BiliPaiImmersiveTopBar(
                    backdrop = backdrop,
                    enabled = progressiveBlurEnabled,
                    headerBlurActive = hazeReady,
                    surfaceColor = pageContainerColor,
                    fadeEnabled = fadeActive,
                ) {
                    AppTopBar(
                        title = title,
                        modifier = if (hazeState != null) {
                            Modifier
                                .unifiedBlur(
                                    hazeState = hazeState,
                                    surfaceType = BlurSurfaceType.HEADER,
                                )
                                .background(
                                    pageContainerColor
                                        .copy(alpha = AppSurfaceTokens.FrostedScrimAlpha)
                                )
                        } else Modifier,
                        navigationIcon = {
                            AppIconButton(onClick = onBack) {
                                AppIcon(
                                    imageVector = rememberAppBackIcon(),
                                    contentDescription = backContentDescription,
                                )
                            }
                        },
                        actions = actions,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = if (!topBarBlurActive || nonGlassMiuix) {
                                pageContainerColor
                            } else {
                                Color.Transparent
                            },
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        style = topBarStyle,
                        collapseBehavior = collapseBehavior,
                    )
                }
            },
            containerColor = pageContainerColor,
            contentWindowInsets = WindowInsets(0.dp),
        ) { padding ->
            val scrollModifier = Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .then(if (hazeState != null) Modifier.hazeSourceCompat(hazeState) else Modifier)
                .background(pageContainerColor)

            val windowSizeClass = LocalWindowSizeClass.current
            val isExpandedScreen = windowSizeClass.isExpandedScreen

            when (scrollHost) {
                SettingsPageScrollHost.LazyColumn -> {
                    Box(
                        modifier = scrollModifier,
                        contentAlignment = Alignment.TopCenter
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxHeight()
                                .then(
                                    if (isExpandedScreen) {
                                        Modifier.widthIn(max = 760.dp).fillMaxWidth()
                                    } else {
                                        Modifier.fillMaxWidth()
                                    }
                                ),
                            contentPadding = PaddingValues(
                                top = padding.calculateTopPadding(),
                                bottom = maxOf(resolvedBottomContentPadding, padding.calculateBottomPadding()),
                            ),
                        ) {
                            if (header != null) {
                                item {
                                    header()
                                }
                            }
                            if (lazyListContent != null) {
                                lazyListContent()
                            } else {
                                item {
                                    content()
                                }
                            }
                        }
                    }
                }

                SettingsPageScrollHost.External -> {
                    val chromeTop = padding.calculateTopPadding()
                    CompositionLocalProvider(
                        LocalSettingsTopContentPadding provides if (header != null) {
                            0.dp
                        } else {
                            chromeTop
                        },
                    ) {
                        Box(
                            modifier = scrollModifier,
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .then(
                                        if (isExpandedScreen) {
                                            Modifier.widthIn(max = 760.dp).fillMaxWidth()
                                        } else {
                                            Modifier.fillMaxWidth()
                                        }
                                    )
                            ) {
                                if (header != null) {
                                    Box(modifier = Modifier.padding(top = chromeTop)) {
                                        header()
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f, fill = true)
                                        .fillMaxSize(),
                                ) {
                                    content()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
