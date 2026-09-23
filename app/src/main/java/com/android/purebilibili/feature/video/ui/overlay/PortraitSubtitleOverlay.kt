package com.android.purebilibili.feature.video.ui.overlay
import com.android.purebilibili.core.ui.components.AppHorizontalDivider

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppSwitch
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.ExoPlayer
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.feature.video.subtitle.SubtitleDisplayMode
import com.android.purebilibili.feature.video.subtitle.SubtitleTrackOption
import com.android.purebilibili.feature.video.subtitle.buildSubtitleTrackOptions
import com.android.purebilibili.feature.video.subtitle.isSubtitleFeatureEnabledForUser
import com.android.purebilibili.feature.video.subtitle.normalizeSubtitleDisplayMode
import com.android.purebilibili.feature.video.subtitle.normalizeSubtitleVerticalOffsetFraction
import com.android.purebilibili.feature.video.subtitle.resolveStickySubtitleText
import com.android.purebilibili.feature.video.subtitle.resolveSubtitleControlAvailability
import com.android.purebilibili.feature.video.subtitle.resolveSubtitleDisplayModeByAutoPreference
import com.android.purebilibili.feature.video.subtitle.resolveSubtitleDisplayOptions
import com.android.purebilibili.feature.video.subtitle.resolveSubtitlePositionPollingIdentity
import com.android.purebilibili.feature.video.subtitle.resolveSubtitleTextAt
import com.android.purebilibili.feature.video.subtitle.resolveSubtitleTextSizeSpec
import com.android.purebilibili.feature.video.subtitle.resolveSubtitleTrackDisplayLabel
import com.android.purebilibili.feature.video.subtitle.shouldKeepSubtitleOverlayMounted
import com.android.purebilibili.feature.video.subtitle.shouldRenderPrimarySubtitle
import com.android.purebilibili.feature.video.subtitle.shouldRenderSecondarySubtitle
import com.android.purebilibili.feature.video.subtitle.SubtitleAutoPreference
import com.android.purebilibili.feature.video.ui.section.resolveSubtitleLanguageLabel
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


/**
 * Bottom padding for portrait subtitle text relative to chrome visibility.
 * Controls strip + progress + home indicator need more clearance when chrome is up.
 */
internal fun resolvePortraitSubtitleBottomPaddingDp(
    controlsVisible: Boolean,
    commentExpansionProgress: Float = 0f
): Int {
    if (commentExpansionProgress > 0.35f) return 36
    return if (controlsVisible) 88 else 40
}

internal fun shouldShowPortraitSubtitleChip(
    featureEnabled: Boolean,
    trackAvailable: Boolean
): Boolean = featureEnabled && trackAvailable

internal fun resolvePortraitSubtitleBelongsToPage(
    success: VideoPlaybackUiState.Success?,
    pageBvid: String,
    pageCid: Long
): Boolean {
    if (success == null || pageCid <= 0L || pageBvid.isBlank()) return false
    return success.subtitleOwnerBvid == pageBvid &&
        success.subtitleOwnerCid == pageCid &&
        success.info.bvid == pageBvid &&
        success.info.cid == pageCid
}

