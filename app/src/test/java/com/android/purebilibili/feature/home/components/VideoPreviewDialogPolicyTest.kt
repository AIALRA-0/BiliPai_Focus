package com.android.purebilibili.feature.home.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoPreviewDialogPolicyTest {

    @Test
    fun `shouldEnableSaveCoverAction should return false for blank cover url`() {
        assertFalse(shouldEnableSaveCoverAction(""))
        assertFalse(shouldEnableSaveCoverAction("   "))
    }

    @Test
    fun `shouldEnableSaveCoverAction should return true for non blank cover url`() {
        assertTrue(shouldEnableSaveCoverAction("//i0.hdslb.com/example.jpg"))
    }

    @Test
    fun `shouldShowBlockCreatorAction only returns true for valid mids`() {
        assertFalse(shouldShowBlockCreatorAction(0L))
        assertFalse(shouldShowBlockCreatorAction(-1L))
        assertTrue(shouldShowBlockCreatorAction(42L))
    }
}
