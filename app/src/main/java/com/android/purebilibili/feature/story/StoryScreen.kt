package com.android.purebilibili.feature.story

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.android.purebilibili.feature.video.ui.pager.PortraitVideoPager
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackViewModel
import com.android.purebilibili.feature.video.viewmodel.VideoEngagementEvent
import com.android.purebilibili.feature.video.viewmodel.VideoEngagementViewModel

@UnstableApi
@Composable
fun StoryScreen(
    seedBvid: String = "",
    seedCid: Long = 0L,
    seedCover: String = "",
    seedTitle: String = "",
    sourceRoute: String? = null,
    transitionEnabled: Boolean = true,
    viewModel: StoryViewModel = viewModel(),
    playerViewModel: VideoPlaybackViewModel = viewModel(),
    engagementViewModel: VideoEngagementViewModel = viewModel(),
    isActive: Boolean = true,
    onBack: () -> Unit,
    onVideoClick: (String, Long, String) -> Unit = { _, _, _ -> },
    onUserClick: (Long) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onRotateToLandscape: () -> Unit = {}
) {
    @Suppress("UNUSED_PARAMETER")
    val unusedSourceRoute = sourceRoute
    @Suppress("UNUSED_PARAMETER")
    val unusedTransitionEnabled = transitionEnabled

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onlyVerticalRecommendations by com.android.purebilibili.core.store.SettingsManager
        .getPortraitOnlyVerticalRecommendations(context)
        .collectAsStateWithLifecycle(initialValue = false)
    LaunchedEffect(context) {
        engagementViewModel.initWithContext(context)
    }
    LaunchedEffect(engagementViewModel) {
        engagementViewModel.events.collect { event ->
            when (event) {
                is VideoEngagementEvent.Message -> playerViewModel.toast(event.text)
                is VideoEngagementEvent.OpenFollowGroups ->
                    playerViewModel.showFollowGroupDialogForUser(event.mid)
                is VideoEngagementEvent.LoadVideo ->
                    playerViewModel.loadVideo(event.bvid, autoPlay = true)
                VideoEngagementEvent.InvalidateFavoriteFolders ->
                    playerViewModel.invalidateFavoriteFolderCache()
            }
        }
    }
    val seed = remember(seedBvid, seedCid, seedCover, seedTitle) {
        if (seedBvid.isNotBlank()) {
            StoryFeedSeed(
                bvid = seedBvid,
                cid = seedCid,
                cover = seedCover,
                title = seedTitle
            )
        } else {
            null
        }
    }
    val portraitFeed = remember(uiState.items, seed) {
        buildStoryPortraitFeed(uiState.items, seed = seed)
    }
    var latestExitSnapshot by remember { mutableStateOf<StoryPortraitExitSnapshot?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            uiState.isLoading && uiState.items.isEmpty() -> {
                com.android.purebilibili.core.ui.CutePersonLoadingIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }

            uiState.error != null && uiState.items.isEmpty() -> {
                StoryErrorState(
                    message = uiState.error ?: "加载失败",
                    onRetry = viewModel::refresh,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            portraitFeed == null -> {
                StoryErrorState(
                    message = "暂时没有可播放的推荐视频",
                    onRetry = viewModel::refresh,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                PortraitVideoPager(
                    initialBvid = portraitFeed.initialInfo.bvid,
                    initialInfo = portraitFeed.initialInfo,
                    recommendations = portraitFeed.recommendations,
                    onlyVerticalRecommendations = onlyVerticalRecommendations,
                    isActive = isActive,
                    onBack = onBack,
                    onHomeClick = onBack,
                    onVideoChange = { bvid ->
                        val index = resolveStoryPortraitIndexForBvid(
                            bvid = bvid,
                            items = uiState.items,
                            seedBvid = seed?.bvid.orEmpty()
                        )
                        if (index >= 0) {
                            viewModel.updateCurrentIndex(index)
                        }
                    },
                    viewModel = playerViewModel,
                    engagementViewModel = engagementViewModel,
                    onExitSnapshot = { bvid, _, cid, coverUrl ->
                        latestExitSnapshot = StoryPortraitExitSnapshot(
                            bvid = bvid,
                            cid = cid,
                            coverUrl = coverUrl,
                        )
                    },
                    onSearchClick = onSearchClick,
                    onUserClick = onUserClick,
                    onRotateToLandscape = {
                        val snapshot = latestExitSnapshot
                        if (snapshot != null) {
                            // 带上当前页封面，避免横屏详情首帧闪成 Story 入口种子封面
                            onVideoClick(snapshot.bvid, snapshot.cid, snapshot.coverUrl)
                        } else {
                            onRotateToLandscape()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StoryErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppText(message, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        AppButton(onClick = onRetry) {
            AppText("重试")
        }
    }
}

private data class StoryPortraitExitSnapshot(
    val bvid: String,
    val cid: Long,
    val coverUrl: String = "",
)
