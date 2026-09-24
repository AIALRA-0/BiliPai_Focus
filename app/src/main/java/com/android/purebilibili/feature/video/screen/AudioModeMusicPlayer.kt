@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.android.purebilibili.feature.video.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import com.android.purebilibili.core.ui.AdaptiveLoadingIndicator
import com.android.purebilibili.core.ui.AppSurfaceTokens
import androidx.compose.material3.ExperimentalMaterial3Api
import com.android.purebilibili.core.ui.AppModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.android.purebilibili.core.player.PlayerVolumeController
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.Page
import com.android.purebilibili.feature.audio.lyrics.BiliSubtitleLyricsPolicy
import com.android.purebilibili.feature.audio.player.AudioNowPlayingSession
import com.android.purebilibili.feature.audio.player.MusicPlayerUiState
import com.android.purebilibili.feature.audio.player.MusicLyricCandidateUi
import com.android.purebilibili.feature.audio.player.MusicQueueItemUi
import com.android.purebilibili.feature.audio.screen.MusicPlayerContent
import com.android.purebilibili.feature.audio.viewmodel.MusicViewModel
import com.android.purebilibili.feature.video.player.MiniPlayerManager
import com.android.purebilibili.feature.video.player.PlaylistManager
import com.android.purebilibili.feature.video.playback.audio.resolveAudioQualityControlPresentation
import com.android.purebilibili.feature.video.share.VideoShareSheet
import com.android.purebilibili.feature.video.share.buildVideoSharePayload
import com.android.purebilibili.feature.video.ui.components.CollectionSheet
import com.android.purebilibili.feature.video.ui.components.PagesSelector
import com.android.purebilibili.feature.video.ui.components.PlaybackSpeed
import com.android.purebilibili.feature.video.ui.components.SpeedSelectionMenuDialog
import com.android.purebilibili.feature.video.ui.components.VideoCommentSheetHost
import com.android.purebilibili.feature.video.viewmodel.VideoCommentViewModel
import com.android.purebilibili.feature.video.viewmodel.VideoEngagementViewModel
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackUiState
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private data class AudioPlaybackSnapshot(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1f
)

internal fun resolveAudioModeTrackTitle(
    videoTitle: String,
    currentCid: Long,
    pages: List<Page>
): String {
    return pages.firstOrNull { it.cid == currentCid }
        ?.part
        ?.takeIf { it.isNotBlank() }
        ?: videoTitle
}

internal data class AudioModeLyricMetadata(
    val title: String,
    val artist: String
)

