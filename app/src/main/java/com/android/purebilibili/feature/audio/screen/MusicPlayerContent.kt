package com.android.purebilibili.feature.audio.screen

import top.yukonga.miuix.kmp.window.WindowListPopup
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider

import com.android.purebilibili.navigation.animatePagerSelection

import coil3.request.allowHardware

import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.AdaptiveLoadingIndicator
import com.android.purebilibili.core.ui.components.AppCircularProgressIndicator
import com.android.purebilibili.core.ui.motion.AppMotionTokens
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.theme.calculateContrastRatio
import com.android.purebilibili.core.ui.components.AppFilledIconButton
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppIconButtonDefaults
import com.android.purebilibili.core.ui.components.AppLinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.AppModalBottomSheet
import com.android.purebilibili.core.ui.components.AppOutlinedTextField
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.android.purebilibili.core.lifecycle.BackgroundManager
import com.android.purebilibili.core.util.AppHingeOrientation
import com.android.purebilibili.core.util.LocalAppWindowAdaptiveInfo
import com.android.purebilibili.feature.audio.lyrics.BiliSubtitleLyricsPolicy
import com.android.purebilibili.feature.audio.lyrics.LyricDocument
import com.android.purebilibili.feature.audio.lyrics.LyricLine
import com.android.purebilibili.feature.audio.lyrics.resolveActiveLyricIndex
import com.android.purebilibili.feature.audio.lyrics.resolveLyricFocusScrollOffsetPx
import com.android.purebilibili.feature.audio.player.MusicPlayerUiState
import com.android.purebilibili.feature.audio.player.MusicQueueItemUi
import com.android.purebilibili.feature.home.components.BottomBarLiquidSegmentedControl
import com.android.purebilibili.feature.home.components.LiquidGlassTuning
import com.android.purebilibili.feature.home.components.biliPaiFloatingDockShell
import com.android.purebilibili.feature.home.components.resolveLiquidGlassTuning
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.store.SettingsManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.android.purebilibili.feature.video.playback.audio.AudioQualityOption
import com.android.purebilibili.feature.video.player.PlayMode
import com.android.purebilibili.feature.video.ui.components.AudioQualitySelectionMenu
import com.android.purebilibili.feature.video.ui.components.DolbyBadge
import com.android.purebilibili.feature.video.ui.components.HiResBadge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.blur.Backdrop as MiuixBackdrop
import com.android.purebilibili.core.ui.blur.rememberChromeBackdropSource

internal enum class MusicGlassMaterialMode {
    LIQUID,
    FROSTED,
    SURFACE,
}

internal data class MusicPlayerMaterial(
    val mode: MusicGlassMaterialMode,
    val backdropColor: Color,
    val surfaceColor: Color,
    val contentColor: Color,
    val accentColor: Color,
    val borderColor: Color,
    val shadowColor: Color,
    val likeColor: Color,
)

internal val LocalMusicPlayerMaterial = staticCompositionLocalOf {
    MusicPlayerMaterial(
        mode = MusicGlassMaterialMode.SURFACE,
        backdropColor = Color.Unspecified,
        surfaceColor = Color.Unspecified,
        contentColor = Color.Unspecified,
        accentColor = Color.Unspecified,
        borderColor = Color.Unspecified,
        shadowColor = Color.Unspecified,
        likeColor = Color.Unspecified,
    )
}

/** 当前听视频页前景色（随封面色板明暗切换，保证可读）。 */
internal val MusicContentColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalMusicPlayerMaterial.current.contentColor

/** 与视频播放器一致的主题强调色（控件高亮、进度、选中态）。 */
internal val MusicAccentColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalMusicPlayerMaterial.current.accentColor

internal val MusicLikeColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalMusicPlayerMaterial.current.likeColor

internal val MusicShadowColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalMusicPlayerMaterial.current.shadowColor

/**
 * 听视频/音乐页正文色：按背景亮度在可读 token 间切换，并确保高对比度。
 *
 * - 亮底 → [onLightBackground]（对应高对比暗色字）
 * - 暗底 → [onDarkBackground]（对应高对比亮色字）
 */
internal fun resolveMusicPlayerContentColor(
    backgroundColor: Color,
    onLightBackground: Color,
    onDarkBackground: Color,
    lightLuminanceThreshold: Float = 0.45f,
): Color {
    val isLightBackground = backgroundColor.luminance() >= lightLuminanceThreshold
    val target = if (isLightBackground) onLightBackground else onDarkBackground
    val alternate = if (isLightBackground) onDarkBackground else onLightBackground
    val targetContrast = calculateContrastRatio(target, backgroundColor)
    val alternateContrast = calculateContrastRatio(alternate, backgroundColor)
    return if (targetContrast >= alternateContrast) {
        target
    } else {
        alternate
    }
}

internal fun resolveMusicPlayerThemeContentColors(
    colorScheme: ColorScheme,
): Pair<Color, Color> {
    val isDark = colorScheme.surface.luminance() < 0.5f
    // onLightBackground: 浅色背景下使用暗色文字
    // onDarkBackground: 深色背景下使用浅色文字
    val onLight = if (isDark) colorScheme.inverseOnSurface else colorScheme.onSurface
    val onDark = if (isDark) colorScheme.onSurface else colorScheme.inverseOnSurface
    return onLight to onDark
}

@Composable
internal fun resolveMusicPlayerThemeContentColors(): Pair<Color, Color> =
    resolveMusicPlayerThemeContentColors(MaterialTheme.colorScheme)

/** Bottom controls inherit the artwork palette while staying on the dark immersive floor. */
internal fun resolveMusicImmersivePanelColor(
    backgroundColor: Color,
    surfaceColor: Color,
    darkOverlayFraction: Float = 0.45f,
): Color = lerp(
    start = backgroundColor,
    stop = surfaceColor,
    fraction = darkOverlayFraction.coerceIn(0f, 1f),
)