@Composable
fun PortraitSubtitleHost(
    success: VideoPlaybackUiState.Success?,
    player: ExoPlayer,
    pageBvid: String,
    pageCid: Long,
    isCurrentPage: Boolean,
    controlsVisible: Boolean,
    commentExpansionProgress: Float,
    subtitleAutoPreference: SubtitleAutoPreference,
    isMuted: Boolean,
    onSubtitleTrackSelected: (String) -> Unit,
    showSubtitlePanel: Boolean,
    onShowSubtitlePanelChange: (Boolean) -> Unit,
    onSubtitleEnabledChange: (Boolean) -> Unit = {},
    onTrackAvailableChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val featureEnabled = isSubtitleFeatureEnabledForUser()
    val belongsToPage = resolvePortraitSubtitleBelongsToPage(
        success = success,
        pageBvid = pageBvid,
        pageCid = pageCid
    )
    if (!featureEnabled || !isCurrentPage || success == null || !belongsToPage) {
        SideEffect {
            onTrackAvailableChange(false)
            onSubtitleEnabledChange(false)
        }
        return
    }

    val primaryTrackBound = !success.subtitlePrimaryTrackKey.isNullOrBlank() ||
        !success.subtitlePrimaryLanguage.isNullOrBlank()
    val secondaryTrackBound = !success.subtitleSecondaryTrackKey.isNullOrBlank() ||
        !success.subtitleSecondaryLanguage.isNullOrBlank()
    val primaryCueAvailable = success.subtitlePrimaryCues.isNotEmpty()
    val secondaryCueAvailable = success.subtitleSecondaryCues.isNotEmpty()
    val availability = resolveSubtitleControlAvailability(
        primaryTrackBound = primaryTrackBound,
        secondaryTrackBound = secondaryTrackBound,
        primaryCueAvailable = primaryCueAvailable,
        secondaryCueAvailable = secondaryCueAvailable
    )
    SideEffect {
        onTrackAvailableChange(availability.trackAvailable)
    }
    if (!availability.trackAvailable) {
        SideEffect { onSubtitleEnabledChange(false) }
        return
    }

    val primaryLabel = success.subtitleTracks.firstOrNull {
        it.trackKey == success.subtitlePrimaryTrackKey
    }?.let(::resolveSubtitleTrackDisplayLabel)
        ?: resolveSubtitleLanguageLabel(
            languageCode = success.subtitlePrimaryLanguage,
            fallbackLabel = "中文"
        )
    val secondaryLabel = success.subtitleTracks.firstOrNull {
        it.trackKey == success.subtitleSecondaryTrackKey
    }?.let(::resolveSubtitleTrackDisplayLabel)
        ?: resolveSubtitleLanguageLabel(
            languageCode = success.subtitleSecondaryLanguage,
            fallbackLabel = "英文"
        )
    val trackOptions = buildSubtitleTrackOptions(
        tracks = success.subtitleTracks,
        selectedTrackKey = success.subtitlePrimaryTrackKey
    )

    val sessionKey = remember(
        pageBvid,
        pageCid,
        success.subtitlePrimaryLanguage,
        success.subtitleSecondaryLanguage,
        success.subtitlePrimaryLikelyAi,
        success.subtitleSecondaryLikelyAi,
        subtitleAutoPreference
    ) {
        "${pageBvid}_${pageCid}_${success.subtitlePrimaryLanguage}_${success.subtitleSecondaryLanguage}_" +
            "${success.subtitlePrimaryLikelyAi}_${success.subtitleSecondaryLikelyAi}_${subtitleAutoPreference.name}"
    }
    var displayModePreference by rememberSaveable(sessionKey) {
        mutableStateOf(
            resolveSubtitleDisplayModeByAutoPreference(
                preference = subtitleAutoPreference,
                hasPrimaryTrack = availability.primarySelectable,
                hasSecondaryTrack = availability.secondarySelectable,
                primaryTrackLikelyAi = success.subtitlePrimaryLikelyAi,
                secondaryTrackLikelyAi = success.subtitleSecondaryLikelyAi,
                isMuted = isMuted
            )
        )
    }
    var largeTextEnabled by rememberSaveable("${sessionKey}_large") {
        mutableStateOf(false)
    }

    val displayMode = normalizeSubtitleDisplayMode(
        preferredMode = displayModePreference,
        hasPrimaryTrack = primaryCueAvailable || primaryTrackBound,
        hasSecondaryTrack = secondaryCueAvailable || secondaryTrackBound
    )
    val overlayEnabled = displayMode != SubtitleDisplayMode.OFF
    SideEffect {
        onSubtitleEnabledChange(overlayEnabled)
    }
    val configuration = LocalConfiguration.current
    val textSizeSpec = remember(configuration.screenWidthDp, largeTextEnabled) {
        resolveSubtitleTextSizeSpec(
            playerWidthDp = configuration.screenWidthDp,
            largeTextEnabled = largeTextEnabled
        )
    }

    val pollingIdentity = resolveSubtitlePositionPollingIdentity(
        bvid = pageBvid,
        cid = pageCid
    )
    val positionMs by produceState(
        initialValue = player.currentPosition.coerceAtLeast(0L),
        key1 = player,
        key2 = pollingIdentity
    ) {
        value = player.currentPosition.coerceAtLeast(0L)
        while (isActive) {
            value = player.currentPosition.coerceAtLeast(0L)
            delay(if (player.isPlaying) 120L else 260L)
        }
    }

    val primaryRaw = if (shouldRenderPrimarySubtitle(displayMode)) {
        resolveSubtitleTextAt(success.subtitlePrimaryCues, positionMs)
    } else {
        null
    }
    val secondaryRaw = if (shouldRenderSecondarySubtitle(displayMode)) {
        resolveSubtitleTextAt(success.subtitleSecondaryCues, positionMs)
    } else {
        null
    }
    var stickyPrimary by remember(pollingIdentity) { mutableStateOf<String?>(null) }
    var stickySecondary by remember(pollingIdentity) { mutableStateOf<String?>(null) }
    var primaryBlankSince by remember(pollingIdentity) { mutableLongStateOf(-1L) }
    var secondaryBlankSince by remember(pollingIdentity) { mutableLongStateOf(-1L) }
    val primaryText = resolveStickySubtitleText(
        currentText = primaryRaw,
        previousText = stickyPrimary,
        blankGapMs = if (primaryRaw.isNullOrBlank() && primaryBlankSince >= 0L) {
            (positionMs - primaryBlankSince).coerceAtLeast(0L)
        } else {
            0L
        }
    )
    val secondaryText = resolveStickySubtitleText(
        currentText = secondaryRaw,
        previousText = stickySecondary,
        blankGapMs = if (secondaryRaw.isNullOrBlank() && secondaryBlankSince >= 0L) {
            (positionMs - secondaryBlankSince).coerceAtLeast(0L)
        } else {
            0L
        }
    )
    SideEffect {
        if (!primaryRaw.isNullOrBlank()) {
            stickyPrimary = primaryRaw
            primaryBlankSince = -1L
        } else if (primaryBlankSince < 0L) {
            primaryBlankSince = positionMs
        }
        if (!secondaryRaw.isNullOrBlank()) {
            stickySecondary = secondaryRaw
            secondaryBlankSince = -1L
        } else if (secondaryBlankSince < 0L) {
            secondaryBlankSince = positionMs
        }
    }

    val keepMounted = shouldKeepSubtitleOverlayMounted(
        overlayEnabled = overlayEnabled,
        isInPipMode = false,
        isAudioOnly = false,
        suppressOverlay = false
    )
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val storedPortraitOffset by SettingsManager
        .getSubtitlePortraitVerticalOffsetFraction(context)
        .collectAsStateWithLifecycle(initialValue = 0f)
    val subtitlePositionLocked by SettingsManager
        .getSubtitlePositionLocked(context)
        .collectAsStateWithLifecycle(initialValue = true)
    val bottomPaddingDp = resolvePortraitSubtitleBottomPaddingDp(
        controlsVisible = controlsVisible && !subtitlePositionLocked,
        commentExpansionProgress = commentExpansionProgress
    )
    var subtitleVerticalOffsetFraction by rememberSaveable(pageBvid) {
        mutableFloatStateOf(storedPortraitOffset)
    }
    var isDraggingSubtitleOffset by remember { mutableStateOf(false) }
    LaunchedEffect(storedPortraitOffset, pageBvid) {
        if (!isDraggingSubtitleOffset) {
            subtitleVerticalOffsetFraction = storedPortraitOffset
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (keepMounted) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset {
                        IntOffset(
                            x = 0,
                            y = with(density) {
                                (configuration.screenHeightDp * subtitleVerticalOffsetFraction)
                                    .dp
                                    .roundToPx()
                            }
                        )
                    }
                    .fillMaxWidth(0.92f)
                    .padding(horizontal = 12.dp)
                    .padding(bottom = bottomPaddingDp.dp)
                    .padding(vertical = 6.dp)
                    .then(
                        if (subtitlePositionLocked) {
                            Modifier
                        } else {
                            Modifier.pointerInput(configuration.screenHeightDp) {
                                detectDragGestures(
                                    onDragStart = { isDraggingSubtitleOffset = true },
                                    onDragEnd = {
                                        isDraggingSubtitleOffset = false
                                        scope.launch {
                                            SettingsManager.setSubtitlePortraitVerticalOffsetFraction(
                                                context,
                                                subtitleVerticalOffsetFraction
                                            )
                                        }
                                    },
                                    onDragCancel = { isDraggingSubtitleOffset = false },
                                    onDrag = { change, dragAmount ->
                                        val screenHeightPx = with(density) {
                                            configuration.screenHeightDp.dp.toPx()
                                        }.coerceAtLeast(1f)
                                        subtitleVerticalOffsetFraction =
                                            normalizeSubtitleVerticalOffsetFraction(
                                                subtitleVerticalOffsetFraction +
                                                    dragAmount.y / screenHeightPx
                                            )
                                        change.consume()
                                    }
                                )
                            }
                        }
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.85f),
                    offset = Offset(0f, 1.5f),
                    blurRadius = 6f
                )
                val showPrimary = !primaryText.isNullOrBlank()
                val showSecondary = !secondaryText.isNullOrBlank()
                val secondaryAsPrimary = showSecondary && !showPrimary
                AppText(
                    text = secondaryText.orEmpty(),
                    color = Color.White.copy(alpha = if (showSecondary) 0.88f else 0f),
                    fontSize = if (secondaryAsPrimary) {
                        textSizeSpec.primarySp.sp
                    } else {
                        textSizeSpec.secondarySp.sp
                    },
                    fontWeight = if (secondaryAsPrimary) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    style = androidx.compose.ui.text.TextStyle(shadow = shadow),
                    modifier = Modifier.then(
                        if (showSecondary) Modifier else Modifier.height(0.dp)
                    )
                )
                AppText(
                    text = primaryText.orEmpty(),
                    color = Color.White.copy(alpha = if (showPrimary) 1f else 0f),
                    fontSize = textSizeSpec.primarySp.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    style = androidx.compose.ui.text.TextStyle(shadow = shadow),
                    modifier = Modifier.then(
                        if (showPrimary) Modifier else Modifier.height(0.dp)
                    )
                )
            }
        }

        if (showSubtitlePanel) {
            PortraitSubtitlePanel(
                displayMode = displayMode,
                primaryLabel = primaryLabel,
                secondaryLabel = secondaryLabel,
                primaryAvailable = availability.primarySelectable,
                secondaryAvailable = availability.secondarySelectable,
                trackOptions = trackOptions,
                largeTextEnabled = largeTextEnabled,
                positionLocked = subtitlePositionLocked,
                canResetPosition = kotlin.math.abs(subtitleVerticalOffsetFraction) > 0.001f,
                onDismiss = { onShowSubtitlePanelChange(false) },
                onDisplayModeChange = { mode ->
                    displayModePreference = mode
                    onShowSubtitlePanelChange(false)
                },
                onTrackSelected = { trackKey ->
                    onSubtitleTrackSelected(trackKey)
                    displayModePreference = SubtitleDisplayMode.PRIMARY_ONLY
                    onShowSubtitlePanelChange(false)
                },
                onLargeTextChange = { largeTextEnabled = it },
                onPositionLockedChange = { locked ->
                    scope.launch {
                        SettingsManager.setSubtitlePositionLocked(context, locked)
                    }
                },
                onResetPosition = {
                    subtitleVerticalOffsetFraction = 0f
                    scope.launch {
                        SettingsManager.setSubtitlePortraitVerticalOffsetFraction(context, 0f)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = (bottomPaddingDp + 8).dp)
            )
        }
    }
}

