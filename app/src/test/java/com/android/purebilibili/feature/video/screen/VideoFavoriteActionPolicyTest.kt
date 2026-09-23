package com.android.purebilibili.feature.video.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoFavoriteActionPolicyTest {

    @Test
    fun favoriteEntries_openFolderPickerSoUserCanChooseOwnFolders() {
        assertEquals(
            VideoFavoriteAction.OpenFavoriteFolders,
            resolveVideoFavoriteAction(VideoFavoriteEntryPoint.FullscreenOverlay)
        )
        assertEquals(
            VideoFavoriteAction.OpenFavoriteFolders,
            resolveVideoFavoriteAction(VideoFavoriteEntryPoint.DetailActionRow)
        )
        assertEquals(
            VideoFavoriteAction.OpenFavoriteFolders,
            resolveVideoFavoriteAction(VideoFavoriteEntryPoint.BottomInputBar)
        )
    }
}