internal fun resolveAudioModeLyricMetadata(
    trackTitle: String,
    fallbackArtist: String
): AudioModeLyricMetadata {
    val match = Regex(
        """^\s*(?:P?\d{1,4}\s*[.、:：_-]\s*)?(.+?)\s+[-–—]\s+(.+?)\s*$""",
        RegexOption.IGNORE_CASE
    ).matchEntire(trackTitle)
    return AudioModeLyricMetadata(
        title = match?.groupValues?.getOrNull(1)?.trim().orEmpty().ifBlank { trackTitle },
        artist = match?.groupValues?.getOrNull(2)?.trim().orEmpty().ifBlank { fallbackArtist }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AudioModeMusicPlayer(
    viewModel: VideoPlaybackViewModel,
    successState: VideoPlaybackUiState.Success?,
    player: Player?,
    onBack: () -> Unit,
    onVideoModeClick: (String, Long) -> Unit,
    isInPipMode: Boolean,
    showPipButton: Boolean,
    onEnterPip: () -> Unit,
    sleepTimerMinutes: Int?,
    titleOverride: String?,
    liquidGlassEffectsEnabled: Boolean,
    onToggleOrientation: (() -> Unit)? = null,
    orientationActionLabel: String = "横屏",
    engagementViewModel: VideoEngagementViewModel = viewModel()
) {
    if (successState == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppSurfaceTokens.background()),
            contentAlignment = Alignment.Center
        ) {
            AdaptiveLoadingIndicator(color = Color.White)
        }
        return
    }

    val context = LocalContext.current
    val info = successState.info
    val displayTitle = resolveAudioModeTrackTitle(
        videoTitle = titleOverride?.takeIf { it.isNotBlank() } ?: info.title,
        currentCid = info.cid,
        pages = info.pages
    )
    val playlist by PlaylistManager.playlist.collectAsStateWithLifecycle()
    val playlistIndex by PlaylistManager.currentIndex.collectAsStateWithLifecycle()
    val playMode by PlaylistManager.playMode.collectAsStateWithLifecycle()
    val shuffleEnabled by PlaylistManager.shuffleEnabled.collectAsStateWithLifecycle()
    val playback = rememberAudioPlaybackSnapshot(player)
    val lyricsViewModel = androidx.lifecycle.viewmodel.compose.viewModel<MusicViewModel>(
        key = "audio_mode_lyrics"
    )
    val lyricsState by lyricsViewModel.uiState.collectAsStateWithLifecycle()
    var showCollectionSheet by remember { mutableStateOf(false) }
    var showPageSelector by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showComments by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }
    val engagementState by engagementViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(engagementViewModel) {
        engagementViewModel.events.collect { event ->
            if (event is com.android.purebilibili.feature.video.viewmodel.VideoEngagementEvent.Message) {
                viewModel.toast(event.text)
            }
        }
    }
    val commentViewModel: VideoCommentViewModel = viewModel()
    val currentSpeed = player?.playbackParameters?.speed ?: 1f

    val metadataDurationMs = info.pages
        .firstOrNull { it.cid == info.cid }
        ?.duration
        ?.times(1_000L)
        ?: 0L
    val lyricsDurationMs = playback.durationMs.takeIf { it > 0L } ?: metadataDurationMs
    val lyricMetadata = remember(displayTitle, info.owner.name) {
        resolveAudioModeLyricMetadata(displayTitle, info.owner.name)
    }

    LaunchedEffect(Unit) {
        lyricsViewModel.initPlayer(context)
    }
    LaunchedEffect(info.bvid, info.cid, displayTitle, info.owner.name, lyricsDurationMs) {
        lyricsViewModel.loadLyricsForVideo(
            title = lyricMetadata.title,
            artist = lyricMetadata.artist,
            bvid = info.bvid,
            cid = info.cid,
            durationMs = lyricsDurationMs
        )
    }

    val queue = if (playlist.isEmpty()) {
        if (info.pages.size > 1) {
            info.pages.mapIndexed { index, page ->
                MusicQueueItemUi(
                    stableId = "video:${info.bvid}:${page.cid}",
                    title = if (page.part.isNotBlank()) page.part else "${displayTitle} P${page.page}",
                    artist = info.owner.name,
                    coverUrl = FormatUtils.fixImageUrl(info.pic)
                )
            }
        } else if (successState.related.isNotEmpty()) {
            val currentItem = MusicQueueItemUi(
                stableId = "video:${info.bvid}:${info.cid}",
                title = displayTitle,
                artist = info.owner.name,
                coverUrl = FormatUtils.fixImageUrl(info.pic)
            )
            listOf(currentItem) + successState.related.take(10).map { rel ->
                MusicQueueItemUi(
                    stableId = "video:${rel.bvid}:${rel.cid}",
                    title = rel.title,
                    artist = rel.owner.name,
                    coverUrl = FormatUtils.fixImageUrl(rel.pic)
                )
            }
        } else {
            listOf(
                MusicQueueItemUi(
                    stableId = "video:${info.bvid}:${info.cid}",
                    title = displayTitle,
                    artist = info.owner.name,
                    coverUrl = FormatUtils.fixImageUrl(info.pic)
                )
            )
        }
    } else {
        playlist.mapIndexed { index, item ->
            MusicQueueItemUi(
                stableId = "video:${item.bvid}:$index",
                title = item.title,
                artist = item.owner,
                coverUrl = FormatUtils.fixImageUrl(item.cover)
            )
        }
    }
    val currentIndex = if (playlist.isNotEmpty()) {
        playlistIndex.takeIf { it in queue.indices } ?: 0
    } else if (info.pages.size > 1) {
        info.pages.indexOfFirst { it.cid == info.cid }.takeIf { it >= 0 } ?: 0
    } else {
        0
    }
    val coverUrl = queue.getOrNull(currentIndex)?.coverUrl ?: FormatUtils.fixImageUrl(info.pic)
    val audioNowPlayingBarEnabled by SettingsManager
        .getAudioNowPlayingBarEnabled(context)
        .collectAsStateWithLifecycle(initialValue = true)
    LaunchedEffect(player, info.bvid, info.cid, displayTitle, coverUrl, audioNowPlayingBarEnabled) {
        if (!audioNowPlayingBarEnabled) return@LaunchedEffect
        val exoPlayer = player as? ExoPlayer ?: return@LaunchedEffect
        MiniPlayerManager.getInstance(context).setVideoInfo(
            bvid = info.bvid,
            title = displayTitle,
            cover = coverUrl,
            owner = info.owner.name,
            cid = info.cid,
            aid = info.aid,
            externalPlayer = exoPlayer
        )
    }
    val audioQualityPresentation = remember(
        successState.availableAudioQualities,
        successState.selectedAudioQuality
    ) {
        resolveAudioQualityControlPresentation(
            options = successState.availableAudioQualities,
            selectedAudioQuality = successState.selectedAudioQuality
        )
    }

    val subtitleLyrics = remember(
        successState.subtitlePrimaryCues,
        successState.subtitleSecondaryCues,
        successState.subtitlePrimaryLikelyAi,
        successState.subtitlePrimaryLanguage
    ) {
        BiliSubtitleLyricsPolicy.convertSubtitlesToLyricDocument(
            primaryCues = successState.subtitlePrimaryCues,
            secondaryCues = successState.subtitleSecondaryCues,
            isAiGenerated = successState.subtitlePrimaryLikelyAi,
            languageLabel = successState.subtitlePrimaryLanguage
        )
    }

    val effectiveLyrics = remember(lyricsState.lyricsDocument, subtitleLyrics) {
        BiliSubtitleLyricsPolicy.resolveEffectiveLyrics(
            musicLyrics = lyricsState.lyricsDocument,
            subtitleLyrics = subtitleLyrics
        )
    }

    MusicPlayerContent(
        state = MusicPlayerUiState(
            title = displayTitle,
            artist = info.owner.name,
            coverUrl = coverUrl,
            isLoading = player == null,
            isPlaying = playback.isPlaying,
            isBuffering = playback.isBuffering,
            positionMs = playback.positionMs,
            durationMs = playback.durationMs.takeIf { it > 0L } ?: metadataDurationMs,
            lyrics = effectiveLyrics,
            lyricsError = if (effectiveLyrics != null) null else lyricsState.lyricsError,
            lyricCandidates = lyricsState.lyricCandidates.map {
                MusicLyricCandidateUi(it.title, it.artist, it.source.name)
            },
            isLyricsSearching = lyricsState.isLyricsSearching && effectiveLyrics == null,
            queue = queue,
            currentQueueIndex = currentIndex,
            playMode = playMode,
            shuffleEnabled = shuffleEnabled,
            playbackSpeed = playback.playbackSpeed
        ),
        onBack = onBack,
        onPlayPause = { player?.handleAudioModePlayPause() },
        onSeek = { positionMs ->
            player?.seekTo(positionMs)
            player?.let(PlayerVolumeController::applyPreferredVolume)
        },
        onPrevious = { viewModel.playPreviousAudioModeTrack() },
        onNext = { viewModel.playNextAudioModeTrack() },
        onQueueItemSelected = { index ->
            if (playlist.isNotEmpty()) {
                PlaylistManager.playAt(index)?.let {
                    viewModel.loadVideo(
                        bvid = it.bvid,
                        cid = it.cid,
                        autoPlay = resolveAudioModePageSwitchAutoPlay()
                    )
                }
            } else if (info.pages.size > 1) {
                info.pages.getOrNull(index)?.let { page ->
                    viewModel.loadVideo(
                        bvid = info.bvid,
                        cid = page.cid,
                        autoPlay = resolveAudioModePageSwitchAutoPlay()
                    )
                }
            } else if (index > 0) {
                successState.related.getOrNull(index - 1)?.let { rel ->
                    viewModel.loadVideo(
                        bvid = rel.bvid,
                        cid = rel.cid,
                        autoPlay = resolveAudioModePageSwitchAutoPlay()
                    )
                }
            }
        },
        onPlayModeChange = PlaylistManager::setPlayMode,
        onShuffleEnabledChange = PlaylistManager::setShuffleEnabled,
        onLyricsOffsetChange = lyricsViewModel::adjustLyricsOffset,
        onLyricsRetry = lyricsViewModel::retryLyrics,
        onLyricsSearch = lyricsViewModel::searchLyrics,
        onLyricsCandidateSelected = lyricsViewModel::selectLyricsCandidate,
        onVideoModeClick = {
            AudioNowPlayingSession.dismiss()
            onVideoModeClick(info.bvid, info.cid)
        },
        onCollectionClick = when {
            info.pages.size > 1 -> ({ showPageSelector = true })
            info.ugc_season != null -> ({ showCollectionSheet = true })
            else -> null
        },
        onSleepTimerClick = { showSleepTimerDialog = true },
        sleepTimerLabel = formatAudioModeSleepTimerButtonLabel(sleepTimerMinutes),
        audioQualityLabel = audioQualityPresentation.label,
        audioQualityOptions = successState.availableAudioQualities,
        requestedAudioQuality = successState.requestedAudioQuality,
        isHiResAudioSelected = audioQualityPresentation.showHiResBadge,
        isDolbyAudioSelected = audioQualityPresentation.showDolbyBadge,
        onAudioQualitySelected = viewModel::setAudioQuality,
        onPipClick = if (showPipButton) onEnterPip else null,
        onToggleOrientation = onToggleOrientation,
        orientationActionLabel = orientationActionLabel,
        isLiked = engagementState.isLiked,
        onLikeClick = {
            engagementViewModel.toggleLike(
                aid = info.aid,
                bvid = info.bvid,
                currentlyLiked = engagementState.isLiked
            )
        },
        onCommentsClick = { showComments = true },
        isFavorited = engagementState.isFavorited,
        onFavoriteClick = { engagementViewModel.toggleFavorite() },
        onDownloadClick = { viewModel.downloadAudio(context) },
        onShareClick = { showShare = true },
        onSpeedClick = { showSpeedMenu = true },
        speedLabel = PlaybackSpeed.formatSpeed(currentSpeed),
        isInPipMode = isInPipMode,
        liquidGlassEffectsEnabled = liquidGlassEffectsEnabled
    )

    if (showPageSelector && info.pages.size > 1) {
        AppModalBottomSheet(onDismissRequest = { showPageSelector = false }) {
            PagesSelector(
                pages = info.pages,
                currentPageIndex = info.pages.indexOfFirst { it.cid == info.cid }.coerceAtLeast(0),
                forceGridMode = true,
                onDismissRequest = { showPageSelector = false },
                onPageSelect = { index ->
                    info.pages.getOrNull(index)?.let { page ->
                        showPageSelector = false
                        viewModel.loadVideo(
                            bvid = info.bvid,
                            cid = page.cid,
                            autoPlay = resolveAudioModePageSwitchAutoPlay()
                        )
                    }
                }
            )
        }
    }

    info.ugc_season?.let { season ->
        if (showCollectionSheet) {
            CollectionSheet(
                ugcSeason = season,
                currentBvid = info.bvid,
                currentCid = info.cid,
                onDismiss = { showCollectionSheet = false },
                onEpisodeClick = { episode ->
                    showCollectionSheet = false
                    viewModel.loadVideo(
                        bvid = episode.bvid,
                        cid = episode.cid,
                        autoPlay = resolveAudioModeCollectionSwitchAutoPlay()
                    )
                }
            )
        }
    }

    LaunchedEffect(showComments, info.aid, info.owner.mid) {
        if (showComments) {
            commentViewModel.init(
                aid = info.aid,
                upMid = info.owner.mid,
                expectedReplyCount = info.stat.reply
            )
        }
    }
    if (showComments) {
        VideoCommentSheetHost(
            mainSheetVisible = true,
            onDismiss = { showComments = false },
            commentViewModel = commentViewModel,
            aid = info.aid,
            upMid = info.owner.mid,
            expectedReplyCount = info.stat.reply,
            onUserClick = {},
            onTimestampClick = { timestampMs -> player?.seekTo(timestampMs) }
        )
    }

    if (showSpeedMenu) {
        SpeedSelectionMenuDialog(
            currentSpeed = currentSpeed,
            onSpeedSelected = { speed ->
                viewModel.applyPlaybackSpeedFromUi(speed)
                showSpeedMenu = false
            },
            onDismiss = { showSpeedMenu = false }
        )
    }

    if (showShare) {
        VideoShareSheet(
            payload = buildVideoSharePayload(
                title = displayTitle,
                bvid = info.bvid,
                coverUrl = coverUrl
            ),
            onDismiss = { showShare = false }
        )
    }

    if (showSleepTimerDialog) {
        AudioModeSleepTimerDialog(
            currentMinutes = sleepTimerMinutes,
            onDismiss = { showSleepTimerDialog = false },
            onSelectPreset = { minutes ->
                viewModel.setSleepTimer(minutes)
                showSleepTimerDialog = false
            },
            onConfirmCustom = { minutes ->
                viewModel.setSleepTimer(minutes)
                showSleepTimerDialog = false
            }
        )
    }
}

@Composable
private fun rememberAudioPlaybackSnapshot(player: Player?): AudioPlaybackSnapshot {
    var snapshot by remember(player) { mutableStateOf(player.readAudioPlaybackSnapshot()) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                snapshot = player.readAudioPlaybackSnapshot()
            }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }

    LaunchedEffect(player) {
        while (isActive && player != null) {
            snapshot = player.readAudioPlaybackSnapshot()
            delay(250L)
        }
    }
    return snapshot
}

private fun Player?.readAudioPlaybackSnapshot(): AudioPlaybackSnapshot {
    val player = this ?: return AudioPlaybackSnapshot()
    return AudioPlaybackSnapshot(
        isPlaying = player.isPlaying,
        isBuffering = player.playbackState == Player.STATE_BUFFERING,
        positionMs = player.currentPosition.coerceAtLeast(0L),
        durationMs = player.duration.coerceAtLeast(0L),
        playbackSpeed = player.playbackParameters.speed
    )
}
