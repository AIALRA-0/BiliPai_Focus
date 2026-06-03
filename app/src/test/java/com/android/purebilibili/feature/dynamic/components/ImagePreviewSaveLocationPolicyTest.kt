package com.android.purebilibili.feature.dynamic.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImagePreviewSaveLocationPolicyTest {

    @Test
    fun imageSaveTreeUri_usesSafOnlyWhenTreeUriIsPresent() {
        assertFalse(shouldUseImageSaveTreeUri(null))
        assertFalse(shouldUseImageSaveTreeUri(""))
        assertFalse(shouldUseImageSaveTreeUri("   "))
        assertTrue(shouldUseImageSaveTreeUri("content://com.android.externalstorage.documents/tree/primary%3Abili"))
    }

    @Test
    fun imageSaveDestination_prefersCustomTreeUriAndFallsBackToMediaStore() {
        assertEquals(
            ImageSaveDestination.MEDIA_STORE,
            resolveImageSaveDestination(null)
        )
        assertEquals(
            ImageSaveDestination.SAF_TREE,
            resolveImageSaveDestination("content://com.android.externalstorage.documents/tree/primary%3Abili")
        )
    }

    @Test
    fun defaultImageMediaStoreRelativePath_keepsExistingAlbum() {
        assertEquals("Pictures/BiliPai", resolveDefaultImageMediaStoreRelativePath())
    }
}
