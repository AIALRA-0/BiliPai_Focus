package com.android.purebilibili.feature.home.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.motion.iosMorphTween
import com.android.purebilibili.core.ui.motion.rememberSystemReduceMotion
import com.android.purebilibili.feature.home.LocalHomeScrollOffset
import kotlinx.coroutines.flow.collect
import dev.chrisbanes.haze.HazeState
import top.yukonga.miuix.kmp.blur.Backdrop

private const val LINKED_DOCK_MERGE_DURATION_MILLIS = 280
private const val LINKED_DOCK_SEARCH_DURATION_MILLIS = 240

typealias LinkedDockNowPlayingSlot = @Composable (
    Modifier,
    () -> Float,
    () -> Float,
    () -> Float,
    (() -> Unit)?,
    Boolean,
) -> Unit

@Composable
internal fun LinkedBottomDock(
    currentItem: BottomNavItem,
    firstItem: BottomNavItem,
    firstLabel: String,
    searchEnabled: Boolean,
    isFeedScrollInProgress: Boolean,
    collapseRequested: Boolean,
    onSearchClick: () -> Unit,
    onSearchKeywordSubmit: (String) -> Unit,
    containerColor: Color,
    backdrop: Backdrop?,
    glassEnabled: Boolean,
    liquidGlassTuning: LiquidGlassTuning,
    iconStyle: SharedFloatingBottomBarIconStyle,
    navigationItemCount: Int,
    navigationLabelMode: Int,
    navigationMinEdgePadding: androidx.compose.ui.unit.Dp,
    nowPlayingContent: LinkedDockNowPlayingSlot?,
    dockPhase: LinkedDockPhase? = null,
    onDockPhaseChange: ((LinkedDockPhase) -> Unit)? = null,
    isTopLevelDestination: Boolean = true,
    modifier: Modifier = Modifier,
    blurEnabled: Boolean = false,
    hazeState: HazeState? = null,
    navigationContent: @Composable () -> Unit,
) {
    val hasAudio = nowPlayingContent != null
    var internalPhase by remember(currentItem, searchEnabled, hasAudio) {
        mutableStateOf(
            resolveLinkedDockInitialPhase(
                currentItem = currentItem,
                collapseRequested = collapseRequested,
                hasAudio = hasAudio,
                savedPhase = dockPhase,
            )
        )
    }
    val phase = dockPhase ?: internalPhase
    val updatePhase: (LinkedDockPhase) -> Unit = { newPhase ->
        if (dockPhase != null && onDockPhaseChange != null) {
            onDockPhaseChange(newPhase)
        } else {
            internalPhase = newPhase
        }
    }
    LaunchedEffect(hasAudio, dockPhase) {
        if (dockPhase == null && !hasAudio && internalPhase == LinkedDockPhase.Playback) {
            internalPhase = LinkedDockPhase.Expanded
        }
    }
    var query by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scroll = LocalHomeScrollOffset.current
    val currentPhase by rememberUpdatedState(phase)
    val scrolling by rememberUpdatedState(isFeedScrollInProgress)
    val threshold = with(LocalDensity.current) { 24.dp.toPx() }
    LaunchedEffect(currentItem, hasAudio, scroll, threshold, isTopLevelDestination) {
        if (currentItem != BottomNavItem.HOME) return@LaunchedEffect
        var previous = scroll.floatValue
        var accumulated = 0f
        snapshotFlow { scroll.floatValue to scrolling }.collect { (offset, active) ->
            val delta = offset - previous
            previous = offset
            if (!active || !isTopLevelDestination || currentPhase == LinkedDockPhase.Search) {
                accumulated = 0f
            } else {
                accumulated = accumulateDockScroll(accumulated, delta)
                if ((offset <= 0f && delta < 0f) || accumulated <= -threshold) {
                    updatePhase(LinkedDockPhase.Expanded)
                    accumulated = 0f
                } else if (hasAudio && accumulated >= threshold) {
                    updatePhase(LinkedDockPhase.Playback)
                    accumulated = 0f
                }
            }
        }
    }
    // Keep the dock phase while a child destination covers the current tab. Keying this effect
    // by isTopLevelDestination made the returning page re-expand/re-collapse the playback strip,
    // which also shifted the predictive-back target after the gesture had started.
    LaunchedEffect(currentItem, collapseRequested, hasAudio) {
        if (isTopLevelDestination && currentItem != BottomNavItem.HOME && currentPhase != LinkedDockPhase.Search) {
            updatePhase(resolveLinkedDockRestingPhase(collapseRequested, hasAudio))
        }
    }
    fun expand() {
        focusManager.clearFocus()
        keyboardController?.hide()
        updatePhase(LinkedDockPhase.Expanded)
    }
    val backEnabled = shouldEnableLinkedDockBackHandler(
        phase = phase,
        isTopLevelDestination = isTopLevelDestination,
    )
    BackHandler(enabled = backEnabled) {
        focusManager.clearFocus()
        keyboardController?.hide()
        updatePhase(resolveLinkedDockPhaseOnSearchDismiss(hasAudio))
    }
    val reduceMotion = rememberSystemReduceMotion()
    val transition = updateTransition(targetState = phase, label = "linkedBottomDock")
    val merge = transition.animateFloat(
        transitionSpec = {
            if (reduceMotion) snap() else iosMorphTween(LINKED_DOCK_MERGE_DURATION_MILLIS)
        },
        label = "dockMerge",
    ) { if (it == LinkedDockPhase.Expanded) 0f else 1f }
    val search = transition.animateFloat(
        transitionSpec = {
            if (reduceMotion) snap() else iosMorphTween(LINKED_DOCK_SEARCH_DURATION_MILLIS)
        },
        label = "dockSearch",
    ) { if (it == LinkedDockPhase.Search) 1f else 0f }
    val imeSettled = WindowInsets.ime
        .getBottom(LocalDensity.current) == 0
    val nowPlayingLayoutStable = !transition.isRunning && imeSettled
    val mergeProgressProvider = remember(merge) {
        { merge.value.coerceIn(0f, 1f) }
    }
    val searchProgressProvider = remember(search) {
        { search.value.coerceIn(0f, 1f) }
    }
    val zeroProgressProvider = remember { { 0f } }
    val identityIconScaleProvider = remember { { 1f } }
    val shape = resolveSharedBottomBarCapsuleShape()
    val contentColor = MaterialTheme.colorScheme.onSurface
    val accentColor = MaterialTheme.colorScheme.primary
    // A single audio child is measured and moved between rows. Playback and artwork stay mounted.
    Layout(
        modifier = modifier.fillMaxWidth().imePadding().navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        content = {
            Box(Modifier.graphicsLayer { alpha = (1f - merge.value * 3f).coerceIn(0f, 1f) }
                .pointerInput(phase) {
                    if (phase != LinkedDockPhase.Expanded) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                            }
                        }
                    }
                }
                .then(if (phase != LinkedDockPhase.Expanded) Modifier.clearAndSetSemantics {} else Modifier)) {
                navigationContent()
            }
            Box(Modifier.graphicsLayer {
                alpha = (merge.value * 2f).coerceIn(0f, 1f)
            }
                .then(if (phase != LinkedDockPhase.Expanded) Modifier.clickable(role = Role.Button) { expand() }
                    else Modifier.clearAndSetSemantics {}), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .biliPaiFloatingDockShell(
                            backdrop = backdrop,
                            containerColor = containerColor,
                            pressProgress = 0f,
                            shape = shape,
                            enabled = glassEnabled,
                            blurEnabled = blurEnabled,
                            hazeState = hazeState,
                            liquidGlassTuning = liquidGlassTuning,
                        )
                )
                AppIcon(
                    imageVector = if (iconStyle == SharedFloatingBottomBarIconStyle.MIUIX) {
                        resolveHomeNavigationBarIcon(firstItem, currentItem == firstItem)
                    } else resolveMaterialBottomBarIcon(firstItem, currentItem == firstItem),
                    contentDescription = "$firstLabel，展开底栏",
                    tint = accentColor,
                )
            }
            Box {
                nowPlayingContent?.invoke(
                    Modifier.fillMaxSize(),
                    mergeProgressProvider,
                    searchProgressProvider,
                    zeroProgressProvider,
                    if (shouldExpandPlaybackFromSearch(phase, hasAudio)) {
                        {
                            if (!transition.isRunning) {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                updatePhase(LinkedDockPhase.Playback)
                            }
                        }
                    } else {
                        null
                    },
                    nowPlayingLayoutStable,
                )
            }
            Box(contentAlignment = Alignment.Center) {
                if (searchEnabled) {
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .biliPaiFloatingDockShell(
                                    backdrop = backdrop,
                                    containerColor = containerColor,
                                    pressProgress = 0f,
                                    shape = shape,
                                    enabled = glassEnabled,
                                    blurEnabled = blurEnabled,
                                    hazeState = hazeState,
                                    liquidGlassTuning = liquidGlassTuning,
                                )
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(shape)
                                .then(
                                    if (phase != LinkedDockPhase.Search) Modifier.clickable(role = Role.Button) {
                                        updatePhase(LinkedDockPhase.Search)
                                    } else Modifier
                                )
                        ) {
                            BiliPaiBottomBarSearchVisualContent(
                                expanded = phase == LinkedDockPhase.Search,
                                query = query,
                                onQueryChange = { query = it },
                                onSubmit = {
                                    focusManager.clearFocus()
                                    if (query.isBlank()) onSearchClick() else onSearchKeywordSubmit(query.trim())
                                },
                                contentColor = contentColor,
                                accentColor = accentColor,
                                iconScale = identityIconScaleProvider,
                                fieldAlpha = searchProgressProvider,
                                interactive = true,
                                iconStyle = iconStyle,
                            )
                        }
                    }
                }
            }
        },
    ) { children, constraints ->
        val maximumWidth = constraints.maxWidth.coerceAtMost(600.dp.roundToPx())
        val button = 56.dp.roundToPx()
        val barHeight = 64.dp.roundToPx()
        val controlHeight = 56.dp.roundToPx()
        // Keep the compact search surface circular; its width starts at [button].
        val searchHeight = button
        val gap = 8.dp.roundToPx()
        val progress = merge.value.coerceIn(0f, 1f)
        val preferredNavigationWidth = resolveBiliPaiFloatingBottomBarWidth(
            containerWidth = maximumWidth.toDp(),
            itemCount = navigationItemCount,
            minEdgePadding = navigationMinEdgePadding,
            labelMode = navigationLabelMode,
            cornerRadius = 32.dp,
        ).roundToPx()
        val reservedSearchWidth = if (searchEnabled) button + gap else 0
        val expandedNavigationWidth = preferredNavigationWidth.coerceAtMost(
            (maximumWidth - reservedSearchWidth).coerceAtLeast(0)
        )
        val geometry = resolveLinkedDockGeometry(
            width = maximumWidth,
            button = button,
            barHeight = barHeight,
            gap = gap,
            hasAudio = hasAudio,
            searchEnabled = searchEnabled,
            mergeProgress = progress,
            searchProgress = search.value,
            verticalGap = 4.dp.roundToPx(),
        )
        val top = geometry.top
        val searchWidth = geometry.searchWidth
        val audioWidth = geometry.audioWidth
        val navWidth = expandedNavigationWidth.coerceAtMost(maximumWidth)
        val navigationX = resolveLinkedDockNavigationX(
            maximumWidth = maximumWidth,
            navigationWidth = navWidth,
            button = button,
            gap = gap,
            searchEnabled = searchEnabled,
        )
        val searchX = resolveLinkedDockSearchX(
            maximumWidth = maximumWidth,
            navigationWidth = navWidth,
            searchWidth = searchWidth,
            button = button,
            gap = gap,
            mergeProgress = progress,
            searchProgress = search.value,
        )
        val nav = children[0].measure(Constraints.fixed(navWidth, barHeight))
        val first = children[1].measure(Constraints.fixed(button, controlHeight))
        val audio = children[2].measure(
            Constraints.fixed(if (hasAudio) audioWidth else 0, if (hasAudio) controlHeight else 0)
        )
        val searchBox = children[3].measure(Constraints.fixed(searchWidth, searchHeight))
        layout(constraints.maxWidth, geometry.height) {
            val left = (constraints.maxWidth - maximumWidth) / 2
            if (progress < 0.999f) nav.placeRelative(left + navigationX, top)
            if (progress > 0.001f) first.placeRelative(left, top + (barHeight - controlHeight) / 2)
            if (hasAudio) {
                audio.placeRelative(
                    left + geometry.audioX,
                    geometry.audioY + (barHeight - controlHeight) / 2,
                )
            }
            searchBox.placeRelative(
                left + searchX,
                top + (barHeight - searchHeight) / 2,
            )
        }
    }
}
