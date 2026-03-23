package com.android.purebilibili.feature.dynamic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.imageLoader
import com.android.purebilibili.R
import com.android.purebilibili.core.util.responsiveContentWidth
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.repository.DynamicRepository
import com.android.purebilibili.feature.dynamic.components.DynamicCardV2
import com.android.purebilibili.feature.dynamic.components.DynamicCommentOverlayHost
import com.android.purebilibili.feature.dynamic.components.RepostDialog

private sealed interface DynamicDetailUiState {
    data object Loading : DynamicDetailUiState
    data class Success(val item: DynamicItem) : DynamicDetailUiState
    data class Error(val message: String) : DynamicDetailUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicDetailScreen(
    dynamicId: String,
    onBack: () -> Unit,
    onVideoClick: (String) -> Unit,
    onUserClick: (Long) -> Unit,
    onLiveClick: (roomId: Long, title: String, uname: String) -> Unit = { _, _, _ -> }
) {
    val interactionViewModel: DynamicViewModel = viewModel()
    var retryToken by rememberSaveable { mutableIntStateOf(0) }
    val screenTitle = stringResource(R.string.dynamic_detail_title)
    val backLabel = stringResource(R.string.common_back)
    val retryLabel = stringResource(R.string.common_retry)
    val loadFailedMessage = stringResource(R.string.dynamic_detail_load_failed)
    val uiState by produceState<DynamicDetailUiState>(
        initialValue = DynamicDetailUiState.Loading,
        key1 = dynamicId,
        key2 = retryToken
    ) {
        value = DynamicDetailUiState.Loading
        value = DynamicRepository.getDynamicDetail(dynamicId).fold(
            onSuccess = { item -> DynamicDetailUiState.Success(item) },
            onFailure = { error ->
                DynamicDetailUiState.Error(error.message ?: loadFailedMessage)
            }
        )
    }

    val context = LocalContext.current
    val gifImageLoader = context.imageLoader
    val likedDynamics by interactionViewModel.likedDynamics.collectAsState()
    var showRepostDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(rememberAppBackIcon(), contentDescription = backLabel)
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            DynamicDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    com.android.purebilibili.core.ui.CutePersonLoadingIndicator()
                }
            }

            is DynamicDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { retryToken++ }) {
                            Text(retryLabel)
                        }
                    }
                }
            }

            is DynamicDetailUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .responsiveContentWidth(),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    item {
                        DynamicCardV2(
                            item = state.item,
                            onVideoClick = onVideoClick,
                            onUserClick = onUserClick,
                            onLiveClick = onLiveClick,
                            gifImageLoader = gifImageLoader,
                            onCommentClick = { interactionViewModel.openCommentSheet(state.item) },
                            onRepostClick = { showRepostDialog = it },
                            onLikeClick = { targetDynamicId ->
                                interactionViewModel.likeDynamic(targetDynamicId) { _, msg ->
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            isLiked = likedDynamics.contains(state.item.id_str)
                        )
                    }
                }

                DynamicCommentOverlayHost(
                    viewModel = interactionViewModel,
                    primaryItems = listOf(state.item),
                    toastContext = context
                )

                showRepostDialog?.let { repostDynamicId ->
                    RepostDialog(
                        onDismiss = { showRepostDialog = null },
                        onRepost = { content ->
                            interactionViewModel.repostDynamic(repostDynamicId, content) { success, msg ->
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                if (success) showRepostDialog = null
                            }
                        }
                    )
                }
            }
        }
    }
}
