package com.android.purebilibili.feature.story

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.android.purebilibili.feature.video.ui.pager.PortraitVideoPager
import com.android.purebilibili.feature.video.viewmodel.PlayerViewModel
import com.android.purebilibili.feature.video.viewmodel.VideoCommentViewModel

@UnstableApi
@Composable
fun StoryScreen(
    viewModel: StoryViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    commentViewModel: VideoCommentViewModel = viewModel(),
    onBack: () -> Unit,
    onVideoClick: (String, Long, String) -> Unit = { _, _, _ -> },
    onUserClick: (Long) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onRotateToLandscape: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val portraitFeed = remember(uiState.items) {
        buildStoryPortraitFeed(uiState.items)
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
                    message = "暂时没有可播放的竖屏视频",
                    onRetry = viewModel::refresh,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                PortraitVideoPager(
                    initialBvid = portraitFeed.initialInfo.bvid,
                    initialInfo = portraitFeed.initialInfo,
                    recommendations = portraitFeed.recommendations,
                    onBack = onBack,
                    onHomeClick = onBack,
                    onVideoChange = { },
                    viewModel = playerViewModel,
                    commentViewModel = commentViewModel,
                    onExitSnapshot = { bvid, _, cid ->
                        latestExitSnapshot = StoryPortraitExitSnapshot(
                            bvid = bvid,
                            cid = cid
                        )
                    },
                    onSearchClick = onSearchClick,
                    onUserClick = onUserClick,
                    onRotateToLandscape = {
                        val snapshot = latestExitSnapshot
                        if (snapshot != null) {
                            onVideoClick(snapshot.bvid, snapshot.cid, "")
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
        Text(message, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

private data class StoryPortraitExitSnapshot(
    val bvid: String,
    val cid: Long
)
