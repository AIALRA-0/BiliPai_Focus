package com.android.purebilibili.feature.video.screen

import androidx.compose.runtime.Composable
import androidx.media3.common.Player
import com.android.purebilibili.feature.video.interaction.InteractiveChoicePanelUiState
import com.android.purebilibili.feature.video.player.PlaylistItem
import com.android.purebilibili.feature.video.ui.components.CoinDialog
import com.android.purebilibili.feature.video.ui.components.InteractiveChoiceOverlay
import com.android.purebilibili.feature.video.viewmodel.VideoEngagementUiState
import com.android.purebilibili.feature.video.viewmodel.VideoEngagementViewModel
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackViewModel
import dev.chrisbanes.haze.HazeState

@Composable
internal fun VideoDetailCommonOverlayAdapter(
    interactiveChoicePanel: InteractiveChoicePanelUiState,
    engagementState: VideoEngagementUiState,
    playbackViewModel: VideoPlaybackViewModel,
    engagementViewModel: VideoEngagementViewModel,
    queueVisible: Boolean,
    queueTitle: String,
    playlist: List<PlaylistItem>,
    playlistCurrentIndex: Int,
    hazeState: HazeState,
    queuePresentation: ExternalPlaylistQueueSheetPresentation,
    player: Player,
    onDismissQueue: () -> Unit,
    onVideoSelected: (Int, PlaylistItem) -> Unit,
) {
    InteractiveChoiceOverlay(
        state = interactiveChoicePanel,
        onSelectChoice = { edgeId, targetCid ->
            playbackViewModel.selectInteractiveChoice(edgeId = edgeId, cid = targetCid)
        },
        onDismiss = playbackViewModel::dismissInteractiveChoicePanel,
    )
    CoinDialog(
        visible = engagementState.coinDialogVisible,
        currentCoinCount = engagementState.coinCount,
        userBalance = engagementState.userCoinBalance,
        onDismiss = { engagementViewModel.setCoinDialogVisible(false) },
        onConfirm = engagementViewModel::doCoin,
    )
    VideoDetailFollowGroupDialog(viewModel = playbackViewModel)
    ExternalPlaylistQueueSheet(
        visible = queueVisible,
        title = queueTitle,
        playlist = playlist,
        currentIndex = playlistCurrentIndex,
        hazeState = hazeState,
        presentation = queuePresentation,
        onDismiss = onDismissQueue,
        onVideoSelected = onVideoSelected,
    )
    VideoDetailPlaybackEndedDialog(viewModel = playbackViewModel, player = player)
}