@Composable
private fun PortraitSubtitlePanel(
    displayMode: SubtitleDisplayMode,
    primaryLabel: String,
    secondaryLabel: String,
    primaryAvailable: Boolean,
    secondaryAvailable: Boolean,
    trackOptions: List<SubtitleTrackOption>,
    largeTextEnabled: Boolean,
    positionLocked: Boolean,
    canResetPosition: Boolean = false,
    onDismiss: () -> Unit,
    onDisplayModeChange: (SubtitleDisplayMode) -> Unit,
    onTrackSelected: (String) -> Unit,
    onLargeTextChange: (Boolean) -> Unit,
    onPositionLockedChange: (Boolean) -> Unit,
    onResetPosition: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onDismiss)
    ) {
        AppSurface(
            modifier = modifier
                .widthIn(min = 148.dp, max = 228.dp)
                .clickable(enabled = false) {},
            shape = AppShapes.container(ContainerLevel.Dialog),
            color = Color.Black.copy(alpha = 0.82f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppText(
                    text = "字幕显示",
                    color = Color.White.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
                resolveSubtitleDisplayOptions(
                    primaryLabel = primaryLabel,
                    secondaryLabel = secondaryLabel,
                    hasPrimaryTrack = primaryAvailable,
                    hasSecondaryTrack = secondaryAvailable
                ).forEach { option ->
                    val selected = displayMode == option.mode
                    AppSurface(
                        onClick = {
                            if (option.enabled) onDisplayModeChange(option.mode)
                        },
                        enabled = option.enabled,
                        shape = AppShapes.container(ContainerLevel.Field),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        } else {
                            Color.Transparent
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AppText(
                            text = option.label,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.White.copy(alpha = if (option.enabled) 1f else 0.4f)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
                if (trackOptions.isNotEmpty()) {
                    AppHorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                    AppText(
                        text = "字幕轨道",
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
                    )
                    trackOptions.forEach { option ->
                        AppSurface(
                            onClick = { onTrackSelected(option.trackKey) },
                            shape = AppShapes.container(ContainerLevel.Field),
                            color = if (option.selected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                            } else {
                                Color.Transparent
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AppText(
                                text = option.label,
                                color = if (option.selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.White
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (option.selected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
                AppHorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    AppText(
                        text = "大字号",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    AppSwitch(
                        checked = largeTextEnabled,
                        onCheckedChange = onLargeTextChange
                    )
                }
                AppText(
                    text = if (positionLocked) "字幕位置已固定" else "上下拖动字幕可调整位置",
                    color = Color.White.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    AppText(
                        text = "锁定位置",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    AppSwitch(
                        checked = positionLocked,
                        onCheckedChange = onPositionLockedChange
                    )
                }
                if (canResetPosition) {
                    AppSurface(
                        onClick = onResetPosition,
                        shape = AppShapes.container(ContainerLevel.Field),
                        color = Color.White.copy(alpha = 0.10f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AppText(
                            text = "重置位置",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
