package com.android.purebilibili.feature.video.viewmodel

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerViewModelContextPolicyTest {

    @Test
    fun `should bootstrap player context when local context missing but app context exists`() {
        assertTrue(
            shouldBootstrapPlayerContext(
                hasBoundContext = false,
                hasGlobalContext = true
            )
        )
    }

    @Test
    fun `should not bootstrap when already bound or global context unavailable`() {
        assertFalse(
            shouldBootstrapPlayerContext(
                hasBoundContext = true,
                hasGlobalContext = true
            )
        )
        assertFalse(
            shouldBootstrapPlayerContext(
                hasBoundContext = false,
                hasGlobalContext = false
            )
        )
    }

    @Test
    fun `favorite folder dialog should prefer explicitly requested aid`() {
        assertTrue(
            resolveFavoriteFolderDialogTargetAid(
                requestedAid = 2002L,
                currentAid = 1001L
            ) == 2002L
        )
    }

    @Test
    fun `favorite folder dialog should fall back to current aid when request missing`() {
        assertTrue(
            resolveFavoriteFolderDialogTargetAid(
                requestedAid = null,
                currentAid = 1001L
            ) == 1001L
        )
    }

    @Test
    fun `favorite ui sync should only affect current player video`() {
        assertTrue(
            shouldSyncFavoriteFolderUiState(
                targetAid = 1001L,
                currentAid = 1001L
            )
        )
        assertFalse(
            shouldSyncFavoriteFolderUiState(
                targetAid = 2002L,
                currentAid = 1001L
            )
        )
    }

    @Test
    fun `comment send should prefer explicitly requested aid`() {
        assertTrue(
            resolveCommentSendTargetAid(
                requestedAid = 2002L,
                currentAid = 1001L
            ) == 2002L
        )
    }

    @Test
    fun `comment send should fall back to current aid when request missing`() {
        assertTrue(
            resolveCommentSendTargetAid(
                requestedAid = null,
                currentAid = 1001L
            ) == 1001L
        )
    }
}