/** Frosted glass container tint adapting to background color and dark/light environment. */
@Composable
internal fun resolveMusicGlassContainerColor(
    glassTintColor: Color,
    isDark: Boolean
): Color {
    val materialColor = LocalMusicPlayerMaterial.current.surfaceColor
    if (materialColor != Color.Unspecified) return materialColor
    val base = glassTintColor.takeOrElse { MaterialTheme.colorScheme.surface }
    val tonalTarget = if (isDark) {
        MaterialTheme.colorScheme.surfaceBright
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    return lerp(base, tonalTarget, if (isDark) 0.24f else 0.40f)
        .copy(alpha = if (isDark) 0.28f else 0.42f)
}

/** Frosted glass subtle border adapting to dark/light environment. */
@Composable
internal fun resolveMusicGlassBorderColor(
    glassTintColor: Color,
    isDark: Boolean
): Color {
    val materialColor = LocalMusicPlayerMaterial.current.borderColor
    if (materialColor != Color.Unspecified) return materialColor
    val base = glassTintColor.takeOrElse { MaterialTheme.colorScheme.surface }
    val edge = if (isDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
    return lerp(base, edge, if (isDark) 0.42f else 0.28f)
        .copy(alpha = if (isDark) 0.30f else 0.22f)
}

/** Pick a theme accent that remains readable in the player controls and lyrics area. */
internal fun resolveMusicPlayerAccentColor(primary: Color, inversePrimary: Color): Color {
    val brightestFloor = Color(0xFF4D4D4D)
    return listOf(primary, inversePrimary, Color.White)
        .firstOrNull { calculateContrastRatio(it, brightestFloor) >= 4.5f }
        ?: Color.White
}

// The blurred artwork can contain bright patches anywhere, regardless of its dominant swatch.
// Keep the entire reading surface dark enough for white controls and secondary text.
internal val MusicArtworkScrimColors = listOf(
    Color.Black.copy(alpha = 0.58f),
    Color.Black.copy(alpha = 0.72f),
    Color.Black.copy(alpha = 0.82f),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MusicPlayerContent(
    state: MusicPlayerUiState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onQueueItemSelected: (Int) -> Unit = {},
    onPlayModeChange: (PlayMode) -> Unit = {},
    onShuffleEnabledChange: (Boolean) -> Unit = {},
    onLyricsOffsetChange: (Long) -> Unit = {},
    onLyricsRetry: () -> Unit = {},
    onLyricsSearch: (String) -> Unit = {},
    onLyricsCandidateSelected: (Int) -> Unit = {},
    onVideoModeClick: (() -> Unit)? = null,
    onCollectionClick: (() -> Unit)? = null,
    onSleepTimerClick: (() -> Unit)? = null,
    sleepTimerLabel: String = "定时关闭",
    audioQualityLabel: String = "音质",
    audioQualityOptions: List<AudioQualityOption> = emptyList(),
    requestedAudioQuality: Int = -1,
    isHiResAudioSelected: Boolean = false,
    isDolbyAudioSelected: Boolean = false,
    onAudioQualitySelected: ((Int) -> Unit)? = null,
    onPipClick: (() -> Unit)? = null,
    onToggleOrientation: (() -> Unit)? = null,
    orientationActionLabel: String = "横屏",
    isLiked: Boolean = false,
    onLikeClick: (() -> Unit)? = null,
    onCommentsClick: (() -> Unit)? = null,
    isFavorited: Boolean = false,
    onFavoriteClick: (() -> Unit)? = null,
    onDownloadClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    onSpeedClick: (() -> Unit)? = null,
    speedLabel: String = "倍速",
    isInPipMode: Boolean = false,
    liquidGlassEffectsEnabled: Boolean = false,
    lyricsBlurEffectsEnabled: Boolean = true,
    reduceMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeSurfaceColor = MaterialTheme.colorScheme.surface
    val adaptiveInfo = LocalAppWindowAdaptiveInfo.current
    val density = LocalDensity.current
    var paletteColor by remember(themeSurfaceColor) { mutableStateOf(themeSurfaceColor) }
    var artworkBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var showQueue by remember { mutableStateOf(false) }
    var isQueueCoverFlow by remember { mutableStateOf(true) }
    var showActions by remember { mutableStateOf(false) }
    var expandedRightPaneTab by remember { mutableStateOf(ExpandedRightPaneTab.LYRICS) }
    var layoutPreferenceName by rememberSaveable {
        mutableStateOf(MusicPlayerLayoutPreference.AUTO.name)
    }
    var showAudioQuality by remember { mutableStateOf(false) }
    var showLyricsSearch by remember { mutableStateOf(false) }
    var progressSeekRevision by remember { mutableIntStateOf(0) }
    var lyricsControlsVisible by remember(state.title) { mutableStateOf(false) }
    var lyricSearchText by remember(state.title) { mutableStateOf(state.title) }
    val currentItem = remember(state.title, state.artist, state.coverUrl) {
        state.queue.getOrNull(state.currentQueueIndex) ?: MusicQueueItemUi(
            stableId = "current",
            title = state.title.ifBlank { "正在播放" },
            artist = state.artist,
            coverUrl = state.coverUrl
        )
    }
    val (effectiveQueue, effectiveCurrentIndex) = remember(state.queue, state.currentQueueIndex, currentItem) {
        val realQueue = state.queue.ifEmpty { listOf(currentItem) }
        realQueue to if (state.queue.isEmpty()) {
            0
        } else {
            state.currentQueueIndex.coerceIn(0, realQueue.lastIndex)
        }
    }
    val systemReduceMotion = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
    val effectiveReduceMotion = reduceMotion || systemReduceMotion || BackgroundManager.isInBackground
    val musicBackdropSource = rememberChromeBackdropSource()
    // The source and all liquid overlays are siblings in this draw tree, so the content layer is
    // recorded before the overlays sample it. Mount the glass chrome on the first composition.
    val musicBackdrop = musicBackdropSource.backdrop
    val homeSettings by SettingsManager
        .getHomeSettings(context)
        .collectAsStateWithLifecycle(initialValue = HomeSettings())
    val liquidGlassTuning = remember(
        homeSettings.liquidGlassProgress,
        homeSettings.liquidGlassAdvancedSettings,
        homeSettings.liquidGlassReadabilityMode,
    ) {
        resolveLiquidGlassTuning(
            progress = homeSettings.liquidGlassProgress,
            advancedSettings = homeSettings.liquidGlassAdvancedSettings,
            readabilityMode = homeSettings.liquidGlassReadabilityMode,
        )
    }

    LaunchedEffect(state.coverUrl, themeSurfaceColor) {
        val result = loadMusicArtwork(context.imageLoader, state.coverUrl, context)
        artworkBitmap = result?.first
        paletteColor = result?.second ?: themeSurfaceColor
    }

    val backgroundColor by animateColorAsState(
        targetValue = paletteColor,
        animationSpec = if (effectiveReduceMotion) snap() else AppMotionTokens.emphasizedSpec(),
        label = "music_palette"
    )
    val glassEnabled = resolveMusicLiquidGlassEnabled(
        sdkInt = Build.VERSION.SDK_INT,
        effectsEnabled = liquidGlassEffectsEnabled,
        isAppInBackground = BackgroundManager.isInBackground,
        reduceMotion = effectiveReduceMotion
    )
    var coverStyle by remember { mutableStateOf(MusicCoverStyle.APPLE_MUSIC_CARD) }
    val chromeSpec = resolveMusicPlayerChromeSpec(
        uiStyle = LocalAppUiStyle.current,
        glassEnabled = glassEnabled,
        coverStyle = coverStyle
    )
    val pageBackground = backgroundColor
    val resolvedContentColor = Color.White
    val resolvedAccentColor = resolveMusicPlayerAccentColor(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.inversePrimary,
    )
    val isDarkEnvironment = true
    val materialMode = when {
        glassEnabled -> MusicGlassMaterialMode.LIQUID
        musicBackdrop != null -> MusicGlassMaterialMode.FROSTED
        else -> MusicGlassMaterialMode.SURFACE
    }
    val materialSurfaceColor = Color.Black.copy(alpha = 0.42f)
    val materialBorderColor = Color.White.copy(alpha = 0.28f)
    val musicMaterial = MusicPlayerMaterial(
        mode = materialMode,
        backdropColor = backgroundColor,
        surfaceColor = materialSurfaceColor,
        contentColor = resolvedContentColor,
        accentColor = resolvedAccentColor,
        borderColor = materialBorderColor,
        shadowColor = MaterialTheme.colorScheme.scrim,
        likeColor = MaterialTheme.colorScheme.error,
    )

    CompositionLocalProvider(
        LocalMusicPlayerMaterial provides musicMaterial,
    ) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(pageBackground)
    ) {
        val availableWidthDp = maxWidth.value.roundToInt()
        val availableHeightDp = maxHeight.value.roundToInt()
        val layoutPreference = remember(layoutPreferenceName) {
            runCatching { MusicPlayerLayoutPreference.valueOf(layoutPreferenceName) }
                .getOrDefault(MusicPlayerLayoutPreference.AUTO)
        }
        val hingeBounds = adaptiveInfo.foldingFeature.hingeBounds
        val hingeStartDp = hingeBounds?.let { bounds ->
            val startPx = when (adaptiveInfo.foldingFeature.hingeOrientation) {
                AppHingeOrientation.Horizontal -> bounds.top
                AppHingeOrientation.Vertical -> bounds.left
                AppHingeOrientation.None -> return@let null
            }
            (startPx / density.density).roundToInt()
        }
        val hingeEndDp = hingeBounds?.let { bounds ->
            val endPx = when (adaptiveInfo.foldingFeature.hingeOrientation) {
                AppHingeOrientation.Horizontal -> bounds.bottom
                AppHingeOrientation.Vertical -> bounds.right
                AppHingeOrientation.None -> return@let null
            }
            (endPx / density.density).roundToInt()
        }
        val layout = resolveMusicPlayerLayout(
            widthDp = availableWidthDp,
            heightDp = availableHeightDp,
            fontScale = density.fontScale,
            isInPipMode = isInPipMode,
            preference = layoutPreference,
            posture = adaptiveInfo.posture,
            hingeOrientation = adaptiveInfo.foldingFeature.hingeOrientation,
            hingeStartDp = hingeStartDp,
            hingeEndDp = hingeEndDp,
            hasObstructingHinge = adaptiveInfo.foldingFeature.hasObstructingHinge,
        )
        if (layout != MusicPlayerLayout.PIP_ARTWORK) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(musicBackdropSource.modifier)
                    .background(pageBackground)
            ) {
                MusicArtworkBackground(
                    coverUrl = state.coverUrl,
                    bitmap = artworkBitmap,
                )
            }
        }
        when (layout) {
            MusicPlayerLayout.PIP_ARTWORK -> MusicArtwork(
                coverUrl = state.coverUrl,
                bitmap = artworkBitmap,
                modifier = Modifier.fillMaxSize(),
                shape = RectangleShape
            )

            MusicPlayerLayout.COMPACT_LANDSCAPE -> {
                var landscapeLyrics by rememberSaveable { mutableStateOf(false) }
                val landscapeHeaderHeight = 48.dp
                Row(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding()
                        .padding(horizontal = 64.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.weight(0.85f).fillMaxHeight(),
                        contentAlignment = if (landscapeLyrics) {
                            Alignment.Center
                        } else {
                            Alignment.TopCenter
                        },
                    ) {
                        val artworkWidth = minOf(maxWidth, maxHeight * 0.70f, 280.dp)
                        Column(
                            modifier = Modifier
                                .width(artworkWidth)
                                .then(
                                    if (landscapeLyrics) {
                                        Modifier
                                    } else {
                                        Modifier.padding(top = landscapeHeaderHeight)
                                    }
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            MusicArtwork(
                                coverUrl = state.coverUrl,
                                bitmap = artworkBitmap,
                                modifier = Modifier.width(artworkWidth),
                                coverStyle = coverStyle,
                                isPlaying = state.isPlaying,
                                rotate = state.isPlaying && !effectiveReduceMotion,
                                playbackSpeed = state.playbackSpeed,
                                reduceMotion = effectiveReduceMotion,
                                isDarkEnvironment = isDarkEnvironment,
                                onClick = { coverStyle = resolveNextCoverStyle(coverStyle) },
                            )
                            if (landscapeLyrics) {
                                Spacer(Modifier.height(8.dp))
                                MusicProgress(
                                    state = state,
                                    onSeek = { positionMs ->
                                        progressSeekRevision += 1
                                        onSeek(positionMs)
                                    },
                                    glassEnabled = glassEnabled,
                                    glassTintColor = backgroundColor,
                                    isDarkEnvironment = isDarkEnvironment,
                                    miuixBackdrop = musicBackdrop,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(4.dp))
                                MusicPlayPauseButton(
                                    state = state,
                                    onPlayPause = onPlayPause,
                                    sizeDp = 56,
                                    isDarkEnvironment = isDarkEnvironment,
                                    glassTintColor = backgroundColor,
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1.15f).fillMaxHeight()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(landscapeHeaderHeight),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppTextButton(onClick = { landscapeLyrics = !landscapeLyrics }) {
                                AppText(if (landscapeLyrics) "返回播放" else "歌词", color = MusicContentColor)
                            }
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            if (landscapeLyrics) {
                            LyricsPage(
                                state = state,
                                glassEnabled = glassEnabled,
                                onPlayPause = onPlayPause,
                                onSeek = { positionMs ->
                                    progressSeekRevision += 1
                                    onSeek(positionMs)
                                },
                                onPrevious = onPrevious,
                                onNext = onNext,
                                onLyricsOffsetChange = onLyricsOffsetChange,
                                onLyricsRetry = onLyricsRetry,
                                onOpenLyricsSearch = { showLyricsSearch = true },
                                blurEffectsEnabled = lyricsBlurEffectsEnabled,
                                reduceMotion = effectiveReduceMotion,
                                glassTintColor = backgroundColor,
                                isDarkEnvironment = isDarkEnvironment,
                                liquidGlassTuning = liquidGlassTuning,
                                miuixBackdrop = musicBackdrop,
                                progressSeekRevision = progressSeekRevision,
                                controlsVisible = lyricsControlsVisible,
                                onControlsVisibleChange = { lyricsControlsVisible = it },
                                showBottomControls = false,
                                modifier = Modifier.fillMaxSize()
                            )
                            } else {
                            PlayerPage(
                                state = state,
                                artworkBitmap = artworkBitmap,
                                artworkSizeDp = resolveMusicArtworkSizeDp(
                                    availableWidthDp,
                                    availableHeightDp,
                                    layout
                                ),
                                chromeSpec = chromeSpec,
                                glassEnabled = glassEnabled,
                                reduceMotion = effectiveReduceMotion,
                                onPlayPause = onPlayPause,
                                onSeek = { positionMs ->
                                    progressSeekRevision += 1
                                    onSeek(positionMs)
                                },
                                onPrevious = onPrevious,
                                onNext = onNext,
                                onPlayModeChange = onPlayModeChange,
                                onShuffleEnabledChange = onShuffleEnabledChange,
                                isLiked = isLiked,
                                onLikeClick = onLikeClick,
                                onCommentsClick = onCommentsClick,
                                onQueueClick = { showQueue = !showQueue },
                                isQueueActive = showQueue,
                                miuixBackdrop = musicBackdrop,
                                audioQualityLabel = audioQualityLabel,
                                isHiResAudioSelected = isHiResAudioSelected,
                                isDolbyAudioSelected = isDolbyAudioSelected,
                                onAudioQualityClick = onAudioQualitySelected?.let {
                                    { showAudioQuality = true }
                                },
                                glassTintColor = backgroundColor,
                                isDarkEnvironment = isDarkEnvironment,
                                coverStyle = coverStyle,
                                onToggleCoverStyle = {
                                    coverStyle = resolveNextCoverStyle(coverStyle)
                                },
                                showLyricsPreview = false,
                                onOpenLyrics = { landscapeLyrics = true },
                                isExpandedLayout = true,
                                compactLandscape = true,
                                modifier = Modifier.fillMaxSize()
                            )
                            }
                        }
                    }
                }
            }

            MusicPlayerLayout.COMPACT_PAGER -> {
                val pagerState = rememberPagerState(pageCount = { 2 })
                val pagerScope = rememberCoroutineScope()
                Box(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        if (page == 0) {
                            PlayerPage(
                                state = state,
                                artworkBitmap = artworkBitmap,
                                artworkSizeDp = resolveMusicArtworkSizeDp(
                                    availableWidthDp,
                                    availableHeightDp,
                                    layout
                                ),
                                chromeSpec = chromeSpec,
                                glassEnabled = glassEnabled,
                                reduceMotion = effectiveReduceMotion,
                                onPlayPause = onPlayPause,
                                onSeek = { positionMs ->
                                    progressSeekRevision += 1
                                    onSeek(positionMs)
                                },
                                onPrevious = onPrevious,
                                onNext = onNext,
                                onPlayModeChange = onPlayModeChange,
                                onShuffleEnabledChange = onShuffleEnabledChange,
                                isLiked = isLiked,
                                onLikeClick = onLikeClick,
                                onCommentsClick = onCommentsClick,
                                onQueueClick = { showQueue = !showQueue },
                                isQueueActive = showQueue,
                                miuixBackdrop = musicBackdrop,
                                audioQualityLabel = audioQualityLabel,
                                isHiResAudioSelected = isHiResAudioSelected,
                                isDolbyAudioSelected = isDolbyAudioSelected,
                                onAudioQualityClick = onAudioQualitySelected?.let {
                                    { showAudioQuality = true }
                                },
                                glassTintColor = backgroundColor,
                                isDarkEnvironment = isDarkEnvironment,
                                coverStyle = coverStyle,
                                onToggleCoverStyle = {
                                    coverStyle = resolveNextCoverStyle(coverStyle)
                                },
                                showLyricsPreview = true,
                                onOpenLyrics = {
                                    pagerScope.launch {
                                        pagerState.animateScrollToPage(1)
                                    }
                                },
                                modifier = Modifier.padding(bottom = MUSIC_PLAYER_COMPACT_DOCK_BOTTOM_PADDING_DP.dp)
                            )
                        } else {
                            LyricsPage(
                                state = state,
                                glassEnabled = glassEnabled,
                                onPlayPause = onPlayPause,
                                onSeek = { positionMs ->
                                    progressSeekRevision += 1
                                    onSeek(positionMs)
                                },
                                onPrevious = onPrevious,
                                onNext = onNext,
                                onLyricsOffsetChange = onLyricsOffsetChange,
                                onLyricsRetry = onLyricsRetry,
                                onOpenLyricsSearch = { showLyricsSearch = true },
                                blurEffectsEnabled = lyricsBlurEffectsEnabled,
                                reduceMotion = effectiveReduceMotion,
                                glassTintColor = backgroundColor,
                                isDarkEnvironment = isDarkEnvironment,
                                liquidGlassTuning = liquidGlassTuning,
                                miuixBackdrop = musicBackdrop,
                                progressSeekRevision = progressSeekRevision,
                                controlsVisible = lyricsControlsVisible,
                                onControlsVisibleChange = { lyricsControlsVisible = it },
                                showBottomControls = true,
                                modifier = Modifier.padding(bottom = MUSIC_PLAYER_COMPACT_DOCK_BOTTOM_PADDING_DP.dp)
                            )
                        }
                    }
                    BottomBarLiquidSegmentedControl(
                        items = resolveMusicPlayerPageTabs(),
                        selectedIndex = pagerState.currentPage,
                        onSelected = { page ->
                            pagerScope.launch {
                                // animateScrollToPage via continuous pager selection
                                animatePagerSelection(pagerState, page)
                            }
                        },
                        itemWidth = 84.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(vertical = 8.dp)
                            .wrapContentWidth(Alignment.CenterHorizontally),
                        height = 48.dp,
                        indicatorHeight = 36.dp,
                        containerVerticalPadding = 6.dp,
                        selectedTextColorOverride = MaterialTheme.colorScheme.onSurface,
                        unselectedTextColorOverride = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        liquidGlassEffectsEnabled = liquidGlassEffectsEnabled,
                        preferInlineContentStyle = false,
                        miuixBackdrop = musicBackdrop,
                        dragSelectionEnabled = true,
                        tapPressRefractionEnabled = true,
                        isScrollInProgressProvider = { pagerState.isScrollInProgress },
                        indicatorPositionProvider = {
                            resolveMusicPagerIndicatorPosition(
                                currentPage = pagerState.currentPage,
                                currentPageOffsetFraction = pagerState.currentPageOffsetFraction
                            )
                        },
                        externalPagerMotionEffectsEnabled = true,
                    )
                }
            }

            MusicPlayerLayout.TABLETOP -> TabletopPlayerLayout(
                coverStyle = coverStyle,
                state = state,
                queue = effectiveQueue,
                currentIndex = effectiveCurrentIndex,
                artworkBitmap = artworkBitmap,
                glassEnabled = glassEnabled,
                reduceMotion = effectiveReduceMotion,
                lyricsBlurEffectsEnabled = lyricsBlurEffectsEnabled,
                backgroundColor = backgroundColor,
                musicBackdrop = musicBackdrop,
                liquidGlassTuning = liquidGlassTuning,
                progressSeekRevision = progressSeekRevision,
                isLiked = isLiked,
                onPlayPause = onPlayPause,
                onSeek = { positionMs ->
                    progressSeekRevision += 1
                    onSeek(positionMs)
                },
                onPrevious = onPrevious,
                onNext = onNext,
                onQueueItemSelected = onQueueItemSelected,
                onLikeClick = onLikeClick,
                onToggleCoverStyle = { coverStyle = resolveNextCoverStyle(coverStyle) },
                onLyricsOffsetChange = onLyricsOffsetChange,
                onLyricsRetry = onLyricsRetry,
                onOpenLyricsSearch = { showLyricsSearch = true },
                availableWidthDp = availableWidthDp,
                hingeStartDp = if (adaptiveInfo.foldingFeature.hingeOrientation == AppHingeOrientation.Horizontal) {
                    hingeStartDp
                } else null,
                hingeEndDp = if (adaptiveInfo.foldingFeature.hingeOrientation == AppHingeOrientation.Horizontal) {
                    hingeEndDp
                } else null,
                isDarkEnvironment = isDarkEnvironment,
                modifier = Modifier.fillMaxSize()
            )

            MusicPlayerLayout.EXPANDED_SPLIT -> {
                    val hasVerticalHinge =
                        adaptiveInfo.foldingFeature.hasObstructingHinge &&
                            adaptiveInfo.foldingFeature.hingeOrientation == AppHingeOrientation.Vertical &&
                            hingeStartDp != null &&
                            hingeEndDp != null
                    val primaryPaneWeight = if (hasVerticalHinge) {
                        (hingeStartDp!! - MUSIC_PLAYER_HINGE_CLEARANCE_DP)
                            .coerceAtLeast(1)
                            .toFloat()
                    } else {
                        1f
                    }
                    val secondaryPaneWeight = if (hasVerticalHinge) {
                        (availableWidthDp - hingeEndDp!! - MUSIC_PLAYER_HINGE_CLEARANCE_DP)
                            .coerceAtLeast(1)
                            .toFloat()
                    } else {
                        1.15f
                    }
                    val horizontalPadding = if (hasVerticalHinge) {
                        0.dp
                    } else {
                        resolveLargeScreenHorizontalPaddingDp(availableWidthDp).dp
                    }
                    val gutter = if (hasVerticalHinge) {
                        (hingeEndDp!! - hingeStartDp!! + MUSIC_PLAYER_HINGE_CLEARANCE_DP * 2).dp
                    } else {
                        resolveLargeScreenGutterDp(availableWidthDp).dp
                    }
                    val maximumContentWidth = if (hasVerticalHinge) {
                        availableWidthDp.dp
                    } else {
                        LARGE_SCREEN_MAX_CONTENT_WIDTH_DP.dp
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = maximumContentWidth)
                                .padding(top = 48.dp, start = horizontalPadding, end = horizontalPadding, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(gutter)
                        ) {
                            PlayerPage(
                                state = state,
                                artworkBitmap = artworkBitmap,
                                artworkSizeDp = resolveMusicArtworkSizeDp(
                                    availableWidthDp,
                                    availableHeightDp,
                                    layout
                                ),
                                chromeSpec = chromeSpec,
                                glassEnabled = glassEnabled,
                                reduceMotion = effectiveReduceMotion,
                                onPlayPause = onPlayPause,
                                onSeek = { positionMs ->
                                    progressSeekRevision += 1
                                    onSeek(positionMs)
                                },
                                onPrevious = onPrevious,
                                onNext = onNext,
                                onPlayModeChange = onPlayModeChange,
                                onShuffleEnabledChange = onShuffleEnabledChange,
                                isLiked = isLiked,
                                onLikeClick = onLikeClick,
                                onCommentsClick = onCommentsClick,
                                onQueueClick = {
                                    expandedRightPaneTab = if (expandedRightPaneTab == ExpandedRightPaneTab.QUEUE) {
                                        ExpandedRightPaneTab.LYRICS
                                    } else {
                                        ExpandedRightPaneTab.QUEUE
                                    }
                                },
                                miuixBackdrop = musicBackdrop,
                                audioQualityLabel = audioQualityLabel,
                                isHiResAudioSelected = isHiResAudioSelected,
                                isDolbyAudioSelected = isDolbyAudioSelected,
                                onAudioQualityClick = onAudioQualitySelected?.let {
                                    { showAudioQuality = true }
                                },
                                glassTintColor = backgroundColor,
                                isDarkEnvironment = isDarkEnvironment,
                                coverStyle = coverStyle,
                                onToggleCoverStyle = {
                                    coverStyle = resolveNextCoverStyle(coverStyle)
                                },
                                showLyricsPreview = false,
                                onOpenLyrics = null,
                                isExpandedLayout = true,
                                isQueueActive = expandedRightPaneTab == ExpandedRightPaneTab.QUEUE,
                                modifier = Modifier.weight(primaryPaneWeight)
                            )
                            Box(modifier = Modifier.weight(secondaryPaneWeight).fillMaxHeight()) {
                                Crossfade(
                                    targetState = expandedRightPaneTab,
                                    label = "expanded_right_pane"
                                ) { tab ->
                                    when (tab) {
                                        ExpandedRightPaneTab.LYRICS -> {
                                            LyricsPage(
                                                state = state,
                                                glassEnabled = glassEnabled,
                                                onPlayPause = onPlayPause,
                                                onSeek = onSeek,
                                                onPrevious = onPrevious,
                                                onNext = onNext,
                                                onLyricsOffsetChange = onLyricsOffsetChange,
                                                onLyricsRetry = onLyricsRetry,
                                                onOpenLyricsSearch = { showLyricsSearch = true },
                                                blurEffectsEnabled = lyricsBlurEffectsEnabled,
                                                reduceMotion = effectiveReduceMotion,
                                                glassTintColor = backgroundColor,
                                                isDarkEnvironment = isDarkEnvironment,
                                                liquidGlassTuning = liquidGlassTuning,
                                                miuixBackdrop = musicBackdrop,
                                                progressSeekRevision = progressSeekRevision,
                                                controlsVisible = lyricsControlsVisible,
                                                onControlsVisibleChange = { lyricsControlsVisible = it },
                                                showBottomControls = false,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        ExpandedRightPaneTab.QUEUE -> {
                                            ExpandedQueuePane(
                                                queue = effectiveQueue,
                                                currentIndex = effectiveCurrentIndex,
                                                onItemClick = onQueueItemSelected,
                                                onClose = { expandedRightPaneTab = ExpandedRightPaneTab.LYRICS },
                                                glassEnabled = glassEnabled,
                                                reduceMotion = effectiveReduceMotion,
                                                miuixBackdrop = musicBackdrop,
                                                glassTintColor = backgroundColor,
                                                isDarkEnvironment = isDarkEnvironment,
                                                liquidGlassTuning = liquidGlassTuning,
                                                isPlaying = state.isPlaying,
                                                onPlayPause = onPlayPause,
                                                onPrevious = onPrevious,
                                                onNext = onNext,
                                                isLiked = isLiked,
                                                onLikeClick = onLikeClick,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
        }

        // 沉浸式悬浮待播唱片架 / 待播列表 (非弹窗式，浮于底部)
        AnimatedVisibility(
            visible = showQueue && layout in setOf(MusicPlayerLayout.COMPACT_PAGER, MusicPlayerLayout.COMPACT_LANDSCAPE) && !isInPipMode,
            enter = if (effectiveReduceMotion) {
                fadeIn(animationSpec = AppMotionTokens.standardSpec())
            } else {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = AppMotionTokens.emphasizedSpec()
                ) + fadeIn(animationSpec = AppMotionTokens.emphasizedSpec())
            },
            exit = if (effectiveReduceMotion) {
                fadeOut(animationSpec = AppMotionTokens.standardSpec())
            } else {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = AppMotionTokens.standardSpec()
                ) + fadeOut(animationSpec = AppMotionTokens.standardSpec())
            },
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 点击上半部分透明区域收起待播架，完全无暗色遮罩 scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showQueue = false
                        }
                )

                // 悬浮于底部的沉浸式唱片架面板
                ImmersiveBottomQueueShelf(
                    queue = effectiveQueue,
                    currentIndex = effectiveCurrentIndex,
                    isPlaying = state.isPlaying,
                    isLiked = isLiked,
                    onPlayPause = onPlayPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onLikeClick = onLikeClick,
                    onQueueItemSelected = onQueueItemSelected,
                    onClose = { showQueue = false },
                    isQueueCoverFlow = isQueueCoverFlow,
                    onToggleQueueCoverFlow = { isQueueCoverFlow = !isQueueCoverFlow },
                    glassEnabled = glassEnabled,
                    reduceMotion = effectiveReduceMotion,
                    miuixBackdrop = musicBackdrop,
                    glassTintColor = backgroundColor,
                    liquidGlassTuning = liquidGlassTuning,
                    isDarkEnvironment = isDarkEnvironment,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .widthIn(max = LARGE_SCREEN_MAX_CONTENT_WIDTH_DP.dp)
                        .fillMaxWidth()
                )
            }
        }

        if (!isInPipMode) {
            MusicTopBar(
                glassEnabled = glassEnabled,
                miuixBackdrop = musicBackdrop,
                liquidGlassTuning = liquidGlassTuning,
                glassTintColor = backgroundColor,
                isDarkEnvironment = isDarkEnvironment,
                onBack = onBack,
                onMore = { showActions = true },
                actionsPopup = {
                    if (showActions) {
                        // 菜单使用主题文字色，与动态封面背景解耦。
                        val sheetContentColor = MaterialTheme.colorScheme.onSurface
                        WindowListPopup(
                            show = showActions,
                            onDismissRequest = { showActions = false },
                            alignment = PopupPositionProvider.Align.End,
                            popupPositionProvider = ListPopupDefaults.dropdownPositionProvider(
                                verticalMargin = 8.dp,
                                horizontalMargin = 12.dp,
                            ),
                            enableWindowDim = false,
                            maxHeight = 440.dp,
                        ) {
                            ListPopupColumn {
                                AppText(
                                    text = "播放器操作",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = sheetContentColor,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                                )
                                MusicActionSheetItem(
                                    "切换封面：${resolveCoverStyleLabel(resolveNextCoverStyle(coverStyle))}",
                                    contentColor = sheetContentColor
                                ) {
                                    showActions = false
                                    coverStyle = resolveNextCoverStyle(coverStyle)
                                }
                                if (
                                    !adaptiveInfo.foldingFeature.hasObstructingHinge &&
                                    adaptiveInfo.windowSizeClass.widthDp.value >= MUSIC_PLAYER_EXPANDED_WIDTH_DP
                                ) {
                                    MusicPlayerLayoutPreference.entries.forEach { preference ->
                                        MusicActionSheetItem(
                                            label = "布局：${resolveMusicPlayerLayoutPreferenceLabel(preference)}" +
                                                if (layoutPreferenceName == preference.name) "（当前）" else "",
                                            contentColor = sheetContentColor,
                                        ) {
                                            layoutPreferenceName = preference.name
                                            showActions = false
                                        }
                                    }
                                }
                                if (onAudioQualitySelected != null) {
                                    MusicActionSheetItem(
                                        "音频音质：$audioQualityLabel",
                                        contentColor = sheetContentColor
                                    ) {
                                        showActions = false
                                        showAudioQuality = true
                                    }
                                }
                                MusicActionSheetItem("3D 唱片架 / 播放队列", contentColor = sheetContentColor) {
                                    showActions = false
                                    showQueue = true
                                }
                                onVideoModeClick?.let { action ->
                                    MusicActionSheetItem("返回视频", contentColor = sheetContentColor) {
                                        showActions = false
                                        action()
                                    }
                                }
                                onCollectionClick?.let { action ->
                                    MusicActionSheetItem("选集 / 合集", contentColor = sheetContentColor) {
                                        showActions = false
                                        action()
                                    }
                                }
                                onSpeedClick?.let { action ->
                                    MusicActionSheetItem(speedLabel, contentColor = sheetContentColor) {
                                        showActions = false
                                        action()
                                    }
                                }
                                onSleepTimerClick?.let { action ->
                                    MusicActionSheetItem(sleepTimerLabel, contentColor = sheetContentColor) {
                                        showActions = false
                                        action()
                                    }
                                }
                                onFavoriteClick?.let { action ->
                                    MusicActionSheetItem(
                                        if (isFavorited) "已收藏" else "收藏",
                                        contentColor = sheetContentColor
                                    ) {
                                        showActions = false
                                        action()
                                    }
                                }
                                onDownloadClick?.let { action ->
                                    MusicActionSheetItem("缓存音频", contentColor = sheetContentColor) {
                                        showActions = false
                                        action()
                                    }
                                }
                                onShareClick?.let { action ->
                                    MusicActionSheetItem("分享", contentColor = sheetContentColor) {
                                        showActions = false
                                        action()
                                    }
                                }
                                onPipClick?.let { action ->
                                    MusicActionSheetItem("画中画", contentColor = sheetContentColor) {
                                        showActions = false
                                        action()
                                    }
                                }
                                onToggleOrientation?.let { action ->
                                    MusicActionSheetItem(orientationActionLabel, contentColor = sheetContentColor) {
                                        showActions = false
                                        action()
                                    }
                                }
                                MusicActionSheetItem("搜索歌词", contentColor = sheetContentColor) {
                                    showActions = false
                                    showLyricsSearch = true
                                }
                            }
                        }
                    }
                },
                onToggleLayout = if (
                    !adaptiveInfo.foldingFeature.hasObstructingHinge &&
                    availableWidthDp >= MUSIC_PLAYER_EXPANDED_WIDTH_DP &&
                    availableHeightDp >= 480
                ) {
                    {
                        layoutPreferenceName = nextMusicPlayerLayoutPreference(layoutPreference).name
                    }
                } else null,
                layoutActionLabel = resolveMusicPlayerLayoutPreferenceLabel(
                    nextMusicPlayerLayoutPreference(layoutPreference)
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }


    if (showAudioQuality && onAudioQualitySelected != null) {
        AudioQualitySelectionMenu(
            options = audioQualityOptions,
            requestedAudioQuality = requestedAudioQuality,
            onAudioQualitySelected = { quality ->
                onAudioQualitySelected(quality)
                showAudioQuality = false
            },
            onDismiss = { showAudioQuality = false }
        )
    }

    if (showLyricsSearch) {
        AppModalBottomSheet(
            onDismissRequest = { showLyricsSearch = false },
            containerColor = AppSurfaceTokens.surface(),
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            AppText(
                text = "手动匹配歌词",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppOutlinedTextField(
                    value = lyricSearchText,
                    onValueChange = { lyricSearchText = it },
                    label = { AppText("歌名") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                AppTextButton(onClick = { onLyricsSearch(lyricSearchText) }) {
                    AppText("搜索")
                }
            }
            if (state.isLyricsSearching) {
                AppCircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(24.dp)
                )
            } else if (state.lyricCandidates.isEmpty()) {
                AppText(
                    text = "输入歌名后搜索网易云、QQ 音乐与酷狗",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    itemsIndexed(state.lyricCandidates) { index, candidate ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLyricsCandidateSelected(index)
                                    showLyricsSearch = false
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            AppText(
                                candidate.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            AppText(
                                text = "${candidate.artist} · ${candidate.sourceLabel}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

/**
 * 沉浸式悬浮待播唱片架 / 待播列表（非弹窗式，浮于底部）。
 *
 * 核心设计：
 * 1. 悬浮浮层：浮于播放器底层舞台之上，无全屏暗色遮罩 (scrim)，保持上半部封面与歌词通透沉浸。
 * 2. 质感材质：RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp) + biliPaiFloatingDockShell 流体毛玻璃。
 * 3. 极简导览：待播清单 (数量) + [切换列表 / 3D 唱片架] 模式胶囊 + [v] 优雅下推收起按钮，支持顶部下推手势收起。
 * 4. 模式无缝切换：3D 实体 CD 唱片架 (Cover Flow) 与 高级毛玻璃清单双模态切换。
 */
@Composable
private fun ImmersiveBottomQueueShelf(
    queue: List<MusicQueueItemUi>,
    currentIndex: Int,
    isPlaying: Boolean,
    isLiked: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    onLikeClick: (() -> Unit)?,
    onQueueItemSelected: (Int) -> Unit,
    onClose: () -> Unit,
    isQueueCoverFlow: Boolean,
    onToggleQueueCoverFlow: () -> Unit,
    glassEnabled: Boolean,
    miuixBackdrop: MiuixBackdrop?,
    glassTintColor: Color,
    liquidGlassTuning: LiquidGlassTuning,
    reduceMotion: Boolean,
    isDarkEnvironment: Boolean = true,
    modifier: Modifier = Modifier
) {
    val panelShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val panelColor = resolveMusicImmersivePanelColor(
        backgroundColor = glassTintColor,
        surfaceColor = MaterialTheme.colorScheme.surface,
    ).copy(alpha = 0.92f)

    AppSurface(
        shape = panelShape,
        color = if (miuixBackdrop != null) Color.Transparent else panelColor,
        contentColor = MusicContentColor,
        border = BorderStroke(1.dp, resolveMusicGlassBorderColor(glassTintColor, isDarkEnvironment)),
        shadowElevation = 16.dp,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}
            .biliPaiFloatingDockShell(
                backdrop = miuixBackdrop,
                containerColor = panelColor,
                pressProgress = 0f,
                shape = panelShape,
                enabled = glassEnabled,
                blurEnabled = !glassEnabled,
                liquidGlassTuning = liquidGlassTuning
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            // 顶栏：待播清单 (数量) + 模式切换 + 收起按钮（支持下拉手势快速收起）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 12f) {
                                onClose()
                            }
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppText(
                        text = if (queue.isNotEmpty()) "待播清单 (${queue.size})" else "待播清单",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MusicContentColor
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (queue.isNotEmpty()) {
                        AppSurface(
                            onClick = onToggleQueueCoverFlow,
                            shape = AppShapes.container(ContainerLevel.Pill),
                            color = resolveMusicGlassContainerColor(glassTintColor, isDarkEnvironment),
                            border = BorderStroke(0.8.dp, resolveMusicGlassBorderColor(glassTintColor, isDarkEnvironment)),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AppText(
                                    text = if (isQueueCoverFlow) "切换列表" else "3D 唱片架",
                                    color = MusicAccentColor,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    AppIconButton(
                        onClick = onClose,
                        modifier = Modifier.size(48.dp)
                    ) {
                        AppIcon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = "收起待播清单",
                            tint = MusicContentColor.copy(alpha = 0.72f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AppText(
                        text = "待播清单为空",
                        color = MusicContentColor.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else if (isQueueCoverFlow) {
                Music3DCoverFlow(
                    queue = queue,
                    currentIndex = currentIndex,
                    isPlaying = isPlaying,
                    onItemClick = onQueueItemSelected,
                    onPlayPause = onPlayPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    isLiked = isLiked,
                    onLikeClick = onLikeClick,
                    cardSizeDp = 150,
                    showTransportControls = true,
                    glassEnabled = glassEnabled,
                    miuixBackdrop = miuixBackdrop,
                    liquidGlassTuning = liquidGlassTuning,
                    glassTintColor = glassTintColor,
                    isDarkEnvironment = isDarkEnvironment,
                    reduceMotion = reduceMotion,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    itemsIndexed(queue, key = { _, item -> item.stableId }) { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onQueueItemSelected(index)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = item.coverUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                AppText(
                                    text = item.title,
                                    color = if (index == currentIndex) MusicAccentColor else MusicContentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(2.dp))
                                AppText(
                                    text = item.artist,
                                    color = MusicContentColor.copy(alpha = 0.65f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (index == currentIndex) {
                                AppIcon(
                                    Icons.Outlined.MusicNote,
                                    contentDescription = null,
                                    tint = MusicAccentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicArtworkBackground(
    coverUrl: String,
    bitmap: ImageBitmap? = null,
) {
    Box(Modifier.fillMaxSize()) {
        if (bitmap != null || coverUrl.isNotBlank()) {
            val imageModifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.55f
                    scaleY = 1.55f
                }
                .blur(80.dp)
            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = imageModifier,
                    contentScale = ContentScale.Crop,
                )
            } else {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    modifier = imageModifier,
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(MusicArtworkScrimColors))
        )
    }
}

@Composable
private fun PlayerPage(
    state: MusicPlayerUiState,
    artworkBitmap: ImageBitmap?,
    artworkSizeDp: Int,
    chromeSpec: MusicPlayerChromeSpec,
    glassEnabled: Boolean,
    reduceMotion: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    onPlayModeChange: (PlayMode) -> Unit,
    onShuffleEnabledChange: (Boolean) -> Unit,
    isLiked: Boolean,
    onLikeClick: (() -> Unit)?,
    onCommentsClick: (() -> Unit)?,
    onQueueClick: () -> Unit,
    miuixBackdrop: MiuixBackdrop?,
    audioQualityLabel: String,
    isHiResAudioSelected: Boolean,
    isDolbyAudioSelected: Boolean,
    onAudioQualityClick: (() -> Unit)?,
    glassTintColor: Color,
    isDarkEnvironment: Boolean = true,
    coverStyle: MusicCoverStyle = MusicCoverStyle.APPLE_MUSIC_CARD,
    onToggleCoverStyle: () -> Unit = {},
    showLyricsPreview: Boolean = true,
    showQuickFormatControls: Boolean = false,
    onOpenLyrics: (() -> Unit)? = null,
    isExpandedLayout: Boolean = false,
    compactLandscape: Boolean = false,
    isQueueActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val topPadding = if (compactLandscape) 0.dp else if (isExpandedLayout) 12.dp else 64.dp
    val bottomPadding = if (isExpandedLayout) 12.dp else 12.dp
    val horizontalPadding = if (isExpandedLayout) 16.dp else chromeSpec.horizontalPaddingDp.dp
    val portraitArtworkSizeDp = if (!isExpandedLayout && !compactLandscape) {
        (artworkSizeDp * 1.12f).roundToInt()
    } else {
        artworkSizeDp
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .then(if (compactLandscape) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .then(if (isExpandedLayout) Modifier else Modifier.navigationBarsPadding())
            .padding(
                start = horizontalPadding,
                top = topPadding,
                end = horizontalPadding,
                bottom = bottomPadding
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (!compactLandscape) {
            // 上半部：封面展示与实时歌词空间（弹性居中占满可用剩余空间，绝不挤压底部控制栏）
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(
                        if (!isExpandedLayout && !compactLandscape) {
                            Modifier.offset(y = 12.dp)
                        } else {
                            Modifier
                        }
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (state.isLoading && state.coverUrl.isBlank()) {
                    AdaptiveLoadingIndicator(color = MusicContentColor)
                } else {
                    MusicArtwork(
                        coverUrl = state.coverUrl,
                        bitmap = artworkBitmap,
                        modifier = Modifier.width(portraitArtworkSizeDp.dp),
                        shape = if (coverStyle == MusicCoverStyle.TURNTABLE) CircleShape else AppShapes.container(ContainerLevel.Card),
                        rotate = shouldRotateMusicArtwork(
                            isPlaying = state.isPlaying,
                            reduceMotion = reduceMotion
                        ),
                        playbackSpeed = state.playbackSpeed,
                        coverStyle = coverStyle,
                        isPlaying = state.isPlaying,
                        reduceMotion = reduceMotion,
                        isDarkEnvironment = isDarkEnvironment,
                        onClick = onToggleCoverStyle
                    )
                }
                if (showLyricsPreview) {
                    Spacer(Modifier.height(14.dp))
                    PlayerLyricsPreview(
                        lyrics = state.lyrics,
                        positionMs = state.positionMs,
                        onOpenLyrics = onOpenLyrics
                    )
                }
            }

        }

        // 下半部：歌曲信息与控制组件区（始终稳定坐落于底端，完整展示播放/暂停与切歌）
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    AppText(
                        text = state.title,
                        color = MusicContentColor,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppText(
                            text = state.artist.ifBlank { "未知艺术家" },
                            color = MusicContentColor.copy(alpha = 0.82f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    state.error?.let {
                        AppText(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
                onLikeClick?.let { like ->
                    AppIconButton(onClick = like, modifier = Modifier.size(48.dp)) {
                        AppIcon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isLiked) "取消点赞" else "点赞",
                            tint = if (isLiked) MusicLikeColor else MusicContentColor.copy(alpha = 0.72f)
                        )
                    }
                }
            }
            if (showQuickFormatControls) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onAudioQualityClick != null) {
                        Box(modifier = Modifier.weight(1f)) {
                            MusicAudioQualityControl(
                                label = audioQualityLabel,
                                isHiResSelected = isHiResAudioSelected,
                                isDolbySelected = isDolbyAudioSelected,
                                onClick = onAudioQualityClick,
                                glassTintColor = glassTintColor,
                                isDarkEnvironment = isDarkEnvironment
                            )
                        }
                    }
                    AppSurface(
                        onClick = onToggleCoverStyle,
                        shape = AppShapes.container(ContainerLevel.Dialog),
                        color = resolveMusicGlassContainerColor(glassTintColor, isDarkEnvironment),
                        border = BorderStroke(0.8.dp, resolveMusicGlassBorderColor(glassTintColor, isDarkEnvironment)),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AppText(
                                text = resolveCoverStyleShortLabel(resolveNextCoverStyle(coverStyle)),
                                color = MusicAccentColor,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            MusicProgress(
                state = state,
                onSeek = onSeek,
                glassEnabled = chromeSpec.glassEnabled,
                glassTintColor = glassTintColor,
                isDarkEnvironment = isDarkEnvironment,
                miuixBackdrop = miuixBackdrop,
            )
            Spacer(Modifier.height(8.dp))
            PlaybackControls(
                state = state,
                playButtonSizeDp = if (compactLandscape) 56 else chromeSpec.playButtonSizeDp,
                skipButtonSizeDp = if (compactLandscape) 48 else chromeSpec.skipButtonSizeDp,
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                isDarkEnvironment = isDarkEnvironment,
                glassTintColor = glassTintColor
            )
            Spacer(Modifier.height(10.dp))
            MusicSecondaryControls(
                mode = state.playMode,
                shuffleEnabled = state.shuffleEnabled,
                showQueue = state.queueControls.showQueue || state.queue.isNotEmpty(),
                onPlayModeChange = onPlayModeChange,
                onShuffleEnabledChange = onShuffleEnabledChange,
                onCommentsClick = onCommentsClick,
                onQueueClick = onQueueClick,
                isQueueActive = isQueueActive
            )
        }
    }
}

@Composable
private fun MusicAudioQualityControl(
    label: String,
    isHiResSelected: Boolean,
    isDolbySelected: Boolean,
    onClick: () -> Unit,
    glassTintColor: Color = Color.Unspecified,
    isDarkEnvironment: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(AppShapes.container(ContainerLevel.Dialog))
            .background(resolveMusicGlassContainerColor(glassTintColor, isDarkEnvironment))
            .border(
                0.8.dp,
                resolveMusicGlassBorderColor(glassTintColor, isDarkEnvironment),
                AppShapes.container(ContainerLevel.Dialog)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppText(
            text = "音质",
            color = MusicContentColor.copy(alpha = 0.82f),
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.weight(1f))
        AppText(
            text = label.ifBlank { "音质" },
            color = MusicAccentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        if (isHiResSelected) {
            HiResBadge()
        }
        if (isDolbySelected) {
            DolbyBadge()
        }
    }
}

@Composable
private fun MusicSecondaryControls(
    mode: PlayMode,
    shuffleEnabled: Boolean,
    showQueue: Boolean,
    onPlayModeChange: (PlayMode) -> Unit,
    onShuffleEnabledChange: (Boolean) -> Unit,
    onCommentsClick: (() -> Unit)?,
    onQueueClick: () -> Unit,
    isQueueActive: Boolean = false
) {
    val transport = resolveMusicSecondaryTransport(mode, shuffleEnabled)
    val active = MusicAccentColor
    val inactive = MusicContentColor.copy(alpha = 0.62f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconButton(
            onClick = { onShuffleEnabledChange(!transport.shuffleEnabled) },
            modifier = Modifier.size(48.dp)
        ) {
            AppIcon(
                Icons.Outlined.Shuffle,
                contentDescription = "随机播放",
                tint = if (transport.shuffleEnabled) active else inactive
            )
        }
        AppIconButton(
            onClick = { onPlayModeChange(resolveRepeatModeAfterToggle(mode)) },
            modifier = Modifier.size(48.dp)
        ) {
            AppIcon(
                imageVector = if (transport.repeatGlyph == MusicRepeatGlyph.ONE) {
                    Icons.Outlined.RepeatOne
                } else {
                    Icons.Outlined.Repeat
                },
                contentDescription = "循环模式",
                tint = if (transport.repeatGlyph == MusicRepeatGlyph.OFF) inactive else active
            )
        }
        AppIconButton(
            onClick = onCommentsClick ?: {},
            enabled = onCommentsClick != null,
            modifier = Modifier.size(48.dp)
        ) {
            AppIcon(
                Icons.AutoMirrored.Outlined.Comment,
                contentDescription = "评论",
                tint = if (onCommentsClick != null) inactive else inactive.copy(alpha = 0.28f)
            )
        }
        AppIconButton(
            onClick = onQueueClick,
            enabled = showQueue,
            modifier = Modifier.size(48.dp)
        ) {
            AppIcon(
                Icons.Outlined.QueueMusic,
                contentDescription = "播放队列",
                tint = if (isQueueActive) active else if (showQueue) inactive else inactive.copy(alpha = 0.28f)
            )
        }
    }
}

@Composable
private fun MusicArtwork(
    coverUrl: String,
    bitmap: ImageBitmap?,
    modifier: Modifier,
    shape: Shape = CircleShape,
    rotate: Boolean = false,
    playbackSpeed: Float = 1f,
    coverStyle: MusicCoverStyle = MusicCoverStyle.APPLE_MUSIC_CARD,
    isPlaying: Boolean = false,
    reduceMotion: Boolean = false,
    isDarkEnvironment: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val artworkShadowColor = MusicShadowColor
    val artworkBorderColor = resolveMusicGlassBorderColor(
        LocalMusicPlayerMaterial.current.backdropColor,
        isDarkEnvironment,
    )
    val artworkFallbackBrush = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surface,
        )
    )
    if (shape == RectangleShape) {
        // PiP 模式：直接铺满画中画窗口
        Box(
            modifier = modifier
                .clip(shape)
                .background(
                    artworkFallbackBrush
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                bitmap != null -> androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = "专辑封面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                coverUrl.isNotBlank() -> AsyncImage(
                    model = coverUrl,
                    contentDescription = "专辑封面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                else -> AppIcon(
                    Icons.Outlined.MusicNote,
                    contentDescription = null,
                    tint = MusicContentColor.copy(alpha = 0.78f),
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    } else if (coverStyle == MusicCoverStyle.TURNTABLE) {
        val rotationDegrees = rememberMusicArtworkRotationDegrees(
            active = rotate,
            contentKey = coverUrl,
            playbackSpeed = playbackSpeed
        )
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .shadow(
                    elevation = if (isPlaying) 18.dp else 10.dp,
                    shape = CircleShape,
                    ambientColor = artworkShadowColor.copy(alpha = if (isDarkEnvironment) 0.55f else 0.20f),
                    spotColor = artworkShadowColor.copy(alpha = if (isDarkEnvironment) 0.65f else 0.25f)
                )
                .graphicsLayer { rotationZ = rotationDegrees() }
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = artworkBorderColor,
                    shape = CircleShape
                )
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            when {
                bitmap != null -> androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = "专辑封面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                coverUrl.isNotBlank() -> AsyncImage(
                    model = coverUrl,
                    contentDescription = "专辑封面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                else -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    AppIcon(
                        Icons.Outlined.MusicNote,
                        contentDescription = null,
                        tint = MusicContentColor.copy(alpha = 0.78f),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }
    } else {
        // Apple Music Style: 宽屏卡片（16:10，自适应视频比例）或经典方图（1:1）与氛围弥散阴影
        val isCard = coverStyle == MusicCoverStyle.APPLE_MUSIC_CARD
        val cardAspectRatio = if (isCard) (16f / 10f) else 1f
        val cornerRadius = if (isCard) APPLE_MUSIC_CARD_CORNER_RADIUS_DP.dp else APPLE_MUSIC_COVER_CORNER_RADIUS_DP.dp
        val cornerShape = RoundedCornerShape(cornerRadius)
        val playbackProgress by animateFloatAsState(
            targetValue = if (isPlaying) 1f else 0f,
            animationSpec = if (reduceMotion) {
                snap()
            } else {
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = APPLE_MUSIC_COVER_MOTION_STIFFNESS,
                )
            },
            label = "music_artwork_playback_progress",
        )
        val playingScale = resolveAppleMusicCoverScale(isPlaying = true)
        val pausedScale = resolveAppleMusicCoverScale(isPlaying = false)
        val artworkScale = pausedScale + (playingScale - pausedScale) * playbackProgress
        val shadowElevation = resolveAppleMusicCoverShadowElevation(playbackProgress).dp
        Box(
            modifier = modifier
                // Put the transform before the visual chrome so the artwork, rounded clip,
                // border and shadow settle as one card. A later graphicsLayer only scales the
                // image subtree and leaves the old-size frame behind while pausing.
                .graphicsLayer {
                    scaleX = artworkScale
                    scaleY = artworkScale
                }
                .aspectRatio(cardAspectRatio)
                .shadow(
                    elevation = shadowElevation,
                    shape = cornerShape,
                    ambientColor = artworkShadowColor.copy(alpha = if (isDarkEnvironment) 0.45f else 0.15f),
                    spotColor = artworkShadowColor.copy(alpha = if (isDarkEnvironment) 0.55f else 0.20f)
                )
                .clip(cornerShape)
                .border(
                    width = 1.dp,
                    color = artworkBorderColor.copy(alpha = if (isDarkEnvironment) 0.20f else 0.14f),
                    shape = cornerShape
                )
                .background(
                    artworkFallbackBrush
                )
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            when {
                bitmap != null -> androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = "专辑封面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                coverUrl.isNotBlank() -> AsyncImage(
                    model = coverUrl,
                    contentDescription = "专辑封面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                else -> AppIcon(
                    Icons.Outlined.MusicNote,
                    contentDescription = null,
                    tint = MusicContentColor.copy(alpha = 0.78f),
                    modifier = Modifier.size(if (isCard) 64.dp else 96.dp)
                )
            }
        }
    }
}

@Composable
private fun MusicProgress(
    state: MusicPlayerUiState,
    onSeek: (Long) -> Unit,
    glassEnabled: Boolean,
    glassTintColor: Color = Color.Unspecified,
    isDarkEnvironment: Boolean = true,
    miuixBackdrop: MiuixBackdrop? = null,
    liquidGlassTuning: LiquidGlassTuning = resolveLiquidGlassTuning(progress = 0.5f),
    modifier: Modifier = Modifier
) {
    val duration = state.durationMs.coerceAtLeast(1L)
    var draggedPosition by remember { mutableStateOf<Float?>(null) }
    val sliderValue = draggedPosition ?: state.positionMs.coerceIn(0L, duration).toFloat()
    val onSliderChange: (Float) -> Unit = { draggedPosition = it }
    val onSliderChangeFinished = {
        draggedPosition?.let { onSeek(it.toLong()) }
        draggedPosition = null
    }
    val inactiveTrackColor = lerp(
        glassTintColor.takeOrElse { MaterialTheme.colorScheme.surface },
        MaterialTheme.colorScheme.onSurface,
        if (isDarkEnvironment) 0.34f else 0.22f,
    ).copy(alpha = if (isDarkEnvironment) 0.36f else 0.24f)
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            MusicWavySlider(
                value = sliderValue,
                onValueChange = onSliderChange,
                onValueChangeFinished = onSliderChangeFinished,
                valueRange = 0f..duration.toFloat(),
                wavy = false,
                activeColor = MusicAccentColor,
                inactiveColor = inactiveTrackColor,
                thumbColor = MusicAccentColor,
                modifier = Modifier.height(48.dp),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val displayedPositionMs = draggedPosition?.toLong() ?: state.positionMs
            AppText(
                formatMusicTime(displayedPositionMs),
                color = MusicContentColor.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelSmall
            )
            AppText(
                "-${formatMusicTime((state.durationMs - displayedPositionMs).coerceAtLeast(0L))}",
                color = MusicContentColor.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    state: MusicPlayerUiState,
    onPlayPause: () -> Unit,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    modifier: Modifier = Modifier,
    playButtonSizeDp: Int = 72,
    skipButtonSizeDp: Int = 56,
    isDarkEnvironment: Boolean = true,
    glassTintColor: Color = Color.Unspecified
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlaybackIconButton(
            icon = Icons.Filled.SkipPrevious,
            description = "上一首",
            enabled = state.queueControls.hasPrevious && onPrevious != null,
            onClick = onPrevious ?: {},
            sizeDp = skipButtonSizeDp
        )
        MusicPlayPauseButton(
            state = state,
            onPlayPause = onPlayPause,
            sizeDp = playButtonSizeDp,
            isDarkEnvironment = isDarkEnvironment,
            glassTintColor = glassTintColor,
        )
        PlaybackIconButton(
            icon = Icons.Filled.SkipNext,
            description = "下一首",
            enabled = state.queueControls.hasNext && onNext != null,
            onClick = onNext ?: {},
            sizeDp = skipButtonSizeDp
        )
    }
}

@Composable
private fun PlayerLyricsPreview(
    lyrics: LyricDocument?,
    positionMs: Long,
    onOpenLyrics: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val activeIndex = lyrics?.let { resolveActiveLyricIndex(it, positionMs) } ?: -1
    val lines = lyrics?.lines.orEmpty()
    val prevLine = if (activeIndex > 0 && activeIndex - 1 in lines.indices) lines[activeIndex - 1] else null
    val activeLine = if (activeIndex in lines.indices) lines[activeIndex] else null
    val nextLine1 = if (activeIndex + 1 in lines.indices) lines[activeIndex + 1] else null
    val nextLine2 = if (activeIndex + 2 in lines.indices) lines[activeIndex + 2] else null

    AppSurface(
        onClick = onOpenLyrics ?: {},
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (activeLine != null) {
                // 上一行（淡出弱化呈现）
                if (prevLine != null) {
                    AppText(
                        text = prevLine.text,
                        color = MusicContentColor.copy(alpha = 0.38f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 当前行（醒目高亮）
                AppText(
                    text = activeLine.text,
                    color = MusicAccentColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // 翻译（若有）
                val translation = activeLine.translations.firstOrNull()
                if (!translation.isNullOrBlank()) {
                    AppText(
                        text = translation,
                        color = MusicAccentColor.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 下一行（预览）
                if (nextLine1 != null) {
                    AppText(
                        text = nextLine1.text,
                        color = MusicContentColor.copy(alpha = 0.58f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 再下一行（若无翻译且存在下下句，展示保持 3~4 行层次感）
                if (translation.isNullOrBlank() && nextLine2 != null) {
                    AppText(
                        text = nextLine2.text,
                        color = MusicContentColor.copy(alpha = 0.32f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else if (lyrics != null && lines.isNotEmpty()) {
                val firstLine = lines.firstOrNull()
                val isPrelude = firstLine != null && positionMs < firstLine.startTimeMs
                val hint = if (isPrelude) "··· 前奏 ···" else "··· 间奏 ···"

                AppText(
                    text = hint,
                    color = MusicAccentColor.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                // 前奏时展示前 2~3 句歌词预览
                if (isPrelude) {
                    lines.take(3).forEachIndexed { idx, line ->
                        val alpha = when (idx) {
                            0 -> 0.65f
                            1 -> 0.45f
                            else -> 0.28f
                        }
                        AppText(
                            text = line.text,
                            color = MusicContentColor.copy(alpha = alpha),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    AppIcon(
                        Icons.Outlined.MusicNote,
                        contentDescription = null,
                        tint = MusicContentColor.copy(alpha = 0.78f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    AppText(
                        text = "轻点查看完整歌词",
                        color = MusicContentColor.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaybackIconButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    sizeDp: Int = 56
) {
    AppIconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(sizeDp.dp)) {
        AppIcon(
            imageVector = icon,
            contentDescription = description,
            tint = MusicContentColor.copy(alpha = if (enabled) 1f else 0.28f),
            modifier = Modifier.size(32.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsPage(
    state: MusicPlayerUiState,
    glassEnabled: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    onLyricsOffsetChange: (Long) -> Unit,
    onLyricsRetry: () -> Unit,
    onOpenLyricsSearch: () -> Unit,
    blurEffectsEnabled: Boolean,
    reduceMotion: Boolean,
    glassTintColor: Color,
    liquidGlassTuning: LiquidGlassTuning,
    miuixBackdrop: MiuixBackdrop?,
    progressSeekRevision: Int,
    controlsVisible: Boolean,
    onControlsVisibleChange: (Boolean) -> Unit,
    showBottomControls: Boolean = true,
    isDarkEnvironment: Boolean = true,
    modifier: Modifier = Modifier
) {
    val document = state.lyrics
    val currentIndex = document?.let { resolveActiveLyricIndex(it, state.positionMs) } ?: -1
    val blurEnabled = resolveMusicLyricsBlurEnabled(
        sdkInt = Build.VERSION.SDK_INT,
        effectsEnabled = blurEffectsEnabled,
        reduceMotion = reduceMotion
    )
    val listState = rememberLazyListState()
    val isLyricsDragged by listState.interactionSource.collectIsDraggedAsState()
    var showTranslations by remember { mutableStateOf(true) }
    var showLyricsSettings by remember { mutableStateOf(false) }
    var isAutoFollowPaused by remember(document) { mutableStateOf(false) }
    LaunchedEffect(progressSeekRevision) {
        if (progressSeekRevision > 0) {
            isAutoFollowPaused = false
        }
    }
    LaunchedEffect(isLyricsDragged) {
        if (isLyricsDragged) {
            isAutoFollowPaused = true
        }
    }
    LaunchedEffect(currentIndex, isAutoFollowPaused, reduceMotion) {
        if (currentIndex >= 0 && !isAutoFollowPaused) {
            val focusOffset = resolveLyricFocusScrollOffsetPx(
                listState.layoutInfo.viewportSize.height
            )
            if (reduceMotion) {
                listState.scrollToItem(currentIndex, focusOffset)
            } else {
                listState.animateScrollToItem(currentIndex, focusOffset)
            }
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable { onControlsVisibleChange(!controlsVisible) }
            .padding(top = if (showBottomControls) 72.dp else 16.dp, bottom = 16.dp)
    ) {
        if (document == null || document.lines.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppText(
                    text = when {
                        state.isLyricsSearching -> "正在匹配歌词…"
                        state.lyricsError != null -> "歌词加载失败"
                        else -> "未找到匹配歌词"
                    },
                    color = MusicContentColor.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.headlineSmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassTextButton(
                        label = "重新匹配",
                        glassEnabled = glassEnabled,
                        miuixBackdrop = miuixBackdrop,
                        glassTintColor = glassTintColor,
                        isDarkEnvironment = isDarkEnvironment,
                        onClick = onLyricsRetry
                    )
                    GlassTextButton(
                        label = "手动搜索",
                        glassEnabled = glassEnabled,
                        miuixBackdrop = miuixBackdrop,
                        glassTintColor = glassTintColor,
                        isDarkEnvironment = isDarkEnvironment,
                        onClick = onOpenLyricsSearch
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = if (showBottomControls) 28.dp else 12.dp,
                    top = if (showBottomControls) 120.dp else 24.dp,
                    end = if (showBottomControls) 28.dp else 16.dp,
                    bottom = 260.dp
                ),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                itemsIndexed(document.lines, key = { index, line -> "${line.startTimeMs}:$index" }) { index, line ->
                    LyricLineContent(
                        line = line,
                        isCurrent = index == currentIndex,
                        positionMs = state.positionMs - document.offsetMs,
                        showTranslations = showTranslations,
                        focusStyle = resolveMusicLyricFocusStyle(index, currentIndex, blurEnabled),
                        reduceMotion = reduceMotion,
                        onClick = {
                            isAutoFollowPaused = false
                            onSeek(line.startTimeMs + document.offsetMs)
                        }
                    )
                }
            }
        }

        if (showBottomControls) {
            AnimatedVisibility(
                visible = controlsVisible,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp),
                enter = if (reduceMotion) EnterTransition.None else fadeIn() + slideInVertically { it / 2 },
                exit = if (reduceMotion) ExitTransition.None else fadeOut() + slideOutVertically { it / 2 }
            ) {
                LyricsPrimaryControls(
                    state = state,
                    glassEnabled = glassEnabled,
                    miuixBackdrop = miuixBackdrop,
                    glassTintColor = glassTintColor,
                    liquidGlassTuning = liquidGlassTuning,
                    isDarkEnvironment = isDarkEnvironment,
                    onPlayPause = onPlayPause,
                    onSeek = onSeek,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onOpenSettings = { showLyricsSettings = true },
                    onHideControls = { onControlsVisibleChange(false) }
                )
            }
            if (!controlsVisible) {
                LyricsImmersiveProgress(
                    state = state,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
        } else {
            AnimatedVisibility(
                visible = controlsVisible,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 20.dp, end = 20.dp),
                enter = if (reduceMotion) EnterTransition.None else fadeIn() + slideInVertically { -it / 2 },
                exit = if (reduceMotion) ExitTransition.None else fadeOut() + slideOutVertically { -it / 2 }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isAutoFollowPaused) {
                        GlassTextButton(
                            label = "回到当前歌词",
                            glassEnabled = glassEnabled,
                            miuixBackdrop = miuixBackdrop,
                            glassTintColor = glassTintColor,
                            isDarkEnvironment = isDarkEnvironment,
                            onClick = { isAutoFollowPaused = false }
                        )
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassTextButton(
                            label = if (showTranslations) "译:开" else "译:关",
                            glassEnabled = glassEnabled,
                            miuixBackdrop = miuixBackdrop,
                            glassTintColor = glassTintColor,
                            isDarkEnvironment = isDarkEnvironment,
                            onClick = { showTranslations = !showTranslations }
                        )
                        GlassTextButton(
                            label = "搜索",
                            glassEnabled = glassEnabled,
                            miuixBackdrop = miuixBackdrop,
                            glassTintColor = glassTintColor,
                            isDarkEnvironment = isDarkEnvironment,
                            onClick = onOpenLyricsSearch
                        )
                        GlassTextButton(
                            label = "歌词设置",
                            glassEnabled = glassEnabled,
                            miuixBackdrop = miuixBackdrop,
                            glassTintColor = glassTintColor,
                            isDarkEnvironment = isDarkEnvironment,
                            onClick = { showLyricsSettings = true }
                        )
                    }
                }
            }
        }
        if (showBottomControls && isAutoFollowPaused && controlsVisible) {
            GlassTextButton(
                label = "回到当前歌词",
                glassEnabled = glassEnabled,
                miuixBackdrop = miuixBackdrop,
                glassTintColor = glassTintColor,
                isDarkEnvironment = isDarkEnvironment,
                onClick = { isAutoFollowPaused = false },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        }
    }

    if (showLyricsSettings) {
        val sheetContentColor = MaterialTheme.colorScheme.onSurface
        val sheetSecondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
        AppModalBottomSheet(
            onDismissRequest = { showLyricsSettings = false },
            containerColor = AppSurfaceTokens.surface(),
            contentColor = sheetContentColor
        ) {
            LyricsSettingsContent(
                showTranslations = showTranslations,
                lyricsOffsetMs = document?.offsetMs ?: 0L,
                sourceLabel = BiliSubtitleLyricsPolicy.resolveSourceLabel(document),
                contentColor = sheetContentColor,
                secondaryColor = sheetSecondaryColor,
                onToggleTranslations = { showTranslations = !showTranslations },
                onLyricsOffsetChange = onLyricsOffsetChange,
                onLyricsRetry = onLyricsRetry,
                onOpenLyricsSearch = {
                    showLyricsSettings = false
                    onOpenLyricsSearch()
                }
            )
        }
    }
}

@Composable
private fun LyricsPrimaryControls(
    state: MusicPlayerUiState,
    glassEnabled: Boolean,
    miuixBackdrop: MiuixBackdrop?,
    glassTintColor: Color,
    liquidGlassTuning: LiquidGlassTuning,
    isDarkEnvironment: Boolean = true,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    onOpenSettings: () -> Unit,
    onHideControls: () -> Unit
) {
    val chromeSpec = resolveMusicPlayerChromeSpec(
        uiStyle = LocalAppUiStyle.current,
        glassEnabled = glassEnabled
    )
    val panelColor = resolveMusicImmersivePanelColor(
        glassTintColor,
        MaterialTheme.colorScheme.surface,
    )
    val (themeOnLight, themeOnDark) = resolveMusicPlayerThemeContentColors()
    val panelContentColor = resolveMusicPlayerContentColor(
        backgroundColor = panelColor,
        onLightBackground = themeOnLight,
        onDarkBackground = themeOnDark,
    )
    val panelShape = AppShapes.borderedContainer(ContainerLevel.Card)
    val panelMaterial = LocalMusicPlayerMaterial.current.copy(
        surfaceColor = panelColor,
        contentColor = panelContentColor,
    )
    AppSurface(
        modifier = Modifier
            .fillMaxWidth()
            .biliPaiFloatingDockShell(
                backdrop = miuixBackdrop,
                containerColor = panelColor,
                pressProgress = 0f,
                shape = panelShape,
                enabled = glassEnabled,
                blurEnabled = !glassEnabled,
                liquidGlassTuning = liquidGlassTuning,
            ),
        shape = panelShape,
        // color = Color.Transparent
        color = if (miuixBackdrop != null) Color.Transparent else panelColor,
        contentColor = panelContentColor,
        tonalElevation = if (
            chromeSpec.uiStyle == com.android.purebilibili.core.theme.AppUiStyle.MATERIAL3 &&
            miuixBackdrop == null
        ) {
            1.dp
        } else {
            0.dp
        }
    ) {
        CompositionLocalProvider(LocalMusicPlayerMaterial provides panelMaterial) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MusicProgress(
                    state = state,
                    onSeek = onSeek,
                    glassEnabled = glassEnabled,
                    glassTintColor = glassTintColor,
                    isDarkEnvironment = isDarkEnvironment,
                    miuixBackdrop = miuixBackdrop,
                    liquidGlassTuning = liquidGlassTuning,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlaybackControls(
                        state = state,
                        onPlayPause = onPlayPause,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        modifier = Modifier.weight(1f),
                        playButtonSizeDp = chromeSpec.playButtonSizeDp,
                        skipButtonSizeDp = chromeSpec.skipButtonSizeDp,
                        isDarkEnvironment = isDarkEnvironment,
                        glassTintColor = glassTintColor
                    )
                    AppTextButton(onClick = onOpenSettings, modifier = Modifier.height(48.dp)) {
                        AppText("歌词设置", color = MusicContentColor, style = MaterialTheme.typography.labelMedium)
                    }
                    AppTextButton(onClick = onHideControls, modifier = Modifier.height(48.dp)) {
                        AppText("收起", color = MusicContentColor, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

private fun formatLyricsOffset(offsetMs: Long): String {
    if (offsetMs == 0L) return "校正 0.00s"
    val absoluteMs = kotlin.math.abs(offsetMs)
    val seconds = absoluteMs / 1_000L
    val hundredths = (absoluteMs % 1_000L) / 10L
    val sign = if (offsetMs > 0L) "+" else "-"
    return "校正 $sign$seconds.${hundredths.toString().padStart(2, '0')}s"
}

@Composable
private fun LyricsImmersiveProgress(
    state: MusicPlayerUiState,
    modifier: Modifier = Modifier
) {
    val duration = state.durationMs.coerceAtLeast(1L)
    AppLinearProgressIndicator(
        progress = { state.positionMs.coerceIn(0L, duration).toFloat() / duration.toFloat() },
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp),
        color = MusicContentColor,
        trackColor = MusicContentColor.copy(alpha = 0.22f)
    )
}

@Composable
private fun MusicPlayPauseButton(
    state: MusicPlayerUiState,
    onPlayPause: () -> Unit,
    sizeDp: Int,
    isDarkEnvironment: Boolean,
    glassTintColor: Color,
    modifier: Modifier = Modifier,
) {
    // Keep the standalone landscape button visually identical to the main player control.
    val playButtonBg = resolveMusicGlassContainerColor(glassTintColor, isDarkEnvironment)
        .copy(alpha = if (isDarkEnvironment) 0.62f else 0.52f)
    val playButtonFg = MusicContentColor
    val playButtonBorder = resolveMusicGlassBorderColor(glassTintColor, isDarkEnvironment)
        .copy(alpha = 0.62f)
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .drawBehind {
                val radius = size.minDimension / 2f
                drawCircle(color = playButtonBg, radius = radius)
                drawCircle(
                    color = playButtonBorder,
                    radius = radius - 0.8.dp.toPx(),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onPlayPause,
            ),
        contentAlignment = Alignment.Center
    ) {
        if (state.isBuffering) {
            AppCircularProgressIndicator(
                color = playButtonFg,
                modifier = Modifier.size((sizeDp * 0.45f).dp)
            )
        } else {
            AppIcon(
                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (state.isPlaying) "暂停" else "播放",
                tint = playButtonFg,
                modifier = Modifier.size((sizeDp * 0.45f).dp)
            )
        }
    }
}

@Composable
private fun LyricsSettingsContent(
    showTranslations: Boolean,
    lyricsOffsetMs: Long,
    sourceLabel: String = "",
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    secondaryColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onToggleTranslations: () -> Unit,
    onLyricsOffsetChange: (Long) -> Unit,
    onLyricsRetry: () -> Unit,
    onOpenLyricsSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppText(
            "歌词设置",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
        if (sourceLabel.isNotBlank()) {
            AppText(
                "当前来源 · $sourceLabel",
                color = secondaryColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        MusicActionSheetItem(
            if (showTranslations) "隐藏翻译与罗马音" else "显示翻译与罗马音",
            contentColor = contentColor,
            onClick = onToggleTranslations
        )
        AppText(
            "歌词时间校正 · ${formatLyricsOffset(lyricsOffsetMs)}",
            color = secondaryColor
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTextButton(onClick = { onLyricsOffsetChange(-250L) }, modifier = Modifier.height(48.dp)) {
                AppText("歌词提前 0.25 秒", color = contentColor)
            }
            AppTextButton(onClick = { onLyricsOffsetChange(250L) }, modifier = Modifier.height(48.dp)) {
                AppText("歌词延后 0.25 秒", color = contentColor)
            }
        }
        AppTextButton(onClick = { onLyricsOffsetChange(-lyricsOffsetMs) }, modifier = Modifier.height(48.dp)) {
            AppText("重置歌词时间", color = contentColor)
        }
        MusicActionSheetItem("重新匹配歌词", contentColor = contentColor, onClick = onLyricsRetry)
        MusicActionSheetItem("手动搜索歌词", contentColor = contentColor, onClick = onOpenLyricsSearch)
    }
}

@Composable
private fun LyricLineContent(
    line: LyricLine,
    isCurrent: Boolean,
    positionMs: Long,
    showTranslations: Boolean,
    focusStyle: MusicLyricFocusStyle,
    reduceMotion: Boolean,
    onClick: () -> Unit
) {
    val transition = updateTransition(targetState = focusStyle, label = "lyric_focus")
    val blurRadius = transition.animateDp(
        transitionSpec = { if (reduceMotion) snap() else AppMotionTokens.standardSpec() },
        label = "lyric_blur"
    ) { it.blurRadiusDp.dp }
    val alpha = transition.animateFloat(
        transitionSpec = { if (reduceMotion) snap() else AppMotionTokens.standardSpec() },
        label = "lyric_alpha"
    ) { it.alphaPercent / 100f }
    val focusModifier = if (Build.VERSION.SDK_INT >= 31 && blurRadius.value > 0.dp) {
        Modifier.blur(blurRadius.value, edgeTreatment = BlurredEdgeTreatment.Unbounded)
    } else {
        Modifier
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(focusModifier)
            .graphicsLayer { this.alpha = alpha.value }
            .clickable(onClick = onClick)
    ) {
        AppText(
            text = buildLyricText(line, isCurrent, positionMs, MusicContentColor),
            color = MusicContentColor,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            lineHeight = 34.sp
        )
        line.translations.firstOrNull()?.takeIf { showTranslations && it.isNotBlank() }?.let {
            AppText(
                text = it,
                color = MusicContentColor.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 5.dp)
            )
        }
        line.romanization?.takeIf { showTranslations && it.isNotBlank() }?.let {
            AppText(
                text = it,
                color = MusicContentColor.copy(alpha = 0.58f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

private fun buildLyricText(
    line: LyricLine,
    isCurrent: Boolean,
    positionMs: Long,
    contentColor: Color,
): AnnotatedString {
    if (!isCurrent || line.spans.isEmpty()) return AnnotatedString(line.text)
    return buildAnnotatedString {
        line.spans.forEach { span ->
            val active = positionMs >= span.startTimeMs
            pushStyle(SpanStyle(color = contentColor.copy(alpha = if (active) 1f else 0.38f)))
            append(span.text)
            pop()
        }
    }
}

@Composable
private fun MusicTopBar(
    glassEnabled: Boolean,
    miuixBackdrop: MiuixBackdrop?,
    liquidGlassTuning: LiquidGlassTuning,
    glassTintColor: Color = Color.Unspecified,
    isDarkEnvironment: Boolean = true,
    onBack: () -> Unit,
    onMore: () -> Unit,
    actionsPopup: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onToggleLayout: (() -> Unit)? = null,
    layoutActionLabel: String = "切换布局",
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassIconButton(
            icon = Icons.Outlined.KeyboardArrowDown,
            description = "返回",
            glassEnabled = glassEnabled,
            miuixBackdrop = miuixBackdrop,
            liquidGlassTuning = liquidGlassTuning,
            glassTintColor = glassTintColor,
            isDarkEnvironment = isDarkEnvironment,
            onClick = onBack
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (onToggleLayout != null) {
                GlassIconButton(
                    icon = Icons.Outlined.QueueMusic,
                    description = layoutActionLabel,
                    glassEnabled = glassEnabled,
                    miuixBackdrop = miuixBackdrop,
                    liquidGlassTuning = liquidGlassTuning,
                    glassTintColor = glassTintColor,
                    isDarkEnvironment = isDarkEnvironment,
                    onClick = onToggleLayout
                )
            }
            Box {
                GlassIconButton(
                    icon = Icons.Outlined.MoreHoriz,
                    description = "更多操作",
                    glassEnabled = glassEnabled,
                    miuixBackdrop = miuixBackdrop,
                    liquidGlassTuning = liquidGlassTuning,
                    glassTintColor = glassTintColor,
                    isDarkEnvironment = isDarkEnvironment,
                    onClick = onMore
                )
                actionsPopup()
            }
        }
    }
}

@Composable
private fun GlassIconButton(
    icon: ImageVector,
    description: String,
    glassEnabled: Boolean,
    miuixBackdrop: MiuixBackdrop?,
    liquidGlassTuning: LiquidGlassTuning,
    glassTintColor: Color = Color.Unspecified,
    isDarkEnvironment: Boolean = true,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val dragX = remember { Animatable(0f) }
    val dragY = remember { Animatable(0f) }
    val maxDragPx = with(LocalDensity.current) { 36.dp.toPx() }
    val expansionPx = with(LocalDensity.current) { 4.dp.toPx() }
    val releaseSpec = remember {
        spring<Float>(
            dampingRatio = 0.5f,
            stiffness = 300f,
        )
    }

    AppIconButton(
        onClick = onClick,
        modifier = Modifier
            .graphicsLayer {
                val transform = resolveMusicTopControlTransform(
                    dragX = dragX.value,
                    dragY = dragY.value,
                    maxDragPx = maxDragPx,
                    widthPx = size.width,
                    heightPx = size.height,
                    expansionPx = expansionPx,
                )
                scaleX = transform.scaleX
                scaleY = transform.scaleY
                translationX = transform.translationX
                translationY = transform.translationY
            }
            .pointerInput(maxDragPx, releaseSpec) {
                detectDragGestures(
                    onDragCancel = {
                        scope.launch {
                            launch { dragX.animateTo(0f, releaseSpec) }
                            launch { dragY.animateTo(0f, releaseSpec) }
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            launch { dragX.animateTo(0f, releaseSpec) }
                            launch { dragY.animateTo(0f, releaseSpec) }
                        }
                    },
                ) { change, dragAmount ->
                    change.consume()
                    scope.launch {
                        dragX.snapTo((dragX.value + dragAmount.x).coerceIn(-maxDragPx, maxDragPx))
                        dragY.snapTo((dragY.value + dragAmount.y).coerceIn(-maxDragPx, maxDragPx))
                    }
                }
            }
            .biliPaiFloatingDockShell(
                backdrop = miuixBackdrop,
                containerColor = resolveMusicGlassContainerColor(glassTintColor, isDarkEnvironment),
                pressProgress = 0f,
                shape = CircleShape,
                enabled = glassEnabled,
                blurEnabled = !glassEnabled,
                liquidGlassTuning = liquidGlassTuning,
            )
            .border(
                width = 0.5.dp,
                color = resolveMusicGlassBorderColor(glassTintColor, isDarkEnvironment),
                shape = CircleShape
            )
    ) {
        AppIcon(
            icon,
            contentDescription = description,
            tint = MusicContentColor,
        )
    }
}

@Composable
private fun GlassTextButton(
    label: String,
    glassEnabled: Boolean,
    miuixBackdrop: MiuixBackdrop?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    glassTintColor: Color = Color.Unspecified,
    isDarkEnvironment: Boolean = true,
    liquidGlassTuning: LiquidGlassTuning = resolveLiquidGlassTuning(progress = 0.5f),
) {
    val shape = CircleShape
    val containerColor = if (isSelected) {
        MusicAccentColor.copy(alpha = 0.26f)
    } else {
        resolveMusicGlassContainerColor(glassTintColor, isDarkEnvironment)
    }
    val borderColor = if (isSelected) {
        MusicAccentColor.copy(alpha = 0.40f)
    } else {
        resolveMusicGlassBorderColor(glassTintColor, isDarkEnvironment)
    }
    val textColor = if (isSelected) {
        MusicAccentColor
    } else {
        MusicContentColor
    }
    Box(
        modifier = modifier
            .height(48.dp)
            .biliPaiFloatingDockShell(
                backdrop = miuixBackdrop,
                containerColor = containerColor,
                pressProgress = 0f,
                shape = shape,
                enabled = glassEnabled,
                blurEnabled = !glassEnabled,
                liquidGlassTuning = liquidGlassTuning,
            )
            .border(0.8.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        AppText(
            label,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun TabletopPlayerLayout(
    coverStyle: MusicCoverStyle,
    state: MusicPlayerUiState,
    queue: List<MusicQueueItemUi>,
    currentIndex: Int,
    artworkBitmap: ImageBitmap?,
    glassEnabled: Boolean,
    reduceMotion: Boolean,
    lyricsBlurEffectsEnabled: Boolean,
    backgroundColor: Color,
    musicBackdrop: MiuixBackdrop?,
    liquidGlassTuning: LiquidGlassTuning,
    progressSeekRevision: Int,
    isLiked: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    onQueueItemSelected: (Int) -> Unit,
    onLikeClick: (() -> Unit)?,
    onToggleCoverStyle: () -> Unit,
    onLyricsOffsetChange: (Long) -> Unit,
    onLyricsRetry: () -> Unit,
    onOpenLyricsSearch: () -> Unit,
    availableWidthDp: Int,
    hingeStartDp: Int? = null,
    hingeEndDp: Int? = null,
    isDarkEnvironment: Boolean = true,
    modifier: Modifier = Modifier
) {
    val tabletopDensity = LocalDensity.current
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 40.dp, bottom = 4.dp)
    ) {
        val contentHeightDp = maxHeight.value.roundToInt()
        val topChromeOffsetDp = with(tabletopDensity) {
            WindowInsets.statusBars.getTop(this).toDp().value.roundToInt()
        } + 40
        val paneSizes = resolveMusicTabletopPaneSizes(
            availableHeightDp = contentHeightDp,
            hingeStartDp = hingeStartDp?.minus(topChromeOffsetDp),
            hingeEndDp = hingeEndDp?.minus(topChromeOffsetDp),
        )
        Column(modifier = Modifier.fillMaxSize()) {
        // 上半部分（观赏区）：左侧封面 + 右侧滚动歌词
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(paneSizes.upperHeightDp.dp)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 封面在独立左栏内居中，与歌词保留稳定间距。
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                val artworkSize = minOf(
                    (((availableWidthDp - 68) / 2) * 0.88f).toInt().coerceAtLeast(0),
                    (paneSizes.upperHeightDp - 24).coerceAtLeast(0),
                    340
                ).coerceAtLeast(0)

                MusicArtwork(
                    coverUrl = state.coverUrl,
                    bitmap = artworkBitmap,
                    isPlaying = state.isPlaying,
                    rotate = state.isPlaying && !reduceMotion,
                    playbackSpeed = state.playbackSpeed,
                    coverStyle = coverStyle,
                    reduceMotion = reduceMotion,
                    shape = CircleShape,
                    isDarkEnvironment = isDarkEnvironment,
                    onClick = onToggleCoverStyle,
                    modifier = Modifier.width(artworkSize.dp)
                )
            }

            // 右侧歌词
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                LyricsPage(
                    state = state,
                    glassEnabled = glassEnabled,
                    onPlayPause = onPlayPause,
                    onSeek = onSeek,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onLyricsOffsetChange = onLyricsOffsetChange,
                    onLyricsRetry = onLyricsRetry,
                    onOpenLyricsSearch = onOpenLyricsSearch,
                    blurEffectsEnabled = lyricsBlurEffectsEnabled,
                    reduceMotion = reduceMotion,
                    glassTintColor = backgroundColor,
                    isDarkEnvironment = isDarkEnvironment,
                    liquidGlassTuning = liquidGlassTuning,
                    miuixBackdrop = musicBackdrop,
                    progressSeekRevision = progressSeekRevision,
                    controlsVisible = false,
                    onControlsVisibleChange = {},
                    showBottomControls = false,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (paneSizes.hingeGapDp > 0) {
            Spacer(Modifier.height(paneSizes.hingeGapDp.dp))
        }

        // 下半部分：同宽进度与控制区，下方展开唱片架。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(paneSizes.lowerHeightDp.dp),
            contentAlignment = Alignment.Center
        ) {
            val cardSizeDp = minOf(
                (availableWidthDp * 0.24f).toInt(),
                215
            ).coerceAtLeast(0)

            Music3DCoverFlow(
                queue = queue,
                currentIndex = currentIndex,
                isPlaying = state.isPlaying,
                onItemClick = onQueueItemSelected,
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                isLiked = isLiked,
                onLikeClick = onLikeClick,
                cardSizeDp = cardSizeDp,
                // 让细进度轨道接近截图中的内容宽度，同时给左右保留呼吸空间。
                controlsWidthDp = (availableWidthDp * 0.80f).toInt().coerceIn(320, 920),
                showTransportControls = true,
                shelfBelowControls = true,
                progressContent = {
                    MusicProgress(
                        state = state,
                        onSeek = onSeek,
                glassEnabled = glassEnabled,
                        glassTintColor = backgroundColor,
                        isDarkEnvironment = isDarkEnvironment,
                        miuixBackdrop = musicBackdrop,
                        liquidGlassTuning = liquidGlassTuning,
                    )
                },
                glassEnabled = glassEnabled,
                miuixBackdrop = musicBackdrop,
                liquidGlassTuning = liquidGlassTuning,
                glassTintColor = backgroundColor,
                isDarkEnvironment = isDarkEnvironment,
                reduceMotion = reduceMotion,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 6.dp)
            )
        }
        }
    }
}

@Composable
private fun ExpandedQueuePane(
    queue: List<MusicQueueItemUi>,
    currentIndex: Int,
    onItemClick: (Int) -> Unit,
    onClose: () -> Unit,
    glassEnabled: Boolean,
    reduceMotion: Boolean,
    miuixBackdrop: MiuixBackdrop?,
    glassTintColor: Color,
    isDarkEnvironment: Boolean = true,
    liquidGlassTuning: LiquidGlassTuning,
    isPlaying: Boolean = false,
    onPlayPause: () -> Unit = {},
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    isLiked: Boolean = false,
    onLikeClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isCoverFlowView by remember { mutableStateOf(true) }
    val panelShape = AppShapes.borderedContainer(ContainerLevel.Card)
    val panelColor = resolveMusicImmersivePanelColor(
        glassTintColor,
        MaterialTheme.colorScheme.surface,
    )
    AppSurface(
        shape = panelShape,
        color = if (miuixBackdrop != null) Color.Transparent else panelColor,
        contentColor = MusicContentColor,
        border = BorderStroke(1.dp, resolveMusicGlassBorderColor(glassTintColor, isDarkEnvironment)),
        modifier = modifier
            .fillMaxSize()
            .biliPaiFloatingDockShell(
                backdrop = miuixBackdrop,
                containerColor = panelColor,
                pressProgress = 0f,
                shape = panelShape,
                enabled = glassEnabled,
                blurEnabled = !glassEnabled,
                liquidGlassTuning = liquidGlassTuning
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppText(
                        text = if (queue.isNotEmpty()) "待播清单 (${queue.size})" else "待播清单",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MusicContentColor
                    )
                    if (queue.isNotEmpty()) {
                        GlassTextButton(
                            label = if (isCoverFlowView) "3D 唱片架" else "列表",
                            isSelected = true,
                            glassEnabled = glassEnabled,
                            miuixBackdrop = miuixBackdrop,
                            glassTintColor = glassTintColor,
                            isDarkEnvironment = isDarkEnvironment,
                            onClick = { isCoverFlowView = !isCoverFlowView }
                        )
                    }
                }
                GlassTextButton(
                    label = "返回歌词",
                    glassEnabled = glassEnabled,
                    miuixBackdrop = miuixBackdrop,
                    glassTintColor = glassTintColor,
                    isDarkEnvironment = isDarkEnvironment,
                    onClick = onClose
                )
            }
            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AppText(
                        text = "待播清单为空",
                        color = MusicContentColor.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else if (isCoverFlowView) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 6.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val availableWidth = maxWidth
                    val availableHeight = maxHeight
                    val adaptiveCardSizeDp = minOf(
                        (availableWidth.value * 0.52f).toInt(),
                        (availableHeight.value * 0.46f).toInt(),
                        230
                    ).coerceAtLeast(165)

                    Music3DCoverFlow(
                        queue = queue,
                        currentIndex = currentIndex,
                        isPlaying = isPlaying,
                        onItemClick = onItemClick,
                        onPlayPause = onPlayPause,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        isLiked = isLiked,
                        onLikeClick = onLikeClick,
                        cardSizeDp = adaptiveCardSizeDp,
                        showTransportControls = false,
                        glassEnabled = glassEnabled,
                        miuixBackdrop = miuixBackdrop,
                        liquidGlassTuning = liquidGlassTuning,
                        glassTintColor = glassTintColor,
                        isDarkEnvironment = isDarkEnvironment,
                        reduceMotion = reduceMotion,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(queue, key = { _, item -> item.stableId }) { index, item ->
                        val isPlayingItem = index == currentIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isPlayingItem) MusicAccentColor.copy(alpha = 0.16f) else Color.Transparent
                                )
                                .clickable { onItemClick(index) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = item.coverUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                AppText(
                                    text = item.title,
                                    color = if (isPlayingItem) MusicAccentColor else MusicContentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (isPlayingItem) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(Modifier.height(2.dp))
                                AppText(
                                    text = item.artist.ifBlank { "未知艺术家" },
                                    color = if (isPlayingItem) MusicAccentColor.copy(alpha = 0.78f) else MusicContentColor.copy(alpha = 0.65f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (isPlayingItem) {
                                Spacer(Modifier.width(8.dp))
                                AppIcon(
                                    Icons.Outlined.MusicNote,
                                    contentDescription = "正在播放",
                                    tint = MusicAccentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicActionSheetItem(
    label: String,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        AppText(label, color = contentColor, style = MaterialTheme.typography.bodyLarge)
    }
}

private suspend fun loadMusicArtwork(
    imageLoader: ImageLoader,
    coverUrl: String,
    context: android.content.Context
): Pair<ImageBitmap, Color>? = withContext(Dispatchers.IO) {
    if (coverUrl.isBlank()) return@withContext null
    runCatching {
        val request = ImageRequest.Builder(context)
            .data(coverUrl)
            .allowHardware(false)
            .size(512, 512)
            .build()
        val result = imageLoader.execute(request) as SuccessResult
        val bitmap = (result.image as coil3.BitmapImage).bitmap
        val palette = Palette.from(bitmap).clearFilters().generate()
        val colorInt = palette.dominantSwatch?.rgb
            ?: palette.vibrantSwatch?.rgb
            ?: palette.lightVibrantSwatch?.rgb
            ?: palette.mutedSwatch?.rgb
            ?: bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
        bitmap.asImageBitmap() to Color(colorInt)
    }.getOrNull()
}

internal fun formatMusicTime(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / 1000L
    return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
