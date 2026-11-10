package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.feature.video.ui.pager.PortraitFavoriteAction
import com.android.purebilibili.feature.video.ui.pager.resolvePortraitFavoriteAction
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoFavoriteActionPolicyTest {

    @Test
    fun tapWithoutQuickSave_opensFolderPickerSoUserCanChooseOwnFolders() {
        assertEquals(
            VideoFavoriteAction.OpenFavoriteFolders,
            resolveVideoFavoriteAction(
                entryPoint = VideoFavoriteEntryPoint.FullscreenOverlay,
                isLongPress = false,
                quickSaveDefaultFolder = false,
            )
        )
        assertEquals(
            VideoFavoriteAction.OpenFavoriteFolders,
            resolveVideoFavoriteAction(
                entryPoint = VideoFavoriteEntryPoint.DetailActionRow,
                isLongPress = false,
                quickSaveDefaultFolder = false,
            )
        )
        assertEquals(
            VideoFavoriteAction.OpenFavoriteFolders,
            resolveVideoFavoriteAction(
                entryPoint = VideoFavoriteEntryPoint.BottomInputBar,
                isLongPress = false,
                quickSaveDefaultFolder = false,
            )
        )
    }

    @Test
    fun tapWithQuickSave_togglesDefaultFavoriteFolder() {
        assertEquals(
            VideoFavoriteAction.ToggleFavorite,
            resolveVideoFavoriteAction(
                entryPoint = VideoFavoriteEntryPoint.DetailActionRow,
                isLongPress = false,
                quickSaveDefaultFolder = true,
            )
        )
    }

    @Test
    fun longPress_alwaysOpensFolderPicker() {
        assertEquals(
            VideoFavoriteAction.OpenFavoriteFolders,
            resolveVideoFavoriteAction(
                entryPoint = VideoFavoriteEntryPoint.DetailActionRow,
                isLongPress = true,
                quickSaveDefaultFolder = true,
            )
        )
        assertEquals(
            VideoFavoriteAction.OpenFavoriteFolders,
            resolveVideoFavoriteAction(
                entryPoint = VideoFavoriteEntryPoint.BottomInputBar,
                isLongPress = true,
                quickSaveDefaultFolder = false,
            )
        )
    }

    @Test
    fun audioMode_alwaysOpensFolderPickerEvenWithQuickSave() {
        assertEquals(
            VideoFavoriteAction.OpenFavoriteFolders,
            resolveVideoFavoriteAction(
                entryPoint = VideoFavoriteEntryPoint.AudioMode,
                isLongPress = false,
                quickSaveDefaultFolder = true,
            )
        )
        assertEquals(
            VideoFavoriteAction.OpenFavoriteFolders,
            resolveVideoFavoriteAction(
                entryPoint = VideoFavoriteEntryPoint.AudioMode,
                isLongPress = false,
                quickSaveDefaultFolder = false,
            )
        )
    }

    @Test
    fun portraitFavoriteAction_followsQuickSaveAndLongPress() {
        assertEquals(
            PortraitFavoriteAction.OpenFavoriteFolders,
            resolvePortraitFavoriteAction(isLongPress = false, quickSaveDefaultFolder = false)
        )
        assertEquals(
            PortraitFavoriteAction.ToggleFavorite,
            resolvePortraitFavoriteAction(isLongPress = false, quickSaveDefaultFolder = true)
        )
        assertEquals(
            PortraitFavoriteAction.OpenFavoriteFolders,
            resolvePortraitFavoriteAction(isLongPress = true, quickSaveDefaultFolder = true)
        )
    }
}
