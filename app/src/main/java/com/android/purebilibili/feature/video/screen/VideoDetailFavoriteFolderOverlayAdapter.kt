package com.android.purebilibili.feature.video.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.feature.video.ui.components.FavoriteFolderSheet
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackViewModel

@Composable
internal fun VideoDetailFavoriteFolderOverlayAdapter(
    visible: Boolean,
    viewModel: VideoPlaybackViewModel,
) {
    if (!visible) return

    val folders by viewModel.favoriteFolders.collectAsStateWithLifecycle()
    val isLoading by viewModel.isFavoriteFoldersLoading.collectAsStateWithLifecycle()
    val selectedFolderIds by viewModel.favoriteSelectedFolderIds.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSavingFavoriteFolders.collectAsStateWithLifecycle()

    FavoriteFolderSheet(
        folders = folders,
        isLoading = isLoading,
        selectedFolderIds = selectedFolderIds,
        isSaving = isSaving,
        onFolderToggle = viewModel::toggleFavoriteFolderSelection,
        onSaveClick = viewModel::saveFavoriteFolderSelection,
        onDismissRequest = viewModel::dismissFavoriteFolderDialog,
        onCreateFolder = viewModel::createFavoriteFolder,
    )
}
