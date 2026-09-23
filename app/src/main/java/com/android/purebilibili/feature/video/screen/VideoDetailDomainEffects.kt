package com.android.purebilibili.feature.video.screen

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.android.purebilibili.feature.video.viewmodel.FavoriteFolderSaveEvent
import com.android.purebilibili.feature.video.viewmodel.VideoComposerViewModel
import com.android.purebilibili.feature.video.viewmodel.VideoEngagementEvent
import com.android.purebilibili.feature.video.viewmodel.VideoEngagementViewModel
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackUiState
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackViewModel
import com.android.purebilibili.feature.video.viewmodel.VideoSubjectSnapshot
import com.android.purebilibili.feature.video.viewmodel.VideoSupplementViewModel
import com.android.purebilibili.feature.video.viewmodel.toEngagementSeed
import com.android.purebilibili.feature.video.viewmodel.toSupplementSeed

/** Keeps cross-domain ViewModel wiring out of the screen renderer. */
@Composable
internal fun VideoDetailDomainEffects(
    context: Context,
    isVisible: Boolean,
    uiState: VideoPlaybackUiState,
    subjectSnapshot: VideoSubjectSnapshot?,
    favoriteFolderSaveEvent: FavoriteFolderSaveEvent?,
    playbackViewModel: VideoPlaybackViewModel,
    engagementViewModel: VideoEngagementViewModel,
    composerViewModel: VideoComposerViewModel,
    supplementViewModel: VideoSupplementViewModel,
) {
    LaunchedEffect(context) {
        engagementViewModel.initWithContext(context)
    }
    LaunchedEffect(engagementViewModel) {
        engagementViewModel.events.collect { event ->
            when (event) {
                is VideoEngagementEvent.Message -> playbackViewModel.toast(event.text)
                is VideoEngagementEvent.OpenFollowGroups ->
                    playbackViewModel.showFollowGroupDialogForUser(event.mid)
                is VideoEngagementEvent.LoadVideo ->
                    playbackViewModel.loadVideo(event.bvid, autoPlay = true)
                VideoEngagementEvent.InvalidateFavoriteFolders ->
                    playbackViewModel.invalidateFavoriteFolderCache()
            }
        }
    }

    val engagementSeed = (uiState as? VideoPlaybackUiState.Success)?.toEngagementSeed()
    val supplementSeed = (uiState as? VideoPlaybackUiState.Success)?.toSupplementSeed()
    LaunchedEffect(subjectSnapshot, engagementSeed) {
        val subject = subjectSnapshot ?: return@LaunchedEffect
        val seed = engagementSeed ?: return@LaunchedEffect
        engagementViewModel.bindSubject(subject, seed)
        composerViewModel.bindSubject(subject)
    }
    LaunchedEffect(subjectSnapshot, supplementSeed) {
        val subject = subjectSnapshot ?: return@LaunchedEffect
        val seed = supplementSeed ?: return@LaunchedEffect
        supplementViewModel.bindSubject(subject, seed)
    }
    LaunchedEffect(favoriteFolderSaveEvent?.version) {
        favoriteFolderSaveEvent?.let { event ->
            if (subjectSnapshot?.aid == event.aid) {
                engagementViewModel.applyFavoriteFolderResult(event.isFavorited)
            }
        }
    }
    LaunchedEffect(isVisible) {
        supplementViewModel.setVisible(isVisible)
    }
}
