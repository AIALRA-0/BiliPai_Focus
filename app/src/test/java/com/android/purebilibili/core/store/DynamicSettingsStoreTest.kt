package com.android.purebilibili.core.store

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.android.purebilibili.core.store.home.DynamicSettingsStore
import com.android.purebilibili.core.store.home.dynamicCommentPreviewVisiblePreferencesKey
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicSettingsStoreTest {
    @Test
    fun commentPreviewDefaultsToHidden() {
        assertFalse(DynamicSettingsStore.resolveCommentPreviewVisible(mutablePreferencesOf()))
    }

    @Test
    fun unrelatedExistingPreferencesKeepCommentPreviewHidden() {
        val preferences = mutablePreferencesOf(
            booleanPreferencesKey("dynamic_image_preview_text_visible") to true,
        )

        assertFalse(DynamicSettingsStore.resolveCommentPreviewVisible(preferences))
    }

    @Test
    fun savedCommentPreviewChoiceIsRespected() {
        assertTrue(
            DynamicSettingsStore.resolveCommentPreviewVisible(
                mutablePreferencesOf(dynamicCommentPreviewVisiblePreferencesKey to true),
            ),
        )
        assertFalse(
            DynamicSettingsStore.resolveCommentPreviewVisible(
                mutablePreferencesOf(dynamicCommentPreviewVisiblePreferencesKey to false),
            ),
        )
    }
}
