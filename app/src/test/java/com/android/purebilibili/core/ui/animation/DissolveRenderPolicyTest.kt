package com.android.purebilibili.core.ui.animation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DissolveRenderPolicyTest {

    @Test
    fun `shouldWrapWithDissolveAnimation only when dissolving`() {
        assertTrue(shouldWrapWithDissolveAnimation(isDissolving = true))
        assertFalse(shouldWrapWithDissolveAnimation(isDissolving = false))
    }

    @Test
    fun `shouldJiggleOnDissolve stays disabled for card already dissolving`() {
        assertFalse(
            shouldJiggleOnDissolve(
                enabled = true,
                isAnyCardDissolving = true,
                dissolvingCardId = "card-b",
                cardId = "card-a",
                isCurrentCardDissolving = true
            )
        )
    }

    @Test
    fun `shouldJiggleOnDissolve keeps neighboring cards animated`() {
        assertTrue(
            shouldJiggleOnDissolve(
                enabled = true,
                isAnyCardDissolving = true,
                dissolvingCardId = "card-b",
                cardId = "card-a",
                isCurrentCardDissolving = false
            )
        )
    }
}
