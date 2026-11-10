package com.android.purebilibili.feature.video.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.data.model.VideoLoadError
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackUiState

@Composable
internal fun VideoDetailPlaybackErrorNotice(
    state: VideoPlaybackUiState.Error,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppText(
                text = state.error.toUserMessage(),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            val canRetry = state.canRetry || state.error is VideoLoadError.RateLimited ||
                state.error is VideoLoadError.GlobalCooldown || state.error is VideoLoadError.PlayUrlEmpty
            if (canRetry) {
                AppButton(onClick = onRetry) {
                    AppText(
                        text = when (state.error) {
                            is VideoLoadError.RateLimited -> "强制重试"
                            is VideoLoadError.GlobalCooldown -> "清除冷却并重试"
                            else -> "重试"
                        }
                    )
                }
            }
        }
    }
}
